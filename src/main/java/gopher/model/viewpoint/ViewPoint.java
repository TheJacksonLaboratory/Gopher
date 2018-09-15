package gopher.model.viewpoint;

import com.google.common.collect.ImmutableList;
import gopher.model.Model;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.log4j.Logger;
import gopher.model.Default;
import gopher.model.RestrictionEnzyme;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A region, usually at the transcription start site (TSS) of a gene, that will be enriched in a Capture-C experiment.
 * However, the region does not necessarily need to be at can be at TSS, it can be anywhere in the genome.
 * <p>
 * Essentially, a viewpoint consists of start and end coordinates, and a map for restriction enzyme cutting sites
 * within the viewpoint, which play an important role for the design of viewpoints according to lab protocol of
 * capture Hi-C.</p>
 * <p>
 * This class provides a set of utility functions that can be used for primarily for editing of the coordinates,
 * which can be either set manually or automatically using different (so far two) approaches.
 * The last editing step will be tracked.</p>
 * @author Peter N Robinson
 * @author Peter Hansen
 * @version 0.2.3 (2018-05-11)
 */
public class ViewPoint implements Serializable {
    private static final Logger logger = Logger.getLogger(ViewPoint.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 4L;
    /** The possible approaches used to generate this Viewpoint. */
    public enum Approach {SIMPLE, EXTENDED}
    /* The approach used to generate this viewpoint */
    private Approach approach;
    /** Size of the "borders" at the edges of a digest that are especially important because we sequence there. */
    private final int marginSize;
    /** Maximum allowable repeat content for a digest to be included. A digest will be deselected
     * if one of the margins has a higher repeat content.*/
    private final double maximumRepeatContent;
    /** "Home" of the viewpoint, usually a chromosome */
    private final String chromosomeID;
    /** Accession number of this gene, e.g., NM_0001234 .*/
    private final String accession;
    /** Name of the target of the viewpoint (often a gene).*/
    private final String targetName;
    /** central genomic coordinate of the viewpoint, usually a transcription start site. One-based fully closed numbering */
    private final int genomicPos;
    /** refers to the  the range around 'genomicPos' in which VPV searches initially for cutting positions (CuttingPositionMap).
     * Note that this is defined with respect to the strand -- it is "reversed" for genes on the - strand. */
    private int upstreamNucleotideLength;
    /** refers to the  the range around 'genomicPos' in which VPV searches initially for cutting positions (CuttingPositionMap).*/
    private int downstreamNucleotideLength;
    /** start position of the viewpoint */
    private int startPos;
    /** end position of the viewpoint */
    private int endPos;
    /** Minimum allowable size of a restriction digest-this will usually be determined by the size of the probes
     * that are used for enrichment (e.g., 130 bp. */
    private final int minFragSize;
    /** Maximum allowable GC content */
    private final double maxGcContent;
    /** Minimum allowable GC content */
    private final double minGcContent;
    /** Is the gene on the forward (positive) strand). */
    private final boolean isPositiveStrand;
    /** Data structure for storing cutting site position relative to 'genomicPos' */
    private SegmentFactory segmentFactory;
    /** List of active and inactive restriction {@link gopher.model.viewpoint.Segment} objects that are contained within the viewpoint. */
    private List<Segment> restrictionSegmentList;
    /** List of restriction enzymes chosen by the User (package scope visibility). */
    static List<RestrictionEnzyme> chosenEnzymes=null;
    /** The "number" of the promoter for the gene in question. */
    private int promoterNumber;
    /** Total number of promoters associated with this gene. */
    private int totalPromoters;
    /** This is a reference to the segment that overlaps the TSS */
    private Segment centerSegment=null;

    private final Model model;

    void setPromoterNumber(int n, int total) { promoterNumber=n; totalPromoters=total;}

    public int getPromoterNumber(){return promoterNumber;}
    public int getTotalPromoterCount(){return totalPromoters;}

    public int getDownstreamNucleotideLength() {
        return downstreamNucleotideLength;
    }

    public int getUpstreamNucleotideLength() {
        return upstreamNucleotideLength;
    }

    static void setChosenEnzymes(List<RestrictionEnzyme> lst) { chosenEnzymes=lst;}
    /** Overall score of this Viewpoint.*/
    private double score;


    public String getAccession() {
        return accession;
    }

    public String getManuallyRevised() {
        if(this.wasModified()) {
            return "\u2714"; // unicode character for a checkmark
        } else {
          return "";
        }
    }

    /**
     * Gets a list of all active (chosen) {@link Segment} objects.
     * @return a list of Segments of a viewpoint that are active and will be displayed on the UCSC Browser. */
    public List<Segment> getActiveSegments() {
        if (restrictionSegmentList==null) {
            logger.error(String.format("Error-- null list of restriction segments for %s",getTargetName()));
            return new ArrayList<>();/* return empty list.*/
        }
        //return a List of all selected segments
        return this.restrictionSegmentList.stream().filter(s -> (s!=null && s.isSelected())).collect(Collectors.toList());
    }

    /**
     *
     */
    public void refreshStartAndEndPos() {
        List<Segment> segments = this.restrictionSegmentList.stream().filter(Segment::isSelected).collect(Collectors.toList());
        // if the user deselects all segments, then none of the segments is empty. In this case, we just
        // leave the start and end position as they were
        if (segments.isEmpty()) return;
        int min = segments.get(0).getStartPos();
        int max = segments.get(0).getEndPos();
        for(int i=1; i< segments.size(); i++) {
            if(segments.get(i).getStartPos() < min) {
                min=segments.get(i).getStartPos();
            }
            if(max < segments.get(i).getEndPos()) {
                max=segments.get(i).getEndPos();
            }
        }
        this.setStartPos(min);
        this.setEndPos(max);
    }

    /** @return List of all segments (selected or not). */
   public List<Segment> getAllSegments() {
        return restrictionSegmentList;
   }

    /** @return a formated String representing the length of the ViewPoint in kb, e.g., 10;203 kb. */
    public String getTotalAndActiveLengthAsString() {
       double lenInKb=(double)getTotalLengthOfActiveSegments()/1000; // kilobases
        return String.format("%.2f kb (all selected fragments: %.2f kb)", (double) getTotalLengthOfViewpoint()/1000,lenInKb);
    }

    /**
     * This function should only be used for the extended approach. It changes the start and end position
     * of the ViewPoint so that more or less Segments can be selected.
     * @param zoomfactor amount of zoom to perform
     */
//    public void zoom(double zoomfactor) {
//        logger.trace(String.format("Zooming...Current upstreamNucleotide length %d; downstream %d; factor=%f", upstreamNucleotideLength,downstreamNucleotideLength,zoomfactor));
//        this.upstreamNucleotideLength =(int)(this.upstreamNucleotideLength *zoomfactor);
//        this.downstreamNucleotideLength =(int)(this.downstreamNucleotideLength *zoomfactor);
//        logger.trace(String.format("After zoom...upstreamNucleotide length %d; downstream %d", upstreamNucleotideLength,downstreamNucleotideLength));
//        // TODO do we need to worry about going over end of chromosome?
//        // or start before zero?
//        setStartPos(genomicPos - upstreamNucleotideLength);
//        setEndPos(genomicPos + downstreamNucleotideLength);
//        // note that if the approach is SIMPLE, then we do not recalculate the selected viewpoints.
//        // if the approach is EXTENDED, then we take a valid fragments in the zoomed region
//        if (this.approach.equals(Approach.EXTENDED)) {
//            setFragmentsForExtendedApproach(this.startPos, this.endPos, false);
//        }
//
//    }


    /**
     * This constructor is intended to be used by the builder.
     *
     * @param builder Builder class aimed to make constructing a ViewPoint object unambiguous.
     */
    private ViewPoint(Builder builder){
        this.chromosomeID =builder.chromosomeID;
        this.genomicPos=builder.genomicPos;
        this.targetName=builder.targetName;
        this.isPositiveStrand=builder.isPositiveStrand;
        if (isPositiveStrand) {
            this.upstreamNucleotideLength = builder.upstreamNtLength;
            this.downstreamNucleotideLength = builder.downstreamNtLength;
        } else { // SWITCH for neg
            this.upstreamNucleotideLength = builder.downstreamNtLength;
            this.downstreamNucleotideLength = builder.upstreamNtLength;
        }
        setStartPos(genomicPos - upstreamNucleotideLength);
        setEndPos(genomicPos + downstreamNucleotideLength);
        this.minGcContent=builder.minGcContent;
        this.maxGcContent=builder.maxGcContent;
        this.minFragSize=builder.minFragSize;
        this.marginSize= builder.marginSize;
        this.accession=builder.accessionNr;
        this.maximumRepeatContent=builder.maximumRepeatContent;
        this.model=builder.model;
        init(builder.fastaReader,builder.c2alignmap, builder.chromosomelen);
    }


    private void init(IndexedFastaSequenceFile fastaReader, AlignabilityMap c2align, int chromosomeLength) {
        this.restrictionSegmentList=new ArrayList<>();
        boolean changed;
        /* Create segmentFactory */
        if(model.getApproach().equals(Model.Approach.SIMPLE)) {
            this.upstreamNucleotideLength=model.getEstAvgRestFragLen().intValue();
            this.downstreamNucleotideLength=model.getEstAvgRestFragLen().intValue();
            /*
             For the simple approach, iteratively increase range for initial restriction fragments as long as
             genomicPos occurs on first or last fragment of the list in order to make sure that adjacent fragments
             can later be added.
             */
            int iteration = 0;
            int increment = 1000;
            do {
                logger.trace("segmentFactory iteration = " + iteration);
                changed=false;
                segmentFactory = new SegmentFactory(this.chromosomeID,
                        this.genomicPos,
                        fastaReader,
                        chromosomeLength,
                        this.upstreamNucleotideLength,
                        this.downstreamNucleotideLength,
                        ViewPoint.chosenEnzymes);
                iteration++;

                logger.trace("Number of frags="+restrictionSegmentList.size());

                if(segmentFactory.getNumOfCutsUpstreamPos(genomicPos) < 2
                        && hasMoreSequenceUpstream() ) {
                    this.upstreamNucleotideLength = this.upstreamNucleotideLength + increment;
                    this.restrictionSegmentList.clear();
                    changed=true;
                }
                if(segmentFactory.getNumOfCutsDownstreamPos(genomicPos) < 2
                        && hasMoreSequenceDownstream(chromosomeLength)) {
                    this.downstreamNucleotideLength = this.downstreamNucleotideLength + increment;
                    this.restrictionSegmentList.clear();
                    changed=true;
                }

                if((0 < segmentFactory.getNumOfCutsUpstreamPos(genomicPos)) && (0 < segmentFactory.getNumOfCutsDownstreamPos(genomicPos))) {
                    logger.trace("0<x and 0<y");
                    initRestrictionFragments(fastaReader, c2align);
                }
                increment *= 2;
            }
            while (changed && (segmentFactory.getNumOfCutsUpstreamPos(genomicPos) < 2 ||
                    segmentFactory.getNumOfCutsDownstreamPos(genomicPos) < 2) &&
                    !segmentFactory.maxDistUpOutOfChromosome() &&
                    !segmentFactory.maxDistDownOutOfChromosome());
        } else {
            /*
            For the extended approach, we want to have all digests that overlap the range specified by the user
            [TSS-5000;TSS+1500] plus the two adjacen digests in up- and downstream direction.
            We start with the range specified by the user and then iteratively increase this range as long as the
            TSS-digest is not included and there are less than two cutting sites in up- and downstream direction
            of the specified range.
             */
            int upstreamLength = this.upstreamNucleotideLength;
            int downstreamLength = this.downstreamNucleotideLength;
            int iteration = 0;
            int increment = model.getEstAvgRestFragLen().intValue()*2;
            do {
                logger.trace("segmentFactory iteration = " + iteration + " (" + this.targetName + ")");
                changed=false;
                segmentFactory = new SegmentFactory(this.chromosomeID,
                        this.genomicPos,
                        fastaReader,
                        chromosomeLength,
                        upstreamLength,
                        downstreamLength,
                        ViewPoint.chosenEnzymes);
                logger.trace("Done with Segment factory");
                iteration++;

                logger.trace("Number of frags = " + restrictionSegmentList.size());

                if(segmentFactory.getNumOfCutsUpstreamPos(genomicPos-upstreamNucleotideLength) < 2
                        && !(genomicPos-upstreamLength < 0) ) {
                    upstreamLength = upstreamLength + increment;
                    this.restrictionSegmentList.clear();
                    changed=true;
                }
                if(segmentFactory.getNumOfCutsDownstreamPos(genomicPos+downstreamNucleotideLength) < 2
                        && !(chromosomeLength < genomicPos + downstreamLength)) {
                    downstreamLength = downstreamLength + increment;
                    this.restrictionSegmentList.clear();
                    changed=true;
                }

                if((0 < segmentFactory.getNumOfCutsUpstreamPos(genomicPos)) && (0 < segmentFactory.getNumOfCutsDownstreamPos(genomicPos))) {
                    logger.trace("0<x and 0<y");
                    initRestrictionFragments(fastaReader, c2align);
                }
            }
            while (changed && (segmentFactory.getNumOfCutsUpstreamPos(genomicPos-upstreamNucleotideLength) < 2 ||
                    segmentFactory.getNumOfCutsDownstreamPos(genomicPos+downstreamNucleotideLength) < 2) &&
                    !(genomicPos-upstreamLength < 0) &&//!segmentFactory.maxDistUpOutOfChromosome() &&
                    !(chromosomeLength < genomicPos + downstreamLength));//!segmentFactory.maxDistDownOutOfChromosome());
        }
        /* The iterative approach can result in more than one adjacent digest in up- or downstream direction.
           Such digests need to be removed from the list.
         */
        int LEN = restrictionSegmentList.size();
        logger.trace("restrictionSegmentList.size(): " + restrictionSegmentList.size());
        int firstSelectedIndex = IntStream.range(0,LEN)
                .filter(i->restrictionSegmentList.get(i).overlapsRange(genomicPos-this.upstreamNucleotideLength,genomicPos+this.downstreamNucleotideLength))//isSelected())
                .findFirst().orElse(0);

        // The map reverses the order.
        int lastSelectedIndex = IntStream.range(0,LEN-1).
                map(i -> LEN - i - 1).
                filter(i->restrictionSegmentList.get(i).overlapsRange(genomicPos-this.upstreamNucleotideLength,genomicPos+this.downstreamNucleotideLength))//.isSelected())
                .findFirst().orElse(0);


        if (firstSelectedIndex+lastSelectedIndex==0) {
            logger.error("Skipping trimming Segment List because no segments are selected for " + getTargetName());
            logger.error("firstSelectedIndex:" + firstSelectedIndex);
            logger.error("lastSelectedIndex:" + lastSelectedIndex);
        } else {
            int i = Math.max(0, firstSelectedIndex - 1);
            int j = Math.min(LEN, lastSelectedIndex + 2);// +2 because we want one more and range is (inclusive,exclusive)
            restrictionSegmentList  = IntStream.range(i, j).boxed().map(k -> restrictionSegmentList.get(k)).collect(Collectors.toList());
        }
    }

    /** @return true if we are at or over the 3' end of the chromosome. */
    private boolean hasMoreSequenceDownstream(int chromlen) {
        return (this.genomicPos + this.downstreamNucleotideLength < chromlen);
    }

    private boolean hasMoreSequenceUpstream() {
        return (this.genomicPos - this.upstreamNucleotideLength > 0);
    }


    /**
     * This function uses the information about cutting position sites from the {@link #segmentFactory} to build
     * a list of {@link Segment} objects in {@link #restrictionSegmentList}.
     */
    private void initRestrictionFragments(IndexedFastaSequenceFile fastaReader, AlignabilityMap c2align) {
        this.restrictionSegmentList = new ArrayList<>();
        // if genomicPos occurs on the first digest of the chromosome, add pseudo digest with length 0.
        if(genomicPos < segmentFactory.getAllCuts().get(0)) {
            Segment restFrag = new Segment.Builder(chromosomeID,
                    0,
                    0).
                    fastaReader(fastaReader).marginSize(marginSize).build();
        }
        for (int j = 0; j < segmentFactory.getAllCuts().size() - 1; j++) {
            Segment restFrag = new Segment.Builder(chromosomeID,
                    segmentFactory.getUpstreamCut(j),
                    segmentFactory.getDownstreamCut(j) - 1).
                    fastaReader(fastaReader).marginSize(marginSize).build();
            double maxMeanAlignabilityScore = 1.0 * model.getMaxMeanKmerAlignability();
            restFrag.setUsableBaits(model,c2align,maxMeanAlignabilityScore);
            restrictionSegmentList.add(restFrag);
        }
/*
        logger.trace("Done init Restriction Frags");
        for (Segment er : restrictionSegmentList){
            logger.trace("\tSegment: " + er.detailedReport());
        }
*/
    }

    /** @return a 2-tuple with the number of baits: <up,down>. */
    private List<Integer> getNumberOfBaitsUpDown() {
        ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<>();
        Integer up=0;
        Integer down=0;
        for (Segment seg : this.getActiveSegments()) {
            up +=seg.getBaitNumUp();
            down += seg.getBaitNumDown();
        }
        builder.add(up);
        builder.add(down);
        return builder.build();
    }

    public String getNumberOfBaitsUpDownAsString() {
        List<Integer> updown = getNumberOfBaitsUpDown();
        return updown.stream().map(String::valueOf).collect(Collectors.joining("/"));
    }



    /** @return The reference ID of the reference sequence (usually, a chromosome) .*/
    public final String getReferenceID() {
        return chromosomeID;
    }

    /** @return the middle (anchor) position of this ViewPoint. */
    public final Integer getGenomicPos() {
        return genomicPos;
    }
    /** @return a string like chr4:29,232,796 */
    public String getGenomicLocationString() { return String.format("%s:%s", chromosomeID, NumberFormat.getNumberInstance(Locale.US).format(genomicPos));}

    /** @return overall score of this ViewPoint */
    public final double getScore() {
        if (hasNoActiveSegment()) return 0.0;
        return this.score;
    }

    /**@return the viewpoint score formated as a percent string. */
    public String getScoreAsPercentString() {
        if (hasNoActiveSegment()) return "0.0%";
        return String.format("%.1f%%",100*score);
    }


    private void setStartPos(int startPos) {
        this.startPos = startPos>0?startPos:0;
    }

//    private void setMinimumAllowableStartPos(int pos) { this.minimumAllowableStartPosition = pos; }
//    private void setMaximumAllowableEndPos(int pos) { this.maximumAllowableEndPosition = pos; }

    public final Integer getStartPos() {
        return startPos;
    }

    public final Integer getEndPos() {
        return endPos;
    }

    public final int getDisplayStart() {
        return Math.min(genomicPos-upstreamNucleotideLength,startPos);
    }
    public final int getDisplayEnd() {
        return Math.max(genomicPos-downstreamNucleotideLength,endPos);
    }



    private void setEndPos(Integer endPos) {
        this.endPos = endPos;
    }

    public final Approach getDerivationApproach() {
        return approach;
    }

    private void setDerivationApproach(Approach derivationApproach) {
        this.approach = derivationApproach;
    }

    /** @return true if this viewpoint has at least one active (selected) probe and it is resolved. */
    public final boolean hasValidDigest() {
        return getNumOfSelectedFrags()>0;
    }
    /** @return Number of Segments in this ViewPoint that are active (selected). */
    public final int getNumOfSelectedFrags() {
        return (int) this.restrictionSegmentList.stream().filter(Segment::isSelected).count();
    }

    public String getTargetName() { return this.targetName; }

    public boolean isTSSfragmentChosen() {
        return this.centerSegment != null && centerSegment.isSelected();
    }


    public String toString() {
        return String.format("%s  [%s:%d-%d]",getTargetName(),getReferenceID(),getStartPos(),getEndPos());
    }

    public String getStrandAsString() {
        if (this.isPositiveStrand) return "Strand: +";
        else return "Strand: -";
    }


    public boolean isPositiveStrand() { return isPositiveStrand; }


    /**
     * Select all valid fragments located between lowerLimit and upperLimit
     * If this function is called when the viewpoint is being created for the first time, then
     * updateOriginallySelected should be set to true. If we are modifying the viewpoint (e.g., zooming),
     * then updateOriginallySelected should be false. This allows us to keep track of whether
     * the current ViewPoint has been modified by the user.
     * @param lowerLimit 3' limit for selecting digests
     * @param upperLimit 5' limit for selecting digests
     * @param updateOriginallySelected if true,alter the originallySelected field in {@link Segment}
     */
    private void setFragmentsForExtendedApproach(int lowerLimit, int upperLimit, boolean updateOriginallySelected) {
//        int c = (int)restrictionSegmentList.stream().filter(Segment::isSelected).count();

        for (Segment segment:restrictionSegmentList) {

            segment.setSelected(true,updateOriginallySelected); // initial segment selection is done here and nowhere else

            // do not select fragments that are too small
            if (segment.length() < this.minFragSize) { // minFragSize should be at least one bait size
                segment.setSelected(false,updateOriginallySelected);
            }

            // do not select segments that are entirely outside the allowed range
            if((segment.getEndPos() < lowerLimit) || (upperLimit < segment.getStartPos()) )
            {
                segment.setSelected(false,updateOriginallySelected);
            }

            // do not select segments that have less than 2 times bmin baits
            if(segment.isUnselectable()) {
                segment.setSelected(false,updateOriginallySelected);
            }

            // if allow single margin is false, do not select unbalanced digests
            if(!model.getAllowUnbalancedMargins() && segment.isUnbalanced()) {
                segment.setSelected(false,updateOriginallySelected);
            }

        }
    }




    /**
     * This function can be used to reshape the viewpoint according to rules that were developed in consultation with bench scientists.
     * In this approach, the viewpoint is seen as a set of selected fragments within a given range around {@link #genomicPos}.
     * Fragments can be discarded because they shorter, or because their margins a higher repetitive content than a given thresholds.
     *
     * @param maxSizeUp    upper limit for the distance between {@link #startPos} and {@link #genomicPos} (e.g. 5000).
     * @param maxSizeDown  upper limit for the distance between {@link #genomicPos} and {@link #endPos} (e.g. 5000).
     */
    void generateViewpointExtendedApproach(Integer maxSizeUp, Integer maxSizeDown, Model model ) {
        boolean allowSingleMargin=model.getAllowUnbalancedMargins();
        if(!this.isPositiveStrand) {
            Integer tmp=maxSizeUp;
            maxSizeUp=maxSizeDown;
            maxSizeDown=tmp;
        }


        approach=Approach.EXTENDED;
        this.centerSegment=null; // the digest that contains the TSS. Always show it!
        restrictionSegmentList.forEach(segment -> segment.setSelected(true,true));

        for (Segment segment : restrictionSegmentList) {
            if (segment.getStartPos() <= genomicPos && genomicPos <= segment.getEndPos()) {
                this.centerSegment = segment; segment.setOverlapsTSS(true); break;
            }
        }
        if (this.centerSegment==null) {
            logger.error("center segment NUll for " + getTargetName() + "\n\t" +
            "maxSizeUp=" + maxSizeUp+ ", maxSizeDown=" + maxSizeDown + " size of restrictionFragmentList = " +
            restrictionSegmentList.size());
        }

        // select segments
        int lowerLimit = genomicPos - maxSizeUp;
        int upperLimit = genomicPos + maxSizeDown;
        setFragmentsForExtendedApproach(lowerLimit,upperLimit,true);


        // set start position of the viewpoint to start position of the most upstream SELECTED digest
        int start=Integer.MAX_VALUE;
        int end=Integer.MIN_VALUE;

        for (Segment seg:restrictionSegmentList) {
            if (!seg.isSelected()) continue;
            if (start >seg.getStartPos()) start=seg.getStartPos();
            if (end < seg.getEndPos()) end=seg.getEndPos();
        }
        setStartPos(start);
        setEndPos(end);



        setDerivationApproach(Approach.EXTENDED);
        calculateViewpointScoreExtended();
    }


    void generateViewpointSimple(Model model) {

        boolean allowSingleMargin = model.getAllowUnbalancedMargins();
        boolean allowPatchedViewpoints = model.getAllowPatching();
        approach = Approach.SIMPLE;

        // find the digest that contains genomicPos
        this.centerSegment = restrictionSegmentList.stream().
                filter(segment -> segment.getStartPos() <= genomicPos && segment.getEndPos() >= genomicPos).
                findFirst().
                orElse(null);

        if (this.centerSegment == null) {
            logger.error(String.format("%s At least one digest must contain 'genomicPos' (%s:%d)", getTargetName(), chromosomeID, genomicPos));
            //restrictionSegmentList.clear(); /* no fragments */
        } else {
            this.centerSegment.setOverlapsTSS(true);
            logger.trace("Setting center segment, overlaps TSS for " + this.getReferenceID() + ": " );
            // originating from the centralized digest containing 'genomicPos' (included) openExistingProject digest-wise in UPSTREAM direction ???
            int length = centerSegment.length();
            logger.trace("length of center segment=" + length +" balanced=" + (centerSegment.isBalanced()?"yes":"no"));
            if ((length >= this.minFragSize &&
                    this.centerSegment.isBalanced())
                    ||
                    (length >= this.minFragSize &&
                            this.centerSegment.isUnbalanced() &&
                            allowSingleMargin)
                    ) {
                logger.trace("In loop set TRUE for " + this.getReferenceID() );
                centerSegment.setSelected(true,true);
                this.setStartPos(centerSegment.getStartPos());
                this.setEndPos(centerSegment.getEndPos());

                // Add the selected digest and the two neighboring segments
                Segment upstreamSegment = null;
                Segment downstreamSegment = null;
                int genomicPosFragIdx = restrictionSegmentList.indexOf(centerSegment);
                if (genomicPosFragIdx > 0) {
                    upstreamSegment = restrictionSegmentList.get(genomicPosFragIdx - 1);
                    logger.trace("SELECTED UPSTREAM " + upstreamSegment.detailedReport() );
                } else {
                    logger.trace("Warning: There is no segment in upstream direction of the center segment!");
                }
                if (genomicPosFragIdx < restrictionSegmentList.size() - 1) {
                    downstreamSegment = restrictionSegmentList.get(genomicPosFragIdx + 1);
                    logger.trace("SELECTED DOWNSTREAM " + upstreamSegment.detailedReport() );

                } else {
                    logger.trace("Warning: There is no segment in downstream direction of the center segment!");
                }
                Double score = calculateViewpointScoreSimple(centerSegment.getStartPos(), genomicPos, centerSegment.getEndPos());
                if(allowPatchedViewpoints && score < 0.6) {
                    // add adjacent segment
                    if(centerSegment.getEndPos() - genomicPos < genomicPos - centerSegment.getStartPos() && downstreamSegment != null) {
                        // try to add adjacent segment in downstream direction
                        if(isSegmentValid(downstreamSegment)) {
                            downstreamSegment.setSelected(true,true);
                            calculateViewpointScoreSimple(centerSegment.getStartPos(), genomicPos, downstreamSegment.getEndPos()); // recalculate score
                            this.setStartPos(centerSegment.getStartPos());
                            this.setEndPos(downstreamSegment.getEndPos());
                        }
                    } else if (upstreamSegment != null) {
                        // try add adjacent segment in upstream direction
                        if(isSegmentValid(upstreamSegment)) {
                            upstreamSegment.setSelected(true,true);
                            calculateViewpointScoreSimple(upstreamSegment.getStartPos(), genomicPos, centerSegment.getEndPos()); // recalculate score
                            this.setStartPos(upstreamSegment.getStartPos());
                            this.setEndPos(centerSegment.getEndPos());
                        }
                    }
                }
                restrictionSegmentList.clear();
                if(upstreamSegment != null) {restrictionSegmentList.add(upstreamSegment);}
                if(centerSegment != null) {restrictionSegmentList.add(centerSegment);}
                if(downstreamSegment != null) {restrictionSegmentList.add(downstreamSegment);}
            } else {
                // in this case, the center fragment was not selectable
                // if the user has not checked "allow unbalanced", then it might be desirable to allow the user to
                // select it manually. We need to show three fragments!
                Segment upstreamSegment = null;
                Segment downstreamSegment = null;
                int genomicPosFragIdx = restrictionSegmentList.indexOf(centerSegment);
                if (genomicPosFragIdx > 0) {
                    upstreamSegment = restrictionSegmentList.get(genomicPosFragIdx - 1);
                }
                if (genomicPosFragIdx < restrictionSegmentList.size() - 1) {
                    downstreamSegment = restrictionSegmentList.get(genomicPosFragIdx + 1);
                }
                restrictionSegmentList.clear();
                if(upstreamSegment != null) {restrictionSegmentList.add(upstreamSegment);}
                if(centerSegment != null) {restrictionSegmentList.add(centerSegment);}
                if(downstreamSegment != null) {restrictionSegmentList.add(downstreamSegment);}
            }
        }
        setDerivationApproach(Approach.SIMPLE);
    }

    private boolean isSegmentValid(Segment seg) {

        if(seg.length() < minFragSize) {
            return false;
        }
        if(seg.isBalanced()) {
            return true;
        }
        return (model.getAllowUnbalancedMargins() && seg.isUnbalanced());
    }



    private boolean isSegmentMarginValid(Segment segment, String dir) {

        double repeat=0;
        double gc=0;

        switch (dir) {
            case "Up":
                repeat = segment.getRepeatContentMarginUp();
                gc = segment.getGcContentMarginUp();
                break;
            case "Down":
                repeat = segment.getRepeatContentMarginDown();
                gc = segment.getGcContentMarginDown();
                break;
            default:
                logger.error("Function 'isSegmentMarginValid()' was called with argument different from 'Up' and 'Down'");
        }
        return (this.minGcContent <= gc) && (gc <= this.maxGcContent) && (repeat <= this.maximumRepeatContent);
    }



    private double getSegmentProbabilityDownstream(int from, int to,NormalDistribution nD) {
        if (from>=to)return 0d;
        // only look at the part of the segment that is downstream, i.e., >0
        double fromPos=from>0?(double)from:0d;
        double toPos=to>0?(double)to:0d;
        double cp1= nD.cumulativeProbability(toPos);
        double cp2 =nD.cumulativeProbability(fromPos);
        return cp1-cp2;
    }

    private double getSegmentProbabilityUpstream(int from, int to,NormalDistribution nD) {
        if (from>=to)return 0d;
        // only look at the part of the segment that is upstream, i.e., <0
        double fromPos=from<0?(double)from:0d;
        double toPos=to<0?(double)to:0d;
        double cp1= nD.cumulativeProbability(toPos);
        double cp2 =nD.cumulativeProbability(fromPos);
        return cp1-cp2;
    }

    /**
     * A simplified version of the extended viewpoint score.
     */
    public void calculateViewpointScoreExtended() {

        Double score = 0.0;
        // three standard deviations cover 99.7% of the data
        double sd = (double)upstreamNucleotideLength/6; // the factor 1/6 was chosen by eye
        double mean = 0; // shifts the normal distribution, so that almost the entire area under the curve is to the left of the y-axis
        logger.error(sd);
        NormalDistribution nDistUpstream = new NormalDistribution(mean,sd);
        sd = (double)downstreamNucleotideLength/6;
        NormalDistribution nDistDownstream = new NormalDistribution(mean,sd);


        //nDistUpstream = model.getNormalDistributionExtendedUp();
        //nDistDownstream = model.getNormalDistributionExtendedDown();

        logger.error("nDistUpstream.getMean(): " + nDistUpstream.getMean());
        logger.error("nDistUpstream.getStandardDeviation(): " + nDistUpstream.getStandardDeviation());

        logger.error("nDistDownstream.getMean(): " + nDistDownstream.getMean());
        logger.error("nDistDownstream.getStandardDeviation(): " + nDistDownstream.getStandardDeviation());



        /* iterate over all selected fragments */
        List<Segment> selectedSegments = restrictionSegmentList.
                stream().
                filter(Segment::isSelected).
                collect(Collectors.toList());

        double totalProbability=0.0;

        for (Segment seg : selectedSegments) {
            // get distance relative to TSS (or genomic pos in general)
            // The following is a two element list with from and to
            List<Integer> distance = seg.posToDistance(this.genomicPos);
           // System.err.println("From "+distance.get(0)+ " To="+distance.get(1));
            double upProb = getSegmentProbabilityUpstream(distance.get(0),distance.get(1),nDistUpstream);
            //logger.error("nDistUpstream: " + nDistUpstream.getMean());
            //logger.error("nDistUpstream: " + nDistUpstream.getStandardDeviation());
            double downProb  = getSegmentProbabilityDownstream(distance.get(0),distance.get(1),nDistDownstream);
            //logger.error("nDistDownstream: " + nDistDownstream.getMean());
            //logger.error("nDistDownstream: " + nDistDownstream.getStandardDeviation());
            totalProbability += (upProb + downProb);
            System.err.println("From "+distance.get(0)+ " To="+distance.get(1) + " up="+upProb +", down="+downProb +", total="+totalProbability);

        }
       this.score= totalProbability;

    }


    public Double calculateViewpointScoreSimple(Integer vpStaPos, Integer centerPos, Integer vpEndPos) {
        this.score = model.getNormalDistributionSimple().cumulativeProbability(vpEndPos - centerPos) - model.getNormalDistributionSimple().cumulativeProbability(vpStaPos - centerPos);
        return score;
    }


    /** @return the total length of the Margins of all active segments of this ViewPoint. */
    public int getTotalMarginSize() {
        return getActiveSegments().stream().mapToInt(Segment::getMarginSize).sum();
    }

    /** @return the total length of all active segments of this ViewPoint. */
    public Integer getTotalLengthOfActiveSegments() {
        return getActiveSegments().stream().mapToInt(Segment::length).sum();
    }

    /**
     * This function is intended to help confirmDialog the right
     * portion of the viewpoint in the UCSC browser.
     * It is either the start position of the first selected segment or the
     * deafult upstream position if no segment was selected.
     * @return the 5' position that we want to confirmDialog.
     */
    private int getMinimumSelectedPosition() {
        return getActiveSegments().stream().mapToInt(Segment::getStartPos).min().orElse(this.genomicPos-upstreamNucleotideLength);
    }
    /**
     * This function is intended to help confirmDialog the right
     * portion of the viewpoint in the UCSC browser.
     * It is either the end position of the last selected segment or the
     * deafult downstream position if no segment was selected.
     * @return the 5' position that we want to confirmDialog.
     */
    private int getMaximumSelectedPosition() {
        return getActiveSegments().stream().mapToInt(Segment::getEndPos).max().orElse(this.genomicPos+downstreamNucleotideLength);
    }

    /** Returns the leftmost position to confirmDialog in the UCSC browser. This is either the boundary of the initial search region, or
     * in the case where a selected digest overlaps that boundary, the start pos of that digest.
     * @return leftmost confirmDialog position
     */
    public int getMinimumDisplayPosition() { return Math.min(getMinimumSelectedPosition(),this.genomicPos-upstreamNucleotideLength); }
    /** Returns the rightmost position to confirmDialog in the UCSC browser. This is either the boundary of the initial search region, or
     * in the case where a selected digest overlaps that boundary, the end pos of that digest.
     * @return rightmost confirmDialog position
     */
    public int getMaximumDisplayPosition() { return Math.max(getMaximumSelectedPosition(),this.genomicPos+downstreamNucleotideLength); }


    public int getUpstreamSpan() {
        if (hasNoActiveSegment()) return 0;
        if (isPositiveStrand) {
            return genomicPos - getMinimumSelectedPosition();
        } else {
            return getMaximumSelectedPosition() - genomicPos;
        }
    }

    public int getDownstreamSpan() {
        if (hasNoActiveSegment()) return 0;
        if (isPositiveStrand) {
            return getMaximumSelectedPosition() - genomicPos;
        } else {
            return genomicPos - getMinimumSelectedPosition();
        }
    }



    /**
     * If no segments are active/selected, then return zero. Otherwise return the length between the 5' end of the
     * first selected segment and the 3' end of the last selected segment.
     * @return length between the 5' nt of first selected segment and 3' nt of the last selected segment.
     */
    public Integer getTotalLengthOfViewpoint() {
        if (getActiveSegments().size()==0) return 0;
        int min=Integer.MAX_VALUE;
        int max=Integer.MIN_VALUE;
        for (Segment s : getActiveSegments()) {
            if (s.getStartPos()<min) min = s.getStartPos();
            if (s.getEndPos()>max) max=s.getEndPos();
        }
        return max - min + 1;
    }

    @Override
    public boolean equals(Object other) {
        if (! (other instanceof ViewPoint)) return false;
        ViewPoint othervp = (ViewPoint)other;
        return (targetName.equals(othervp.targetName) &&
        genomicPos == othervp.genomicPos &&
        chromosomeID.equals(othervp.chromosomeID));

    }

    public boolean hasNoActiveSegment() {
        return this.getActiveSegments().size()==0;
    }


    /**
     * A Builder class. To create a {@link ViewPoint} object, use code such as
     * <pre>
     *  refID="chr15";
     *  int gpos=48937985;
     *  ViewPoint vp = new ViewPoint.Builder(refID,gpos).targetName("FBN1").upstreamNtLength(1500).build();
     * </pre>
     * and add Builders for each parameter with a non-default value.
     */
    public static class Builder {
        //  parameters required in the constructor
        private final String chromosomeID;
        private String accessionNr=null;
        private final int genomicPos;
        // other params
        private IndexedFastaSequenceFile fastaReader;
        private String targetName="";
        // Optional parameters - initialized to default values
        /* upstream nucleotide length for digest generation (upstream of genomic pos).*/
        private Integer upstreamNtLength = Default.SIZE_UPSTREAM;
        /* downstream nucleotide length for digest generation (downstream of genomic pos).*/
        private Integer downstreamNtLength = Default.SIZE_DOWNSTREAM;
        /** Need to choose a default strand, but this will always be overwritten. */
        private boolean isPositiveStrand =true;
        private Integer minFragSize=Default.MINIMUM_FRAGMENT_SIZE;
        private double maximumRepeatContent=Default.MAXIMUM_KMER_ALIGNABILITY;
        private double maxGcContent=Default.MAX_GC_CONTENT;
        private double minGcContent=Default.MIN_GC_CONTENT;
        private int marginSize=Default.MARGIN_SIZE;
        private Model model;
        private AlignabilityMap c2alignmap;

        private final int chromosomelen;

        /**
         *
         * @param refID reference sequence ID (eg, chr5)
         * @param pos central position of the viewpoint on the reference sequence
         */
        Builder(String refID, int pos, int chromlen) {
            this.chromosomeID = refID;
            this.genomicPos    = pos;
            this.chromosomelen=chromlen;

        }
        Builder targetName(String val)
        { targetName = val;  return this; }
        Builder fastaReader(IndexedFastaSequenceFile val) {
            this.fastaReader=val; return this;
        }
        Builder maximumGcContent(double maxGC) {
            maxGcContent=maxGC; return this;
        }
        Builder isForwardStrand(boolean strand) {
            this.isPositiveStrand = strand;
            return this;
        }
        Builder minimumGcContent(double minGC) {
            this.minGcContent=minGC; return this;
        }
        Builder accessionNr(String acc) {
            this.accessionNr=acc;return this;
        }
        Builder downstreamLength(int val) {
            this.downstreamNtLength=val; return this;
        }
        Builder upstreamLength(int val) {
            this.upstreamNtLength=val; return this;
        }
        Builder minimumFragmentSize(int val) {
            this.minFragSize=val; return this;
        }
        Builder maximumRepeatContent(double val) {
            this.maximumRepeatContent=val; return this;
        }
        Builder marginSize(int val) {
            this.marginSize=val; return this;
        }
        Builder model(Model model) {
            this.model=model; return this;
        }
        Builder c2alignabilityMap(AlignabilityMap c2am) {
            this.c2alignmap = c2am; return this;
        }

        public ViewPoint build() {
            return new ViewPoint(this);
        }
    }

    /**
     * This function can be used in order to determine if the set of selected segments has changed after creation
     * of the viewpoint.
     */
    public boolean wasModified() {
        // iterate over all segments (selected and deselected)
        for(Segment s : this.restrictionSegmentList) {
            if(s.wasOriginallySelected() != s.isSelected()) {
                // return true for at the first modified segment found
                return true;
            }
        }
        // if no modified segment was found return false
        return false;
    }

    /**
     * This fuction can be used to reset the set of segments to the original state
     */
    public void resetSegmentsToOriginalState() {
        for(Segment s : this.restrictionSegmentList) {
            if(s.wasOriginallySelected()) {
                s.setSelected(true,false);
            } else {
                s.setSelected(false, false);
            }
        }
    }



}
