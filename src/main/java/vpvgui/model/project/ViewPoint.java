package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.log4j.Logger;
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;

import java.util.*;

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
 * TODO, implement utility functions calculating characteristics of the viewpoint, such as repetitive or GC content, or the number of restriction enzyme cutting sites.
 * <p>
 *
 * @author Peter N Robinson
 * @author Peter Hansen
 * @version 0.0.4 (2017-07-24)
 */
public class ViewPoint {
    static Logger logger = Logger.getLogger(ViewPoint.class.getName());
    /** Size of the "borders" at the edges of a fragment that are especially important because we sequence there. */
    private int marginSize;
    /** Maximum allowable repeat content for a fragment to be included. A fragment will be deselected
     * if one of the margins has a higher repeat content.*/
    private double maximumRepeatContent;
    /** "Home" of the viewpoint, usually a chromosome */
    private String referenceSequenceID;
    /** Name of the target of the viewpoint (often a gene).*/
    private String targetName;
    /** central genomic coordinate of the viewpoint, usually a trancription start site */
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
    private String derivationApproach;
    /** A viewpoint is marked as resolved, if it has the required number of segments after application of the function {@link #generateViewpointLupianez}. */
    private boolean resolved;
    /** Number of fragments within the viewpoint that are selected for generating capture hi-c probes. */
    private Integer numOfSelectedFrags;
    /** Data structure for storing cutting site position relative to 'genomicPos' */
    private CuttingPositionMap cuttingPositionMap;
    /* List of restriction 'Fragment' objects that are within the viewpoint */
    private HashMap<String, ArrayList<Segment>> restSegListMap;
    /** Reference to the indexed FASTA file that corresponds to {@link #referenceSequenceID}.*/
    private IndexedFastaSequenceFile fastaReader;
    /** Array of restriction enzyme patterns. */
    private String[] cuttingPatterns;
    /** Warnings that occur during automatic generation of the viewpoint can be written to this variable. */
    private String warnings;
    /** Overall score of this Viewpoint.*/
    private Double score;
    /** TODO Hack -- we need to refactor, but this will keep us at the last used restriction fragment as
     * passed to the Lupianez method so that we can recalculate the score from the GUI.
     */
    private String currentMotif=null;

    /**
     * Gets a list of all active (chosen) {@link Segment} objects.
     * @return a list of Segments of a viewpoint that are active and will be displayed on the UCSC Browser. */
    public List<Segment> getActiveSegments() {
        List<Segment> segs=new ArrayList<>();
        if (restSegListMap==null) {
            logger.error(String.format("Error-- null list of restriction segments for %s",getTargetName()));
            return segs;/* return empty list.*/
        }
        for (ArrayList<Segment> seglst:restSegListMap.values()) {
            for (Segment seg:seglst) {
                if (seg.isSelected())
                   segs.add(seg);
            }
        }
        return segs;
    }


    /**
     * The constructor sets fields and creates a {@link CuttingPositionMap} object.
     *
     * @param referenceSequenceID     name of the genomic sequence, e.g. {@code chr1}.
     * @param genomicPos              central position of the region for which the CuttingPositionMap is created.
     * @param maxDistToGenomicPosUp   maximal distance to 'genomicPos' in upstream direction.
     * @param maxDistToGenomicPosDown maximal distance to 'genomicPos' in downstream direction.
     * @param cuttingPatterns         array of cutting motifs, e.g. {@code A^AGCTT} for the restriction enzyme {@code HindIII}. The '^' indicates the cutting position within the motif.
     * @param fastaReader             indexed FASTA file corresponding to referenceSequenceID that has the sequence for restriction..
     */
    public ViewPoint(String referenceSequenceID,
                     Integer genomicPos,
                     Integer maxDistToGenomicPosUp,
                     Integer maxDistToGenomicPosDown,
                     String[] cuttingPatterns,
                     IndexedFastaSequenceFile fastaReader) {

        setReferenceID(referenceSequenceID);
        setGenomicPos(genomicPos);
        this.fastaReader=fastaReader;
        this.cuttingPatterns=cuttingPatterns;
        setMaxUpstreamGenomicPos(maxDistToGenomicPosUp);
        setMaxUpstreamGenomicPos(maxDistToGenomicPosUp);
        setMaxDownstreamGenomicPos(maxDistToGenomicPosDown);
        init();
    }


    private void init() {

        setStartPos(genomicPos - maxDistToGenomicPosUp);
        setEndPos(genomicPos + maxDistToGenomicPosDown);
        setDerivationApproach("INITIAL");
        setResolved(false);
        warnings="";

        /* Create cuttingPositionMap */
        initCuttingPositionMap();
        initRestrictionFragments();
    }


    /**
     * This constructor is intended to be used by the builder.
     *
     * @param builder TODO: Describe 'builder'
     */
    private ViewPoint(Builder builder){
        this.referenceSequenceID=builder.referenceSequenceID;
        this.genomicPos=builder.genomicPos;
        this.targetName=builder.targetName;
        this.maxDistToGenomicPosUp=builder.maxDistToGenomicPosUp;
        this.maxDistToGenomicPosDown=builder.maxDistToGenomicPosDown;
        this.cuttingPatterns=builder.cuttingPatterns;
        this.fastaReader=builder.fastaReader;
        this.minFragSize=builder.minFragSize;
        this.maxDistToGenomicPosUp=builder.maxDistToGenomicPosUp;
        this.maxDistToGenomicPosDown=builder.maxDistToGenomicPosDown;
        this.minDistToGenomicPosUp=builder.minSizeUp;
        this.minDistToGenomicPosDown=builder.minSizeDown;
        this.marginSize= builder.marginSize;
        this.maximumRepeatContent=builder.maximumRepeatContent;
        init();
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
        private Integer maxDistToGenomicPosUp  = 2000;
        private Integer maxDistToGenomicPosDown  = 2000;
        private String[] cuttingPatterns;
        private Integer minSizeUp=1500;
        private Integer maxSizeUp=4000;
        private Integer minSizeDown=1500;
        private Integer maxSizeDown=4000;
        private Integer minFragSize=120;
        private double maximumRepeatContent=0.6;
        private int marginSize=250;


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
        }

        public Builder maxDistToGenomicPosDown(int val)
        { maxDistToGenomicPosDown = val;    return this; }
        public Builder maxDistToGenomicPosUp(int val)
        { maxDistToGenomicPosUp = val;           return this; }
        public Builder targetName(String val)
        { targetName = val;  return this; }
        public Builder cuttingPatterns(String [] val) {
            this.cuttingPatterns=val; return this;
        }
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


    /** Initialize {@link #cuttingPositionMap} on the basis of the chosen enzyme cutting patterns in {@link #cuttingPatterns}.*/
    public void initCuttingPositionMap() {
        // TODO: Exception: genomicPos + maxDistToGenomicPosDown outside genomic range.
        // TODO: Handling: Set maxDistToGenomicPosDown to largest possible value and throw warning.
        // TODO: Exception: genomicPos.
        // TODO: Handling: Discard viewpoint and throw warning.
        cuttingPositionMap = new CuttingPositionMap(this.referenceSequenceID,
                this.genomicPos,
                this.fastaReader,
                this.maxDistToGenomicPosUp,
                this.maxDistToGenomicPosDown,
                this.cuttingPatterns);
    }


    /**
     * This function uses the information about cutting position sites from the {@link #cuttingPositionMap}
     * to init a hash ({@link #restSegListMap}), in which the keys are the cutting motifs and the values are lists of objects of class
     * Segments.
     */
    private void initRestrictionFragments() {

        this.restSegListMap = new HashMap<String, ArrayList<Segment>>();
        for (int i = 0; i < this.cuttingPatterns.length; i++) {
            ArrayList arrList = new ArrayList<Segment>();
            restSegListMap.put(cuttingPatterns[i], arrList);
        }
        ArrayList arrList = new ArrayList<Segment>();
        restSegListMap.put("ALL", arrList);

        for (int i = 0; i < this.cuttingPatterns.length; i++) {
            for (int j = 0; j < cuttingPositionMap.getHashMapOnly().get(this.cuttingPatterns[i]).size() - 1; j++) {
                Segment restFrag=new Segment.Builder(referenceSequenceID,
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j)+1),
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j + 1))).
                        fastaReader(fastaReader).marginSize(marginSize).build();
                restSegListMap.get(cuttingPatterns[i]).add(restFrag);
            }
        }

        /* finally add the segments for the key 'ALL', the combination of cutting sites derived from all motifs */
        for (int j = 0; j < cuttingPositionMap.getHashMapOnly().get("ALL").size() - 1; j++) {
            Segment restFrag=new Segment.Builder(referenceSequenceID,
                    relToAbsPos(cuttingPositionMap.getHashMapOnly().get("ALL").get(j)+1),
                    relToAbsPos(cuttingPositionMap.getHashMapOnly().get("ALL").get(j + 1))).
                    fastaReader(fastaReader).marginSize(marginSize).build();
            restSegListMap.get("ALL").add(restFrag);
        }
    }


    /* --------------------------- */
    /* getter and setter functions */
    /* --------------------------- */

    public final String getReferenceID() {
        return referenceSequenceID;
    }

    public final void setReferenceID(String setReferenceID) {
        this.referenceSequenceID = setReferenceID;
    }


    public final void setGenomicPos(Integer genomicPos) {
        this.genomicPos = genomicPos;
    }

    public final Integer getGenomicPos() {
        return genomicPos;
    }

    public String getGenomicLocationString() { return String.format("%s:%d",referenceSequenceID,genomicPos);}


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


    public final void setScore(double score) {
        this.score = score;
    }

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
        return derivationApproach;
    }

    public final void setDerivationApproach(String derivationApproach) {
        this.derivationApproach = derivationApproach;
    }


    public final boolean getResolved() {
        return resolved;
    }

    public final void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public final Integer getNumOfSelectedFrags() {
        return numOfSelectedFrags;
    }

    public final String getWarnings() {
        return warnings;
    }


    public void setTargetName(String name) { this.targetName=name;}

    public String getTargetName() { return this.targetName; }


    public Integer getGenomicPosOfGenomicRelativePos(Integer genomicPos, Integer genomicPosRelPos) {
        return this.genomicPos + genomicPosRelPos;
    }

    public final CuttingPositionMap getCuttingPositionMap() {
        return cuttingPositionMap;
    }


    public final HashMap<String, ArrayList<Segment>> getRestSegListMap() {
        return restSegListMap;
    }

    public final ArrayList<Segment> getSelectedRestSegList(String cuttingMotif) {
        ArrayList<Segment> selectedRestSegList = new ArrayList<Segment>();
        for(Segment seg : restSegListMap.get(cuttingMotif)) {
            if(seg.isSelected()==true) {
                selectedRestSegList.add(seg);
            }
        }
        return selectedRestSegList;
    }



    /* ------------------------ */
    /* wrapper/helper functions */
    /* ------------------------ */


    /**
     * Given a position the fragment containing this position will be selected if it is selected and the other way around.
     * @param pos genomic position.
     */
    public void selectOrDeSelectFragment(Integer pos) {

        /* find the fragments that contains 'pos' */

        Object[] cuttingPatterns = restSegListMap.keySet().toArray();

        boolean found = false;
        for (int i = 0; i < cuttingPatterns.length; i++) {
            for (int j = 0; j < restSegListMap.get(cuttingPatterns[i]).size(); j++) {
                Integer sta = restSegListMap.get(cuttingPatterns[i]).get(j).getStartPos();
                Integer end = restSegListMap.get(cuttingPatterns[i]).get(j).getEndPos();
                if (sta < pos && pos < end) {
                    restSegListMap.get(cuttingPatterns[i]).get(j).setSelected(!restSegListMap.get(cuttingPatterns[i]).get(j).isSelected());
                    found = true;
                }
            }
        }
        if (!found) {
            logger.error("ERROR: At least one fragment must contain 'genomicPos' (" + referenceSequenceID + ":" + startPos + "-" + endPos + ").");
        }
    }


    /**
     * This function converts absolute coordinates of the genomic sequence to coordinates relative to <i>genomicPos</i>.
     *
     * @param absPos   absolute genomic position.
     * @return         the coordinate relative to <i>genomicPos</i>.
     */
    public Integer absToRelPos(Integer absPos) {
        return absPos - genomicPos;
    }

    /**
     * This function converts coordinates relative to <i>genomicPos</i> to absolute coordinates in the genomic sequence.
     *
     * @param relPos   position relative to <i>genomicPos</i>.
     * @return         absolute genomic position.
     */
    public Integer relToAbsPos(Integer relPos) {
        return relPos + genomicPos;
    }


    /**
     * This function converts absolute genomic coordinates to absolute coordinates within the viewpoint (<i>startPos</i> corresponds to 0).
     *
     * @param absPos   absolute genomic position.
     * @return         postion within the viewpoint.
     */
    public Integer absToVpIdxPos(Integer absPos) {
        return absPos - startPos;
    }

    /**
     * This function converts absolute coordinates within the viewpoint to absolute genomic coordinates.
     *
     * @param vpIdx    position within the viewpoint.
     * @return         absolute genomic position.
     */
    public Integer vpIdxToAbsPos(Integer vpIdx) {
        return vpIdx + startPos;
    }

    /**
     * This function converts coordinates relative to <i>genomicPos</i> to absolute coordinates within the viewpoint.
     *
     * @param relPos   position relative to <i>genomicPos</i>.
     * @return         position within the viewpoint.
     */
    public Integer relToVpIdxPos(Integer relPos) {
        return relPos - startPos + genomicPos;
    }

    /**
     * This function converts absolute coordinates within the viewpoint to coordinates relative to <i>genomicPos</i>.
     *
     * @param vpIdx    position within the viewpoint.
     * @return         position relative to <i>genomicPos</i>.
     */
    public Integer vpIdxToRelPos(Integer vpIdx) {
        return vpIdx - genomicPos + startPos;
    }

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
     * @param motif        recognition motif of the restriction enzyme (e.g. GATC). Use <i>ALL</i> to take into account the cutting sites of used enzymes.
     * @param maxSizeUp    upper limit for the distance between {@link #startPos} and {@link #genomicPos} (e.g. 5000).
     * @param maxSizeDown  upper limit for the distance between {@link #genomicPos} and {@link #endPos} (e.g. 5000).
     */
    public void generateViewpointLupianez(Integer fragNumUp,
                                          Integer fragNumDown,
                                          String motif,
                                          Integer maxSizeUp,
                                          Integer maxSizeDown) {

        boolean resolved = true;
        logger.trace("entering generateViewpointLupianez for motif="+motif);

        // iterate over all fragments of the viewpoint and set them to true
        for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
            restSegListMap.get(motif).get(i).setSelected(true);
        }

        // find the index of the fragment that contains genomicPos
        Integer genomicPosFragIdx = -1;
        for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
            Segment segment = restSegListMap.get(motif).get(i);
            Integer fragStaPos = segment.getStartPos();
            Integer fragEndPos = segment.getEndPos();
            if (fragStaPos <= genomicPos && genomicPos <= fragEndPos) {
                genomicPosFragIdx = i;
                break;
            }
        }

        if (genomicPosFragIdx == -1) {
            logger.error("ERROR: At least one fragment must contain 'genomicPos' (" + referenceSequenceID + ":" + startPos + "-" + endPos + ").");
            resolved = false;
        }

        // originating from the centralized fragment containing 'genomicPos' (included) openExistingProject fragment-wise in UPSTREAM direction

        Integer fragCountUp = 0;
        for (int i = genomicPosFragIdx; 0 <= i; i--) { // upstream

            Segment segment = restSegListMap.get(motif).get(i);

            // set fragment to 'false', if it is shorter than 'minFragSize'
            Integer len = segment.length();
            if (len < this.minFragSize) {
                restSegListMap.get(motif).get(i).setSelected(false);
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
            if (segment.isSelected() == true) {
                fragCountUp++;
            }
        }

        if (fragCountUp < fragNumUp + 1) { // fragment containing 'genomicPos' is included in upstream direction, hence '+1'
            warnings += "WARNING: Could not find the required number of fragments (" + (fragNumUp + 1) + ") in upstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }

        // originating from the centralized fragment containing 'genomicPos' (excluded) openExistingProject fragment-wise in DOWNSTREAM direction

        Integer fragCountDown = 0;
        for (int i = genomicPosFragIdx + 1; i < restSegListMap.get(motif).size(); i++) { // downstream

            Segment segment = restSegListMap.get(motif).get(i);

            // set fragment to 'false', if it is shorter than 'minFragSize'
            if (segment.length() < minFragSize) {
                restSegListMap.get(motif).get(i).setSelected(false);
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
            if (segment.isSelected() == true) {
                fragCountDown++;
            }
        }

        if (fragCountDown < fragNumDown) {
            warnings += "WARNING: Could not find the required number of fragments (" + fragNumDown + ") in downstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }
        numOfSelectedFrags = fragCountUp + fragCountDown;

        /* set start and end position of the viewpoint */

        // set start position of the viewpoint to start position of the most upstream SELECTED fragment
        for (Segment segment : restSegListMap.get(motif)) {
            if (segment.isSelected() == true) {
                setStartPos(segment.getStartPos());
                break;
            }
        }

        // set end position of the viewpoint to end position of the most downstream fragment
        for (int i = restSegListMap.get(motif).size() - 1; --i >= 0;) {
            Segment segment = restSegListMap.get(motif).get(i);
            if (segment.isSelected() == true) {
                setEndPos(segment.getEndPos());
                break;
            }
        }

        setDerivationApproach("LUPIANEZ");
        calculateViewpointScore(motif);
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
     * @param motif The restriction fragment used to cut this viewpoint.
     */
    public void calculateViewpointScore(String motif) {
        /*todo HACK refactor and remove this (See above for comment)*/
        currentMotif=motif;
        Double score = 0.0;

        /* iterate over all selected fragments */

        Integer posCnt = 0;
        for (Segment currentSegment : restSegListMap.get(motif)) {

            double repCont = 0;
            double positionScoreSumFragment = 0;

            if (currentSegment.isSelected() == true) {

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

    public void calculateViewpointScore() {
        calculateViewpointScore(this.currentMotif);
    }

}
