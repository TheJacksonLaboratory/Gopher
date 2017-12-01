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
 * @version 0.2.2 (2017-11-12)
 */
public class ViewPoint implements Serializable {
    private static final Logger logger = Logger.getLogger(ViewPoint.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 4L;
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
    private String chromosomeID;
    /** Accession number of this gene, e.g., NM_0001234 .*/
    private String accession=null;
    /** Name of the target of the viewpoint (often a gene).*/
    private String targetName;
    /** central genomic coordinate of the viewpoint, usually a transcription start site. One-based fully closed numbering */
    private int genomicPos;
    /** refers to the  the range around 'genomicPos' in which VPV searches initially for cutting positions (CuttingPositionMap).
     * Note that this is defined with respect to the strand -- it is "reversed" for genes on the - strand. */
    private int upstreamNucleotideLength;
    /** refers to the  the range around 'genomicPos' in which VPV searches initially for cutting positions (CuttingPositionMap).*/
    private int downstreamNucleotideLength;
    /** start position of the viewpoint */
    private int startPos;
    /** end position of the viewpoint */
    private int endPos;
    /** Minimum allowable size of a restriction fragment-this will usually be determined by the size of the probes
     * that are used for enrichment (e.g., 130 bp. */
    private int minFragSize;
    /** Maximum allowable GC content */
    private double maxGcContent;
    /** Minimum allowable GC content */
    private double minGcContent;
    /** Is the gene on the forward (positive) strand). */
    private boolean isPositiveStrand;
    /** A viewpoint is marked as resolved, if it has the required number of segments after application of the function {@link #generateViewpointExtendedApproach}. */
    private boolean resolved;
    /** Data structure for storing cutting site position relative to 'genomicPos' */
    private SegmentFactory segmentFactory;
    /** List of active and inactive restriction {@link vpvgui.model.viewpoint.Segment} objects that are contained within the viewpoint. */
    private List<Segment> restrictionSegmentList;
    /** List of restriction enzymes chosen by the User. */
    static List<RestrictionEnzyme> chosenEnzymes=null;
    /** The "number" of the promoter for the gene in question. */
    private int promoterNumber;
    /** Total number of promoters associated with this gene. */
    private int totalPromoters;
    /** This is a reference to the segment that overlaps the TSS */
    private Segment centerSegment=null;

    public void setPromoterNumber(int n, int total) { promoterNumber=n; totalPromoters=total;}

    public int getPromoterNumber(){return promoterNumber;}
    public int getTotalPromoterCount(){return totalPromoters;}

    public int getDownstreamNucleotideLength() {
        return downstreamNucleotideLength;
    }

    public int getUpstreamNucleotideLength() {
        return upstreamNucleotideLength;
    }

    public static void setChosenEnzymes(List<RestrictionEnzyme> lst) { chosenEnzymes=lst;}
    /** A list of restriction enzymes (at least one) as chosen by the user. */
    public static RestrictionEnzyme getChosenEnzyme(int i) { return chosenEnzymes.get(i);}
    /** Overall score of this Viewpoint.*/
    private double score;
    /** Maximim allowable fragment size for simple approach */
    private static final int SIMPLE_APPROACH_MAXSIZE=20_000;

    public String getAccession() {
        return accession;
    }

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

    /** @return a formated String representing the length of the ViewPoint in kb, e.g., 10;203 kb. */
    public String getTotalAndActiveLengthAsString() {
       double lenInKb=(double)getTotalLengthOfActiveSegments()/1000; // kilobases
        return String.format("%.2f kb (all selected fragments: %.2f kb)", (double) getTotalLengthOfViewpoint()/1000,lenInKb);
    }

    /**
     * This constructor is used to zoom in or out by applying a zoom factor to an existing ViewPoint object.
     * @param vp An existing ViewPoint that is used as the center point of the zoom
     * @param zoomfactor Make the viewpoint bigger for zoom factor greater than 1. Make it smaller for factor less than 1
     * @param fastaReader The reader used to get new sequences.
     */

    public ViewPoint(ViewPoint vp, double zoomfactor,IndexedFastaSequenceFile fastaReader) {
        this.chromosomeID =vp.chromosomeID;
        this.genomicPos=vp.genomicPos;
        this.targetName=vp.targetName;
        this.upstreamNucleotideLength =(int)(vp.upstreamNucleotideLength *zoomfactor);
        this.downstreamNucleotideLength =(int)(vp.downstreamNucleotideLength *zoomfactor);
        setStartPos(genomicPos - upstreamNucleotideLength);
        setEndPos(genomicPos + downstreamNucleotideLength);
        this.maxGcContent=vp.maxGcContent;
        this.minGcContent=vp.minGcContent;
        this.minFragSize=vp.minFragSize;
        this.marginSize= vp.marginSize;
        this.accession=vp.accession;
        this.isPositiveStrand=vp.isPositiveStrand;
        this.maximumRepeatContent=vp.maximumRepeatContent;
        logger.error(String.format("max rep %.2f maxGC %.2f  minGC %.2f",this.maximumRepeatContent,this.maxGcContent,this.minGcContent ));
        init(fastaReader);
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
        init(builder.fastaReader);
    }

    /**
     * TODO We need to know what strand in order to calculate start/end pos!!!!!!
     * @param fastaReader
     */
    private void init(IndexedFastaSequenceFile fastaReader) {
        this.restrictionSegmentList=new ArrayList<>();
        setResolved(false);
        /* Create segmentFactory */
        segmentFactory = new SegmentFactory(this.chromosomeID,
                this.genomicPos,
                fastaReader,
                this.upstreamNucleotideLength,
                this.downstreamNucleotideLength,
                ViewPoint.chosenEnzymes);
        initRestrictionFragments(fastaReader);
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
        private String chromosomeID =null;
        private String accessionNr=null;
        private int genomicPos;
        // other params
        private IndexedFastaSequenceFile fastaReader;
        private String targetName="";
        // Optional parameters - initialized to default values
        /* upstream nucleotide length for fragment generation (upstream of genomic pos).*/
        private Integer upstreamNtLength = Default.SIZE_UPSTREAM;
        /* downstream nucleotide length for fragment generation (downstream of genomic pos).*/
        private Integer downstreamNtLength = Default.SIZE_DOWNSTREAM;
        /** Need to choose a default strand, but this will always be overwritten. */
        private boolean isPositiveStrand =true;
        private Integer minFragSize=Default.MINIMUM_FRAGMENT_SIZE;
        private double maximumRepeatContent=Default.MAXIMUM_REPEAT_CONTENT;
        private double maxGcContent=Default.MAX_GC_CONTENT;
        private double minGcContent=Default.MIN_GC_CONTENT;
        private int marginSize=Default.MARGIN_SIZE;

        /**
         *
         * @param refID reference sequence ID (eg, chr5)
         * @param pos central position of the viewpoint on the reference sequence
         */
        public Builder(String refID, int pos) {
            this.chromosomeID = refID;
            this.genomicPos    = pos;
        }
        public Builder targetName(String val)
        { targetName = val;  return this; }
        public Builder fastaReader(IndexedFastaSequenceFile val) {
            this.fastaReader=val; return this;
        }
        public Builder maximumGcContent(double maxGC) {
            maxGcContent=maxGC; return this;
        }
        public Builder isForwardStrand(boolean strand) {
           this.isPositiveStrand = strand;
            return this;
        }
        public Builder minimumGcContent(double minGC) {
            this.minGcContent=minGC; return this;
        }
        public Builder accessionNr(String acc) {
            this.accessionNr=acc;return this;
        }
        public Builder downstreamLength(int val) {
            this.downstreamNtLength=val; return this;
        }
        public Builder upstreamLength(int val) {
            this.upstreamNtLength=val; return this;
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
     * This function uses the information about cutting position sites from the {@link #segmentFactory} to build
     * a list of {@link Segment} objects in {@link #restrictionSegmentList}.
     */
    private void initRestrictionFragments(IndexedFastaSequenceFile fastaReader) {
        this.restrictionSegmentList = new ArrayList<>();
        for (int j = 0; j < segmentFactory.getAllCuts().size() - 1; j++) {
            Segment restFrag = new Segment.Builder(chromosomeID,
                    segmentFactory.getUpstreamCut(j),
                    segmentFactory.getDownstreamCut(j) - 1).
                    fastaReader(fastaReader).marginSize(marginSize).build();
            restrictionSegmentList.add(restFrag);
        }
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
    /** @return overall score of this ViewPoint. TODO what about simple?     */
    public final double getScore() {
        return score;
    }
    public String getScoreAsPercentString() { return String.format("%.2f%%",100*score);}


    private final void setStartPos(Integer startPos) {
        this.startPos = startPos;
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
    /** @return true if this viewpoint has at least one active (selected) probe and it is resolved. */
    public final boolean hasValidProbe() {
        return getNumOfSelectedFrags()>0 && resolved;
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
     * This function can be used to reshape the viewpoint according to rules that were developed in consultation with bench scientists.
     * In this approach, the viewpoint is seen as a set of selected fragments within a given range around {@link #genomicPos}.
     * Fragments can be discarded because they shorter, or because their margins a higher repetitive content than a given thresholds.
     *
     * @param maxSizeUp    upper limit for the distance between {@link #startPos} and {@link #genomicPos} (e.g. 5000).
     * @param maxSizeDown  upper limit for the distance between {@link #genomicPos} and {@link #endPos} (e.g. 5000).
     */
    public void generateViewpointExtendedApproach(Integer maxSizeUp, Integer maxSizeDown, boolean allowSingleMargin) {
        if(!this.isPositiveStrand) {
            Integer tmp=maxSizeUp;
            maxSizeUp=maxSizeDown;
            maxSizeDown=tmp;
        }

        boolean resolved = true;
        approach=Approach.EXTENDED;
        this.centerSegment=null; // the fragment that contains the TSS. Always show it!
        restrictionSegmentList.stream().forEach(segment -> segment.setSelected(true));

        for (Segment segment : restrictionSegmentList) {
            if (segment.getStartPos() <= genomicPos && genomicPos <= segment.getEndPos()) {
                this.centerSegment = segment; segment.setOverlapsTSS(true); break;
            }
        }
        if (this.centerSegment==null) {
            logger.error("center segment NUll for " + getTargetName());
        }


        for (Segment segment:restrictionSegmentList) {
            if (segment.length() < this.minFragSize) {
                segment.setSelected(false);
            } else if (maxSizeUp < genomicPos - segment.getStartPos()) {
                segment.setSelected(false);
            } else if (maxSizeDown > genomicPos + segment.getEndPos()) {
                segment.setSelected(false);
            } else if ( allowSingleMargin &&
                    (segment.getRepeatContentMarginDown() > this.maximumRepeatContent ||
                     segment.getGcContentMarginDown() > this.maxGcContent)
                    &&
                    (segment.getRepeatContentMarginUp() > this.maximumRepeatContent ||
                     segment.getGcContentMarginUp() > this.maxGcContent)
                    ) {
                segment.setSelected(false);
            } else if (!allowSingleMargin && (segment.getRepeatContentMarginDown() > this.maximumRepeatContent || segment.getRepeatContentMarginUp() > this.maximumRepeatContent)) {
                segment.setSelected(false);
            } else if (!allowSingleMargin && (segment.getGcContentMarginDown() > this.maxGcContent || segment.getGcContentMarginUp() > this.maxGcContent)) {
                segment.setSelected(false);
            } else if (segment.getGCcontent() > this.maxGcContent || segment.getGCcontent() < this.minGcContent) {
                segment.setSelected(false);
            } else {
                resolved=true; // at least one segment OK, thus ViewPoint is OK
            }
        }

        // set start position of the viewpoint to start position of the most upstream SELECTED fragment
        int start=Integer.MAX_VALUE;
        int end=Integer.MIN_VALUE;

        for (Segment seg:restrictionSegmentList) {
            if (!seg.isSelected()) continue;
            if (start >seg.getStartPos()) start=seg.getStartPos();
            if (end < seg.getEndPos()) end=seg.getEndPos();
        }
        setStartPos(start);
        setEndPos(end);

//        restrictionSegmentList.stream().filter(segment -> segment.isSelected()).forEach(segment -> { });


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


        if (firstSelectedIndex+lastSelectedIndex==0) {
            logger.error("Skipping trimming Segment List because no segments are selected for "+getTargetName());
        } else {
            int i = Math.max(0, firstSelectedIndex - 1);
            int j = Math.min(LEN, lastSelectedIndex + 2);// +2 because we want one more and range is (inclusive,exclusive)
            List<Segment> newlist = IntStream.range(i, j).boxed().map(k -> restrictionSegmentList.get(k)).collect(Collectors.toList());
            restrictionSegmentList = newlist;
        }




        setDerivationApproach(Approach.EXTENDED);
        calculateViewpointScore();
        setResolved(resolved);
    }


    /**
     * TODO this function intends to replicate the logic of Justin's simple probe selection approach.
     * TODO Needs CHANGE for allowSingleMargin!!
     *
     */
    public void generateViewpointSimple(boolean allowSingleMargin) {
        boolean resolved = true;
        approach = Approach.SIMPLE;
        // find the fragment that contains genomicPos
        this.centerSegment = restrictionSegmentList.stream().
                filter(segment -> segment.getStartPos() < genomicPos && segment.getEndPos() >= genomicPos).
                findFirst().
                orElse(null);

        if (this.centerSegment == null) {
            logger.error(String.format("%s At least one fragment must contain 'genomicPos' (%s:%d-%d)", getTargetName(), chromosomeID, startPos , endPos ));
            resolved = false;
            restrictionSegmentList.clear(); /* no fragments */
        } else {
            this.centerSegment.setOverlapsTSS(true);
            // originating from the centralized fragment containing 'genomicPos' (included) openExistingProject fragment-wise in UPSTREAM direction
            double gc = centerSegment.getGCcontent();
            double repeatUp = centerSegment.getRepeatContentMarginUp();
            double repeatDown = centerSegment.getRepeatContentMarginDown();
            int length = centerSegment.length();
            if (gc >= this.minGcContent
                    && gc <= this.maxGcContent
                    && length >= this.minFragSize
                    && length <= SIMPLE_APPROACH_MAXSIZE
                    && repeatUp <= this.maximumRepeatContent
                    && repeatDown <= this.maximumRepeatContent) {
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
     * The scores for all positions are summed up and in the end divided by the sum of all
     * <i>position distance scores</i> for all positions within the interval
     * [genomicPos - upstreamNucleotideLength; genomicPos + downstreamNucleotideLength ].
     * <p>
     * The overall score for the viewpoint is between 0 and 1.
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
                        positionScoreSumFragment += getViewpointPositionDistanceScore(-1 * dist, upstreamNucleotideLength);
                    } else {
                        positionScoreSumFragment += getViewpointPositionDistanceScore(dist, downstreamNucleotideLength);
                    }
                    posCnt++;
                }
            }
            score += (1 - repCont) * positionScoreSumFragment;
        }

        /* calculate reference: all position within specified range covered, no repeats */

        double positionScoreSumRef = 0;
        for(int i = genomicPos-upstreamNucleotideLength; i<genomicPos+downstreamNucleotideLength; i++) {

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
    public void calculateViewpointScore_v1() {


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
                        positionScoreSumFragment += getViewpointPositionDistanceScore(-1 * dist, upstreamNucleotideLength);
                    } else {
                        positionScoreSumFragment += getViewpointPositionDistanceScore(dist, downstreamNucleotideLength);
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

    /** @return the total length of all active segments of this ViewPoint. */
    public Integer getTotalLengthOfActiveSegments() {
        return getActiveSegments().stream().mapToInt(segment -> segment.length()).sum();
    }

    /**
     * This function is intended to help confirmDialog the right
     * portion of the viewpoint in the UCSC browser.
     * It is either the start position of the first selected segment or the
     * deafult upstream position if no segment was selected.
     * @return the 5' position that we want to confirmDialog.
     */
    public int getMinimumSelectedPosition() {
        return getActiveSegments().stream().mapToInt(s -> s.getStartPos()).min().orElse(this.genomicPos-upstreamNucleotideLength);
    }
    /**
     * This function is intended to help confirmDialog the right
     * portion of the viewpoint in the UCSC browser.
     * It is either the end position of the last selected segment or the
     * deafult downstream position if no segment was selected.
     * @return the 5' position that we want to confirmDialog.
     */
    public int getMaximumSelectedPosition() {
        return getActiveSegments().stream().mapToInt(s -> s.getEndPos()).max().orElse(this.genomicPos+downstreamNucleotideLength);
    }

    /** Returns the leftmost position to confirmDialog in the UCSC browser. This is either the boundary of the initial search region, or
     * in the case where a selected fragment overlaps that boundary, the start pos of that fragment.
     * @return leftmost confirmDialog position
     */
    public int getMinimumDisplayPosition() { return Math.min(getMinimumSelectedPosition(),this.genomicPos-upstreamNucleotideLength); }
    /** Returns the rightmost position to confirmDialog in the UCSC browser. This is either the boundary of the initial search region, or
     * in the case where a selected fragment overlaps that boundary, the end pos of that fragment.
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
     * @return
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



}
