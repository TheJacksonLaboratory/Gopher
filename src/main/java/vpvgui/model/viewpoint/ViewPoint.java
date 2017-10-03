package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.log4j.Logger;
import vpvgui.model.Default;
import vpvgui.model.RestrictionEnzyme;

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
 * <p>
 * TODO, implement utility functions calculating characteristics of the viewpoint, such as repetitive or GC content,
 * or the number of restriction enzyme cutting sites.
 * <p>
 *
 * @author Peter N Robinson
 * @author Peter Hansen
 * @version 0.0.8 (2017-09-22)
 */
public class ViewPoint implements Serializable {
    private static final Logger logger = Logger.getLogger(ViewPoint.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 2L;
    /** The possible approaches used to generate this Viewpoint. */
    public static enum Approach {SIMPLE, EXTENDED};
    /* The approach used to generate this viewpoint */
    private Approach approach;
    /** Size of the "borders" at the edges of a fragment that are especially important because we sequence there. */
    private int marginSize;
    /** Maximum allowable repeat content for a fragment to be included. A fragment will be deselected
     * if one of the margins has a higher repeat content.*/
    private double maximumRepeatContent;
    /** "Home" of the viewpoint, usually a chromosome */
    private String referenceSequenceID;
    /** Name of the target of the viewpoint (often a gene).*/
    private String targetName;
    /** central genomic coordinate of the viewpoint, usually a transcription start site. One-based fully closed numbering */
    private Integer genomicPos;
    /** refers to the  the range around 'genomicPos' in which VPV searches initially for cutting positions (CuttingPositionMap).*/
    private Integer maxDistToGenomicPosUp;
    /** refers to the  the range around 'genomicPos' in which VPV searches initially for cutting positions (CuttingPositionMap).*/
    private Integer maxDistToGenomicPosDown;
    /** The viewpoint must be at least as large as the interval [{@link #minDistToGenomicPosUp},{@link #minDistToGenomicPosDown}] with respect to {@link #startPos}. */
    private Integer minDistToGenomicPosUp;
    /** The viewpoint must be at least as large as the interval [{@link #minDistToGenomicPosUp},{@link #minDistToGenomicPosDown}] with respect to {@link #startPos}*/
    private Integer minDistToGenomicPosDown;
    /** start position of the viewpoint */
    private Integer startPos;
    /** end position of the viewpoint */
    private Integer endPos;
    /** Minimum allowable size of a restriction fragment-this will usually be determined by the size of the probes
     * that are used for enrichment (e.g., 130 bp. */
    private int minFragSize;
    /** Derivation approach, either combined (CA), Andrey 2016 (AEA), or manually (M) */
//    private String derivationApproach;
    /** A viewpoint is marked as resolved, if it has the required number of segments after application of the function {@link #generateViewpointExtendedApproach}. */
    private boolean resolved;
    /** Number of fragments within the viewpoint that are selected for generating capture hi-c probes. */
//    private int numOfSelectedFrags=0;
    /** Data structure for storing cutting site position relative to 'genomicPos' */
    private SegmentFactory segmentFactory;
    /** List of active and inactive restriction {@link vpvgui.model.viewpoint.Segment} objects that are contained within the viewpoint. */
    private List<Segment> restrictionSegmentList;
    /** List of restriction enzymes chosen by the User. */
    static List<RestrictionEnzyme> chosenEnzymes=null;

    public static void setChosenEnzymes(List<RestrictionEnzyme> lst) { chosenEnzymes=lst;}
    /** A list of restriction enzymes (at least one) as chosen by the user. */
    public static RestrictionEnzyme getChosenEnzyme(int i) { return chosenEnzymes.get(i);}
    /** Warnings that occur during automatic generation of the viewpoint can be written to this variable. */
    private String warnings;
    /** Overall score of this Viewpoint.*/
    private Double score;
    /** Minimum allowable fragment size for the simple approach */
    private static final int SIMPLE_APPROACH_MINSIZE=146;
    /** Maximim allowable fragment size for simple approach */
    private static final int SIMPLE_APPROACH_MAXSIZE=20000;
    /** Minimum allowable GC content for simple approach */
    private static final double SIMPLE_APPROACH_MIN_GC=0.25;
    /** Maximum allowable GC content for simple approach */
    private static final double SIMPLE_APPROACH_MAX_GC=0.65;


    /**
     * Gets a list of all active (chosen) {@link Segment} objects.
     * @return a list of Segments of a viewpoint that are active and will be displayed on the UCSC Browser. */
    public List<Segment> getActiveSegments() {
        if (restrictionSegmentList==null) {
            logger.error(String.format("Error-- null list of restriction segments for %s",getTargetName()));
            return new ArrayList<Segment>();/* return empty list.*/
        }
        //return a List of all selected segments
        return this.restrictionSegmentList.stream().filter(s -> s.isSelected()).collect(Collectors.toList());
    }

    /** @return List of all segments (selected or not). */
   public List<Segment> getAllSegments() {
        return restrictionSegmentList;
   }

    /** @return A string with the length from the start of the first to end of the last active segment in kb */
   public double getActiveLength() {
       Integer from=Integer.MAX_VALUE;
       Integer to = Integer.MIN_VALUE;
       Integer combined=0;

       for (Segment seg:getActiveSegments()) {
           if (from > seg.getStartPos()) from = seg.getStartPos();
           if (to<seg.getEndPos()) to = seg.getEndPos();
           combined += seg.length();
       }
       Double len = to-from+1.0;
       return len;
    }

    /** @return a formated String representing the length of the ViewPoint in kb, e.g., 10;203 kb. */
    public String getActiveLengthInKilobases() {
       double len = getActiveLength();

        double lenInKb=len/1000; // kilobases
        return String.format("%s kb (all selected fragments: %s kb)",
                NumberFormat.getNumberInstance(Locale.US).format(len),
                NumberFormat.getNumberInstance(Locale.US).format(lenInKb));
    }

    /**
     * This constructor is used to zoom in or out by applying a zoom factor to an existing ViewPoint object.
     * @param vp An existing ViewPoint that is used as the center point of the zoom
     * @param zoomfactor Make the viewpoint bigger for zoom factor greater than 1. Make it smaller for factor less than 1
     * @param fastaReader The reader used to get new sequences.
     */

    public ViewPoint(ViewPoint vp, double zoomfactor,IndexedFastaSequenceFile fastaReader) {
        this.referenceSequenceID=vp.referenceSequenceID;
        this.genomicPos=vp.genomicPos;
        this.targetName=vp.targetName;
        this.maxDistToGenomicPosUp=(int)(vp.maxDistToGenomicPosUp*zoomfactor);
        this.maxDistToGenomicPosDown=(int)(vp.maxDistToGenomicPosDown*zoomfactor);
        this.minFragSize=vp.minFragSize;
        this.marginSize= vp.marginSize;
        this.maximumRepeatContent=vp.maximumRepeatContent;
        logger.trace(String.format("Constructing ViewPoint from Builder at Genomic Pos = %d",this.genomicPos));
        init(fastaReader);
    }




    /**
     * This constructor is intended to be used by the builder.
     *
     * @param builder Builder class aimed to make constructing a ViewPoint object unambiguous.
     */
    private ViewPoint(Builder builder){
        this.referenceSequenceID=builder.referenceSequenceID;
        this.genomicPos=builder.genomicPos;
        this.targetName=builder.targetName;
        this.maxDistToGenomicPosUp=builder.maxDistToGenomicPosUp;
        this.maxDistToGenomicPosDown=builder.maxDistToGenomicPosDown;
        this.minFragSize=builder.minFragSize;
        this.marginSize= builder.marginSize;
        this.maximumRepeatContent=builder.maximumRepeatContent;
        logger.trace(String.format("Constructing ViewPoint from Builder at Genomic Pos = %d",this.genomicPos));
        init(builder.fastaReader);
    }


    private void init(IndexedFastaSequenceFile fastaReader) {
        setStartPos(genomicPos - maxDistToGenomicPosUp);
        setEndPos(genomicPos + maxDistToGenomicPosDown);
        this.restrictionSegmentList=new ArrayList<>();
        setResolved(false);
        warnings="";
        /* Create segmentFactory */
        segmentFactory = new SegmentFactory(this.referenceSequenceID,
                this.genomicPos,
                fastaReader,
                this.maxDistToGenomicPosUp,
                this.maxDistToGenomicPosDown,
                ViewPoint.chosenEnzymes);
        logger.trace("The segment factory was initialized for genomic Pos "+ segmentFactory.getGenomicPos());
        initRestrictionFragments(fastaReader);
    }

    /**
     * A Builder class. To create a {@link ViewPoint} object, use code such as
     * <pre>
     *  refID="chr15";
     *  int gpos=48937985;
     *  ViewPoint vp = new ViewPoint.Builder(refID,gpos).targetName("FBN1").maxDistToGenomicPosUp(1500).build();
     * </pre>
     * adding setters for each parameter with a non-default value.
     */
    public static class Builder {
        //  parameters required in the constructor
        private String referenceSequenceID=null;
        private int genomicPos;
        // other params
        private IndexedFastaSequenceFile fastaReader;
        private  String targetName="";
        // Optional parameters - initialized to default values
        /* ToDo Check is this correct for maxDistToGenomicPosUp etc. ?*/
        private Integer maxDistToGenomicPosUp  = Default.MAXIMUM_SIZE_UPSTREAM+2000;
        private Integer maxDistToGenomicPosDown  = Default.MAXIMUM_SIZE_DOWNSTREAM+2000;
        private Integer minSizeUp=Default.MINIMUM_SIZE_UPSTREAM;
        private Integer maxSizeUp=Default.MAXIMUM_SIZE_UPSTREAM;
        private Integer minSizeDown=Default.MINIMUM_SIZE_DOWNSTREAM;
        private Integer maxSizeDown=Default.MAXIMUM_SIZE_DOWNSTREAM;
        private Integer minFragSize=Default.MINIMUM_FRAGMENT_SIZE;
        private double maximumRepeatContent=Default.MAXIMUM_REPEAT_CONTENT;
        private int marginSize=Default.MARGIN_SIZE;
        /** Derivation approach, either combined (CA), Andrey 2016 (AEA), or manually (M) */
        private String derivationApproach;


        /**
         *
         * @param refID reference sequence ID (eg, chr5)
         * @param pos central position of the viewpoint on the reference sequence
         */
        public Builder(String refID, int pos) {
            this.referenceSequenceID = refID;
            this.genomicPos    = pos;
            logger.trace(String.format("Builder for refID=%s at pos=%d",refID,pos));
        }

       public Builder maxDistToGenomicPosDown(int val)
        { maxDistToGenomicPosDown = val;    return this; }
        public Builder maxDistToGenomicPosUp(int val)
        { maxDistToGenomicPosUp = val;           return this; }
        public Builder targetName(String val)
        { targetName = val;  return this; }
        public Builder fastaReader(IndexedFastaSequenceFile val) {
            this.fastaReader=val; return this;
        }
        public Builder minimumSizeUp(int val) {
            this.minSizeUp=val; return this;
        }
        public Builder maximumSizeUp(int val) {
            this.maxSizeUp=val; return this;
        }
        public Builder minimumSizeDown(int val) {
            this.minSizeDown=val; return this;
        }
        public Builder maximumSizeDown(int val) {
            this.maxSizeDown=val; return this;
        }
        public Builder minimumFragmentSize(int val) {
            this.minFragSize=val; return this;
        }
        public Builder maximumRepeatContent(double val) {
            this.maximumRepeatContent=val; return this;
        }
        public Builder marginSize(int val) {
            this.marginSize=val; return this;
        }
        public ViewPoint build() {
            return new ViewPoint(this);
        }
    }


    /**
     * This function uses the information about cutting position sites from the {@link #segmentFactory}
     * to init a hash, in which the keys are the cutting motifs and the values are lists of objects of class
     * Segments.
     */
    private void initRestrictionFragments(IndexedFastaSequenceFile fastaReader) {
        this.restrictionSegmentList = new ArrayList<>();

        for (int j = 0; j < segmentFactory.getAllCuts().size() - 1; j++) {
            Segment restFrag = new Segment.Builder(referenceSequenceID,
                    segmentFactory.getUpstreamCut(j),
                    segmentFactory.getDownstreamCut(j) - 1).
                    fastaReader(fastaReader).marginSize(marginSize).build();
            restrictionSegmentList.add(restFrag);
        }
    }
    /** @return The reference ID of the reference sequence (usually, a chromosome) .*/
    public final String getReferenceID() {
        return referenceSequenceID;
    }

    /** @return the middle (anchor) position of this ViewPoint. */
    public final Integer getGenomicPos() {
        return genomicPos;
    }
    /** @return a string like chr4:29,232,796 */
    public String getGenomicLocationString() { return String.format("%s:%s",referenceSequenceID, NumberFormat.getNumberInstance(Locale.US).format(genomicPos));}


    public final void setMaxUpstreamGenomicPos(Integer maxDistToGenomicPosUp) {
        this.maxDistToGenomicPosUp = maxDistToGenomicPosUp;
    }

    public final Integer getMaxUpstreamGenomicPos() {
        return maxDistToGenomicPosUp;
    }


    public final void setMaxDownstreamGenomicPos(Integer maxDistToGenomicPosDown) {
        this.maxDistToGenomicPosDown = maxDistToGenomicPosDown;
    }

    public final Integer getMaxDownstreamGenomicPos() {
        return maxDistToGenomicPosDown;
    }

    /** @return overall score of this ViewPoint. TODO what about simple?     */
    public final double getScore() {
        return score;
    }
    public String getScoreAsPercentString() { return String.format("%.2f%%",100*score);}


    public final void setStartPos(Integer startPos) {
        this.startPos = startPos;
    }

    public final Integer getStartPos() {
        return startPos;
    }


    public final Integer getEndPos() {
        return endPos;
    }

    public final void setEndPos(Integer endPos) {
        this.endPos = endPos;
    }


    public final String getDerivationApproach() {
        return approach.toString();
    }

    public final void setDerivationApproach(Approach derivationApproach) {
        this.approach = derivationApproach;
    }


    public final boolean getResolved() {
        return resolved;
    }

    public final void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
    /** @return Number of Segments in this ViewPoint that are active (selected). */
    public final int getNumOfSelectedFrags() {
        return (int) this.restrictionSegmentList.stream().filter(s -> s.isSelected()).count();
    }

    public void setTargetName(String name) { this.targetName=name;}

    public String getTargetName() { return this.targetName; }

    public String toString() {
        return String.format("%s  [%s:%d-%d]",getTargetName(),getReferenceID(),getStartPos(),getEndPos());
    }


    /**
     * This function can be used to reshape the viewpoint according to rules that were developed in consultation with bench scientists.
     * In this approach, the viewpoint is seen as a set of selected fragments within a given range around {@link #genomicPos}.
     * Fragments can be discarded because they shorter, or because their margins a higher repetitive content than a given thresholds.
     *
     * @param fragNumUp    required number of fragments upstream of the fragment that contains {@link #genomicPos} (e.g. 4).
     * @param fragNumDown  required number of fragments downstream of the fragment that contains {@link #genomicPos} (e.g. 4).
     * @param maxSizeUp    upper limit for the distance between {@link #startPos} and {@link #genomicPos} (e.g. 5000).
     * @param maxSizeDown  upper limit for the distance between {@link #genomicPos} and {@link #endPos} (e.g. 5000).
     */
    public void generateViewpointExtendedApproach(Integer fragNumUp,
                                                  Integer fragNumDown,
                                                  Integer maxSizeUp,
                                                  Integer maxSizeDown) {

        boolean resolved = true;
        approach=Approach.EXTENDED;
        logger.trace("entering generateViewpointExtendedApproach");
        Segment centerSegment=null; // the fragment that contains the TSS. Always show it!

        // iterate over all fragments of the viewpoint and set them to true
        restrictionSegmentList.stream().forEach(segment -> segment.setSelected(true));

        // find the index of the fragment that contains genomicPos
        Integer genomicPosFragIdx = -1;
        for (int i=0;i< restrictionSegmentList.size();i++) {
            Segment segment  =restrictionSegmentList.get(i);
            Integer fragStaPos = segment.getStartPos();
            Integer fragEndPos = segment.getEndPos();
            if (fragStaPos <= genomicPos && genomicPos <= fragEndPos) {
                genomicPosFragIdx = i;
                centerSegment=segment;
                break;
            }
        }

        if (genomicPosFragIdx == -1) {
            logger.error("At least one fragment must contain 'genomicPos' (" + referenceSequenceID + ":" + startPos + "-" + endPos + ").");
            resolved = false;
            return;
        }

        // starting from the centralized fragment containing 'genomicPos' (included)
        // openExistingProject fragment-wise in UPSTREAM direction

        Integer fragCountUp = 0;
        for (int i = genomicPosFragIdx; 0 <= i; i--) { // upstream

            Segment segment = restrictionSegmentList.get(i);

            // set fragment to 'false', if it is shorter than 'getMinFragSize'
            if (segment.length() < this.minFragSize) {
                restrictionSegmentList.get(i).setSelected(false);
            }

            // set fragments to 'false' that are not entirely within the allowed range
            Integer upLen = genomicPos - segment.getStartPos();
            if (maxSizeUp < upLen) {
                segment.setSelected(false);
            }

            // set fragment to 'false', if required number of fragments has already been found
            if (fragNumUp + 1 <= fragCountUp) {
                segment.setSelected(false);
            }

            // set fragment to false, if one of the margins have a repeat content is higher than a given threshold
            if (segment.getRepeatContentMarginDown() > this.maximumRepeatContent) {
                segment.setSelected(false);
            } else if (segment.getRepeatContentMarginUp() > this.maximumRepeatContent) {
                segment.setSelected(false);
            }

            // if after all this the fragment is still selected, increase count
            if (segment.isSelected()) {
                fragCountUp++;
            }
        }

        if (fragCountUp < fragNumUp + 1) { // fragment containing 'genomicPos' is included in upstream direction, hence '+1'
            warnings += "WARNING: Could not find the required number of fragments (" + (fragNumUp + 1) + ") in upstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }

        // originating from the centralized fragment containing 'genomicPos' (excluded) openExistingProject fragment-wise in DOWNSTREAM direction

        Integer fragCountDown = 0;
        for (int i = genomicPosFragIdx + 1; i < restrictionSegmentList.size(); i++) { // downstream

            Segment segment = restrictionSegmentList.get(i);

            // set fragment to 'false', if it is shorter than 'getMinFragSize'
            if (segment.length() < minFragSize) {
                segment.setSelected(false);
            }

            // set fragments to 'false' that are not entirely within the allowed range
            Integer downLen = genomicPos - segment.getEndPos();
            if (maxSizeDown < -downLen) {
                segment.setSelected(false);
            }

            // set fragment to 'false', if required number of fragments has already been found
            if (fragNumDown <= fragCountDown) {
                segment.setSelected(false);
            }

            // set fragment to false, if one of the margins have a repeat content is higher than a given threshold
            if (segment.getRepeatContentMarginDown() > this.maximumRepeatContent) {
                segment.setSelected(false);
            } else if (segment.getRepeatContentMarginUp() > this.maximumRepeatContent) {
                segment.setSelected(false);
            }

            // if after all this the fragment is still selected, increase count
            if (segment.isSelected()) {
                fragCountDown++;
            }
        }

        if (fragCountDown < fragNumDown) {
            warnings += "WARNING: Could not find the required number of fragments (" + fragNumDown + ") in downstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }
        // set start position of the viewpoint to start position of the most upstream SELECTED fragment
        Segment firstSelectedSegment = restrictionSegmentList.stream().filter(segment -> segment.isSelected()).findFirst().orElse(null);
        if (firstSelectedSegment != null) {
            setStartPos(firstSelectedSegment.getStartPos());
        }
        int centerpos=centerSegment.getStartPos();
        if (this.startPos>centerpos) setStartPos(centerpos);
        // set end position of the viewpoint to end position of the most downstream fragment
        // Get the last active segment (i.e., most 3'/most downstream
        Segment lastSelectedSegment = restrictionSegmentList.stream().filter(segment->segment.isSelected()).reduce((a, b) -> b).orElse(null);
        if (lastSelectedSegment != null) {
            setEndPos(lastSelectedSegment.getEndPos());
        }
        centerpos=centerSegment.getEndPos();
        if (this.endPos<centerpos) setEndPos(centerpos);

        // discard fragments except for the selecxted fragments and their immediate neighbors, i.e.,
        // retain one unselected fragment on each end
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
        logger.trace(String.format("Reducing indices from (0,%d) to (%d,%d)",LEN,firstSelectedIndex,lastSelectedIndex));

        if (firstSelectedIndex+lastSelectedIndex==0) {
            logger.trace("Skipping trimming Segment List because no segments are selected for "+getTargetName());
        } else {
            int i = Math.max(0, firstSelectedIndex - 1);
            int j = Math.min(LEN, lastSelectedIndex + 2);// +2 because we want one more and range is (inclusive,exclusive)
            List<Segment> newlist = IntStream.range(i, j).boxed().map(k -> restrictionSegmentList.get(k)).collect(Collectors.toList());
            restrictionSegmentList = newlist;
        }




        setDerivationApproach(Approach.EXTENDED);
        calculateViewpointScore();
        logger.trace("Done calculated extended viewpoint; start pos of view point is "+getStartPos()+
                ", score="+getScoreAsPercentString());
        setResolved(resolved);
    }


    /**
     * TODO this function intends to replicate the logic of Justin's simple probe selection approach.
     * TODO Needs refactoring
     *
     */
    public void generateViewpointSimple() {
        boolean resolved = true;
        approach = Approach.SIMPLE;
        logger.trace("entering generateViewpointSimple");
        // find the fragment that contains genomicPos
        Segment centerSegment = restrictionSegmentList.stream().
                filter(segment -> segment.getStartPos() < genomicPos && segment.getEndPos() >= genomicPos).
                findFirst().
                orElse(null);

        if (centerSegment == null) {
            logger.error("At least one fragment must contain 'genomicPos' (" + referenceSequenceID + ":" + startPos + "-" + endPos + ").");
            resolved = false;
            restrictionSegmentList.clear(); /* no fragments */
        } else {

            // originating from the centralized fragment containing 'genomicPos' (included) openExistingProject fragment-wise in UPSTREAM direction
            double gc = centerSegment.getGCcontent();
            int length = centerSegment.length();
            if (gc >= SIMPLE_APPROACH_MIN_GC
                    && gc <= SIMPLE_APPROACH_MAX_GC
                    && length >= SIMPLE_APPROACH_MINSIZE
                    && length <= SIMPLE_APPROACH_MAXSIZE) {
                List<Segment> newsegs = new ArrayList<>();
                centerSegment.setSelected(true);
                setEndPos(centerSegment.getEndPos());
                setStartPos(centerSegment.getStartPos());
                // Add the selected fragment and the two neighboring fragments (which are deselected)
                int genomicPosFragIdx = restrictionSegmentList.indexOf(centerSegment);
                if (genomicPosFragIdx > 0) {
                    newsegs.add(restrictionSegmentList.get(genomicPosFragIdx - 1));
                }
                newsegs.add(centerSegment);
                if (genomicPosFragIdx < restrictionSegmentList.size() - 1) {
                    newsegs.add(restrictionSegmentList.get(genomicPosFragIdx + 1));
                }
                restrictionSegmentList.clear();
                restrictionSegmentList.addAll(newsegs);
                resolved = true;
            }
        }

        setDerivationApproach(Approach.SIMPLE);
        calculateViewpointScore();
        logger.trace("Done calculating Simple viewpoint, start pos of view point is " + getStartPos() +
                ", score=" + getScoreAsPercentString());
        setResolved(resolved);
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
    public double getViewpointPositionDistanceScore(Integer dist, Integer maxDistToGenomicPos) {
        double sd = maxDistToGenomicPos/6; // the factor 1/6 was chosen by eye
        double mean = -3*sd; // shifts the normal distribution, so that almost the entire area under the curve is to the left of the y-axis
        NormalDistribution nD = new NormalDistribution(mean,sd);
        double score = nD.cumulativeProbability(-dist);
        return score;
    }


    /**
     * This function calculates the viewpoint score and sets the field 'score' of this class.
     * The function is also intended to update the score.
     * <p>
     * The function iterates over all restriction segments of the viewpoint.
     * For selected segments a <i>position distance score</i> is calculated for each position.
     * The scores for all positions are summed up and in the end divided by the total number of positions for which
     * <i>position distance scores</i> were calculated.
     * <p>
     * The overall score for the viewpoint is again between 0 and 1.
     *
     */
    public void calculateViewpointScore() {
        Double score = 0.0;

        /* iterate over all selected fragments */

        Integer posCnt = 0;
        List<Segment> allFrags=restrictionSegmentList;
        for (Segment currentSegment : allFrags) {
            double repCont = 0;
            double positionScoreSumFragment = 0;

            if (currentSegment.isSelected()) {

                repCont=currentSegment.getMeanMarginRepeatContent();

                /* get position distance score for each position of the fragment */

                positionScoreSumFragment = 0;
                for (int j = currentSegment.getStartPos(); j < currentSegment.getEndPos(); j++) {
                    Integer dist = j - genomicPos;
                    if (dist < 0) {
                        positionScoreSumFragment += getViewpointPositionDistanceScore(-1 * dist, maxDistToGenomicPosUp);
                    } else {
                        positionScoreSumFragment += getViewpointPositionDistanceScore(dist, maxDistToGenomicPosDown);
                    }
                    posCnt++;
                }
            }
            score += (1 - repCont) * positionScoreSumFragment;
        }
        if (posCnt == 0) {
            this.score = 0.0;
        } else {
            this.score = score / posCnt;
        }
    }

    /** @return the total length of the Margins of all active segments of this ViewPoint. */
    public int getTotalMarginSize() {
        return getActiveSegments().stream().mapToInt(segment -> segment.getMarginSize()).sum();
    }

}
