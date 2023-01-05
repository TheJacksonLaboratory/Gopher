package gopher.service.model.viewpoint;

import com.google.common.collect.ImmutableList;
import gopher.service.GopherService;
import gopher.service.model.Approach;
import gopher.service.model.Default;
import gopher.service.model.RestrictionEnzyme;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewPoint.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 4L;
    /* The approach used to generate this viewpoint (Simple or Extended) */
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
    /** List of active and inactive restriction {@link gopher.service.model.viewpoint.Segment} objects that are contained within the viewpoint. */
    private List<Segment> restrictionSegmentList;
    /** List of restriction enzymes chosen by the User (package scope visibility). */
    static List<RestrictionEnzyme> chosenEnzymes=null;
    /** The "number" of the promoter for the gene in question. */
    private int promoterNumber;
    /** Total number of promoters associated with this gene. */
    private int totalPromoters;
    /** This is a reference to the segment that overlaps the TSS */
    private Segment centerSegment=null;

    private final GopherService gopherService;

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
            LOGGER.error(String.format("Error-- null list of restriction segments for %s",getTargetName()));
            return new ArrayList<>();/* return empty list.*/
        }
        //return a List of all selected segments
        return this.restrictionSegmentList.stream().filter(s -> (s!=null && s.isSelected())).collect(Collectors.toList());
    }

    /**
     *
     */
    public void refreshStartAndEndPos() {
        List<Segment> segments = this.restrictionSegmentList.stream().filter(Segment::isSelected).toList();
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
        this.gopherService =builder.service;
        init(builder.fastaReader,builder.c2alignmap, builder.chromosomelen);
    }


    private void init(IndexedFastaSequenceFile fastaReader, AlignabilityMap c2align, int chromosomeLength) {
        this.restrictionSegmentList=new ArrayList<>();
        boolean changed;
        /* Create segmentFactory */
        if(gopherService.getApproach().     equals(Approach.SIMPLE)) {
            this.upstreamNucleotideLength= gopherService.getEstAvgRestFragLen().intValue();
            this.downstreamNucleotideLength= gopherService.getEstAvgRestFragLen().intValue();
            /*
             For the simple approach, iteratively increase range for initial restriction fragments as long as
             genomicPos occurs on first or last fragment of the list in order to make sure that adjacent fragments
             can later be added.
             */
            int iteration = 0;
            int increment = 1000;
            do {
                LOGGER.trace("segmentFactory iteration = " + iteration);
                changed=false;
                segmentFactory = new SegmentFactory(this.chromosomeID,
                        this.genomicPos,
                        fastaReader,
                        chromosomeLength,
                        this.upstreamNucleotideLength,
                        this.downstreamNucleotideLength,
                        ViewPoint.chosenEnzymes);
                iteration++;

                LOGGER.trace("Number of frags="+restrictionSegmentList.size());

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
                    LOGGER.trace("0<x and 0<y");
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
            int increment = gopherService.getEstAvgRestFragLen().intValue()*2;
            do {
                LOGGER.trace("segmentFactory iteration = " + iteration + " (" + this.targetName + ")");
                changed=false;
                segmentFactory = new SegmentFactory(this.chromosomeID,
                        this.genomicPos,
                        fastaReader,
                        chromosomeLength,
                        upstreamLength,
                        downstreamLength,
                        ViewPoint.chosenEnzymes);
                LOGGER.trace("Done with Segment factory");
                iteration++;

                LOGGER.trace("Number of frags = " + restrictionSegmentList.size());

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
                    LOGGER.trace("0<x and 0<y");
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
        LOGGER.trace("restrictionSegmentList.size(): " + restrictionSegmentList.size());
        int firstSelectedIndex = IntStream.range(0,LEN)
                .filter(i->restrictionSegmentList.get(i).overlapsRange(genomicPos-this.upstreamNucleotideLength,genomicPos+this.downstreamNucleotideLength))//isSelected())
                .findFirst().orElse(0);

        // The map reverses the order.
        int lastSelectedIndex = IntStream.range(0,LEN-1).
                map(i -> LEN - i - 1).
                filter(i->restrictionSegmentList.get(i).overlapsRange(genomicPos-this.upstreamNucleotideLength,genomicPos+this.downstreamNucleotideLength))//.isSelected())
                .findFirst().orElse(0);


        if (firstSelectedIndex+lastSelectedIndex==0) {
            LOGGER.error("Skipping trimming Segment List because no segments are selected for " + getTargetName());
            LOGGER.error("firstSelectedIndex:" + firstSelectedIndex);
            LOGGER.error("lastSelectedIndex:" + lastSelectedIndex);
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
            Segment restFrag = new Segment(chromosomeID,
                    0,
                    0,fastaReader, marginSize); // needed?
        }
        for (int j = 0; j < segmentFactory.getAllCuts().size() - 1; j++) {
            Segment restFrag = new Segment(chromosomeID,
                    segmentFactory.getUpstreamCut(j),
                    segmentFactory.getDownstreamCut(j) - 1,
                    fastaReader ,marginSize);
            double maxMeanAlignabilityScore = 1.0 * gopherService.getMaxMeanKmerAlignability();
            restFrag.setUsableBaits(gopherService,c2align,maxMeanAlignabilityScore);
            restrictionSegmentList.add(restFrag);
        }
        LOGGER.trace("Done init Restriction Frags");
    }

    /** @return a 2-tuple with the number of baits: <up,down>. */
    private List<Integer> getNumberOfBaitsUpDown() {
        ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<>();
        int up=0;
        int down=0;
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
        this.startPos = Math.max(startPos, 0);
    }
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
        for (Segment segment:restrictionSegmentList) {
            segment.setSelected(true,updateOriginallySelected); // initial segment selection is done here and nowhere else
            // do not select fragments that are too small
            if (segment.length() < this.minFragSize) { // minFragSize should be at least one bait size
                segment.setSelected(false,updateOriginallySelected);
            }
            // do not select segments that are entirely outside the allowed range
            if((segment.getEndPos() < lowerLimit) || (upperLimit < segment.getStartPos()) ) {
                segment.setSelected(false,updateOriginallySelected);
            }

            // do not select segments that have less than 2 times bmin baits
            if(segment.isUnselectable()) {
                segment.setSelected(false,updateOriginallySelected);
            }

            // if allow single margin is false, do not select unbalanced digests
            if(!gopherService.getAllowUnbalancedMargins() && segment.isUnbalanced()) {
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
    void generateViewpointExtendedApproach(Integer maxSizeUp, Integer maxSizeDown, GopherService model ) {
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
            LOGGER.error("center segment NUll for " + getTargetName() + "\n\t" +
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


    void generateViewpointSimple(GopherService service) {

        boolean allowSingleMargin = service.getAllowUnbalancedMargins();
        boolean allowPatchedViewpoints = service.getAllowPatching();
        approach = Approach.SIMPLE;

        // find the digest that contains genomicPos
        this.centerSegment = restrictionSegmentList.stream().
                filter(segment -> segment.getStartPos() <= genomicPos && segment.getEndPos() >= genomicPos).
                findFirst().
                orElse(null);

        if (this.centerSegment == null) {
            LOGGER.error(String.format("%s At least one digest must contain 'genomicPos' (%s:%d)", getTargetName(), chromosomeID, genomicPos));
            //restrictionSegmentList.clear(); /* no fragments */
        } else {
            this.centerSegment.setOverlapsTSS(true);
            LOGGER.trace("Setting center segment, overlaps TSS for " + this.getReferenceID() + ": " );
            // originating from the centralized digest containing 'genomicPos' (included) openExistingProject digest-wise in UPSTREAM direction ???
            int length = centerSegment.length();
            LOGGER.trace("length of center segment=" + length +" balanced=" + (centerSegment.isBalanced()?"yes":"no"));
            if ((length >= this.minFragSize &&
                    this.centerSegment.isBalanced())
                    ||
                    (length >= this.minFragSize &&
                            this.centerSegment.isUnbalanced() &&
                            allowSingleMargin)
                    ) {
                centerSegment.setSelected(true,true);
                this.setStartPos(centerSegment.getStartPos());
                this.setEndPos(centerSegment.getEndPos());

                // Add the selected digest and the two neighboring segments
                Segment upstreamSegment = null;
                Segment downstreamSegment = null;
                int genomicPosFragIdx = restrictionSegmentList.indexOf(centerSegment);
                if (genomicPosFragIdx > 0) {
                    upstreamSegment = restrictionSegmentList.get(genomicPosFragIdx - 1);
                    LOGGER.trace("Selected upstream {}", upstreamSegment.detailedReport());
                } else {
                    LOGGER.trace("Warning: There is no segment in upstream direction of the center segment!");
                }
                if (genomicPosFragIdx < restrictionSegmentList.size() - 1) {
                    downstreamSegment = restrictionSegmentList.get(genomicPosFragIdx + 1);
                    LOGGER.trace("Selected downstream {}", downstreamSegment.detailedReport());

                } else {
                    LOGGER.trace("Warning: There is no segment in downstream direction of the center segment!");
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
        return (gopherService.getAllowUnbalancedMargins() && seg.isUnbalanced());
    }



    private boolean isSegmentMarginValid(Segment segment, String dir) {

        double repeat=0;
        double gc=0;

        switch (dir) {
            case "Up" -> {
                repeat = segment.getRepeatContentMarginUp();
                gc = segment.getGcContentMarginUp();
            }
            case "Down" -> {
                repeat = segment.getRepeatContentMarginDown();
                gc = segment.getGcContentMarginDown();
            }
            default -> LOGGER.error("Function 'isSegmentMarginValid()' was called with argument different from 'Up' and 'Down'");
        }
        return (this.minGcContent <= gc) && (gc <= this.maxGcContent) && (repeat <= this.maximumRepeatContent);
    }


    /**
     * This function calculates the probability of a segment according to the Normal distribution
     * that is passed to it. It only takes positions that are 3' (to the right of ) the
     * transcription start site ({@link #genomicPos}). These positions are encoded using
     * positive integers (See {@link Segment}, function posToDistance).
     * @param from The most 5' (upstream) position of the segment in genomic coordinates
     * @param to The most 3' (downstream) position of the segment in genomic coordinates
     * @param nD THe Normal distribution for calculating the probability
     * @return THe calculated probability for this segment
     */
    private double getSegmentProbability3primeOfTTT(int from, int to, NormalDistribution nD) {
        if (from>=to)return 0d;
        // only look at the part of the segment that is downstream, i.e., >0
        double fromPos=from>0?(double)from:0d;
        double toPos=to>0?(double)to:0d;
        double cp1= nD.cumulativeProbability(toPos);
        double cp2 =nD.cumulativeProbability(fromPos);
        return cp1-cp2;
    }

    /**
     * This function calculates the probability of a segment according to the Normal distribution
     * that is passed to it. It only takes positions that are 5' (to the left of ) the
     * transcription start site ({@link #genomicPos}). These positions are encoded using
     * negative integers (See {@link Segment}, function posToDistance).
     * @param from The most 5' (upstream) position of the segment in genomic coordinates
     * @param to The most 3' (downstream) position of the segment in genomic coordinates
     * @param nD THe Normal distribution for calculating the probability
     * @return THe calculated probability for this segment
     */
    private double getSegmentProbability5primeOfTSS(int from, int to, NormalDistribution nD) {
        if (from>=to)return 0d;
        // only look at the part of the segment that is upstream, i.e., <0
        double fromPos=from<0?(double)from:0d;
        double toPos=to<0?(double)to:0d;
        double cp1= nD.cumulativeProbability(toPos);
        double cp2 =nD.cumulativeProbability(fromPos);
        return cp1-cp2;
    }

    /**
     *  The extended viewpoint score essentially checks how much area of two half-normal distributinos
     *  (for up and down stream) are filled in by the active segments.
     */
    public void calculateViewpointScoreExtended() {
        NormalDistribution nDistUpstream= gopherService.getNormalDistributionExtendedUp();
        NormalDistribution nDistDownstream= gopherService.getNormalDistributionExtendedDown();

        /* iterate over all selected fragments */
        List<Segment> selectedSegments = restrictionSegmentList.
                stream().
                filter(Segment::isSelected).toList();

        double totalProbability=0.0;

        for (Segment seg : selectedSegments) {
            // get distance relative to TSS (or genomic pos in general)
            // The following is a two element list with from and to for each segment
            List<Integer> distance = seg.posToDistance(this.genomicPos);
            int FROM=distance.get(0);
            int TO=distance.get(1);
            // if this gene is on the negative strand, we need to switch the two distrubtions.
            double upProb,downProb;
            if (this.isPositiveStrand) {
                // switch upstream and downstream if the gene/viewpoint is on the minus strand
                upProb = getSegmentProbability5primeOfTSS(FROM, TO, nDistUpstream);
                downProb = getSegmentProbability3primeOfTTT(FROM, TO, nDistDownstream);
            } else {
                 upProb = getSegmentProbability5primeOfTSS(FROM, TO, nDistDownstream);
                 downProb = getSegmentProbability3primeOfTTT(FROM, TO, nDistUpstream);
            }
            totalProbability += (upProb + downProb);
        }
       this.score= totalProbability;

    }


    public Double calculateViewpointScoreSimple(Integer vpStaPos, Integer centerPos, Integer vpEndPos) {
        this.score = gopherService.getNormalDistributionSimple().cumulativeProbability(vpEndPos - centerPos) - gopherService.getNormalDistributionSimple().cumulativeProbability(vpStaPos - centerPos);
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
     * @param zoomfactor Amount of zooming to do
     * @return leftmost confirmDialog position
     */
    public int getMinimumDisplayPosition(double zoomfactor) {
        return this.genomicPos-(int)(upstreamNucleotideLength*zoomfactor);
    }

    public int getMaximumDisplayPosition(double zoomfactor) {
        return this.genomicPos+(int)(downstreamNucleotideLength*zoomfactor);
    }


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
        if (! (other instanceof ViewPoint othervp)) return false;
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
        private double minGcContent= Default.MIN_GC_CONTENT;
        private int marginSize=Default.MARGIN_SIZE;
        private GopherService service;
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
        Builder model(GopherService model) {
            this.service =model; return this;
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
            s.setSelected(s.wasOriginallySelected(),false);
        }
    }



}
