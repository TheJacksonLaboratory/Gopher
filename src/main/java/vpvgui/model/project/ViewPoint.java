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
    /** "Home" of the viewpoint, usually a chromosome */
    private String referenceSequenceID;
    /** Name of the target of the viewpoint (often a gene).*/
    private String targetName;
    /** central genomic coordinate of the viewpoint, usually a trancription start site */
    private Integer genomicPos;
    /** The viewpoint must be located within the interval [maxDistToGenomicPosUp,maxDistToGenomicPosDown]. */
    private Integer maxDistToGenomicPosUp;
    private Integer maxDistToGenomicPosDown;
    /** start position of the viewpoint */
    private Integer startPos;
    /** end position of the viewpoint */
    private Integer endPos;
    /** Derivation approach, either combined (CA), Andrey et al. 2016 (AEA), or manually (M) */
    private String derivationApproach;
    /** TODO define me */
    private boolean resolved;
    /** Number of fragments withint the viewpoint that are selected for generatingcapture hi-c probes. */
    private Integer numOfSelectedFrags;
    /** Data structure for storing cutting site position relative to 'genomicPos' */
    private CuttingPositionMap cuttingPositionMap;
    /* List of restriction 'Fragment' objects that are within the viewpoint */
    //private HashMap<String, ArrayList<Fragment>> restFragListMap;
    private HashMap<String, ArrayList<Segment>> restSegListMap;
    /** Reference to the indexed FASTA file that corresponds to {@link #referenceSequenceID}.*/
    private IndexedFastaSequenceFile fastaReader;
    /** Array of restriction enzyme patterns. */
    private String[] cuttingPatterns;

    private String warnings;

    /**
     * Note -- currently a FAKE method for testing the GUI. TODO revise this
     * @return a list of Segments of a viewpoint that are active and will be displayed on the UCSC Browser. */
    public List<Segment> getActiveSegments() {
        List<Segment> segs=new ArrayList<>();
        if (restSegListMap==null) {
            int len=this.getEndPos()-this.startPos;
            int fakestart=this.startPos + len/10;
            int fakeend=this.startPos+ len/5;
            Segment fake = new Segment(this.referenceSequenceID,fakestart,fakeend,true); //(String referenceSequenceID, Integer startPos, Integer endPos, boolean selected)
            segs.add(fake);
            fakestart=this.startPos + 3*len/10;
            fakeend=this.startPos+ 4*len/10;
            fake = new Segment(this.referenceSequenceID,fakestart,fakeend,true); //(String referenceSequenceID, Integer startPos, Integer endPos, boolean selected)
            segs.add(fake);
            return segs;
        }
        for (ArrayList<Segment> seglst:restSegListMap.values()) {
            for (Segment seg:seglst) {
                if (seg.isSelected())
                   segs.add(seg);
            }
            break; /* For testing just take the first enzyme!! TODO CHECK THIS */
        }
        return segs;
    }



    /* constructor function */

    /**
     * This is the contructor of this class. It will set all fields create an <i>CuttingPositionMap</i> object.
     *
     * @param referenceSequenceID     name of the genomic sequence, e.g. <i>chr1</i>.
     * @param genomicPos              central position of the region for which the CuttingPositionMap is created.
     * @param maxDistToGenomicPosUp   maximal distance to 'genomicPos' in upstream direction.
     * @param maxDistToGenomicPosDown maximal distance to 'genomicPos' in downstream direction.
     * @param cuttingPatterns         array of cutting motifs, e.g. <i>A^AGCTT</i> for the resrtiction enzyme <i>HindIII</i>. The '^' indicates the cutting position within the motif.
     * @param fastaReader             indexed FASTA file corresponding to referenceSequenceID that has the sequence for restriction..
     */
    public ViewPoint(String referenceSequenceID,
                     Integer genomicPos,
                     Integer maxDistToGenomicPosUp,
                     Integer maxDistToGenomicPosDown,
                     String[] cuttingPatterns,
                     IndexedFastaSequenceFile fastaReader) {
        logger.trace(String.format("Entering ViewPoint constructor. referenceSequenceID=%s, genomicPos=%d, maxDistToGenomicPosUp=%d",referenceSequenceID,genomicPos,maxDistToGenomicPosUp));
        logger.trace(String.format(" maxDistToGenomicPosDown=%d, fastaReader=%s",maxDistToGenomicPosDown,genomicPos,fastaReader.toString()));
        /* Set fields */
        setReferenceID(referenceSequenceID);
        setGenomicPos(genomicPos);
        setStartPos(genomicPos - maxDistToGenomicPosUp);
        setEndPos(genomicPos + maxDistToGenomicPosDown);
        setDerivationApproach("INITIAL");
        setResolved(false);
        warnings="";
        setMaxUpstreamGenomicPos(maxDistToGenomicPosUp);
        setMaxDownstreamGenomicPos(maxDistToGenomicPosDown);
        this.fastaReader=fastaReader;
        this.cuttingPatterns=cuttingPatterns;

        /* Create cuttingPositionMap */
        initCuttingPositionMap();
        initRestrictionFragments();
    }


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
     * <p>
     * TODO -- need to execute this to finish initializing ViewPoint ojects made from GUI
     *
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
                Segment restFrag = new Segment(referenceSequenceID,
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j)+1),
                        relToAbsPos(cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j + 1)+1),
                        false);
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
     * <p style="color:red">This function is unfinished!</p>
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
        return String.format("%s: [%d-%d]",getReferenceID(),getStartPos(),getEndPos());
    }

    /**
     * This function can be used to reshape the viewpoint according to rules that were developed in consultation with bench scientists.
     * In this approach, the viewpoint is seen as a set of selected fragments within a given range around <i>genomicPos</i>.
     * Fragments can be discarded because they shorter, or because their margins a higher repetitive content than a given thresholds.
     *
     * @param fragNumUp   required number of fragments upstream of the fragment that contains <i>genomicPos</i> (e.g. 4).
     * @param fragNumDown required number of fragments downstream of the fragment that contains <i>genomicPos</i> (e.g. 4).
     * @param motif       recognition motif of the restriction enzyme (e.g. GATC). Use <i>ALL</i> to take into account the cutting sites of used enzymes.
     * @param minSizeUp   minimal number of base pairs upstream of <i>genomicPos</i>  (e.g. 1500 bp).
     * @param maxSizeUp   maximal number of base pairs upstream of <i>genomicPos</i>  (e.g. 5000 bp).
     * @param minSizeDown minimal number of base pairs downstream of <i>genomicPos</i> (e.g. 1500 bp).
     * @param maxSizeDown maximal number of base pairs downstream of <i>genomicPos</i> (e.g. 5000 bp).
     * @param minFragSize minimal allowed length of a fragment. Is oriented towards the lengths of probes (e.g. 130 bp).
     * @param maxRepFrag  threshold for repetitive content of the margins of the fragments (e.g. 0.4). The fragment is deselected, if one of the margins have a higher repetitive content.
     * @param marginSize  size of the margins of fragments. Only for these will later be used for probe selection.
     */
    public void generateViewpointLupianez(Integer fragNumUp,
                                          Integer fragNumDown,
                                          String motif,
                                          Integer minSizeUp,
                                          Integer maxSizeUp,
                                          Integer minSizeDown,
                                          Integer maxSizeDown,
                                          Integer minFragSize,
                                          double maxRepFrag,
                                          Integer marginSize) {

        boolean resolved = true;
        logger.trace("entering generateViewpointLupianez for motif="+motif);

        // iterate over all fragments of the viewpoint and set them to true
        for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
            restSegListMap.get(motif).get(i).setSelected(true);
        }

        // find the index of the fragment that contains genomicPos
        Integer genomicPosFragIdx = -1;
        for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
            Integer fragStaPos = restSegListMap.get(motif).get(i).getStartPos();
            Integer fragEndPos = restSegListMap.get(motif).get(i).getEndPos();
            if (fragStaPos <= genomicPos && genomicPos <= fragEndPos) {
                genomicPosFragIdx = i;
                break;
            }
        }
        if (genomicPosFragIdx == -1) {
            logger.error("ERROR: At least one fragment must contain 'genomicPos' (" + referenceSequenceID + ":" + startPos + "-" + endPos + ").");
            resolved = false;
        }

        // originating from the centralized fragment containing 'genomicPos' (included) go fragment-wise in UPSTREAM direction
        Integer fragCountUp = 0;
        for (int i = genomicPosFragIdx; 0 <= i; i--) { // upstream

            // set fragment to 'false', if it is shorter than 'minFragSize'
            Integer len = restSegListMap.get(motif).get(i).getEndPos() - restSegListMap.get(motif).get(i).getStartPos();
            if (len < minFragSize) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // set fragments to 'false' that are not entirely within the allowed range
            Integer upLen = genomicPos - restSegListMap.get(motif).get(i).getStartPos();
            if (maxSizeUp < upLen) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // set fragment to 'false', if required number of fragments has already been found
            if (fragNumUp + 1 <= fragCountUp) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // change this to: set fragment to false, if one of the margins have a repetitive content is higher than a given threshold
            ArrayList<Segment> SegMargins = restSegListMap.get(motif).get(i).getSegmentMargins(marginSize);
            for (int j = 0; j < SegMargins.size(); j++) {
                SegMargins.get(j).setRepetitiveContent(fastaReader);
                if (maxRepFrag < SegMargins.get(j).getRepetitiveContent()) {
                    restSegListMap.get(motif).get(i).setSelected(false);
                }
            }

            // if after all this the fragment is still selected, increase count
            if (restSegListMap.get(motif).get(i).isSelected() == true) {
                fragCountUp++;
            }
        }
        if (fragCountUp < fragNumUp + 1) { // fragment containing 'genomicPos' is included in upstream direction, hence '+1'
            warnings += "WARNING: Could not find the required number of fragments (" + (fragNumUp + 1) + ") in upstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }

        // originating from the centralized fragment containing 'genomicPos' (excluded) go fragment-wise in DOWNSTREAM direction
        Integer fragCountDown = 0;
        for (int i = genomicPosFragIdx + 1; i < restSegListMap.get(motif).size(); i++) { // downstream

            // set fragment to 'false', if it is shorter than 'minFragSize'
            Integer len = restSegListMap.get(motif).get(i).getEndPos() - restSegListMap.get(motif).get(i).getStartPos();
            if (len < minFragSize) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // set fragments to 'false' that are not entirely within the allowed range
            Integer downLen = genomicPos - restSegListMap.get(motif).get(i).getEndPos();
            if (maxSizeDown < downLen) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // set fragment to 'false', if required number of fragments has already been found
            if (fragNumDown <= fragCountDown) {
                restSegListMap.get(motif).get(i).setSelected(false);
            }

            // change this to: set fragment to false, if one of the margins have a repetitive content is higher than a given threshold
            ArrayList<Segment> SegMargins = restSegListMap.get(motif).get(i).getSegmentMargins(marginSize);
            for (int j = 0; j < SegMargins.size(); j++) {
                SegMargins.get(j).setRepetitiveContent(fastaReader);
                if (maxRepFrag < SegMargins.get(j).getRepetitiveContent() && restSegListMap.get(motif).get(i).isSelected()) {
                    restSegListMap.get(motif).get(i).setSelected(false);
                }
            }

            // if after all this the fragment is still selected, increase count
            if (restSegListMap.get(motif).get(i).isSelected() == true) {
                fragCountDown++;
            }
        }
        if (fragCountDown < fragNumDown) {
            warnings += "WARNING: Could not find the required number of fragments (" + fragNumDown + ") in downstream direction, only " + fragCountUp + " fragments were found at " + referenceSequenceID + ":" + startPos + "-" + endPos + ".";
            resolved = false;
        }
        numOfSelectedFrags = fragCountUp + fragCountDown;

        /* set start and end position of the viewpoint */

        // set start position of the viewpoint to start position of the most upstream fragment
        for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
            if (restSegListMap.get(motif).get(i).isSelected() == true) {
                setStartPos(restSegListMap.get(motif).get(i).getStartPos());
                break;
            }
        }

        // set end position of the viewpoint to end position of the most downstream fragment
        for (int i = restSegListMap.get(motif).size() - 1; 0 < i; i--) {
            if (restSegListMap.get(motif).get(i).isSelected() == true) {
                setEndPos(restSegListMap.get(motif).get(i).getEndPos());
                break;
            }
        }

        /* set derivation approach */

        setDerivationApproach("LUPIANEZ");
        setResolved(resolved);
    }


    public double getViewpointPositionDistanceScore(Integer dist, Integer maxDistToGenomicPos) {
        double sd = maxDistToGenomicPos/6;
        double mean = -3*sd;
        NormalDistribution nD = new NormalDistribution(mean,sd);
        double score = nD.cumulativeProbability(-dist);
        return score;
    }

    public double getViewpointScore(String motif, Integer marginSize) {

        double score = 0.0;

        /* iterate over all selected fragments */

        for (int i = 0; i < restSegListMap.get(motif).size(); i++) {
            double repCont = 0;
            double positionScoreSumFragment = 0;
            if (restSegListMap.get(motif).get(i).isSelected() == true) {

                /* get repetitive content of the fragment margins */

                repCont = 0;
                ArrayList<Segment> SegMargins = restSegListMap.get(motif).get(i).getSegmentMargins(marginSize);
                for (int j = 0; j < SegMargins.size(); j++) {
                    SegMargins.get(j).setRepetitiveContent(fastaReader);
                    repCont = repCont + SegMargins.get(j).getRepetitiveContent();
                }
                System.out.println(repCont);

                /* get position distance score for each position of the fragment */

                positionScoreSumFragment = 0;
                for (int j = restSegListMap.get(motif).get(i).getStartPos(); j < restSegListMap.get(motif).get(i).getEndPos(); j++) {
                    Integer dist = j - genomicPos;
                    if (dist < 0) {
                        positionScoreSumFragment = positionScoreSumFragment + getViewpointPositionDistanceScore(-1 * dist, maxDistToGenomicPosUp);
                    } else {
                        positionScoreSumFragment = positionScoreSumFragment + getViewpointPositionDistanceScore(dist, maxDistToGenomicPosDown);
                    }
                }
            }
            score = score + (1 - repCont) * positionScoreSumFragment;
        }
        return score;
    }
}
