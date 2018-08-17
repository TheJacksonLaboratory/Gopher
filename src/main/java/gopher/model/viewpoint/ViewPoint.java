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
//    /** The minimum allowable start position if the user zooms. */
//    private int minimumAllowableStartPosition;
//    /** The maximum allowable end position if the user zooms. */
//    private int maximumAllowableEndPosition;
    /** Minimum allowable size of a restriction digest-this will usually be determined by the size of the probes
     * that are used for enrichment (e.g., 130 bp. */
    private final int minFragSize;
    /** Maximum allowable GC content */
    private final double maxGcContent;
    /** Minimum allowable GC content */
    private final double minGcContent;
    /** Is the gene on the forward (positive) strand). */
    private final boolean isPositiveStrand;
    /** A viewpoint is marked as resolved, if it has the required number of segments after application of the function {@link #generateViewpointExtendedApproach}. */
    private boolean resolved;
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

    //private transient AlignabilityMap chromosome2AlignabilityMap;

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
//    /** A list of restriction enzymes (at least one) as chosen by the user. */
//    public static RestrictionEnzyme getChosenEnzyme(int i) {
//        if (chosenEnzymes==null || i-1>chosenEnzymes.size()) return null;
//        else return chosenEnzymes.get(i);
//    }
    /** Overall score of this Viewpoint.*/
    private double score;
    /** Maximim allowable digest size for simple approach */
    private static final int SIMPLE_APPROACH_MAXSIZE=20_000;

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
        return this.restrictionSegmentList.stream().filter(Segment::isSelected).collect(Collectors.toList());
    }

    /**
     *
     */
    public void refreshStartAndEndPos() {
        List<Segment> segments = this.restrictionSegmentList.stream().filter(Segment::isSelected).collect(Collectors.toList());
        int min = segments.get(0).getStartPos();
        int max = segments.get(0).getEndPos();
        for(int i=1; i< segments.size(); i++) {
            if(segments.get(i).getStartPos() < min) {
                min=segments.get(i).getStartPos();
            }
            if(max < segments.get(i).getEndPos()) {
                max=segments.get(i).getEndPos();
            }
            this.setStartPos(min);
            this.setEndPos(max);
        }
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
    public void zoom(double zoomfactor) {
        logger.trace(String.format("Zooming...Current upstreamNucleotide length %d; downstream %d; factor=%f", upstreamNucleotideLength,downstreamNucleotideLength,zoomfactor));
        this.upstreamNucleotideLength =(int)(this.upstreamNucleotideLength *zoomfactor);
        this.downstreamNucleotideLength =(int)(this.downstreamNucleotideLength *zoomfactor);
        logger.trace(String.format("After zoom...upstreamNucleotide length %d; downstream %d", upstreamNucleotideLength,downstreamNucleotideLength));
        // TODO do we need to worry about going over end of chromosome?
        // or start before zero?
        setStartPos(genomicPos - upstreamNucleotideLength);
        setEndPos(genomicPos + downstreamNucleotideLength);
        // note that if the approach is SIMPLE, then we do not recalculate the selected viewpoints.
        // if the approach is EXTENDED, then we take a valid fragments in the zoomed region
        if (this.approach.equals(Approach.EXTENDED)) {
            setFragmentsForExtendedApproach(this.startPos, this.endPos, false);
        }

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
//        setMinimumAllowableStartPos(genomicPos - SegmentFactory.MAXIMUM_ZOOM_FACTOR * upstreamNucleotideLength);
//        setMaximumAllowableEndPos(genomicPos + SegmentFactory.MAXIMUM_ZOOM_FACTOR * downstreamNucleotideLength);
        this.minGcContent=builder.minGcContent;
        this.maxGcContent=builder.maxGcContent;
        this.minFragSize=builder.minFragSize;
        this.marginSize= builder.marginSize;
        this.accession=builder.accessionNr;
        this.maximumRepeatContent=builder.maximumRepeatContent;
        this.model=builder.model;
        logger.trace("Done with vars, about to init in VP CTOR");
        init(builder.fastaReader,builder.c2alignmap);
    }


    private void init(IndexedFastaSequenceFile fastaReader, AlignabilityMap c2align) {
        this.restrictionSegmentList=new ArrayList<>();
        setResolved(false);
        /* Create segmentFactory */
        segmentFactory = new SegmentFactory(this.chromosomeID,
                this.genomicPos,
                fastaReader,
                this.upstreamNucleotideLength,
                this.downstreamNucleotideLength,
                ViewPoint.chosenEnzymes);
        logger.trace("Done with Segment factory");
        initRestrictionFragments(fastaReader, c2align);
    }


    /**
     * This function uses the information about cutting position sites from the {@link #segmentFactory} to build
     * a list of {@link Segment} objects in {@link #restrictionSegmentList}.
     */
    private void initRestrictionFragments(IndexedFastaSequenceFile fastaReader, AlignabilityMap c2align) {
        this.restrictionSegmentList = new ArrayList<>();
        for (int j = 0; j < segmentFactory.getAllCuts().size() - 1; j++) {
            Segment restFrag = new Segment.Builder(chromosomeID,
                    segmentFactory.getUpstreamCut(j),
                    segmentFactory.getDownstreamCut(j) - 1).
                    fastaReader(fastaReader).marginSize(marginSize).build();
            Double maxMeanAlignabilityScore = 1.0 * model.getMaxMeanKmerAlignability();
            restFrag.setUsableBaits(model,c2align,maxMeanAlignabilityScore);
            restrictionSegmentList.add(restFrag);
        }
//        logger.trace("Done init Restriction Frags");
//        for (Segment er : restrictionSegmentList){
//            logger.trace("\tSegment: " + er.detailedReport());
//        }
    }

    /** @return a 2-tuple with the number of baits: <up,down>. */
    public List<Integer> getNumberOfBaitsUpDown() {
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
        return String.format("%.2f%%",100*score);
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

    public final String getDerivationApproach() {
        return approach.toString();
    }

    private void setDerivationApproach(Approach derivationApproach) {
        this.approach = derivationApproach;
    }


    public final boolean getResolved() {
        return resolved;
    }
    /** @return true if this viewpoint has at least one active (selected) probe and it is resolved. */
    public final boolean hasValidProbe() {
        return getNumOfSelectedFrags()>0 && resolved;
    }

    private void setResolved(boolean resolved) {
        this.resolved = resolved;
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
        if (this.isPositiveStrand) return "+ strand";
        else return "- strand";
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

            // if allow single margin is false, do not select segments that are rescuable
            if(!model.getAllowUnbalancedMargins() && segment.isUnbalanced()) {
                segment.setSelected(false,updateOriginallySelected);
            }

//            if(segment.isSelected() && segment.isUnbalanced()) {
//                //logger.trace(segment.getReferenceSequenceID() + ":" + segment.getStartPos() + "-" + segment.getEndPos());
//            }

            // if at least one segment is selected, declare viewpoint to be resolved
            if(segment.isSelected()) {
                this.resolved = true;
            }
        }
//        c = (int)restrictionSegmentList.stream().filter(Segment::isSelected).count();
    }




    /**
     * This function can be used to reshape the viewpoint according to rules that were developed in consultation with bench scientists.
     * In this approach, the viewpoint is seen as a set of selected fragments within a given range around {@link #genomicPos}.
     * Fragments can be discarded because they shorter, or because their margins a higher repetitive content than a given thresholds.
     *
     * @param maxSizeUp    upper limit for the distance between {@link #startPos} and {@link #genomicPos} (e.g. 5000).
     * @param maxSizeDown  upper limit for the distance between {@link #genomicPos} and {@link #endPos} (e.g. 5000).
     */
    public void generateViewpointExtendedApproach(Integer maxSizeUp, Integer maxSizeDown, Model model ) {
        boolean allowSingleMargin=model.getAllowUnbalancedMargins();
        if(!this.isPositiveStrand) {
            Integer tmp=maxSizeUp;
            maxSizeUp=maxSizeDown;
            maxSizeDown=tmp;
        }

        boolean resolved = true;
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
            "maxSizeUp=" + maxSizeUp+ ", maxSizeDown=" + maxSizeDown + " size of restrictionFragmentList =" +
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

        // discard fragments except for the selected fragments and their immediate neighbors, i.e.,
        // retain one unselected digest on each end
        // this will keep the table from having lots of unselected fragments

        int LEN = restrictionSegmentList.size();
        int firstSelectedIndex = IntStream.range(0,LEN)
               .filter(i->restrictionSegmentList.get(i).isSelected())
                .findFirst().orElse(0);

        // The map reverses the order.
        int lastSelectedIndex = IntStream.range(0,LEN-1).
                map(i -> LEN - i - 1).
                filter(i->restrictionSegmentList.get(i).isSelected())
               .findFirst().orElse(0);


        if (firstSelectedIndex+lastSelectedIndex==0) {
            logger.error("Skipping trimming Segment List because no segments are selected for "+getTargetName());
        } else {
            int i = Math.max(0, firstSelectedIndex - 1);
            int j = Math.min(LEN, lastSelectedIndex + 2);// +2 because we want one more and range is (inclusive,exclusive)
            restrictionSegmentList  = IntStream.range(i, j).boxed().map(k -> restrictionSegmentList.get(k)).collect(Collectors.toList());
        }

        setDerivationApproach(Approach.EXTENDED);
        calculateViewpointScoreExtended();
        setResolved(resolved);
    }


    public void generateViewpointSimple(Model model) {

        boolean allowSingleMargin = model.getAllowUnbalancedMargins();
        boolean allowPatchedViewpoints = model.getAllowPatching();
        boolean resolved = true;
        approach = Approach.SIMPLE;

        // find the digest that contains genomicPos
        this.centerSegment = restrictionSegmentList.stream().
                filter(segment -> segment.getStartPos() < genomicPos && segment.getEndPos() >= genomicPos).
                findFirst().
                orElse(null);

        if (this.centerSegment == null) {
            logger.error(String.format("%s At least one digest must contain 'genomicPos' (%s:%d-%d)", getTargetName(), chromosomeID, startPos , endPos ));
            resolved = false;
            restrictionSegmentList.clear(); /* no fragments */
        } else {
            this.centerSegment.setOverlapsTSS(true);
            logger.trace("Setting center segment, overlaps TSS for " + this.getReferenceID() + ": " );
            // originating from the centralized digest containing 'genomicPos' (included) openExistingProject digest-wise in UPSTREAM direction ???
            int length = centerSegment.length();
            logger.trace("length of center segment=" + length +" balanced=" + (centerSegment.isBalanced()?"yes":"no"));
            if ((length >= this.minFragSize &&
                    length <= SIMPLE_APPROACH_MAXSIZE &&
                    this.centerSegment.isBalanced())
                    ||
                    (length >= this.minFragSize &&
                            length <= SIMPLE_APPROACH_MAXSIZE &&
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
                resolved = true;
                Double score = calculateViewpointScoreSimple(model.getEstAvgRestFragLen(), centerSegment.getStartPos(), genomicPos, centerSegment.getEndPos());
                if(allowPatchedViewpoints && score < 0.6) {
                    // add adjacent segment
                    if(centerSegment.getEndPos() - genomicPos < genomicPos - centerSegment.getStartPos() && downstreamSegment != null) {
                        // try to add adjacent segment in downstream direction
                        if(isSegmentValid(downstreamSegment)) {
                            downstreamSegment.setSelected(true,true);
                            calculateViewpointScoreSimple(model.getEstAvgRestFragLen(), centerSegment.getStartPos(), genomicPos, downstreamSegment.getEndPos()); // recalculate score
                            this.setStartPos(centerSegment.getStartPos());
                            this.setEndPos(downstreamSegment.getEndPos());
                        }
                    } else if (upstreamSegment != null) {
                        // try add adjacent segment in upstream direction
                        if(isSegmentValid(upstreamSegment)) {
                            upstreamSegment.setSelected(true,true);
                            calculateViewpointScoreSimple(model.getEstAvgRestFragLen(), upstreamSegment.getStartPos(), genomicPos, centerSegment.getEndPos()); // recalculate score
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
        setResolved(resolved);

    }

    private boolean isSegmentValid(Segment seg) {

        if(seg.length() < minFragSize || seg.length()>= SIMPLE_APPROACH_MAXSIZE) {
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

        if(dir.equals("Up")) {
            repeat = segment.getRepeatContentMarginUp();
            gc = segment.getGcContentMarginUp();
        }
        else if(dir.equals("Down")) {
            repeat = segment.getRepeatContentMarginDown();
            gc = segment.getGcContentMarginDown();
        } else {
            logger.error("Function 'isSegmentMarginValid()' was called with argument different from 'Up' and 'Down'");

        }

        return (this.minGcContent <= gc) && (gc <= this.maxGcContent) && (repeat <= this.maximumRepeatContent);
    }

    /**
     * Helper function for the calculation of the viewpoint score.
     * It calculates a score for a given distance based on the cumulative normal distribution function.
     * <p>
     * The distance of 0 receives a score of almost one.
     * Greater distances receive a lower score.
     * The distance maxDistToGenomicPos receives a score of almost 0.
     *
     * @param dist distance (to {@link #genomicPos}) for which the distance score will be calculated.
     * @param maxDistToGenomicPos is used to init the normal distribution used to model the distance score.
     * @return position distance score between 0 and 1 (see maunscript).
     */
    private double getViewpointPositionDistanceScore(Integer dist, Integer maxDistToGenomicPos) {
        double sd = maxDistToGenomicPos/6; // the factor 1/6 was chosen by eye
        double mean = -3*sd; // shifts the normal distribution, so that almost the entire area under the curve is to the left of the y-axis
        NormalDistribution nD = new NormalDistribution(mean,sd);
        return nD.cumulativeProbability(-dist);
    }


    /**
     * This function calculates the viewpoint score and sets the field 'score' of this class.
     * The function is also intended to update the score.
     * <p>
     * The function iterates over all restriction segments of the viewpoint.
     * For selected segments a <i>position distance score</i> is calculated for each position.
     * The scores for all positions are summed up and in the end divided by the sum of all
     * <i>position distance scores</i> for all positions within the interval
     * [genomicPos - upstreamNucleotideLength; genomicPos + downstreamNucleotideLength ].
     * <p>
     * The overall score for the viewpoint is between 0 and 1.
     *
     */
    public void calculateViewpointScoreExtended() {
        Double score = 0.0;

        /* iterate over all selected fragments */

        Integer posCnt = 0;
        List<Segment> allFrags=restrictionSegmentList;
        for (Segment currentSegment : allFrags) {
            double repCont = 0;
            double positionScoreSumFragment = 0;

            if (currentSegment.isSelected()) {

                repCont=currentSegment.getMeanMarginRepeatContent();

                /* get position distance score for each position of the digest */

                positionScoreSumFragment = 0;
                for (int j = currentSegment.getStartPos(); j <= currentSegment.getEndPos(); j++) {
                    Integer dist = j - genomicPos;
                    if (dist < 0) {
                        if (dist*-1 > upstreamNucleotideLength) { continue; } // regions outside the specified range are not taken into account
                        positionScoreSumFragment += getViewpointPositionDistanceScore(-1 * dist, upstreamNucleotideLength);
                    } else {
                        if (dist*-1 > downstreamNucleotideLength) { continue; } // regions outside the specified range are not taken into account
                        positionScoreSumFragment += getViewpointPositionDistanceScore(dist, downstreamNucleotideLength);
                    }
                    posCnt++;
                }
            }
            score += (1 - repCont) * positionScoreSumFragment;
        }

        /* calculate reference: all position within specified range covered, no repeats */

        double positionScoreSumRef = 0;
        for(int i = genomicPos-upstreamNucleotideLength; i<=genomicPos+downstreamNucleotideLength; i++) {

            Integer dist = i - genomicPos;
            if (dist < 0) {
                positionScoreSumRef += getViewpointPositionDistanceScore(-1 * dist, upstreamNucleotideLength);
            } else {
                positionScoreSumRef += getViewpointPositionDistanceScore(dist, downstreamNucleotideLength);
            }
        }

        /* set final score */

        //System.out.println( upstreamNucleotideLength+downstreamNucleotideLength + "\t" + score + "\t" + positionScoreSumRef + "\t" + score/positionScoreSumRef);
        if (posCnt == 0) {
            this.score = 0.0;
        } else {
            this.score = score / positionScoreSumRef;
        }

    }

    public Double calculateViewpointScoreSimple(Double avgRestFragSize, Integer vpStaPos, Integer centerPos, Integer vpEndPos) {

        double mean = 0; // corresponds to TSS position
        double sd = avgRestFragSize/6; // chosen by eye
        NormalDistribution nD = new NormalDistribution(mean,sd);
        this.score = nD.cumulativeProbability(vpEndPos - centerPos) - nD.cumulativeProbability(vpStaPos - centerPos);
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

        /**
         *
         * @param refID reference sequence ID (eg, chr5)
         * @param pos central position of the viewpoint on the reference sequence
         */
        Builder(String refID, int pos) {
            this.chromosomeID = refID;
            this.genomicPos    = pos;
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
