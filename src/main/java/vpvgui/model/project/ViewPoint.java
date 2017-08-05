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
 * Capture-C.</p>
 * <p>
 * This class provides a set of utility functions that can be used for primarily for editing of the coordinates,
 * which can be either set manually or automatically using different (so far two) approaches.
 * The last editing step will be tracked.</p>
 * <p>
 * TODO, implement utility functions calculating characteristics of the viewpoint,
 * such as repetitive or GC content, or the number of restriction enzyme cutting sites.
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
    /** The viewpoint must be located within the interval [{@link #maxDistToGenomicPosUp},{@link #maxDistToGenomicPosDown}] with respect to {@link #startPos}. */
    private Integer maxDistToGenomicPosUp;
    /** The viewpoint must be located within the interval [{@link #maxDistToGenomicPosUp},{@link #maxDistToGenomicPosDown}] with respect to {@link #startPos}*/
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
    /** TODO define me */
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
     * The constructor sets  fields and creates a {@link CuttingPositionMap} object.
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
        logger.trace(String.format("Entering ViewPoint constructor for %s",getTargetName()));
        /* Set fields */
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




    /** This constructor is intended to be used by the builder. */
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
     * adding setters for each parameter with a nondefault value.
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
     * Initializes {@link #restSegListMap} with the information in
     * cuttingPatterns.
     */
    private void initRestrictionFragments() {
         /* Retrieve all restriction fragments within the viewpoint */
        this.restSegListMap = new HashMap<String, ArrayList<Segment>>();
        for (int i = 0; i < this.cuttingPatterns.length; i++) {
            ArrayList arrList = new ArrayList<Fragment>();
            restSegListMap.put(cuttingPatterns[i], arrList);
        }

        for (int i = 0; i < this.cuttingPatterns.length; i++) {
            for (int j = 0; j < cuttingPositionMap.getHashMapOnly().get(this.cuttingPatterns[i]).size() - 1; j++) {
                Segment restFrag=new Segment.Builder(referenceSequenceID,
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j)+1),
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j + 1)+1)).
                        fastaReader(fastaReader).marginSize(marginSize).build();
               /*
                Segment restFrag = new Segment(referenceSequenceID,
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j)+1),
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j + 1)+1),
                        false, fastaReader);
                        */
                restSegListMap.get(cuttingPatterns[i]).add(restFrag);
            }
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

    /*public final HashMap<String, ArrayList<Fragment>> getRestFragListMap() {
        return restFragListMap;
    }*/

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
     * <p style="color:red">This function is unfinished! ToDo do we still need this??</p>
     * This function was intended to support the selection of fragments through the GUI.
     * The implementation turned out to be more difficult than expected, especially the handling of exceptions.
     * This lead to a new concept of selected and unselected <i>Segments</i> which seems to be more suitable.
     *
     * @param fromThisPos genomic position.
     * @throws IntegerOutOfRangeException
     * @throws NoCuttingSiteFoundUpOrDownstreamException is thrown by the function <i>getNextCutPos()</i>.
     */
    public void extendFragmentWise(Integer fromThisPos) throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {

        /* The viewpoint will be extended up to the next cutting site. */

        /* CASE 1: If 'fromThisPos' is upstream of the interval ['genomicPos'-'maxDistToGenomicPosUp','genomicPos'+'maxDistToGenomicPosDown'],
                   the viewpoint extended to the outmost cutting site upstream within this interval. */

        if (fromThisPos + genomicPos < startPos) {
            startPos = cuttingPositionMap.getNextCutPos(fromThisPos, "up") + genomicPos;
        }

         /* CASE 2: If 'fromThisPos' is downstream of 'endPos' and within the ['genomicPos'-'maxDistToGenomicPosUp','genomicPos'+'maxDistToGenomicPosDown'],
                   'endPos' will be set to position of the next cutting site downstream of 'fromThisPos'. */

        if (endPos < fromThisPos + genomicPos) {
            endPos = cuttingPositionMap.getNextCutPos(fromThisPos, "down") + genomicPos;
        }


            /* CASE 3: If 'fromThisPos' is upstream of 'startPos' and within the interval ['genomicPos'-'maxDistToGenomicPosUp','genomicPos'+'maxDistToGenomicPosDown'],
                    'startPos' will be set to position of the next cutting site upstream of 'fromThisPos'. */

            /* CASE 4: If 'fromThisPos' within the interval [startPos,endPos],
                    nothing will happen. */



            /* CASE 5: If 'fromThisPos' is downstream of the interval ['genomicPos'-'maxDistToGenomicPosUp','genomicPos'+'maxDistToGenomicPosDown'],
                    the viewpoint extended in to the outmost cutting site downstream within this interval. */

    }

    /**
     * <p style="color:red">This function is unfinished!</p>
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
            // TODO: Throw exception
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
     * In this approach, the viewpoint is seen as a set of selected fragments within a given range around <i>genomicPos</i>.
     * Fragments can be discarded because they shorter, or because their margins a higher repetitive content than a given thresholds.
     *
     * @param fragNumUp   required number of fragments upstream of the fragment that contains <i>genomicPos</i> (e.g. 4).
     * @param fragNumDown required number of fragments downstream of the fragment that contains <i>genomicPos</i> (e.g. 4).
     * @param motif       recognition motif of the restriction enzyme (e.g. GATC). Use <i>ALL</i> to take into account the cutting sites of used enzymes.
     */
    public void generateViewpointLupianez(Integer fragNumUp,
                                          Integer fragNumDown,
                                          String motif) {

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
        logger.trace(String.format("found genomicPosFragIdx=%d",genomicPosFragIdx));
        // originating from the centralized fragment containing 'genomicPos' (included) go fragment-wise in UPSTREAM direction
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
            if (this.maxDistToGenomicPosUp < upLen) {
                segment.setSelected(false);
            }

            // set fragment to 'false', if required number of fragments has already been found
            if (fragNumUp + 1 <= fragCountUp) {
                segment.setSelected(false);
            }

            // TODO PeterH bitte kontrollieren--ich habe den Code vereinfacht.
            // set fragment to false, if one of the margins have a repeat content is higher than a given threshold
            /*ArrayList<Segment> SegMargins = restSegListMap.get(motif).get(i).getSegmentMargins(this.marginSize);
            for (int j = 0; j < SegMargins.size(); j++) {
                SegMargins.get(j).calculateRepeatContent(fastaReader);
                if (this.maximumRepeatContent < SegMargins.get(j).getRepeatContent()) {
                    restSegListMap.get(motif).get(i).setSelected(false);
                }
            }*/
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
        /* TODO Warum +1: sollen wir nicht sagen groesser gleich fragNumUp ? */
        if (fragCountUp < fragNumUp + 1) { // fragment containing 'genomicPos' is included in upstream direction, hence '+1'
            warnings += "WARNING: Could not find the required number of fragments (" + (fragNumUp + 1) + ") in upstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }

        // originating from the centralized fragment containing 'genomicPos' (excluded) go fragment-wise in DOWNSTREAM direction
        Integer fragCountDown = 0;
        for (int i = genomicPosFragIdx + 1; i < restSegListMap.get(motif).size(); i++) { // downstream
            Segment segment = restSegListMap.get(motif).get(i);
            // set fragment to 'false', if it is shorter than 'minFragSize'
            //Integer len = restSegListMap.get(motif).get(i).getEndPos() - restSegListMap.get(motif).get(i).getStartPos();
            Integer len=segment.length();
            if (len < minFragSize) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // set fragments to 'false' that are not entirely within the allowed range
            Integer downLen = genomicPos - segment.getEndPos();
            if (this.maxDistToGenomicPosDown < -downLen) {
                segment.setSelected(false);
            }

            // set fragment to 'false', if required number of fragments has already been found
            if (fragNumDown <= fragCountDown) {
                segment.setSelected(false);
            }

            // set fragment to false, if one of the margins have a repeat content is higher than a given threshold
            // TODO PeterH bitte kontrollieren, ich habe den Code vereinfacht--margin repeat content wird nun
            // ein fuer alle Mal im Constructor von Segment berechnet.
            /*
            ArrayList<Segment> SegMargins = restSegListMap.get(motif).get(i).getSegmentMargins(this.marginSize);
            for (int j = 0; j < SegMargins.size(); j++) {
                // Note commented out because the repeat content only needs tobe calculated by the constructor of Segment!
                //SegMargins.get(j).calculateRepeatContent(fastaReader);
                if (this.maximumRepeatContent < SegMargins.get(j).getRepeatContent() && restSegListMap.get(motif).get(i).isSelected()) {
                    restSegListMap.get(motif).get(i).setSelected(false);
                }
            }
            */
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
        //for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
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

        /* set derivation approach */

        setDerivationApproach("LUPIANEZ");
        calculateViewpointScore(motif);
        setResolved(resolved);
    }


    /**
     * Helper function for the calculation of the viewpoint score.
     *
     * @param dist
     * @param maxDistToGenomicPos
     * @return
     */
    public double getViewpointPositionDistanceScore(Integer dist, Integer maxDistToGenomicPos) {
        double sd = maxDistToGenomicPos/6;
        double mean = -3*sd;
        NormalDistribution nD = new NormalDistribution(mean,sd);
        double score = nD.cumulativeProbability(-dist);
        return score;
    }

    /**
     * This function calculates the viewpoint score and sets the field 'score' of this class.
     * The function is also intended to update the score.
     *
     * @param motif The restriction fragment used to cut this viewpoint.
     */
    public void calculateViewpointScore(String motif) {
        /*todo HACK refactor and remove this (See above for comment)*/
        currentMotif=motif;
        Double score = 0.0;

        /* iterate over all selected fragments */
        Integer posCnt = 0;
        //for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
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
