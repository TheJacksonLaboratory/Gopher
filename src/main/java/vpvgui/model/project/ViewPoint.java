package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;

import javax.swing.text.View;
import java.util.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A region, usually at the transcription start site (TSS) of a gene, that will be enriched in a Capture-C experiment.
 * However, the region does not necessarily need to be at can be at TSS, it can be everywhere in the genome.
 * <p>
 * Essentially, a viewpoint consists of a start and end coordinate, and a map for restriction enzyme cutting sites
 * within the viewpoint, which play an important role for the design of viewpoints due to lab protocol of Capture-C.
 * <p>
 * This class provides a set of utility functions that can be used for primarily for editing of the coordinates,
 * which can be either set manually or automatically using different (so far two) approaches.
 * The last editing step will be tracked.
 * <p>
 * Furthermore, there will be utility functions calculating characteristics of the viewpoint,
 * such as repetitive or GC content, or the number of restriction enzyme cutting sites.
 * <p>
 *
 * @author Peter Nick Robinson
 * @author Peter Hansen
 */
public class ViewPoint {

    /* usually a chromosome */
    private String referenceSequenceID;

    /* central genomic coordinate of the viewpoint, usually a trancription start site */
    private Integer genomicPos;

    /* viewpoint cannot be outside the interval [maxDistToGenomicPosUp,maxDistToGenomicPosDown]. */
    private Integer maxDistToGenomicPosUp;
    private Integer maxDistToGenomicPosDown;

    /* start and end position of the viewpoint */
    private Integer startPos, endPos;

    /* derivation approach, either combined (CA), Andrey et al. 2016 (AEA), or manually (M) */
    private String derivationApproach;

    /* data structure for storing cutting site position relative to 'genomicPos' */
    private CuttingPositionMap cuttingPositionMap;

    /* list of restriction 'Fragment' objects that are within the viewpoint */
    private HashMap<String, ArrayList<Fragment>> restFragListMap;


    /* constructor function */

    /**
     * This is the contructor of this class. It will set all fields create an <i>CuttingPositionMap</i> object.
     *
     * @param referenceSequenceID     name of the genomic sequence, e.g. <i>chr1</i>.
     * @param genomicPos              central position of the region for which the CuttingPositionMap is created.
     * @param maxDistToGenomicPosUp   maximal distance to 'genomicPos' in upstream direction.
     * @param maxDistToGenomicPosDown maximal distance to 'genomicPos' in downstream direction.
     * @param cuttingPatterns         array of cutting motifs, e.g. <i>A^AGCTT</i> for the resrtiction enzyme <i>HindIII</i>. The '^' indicates the cutting position within the motif.
     * @param fastaReader             indexed FASTA file that contains the sequence information required for the calculation of cutting positions.
     */
    public ViewPoint(String referenceSequenceID, Integer genomicPos, Integer maxDistToGenomicPosUp, Integer maxDistToGenomicPosDown, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader) {

        /* Set fields */

        setReferenceID(referenceSequenceID);
        setGenomicPos(genomicPos);
        setStartPos(genomicPos - maxDistToGenomicPosUp);
        setEndPos(genomicPos + maxDistToGenomicPosDown);
        setDerivationApproach("INITIAL");

        /* Peter: ich habe denfolgenden Code in Funktionen ausgelagert, damit er von wonaders ausgerufen werden kann */
        initCuttingPositionMap(referenceSequenceID, genomicPos, fastaReader, maxDistToGenomicPosUp, maxDistToGenomicPosDown, cuttingPatterns);
        initRestrictionFragments(cuttingPatterns);

    }


    /**
     * This is the contructor of this class that is called from the input genes window.
     * It will set ONLY SOME of fields create an <i>CuttingPositionMap</i> object. We need
     * to subsequently add the restirction enzyme and IndexedFastaSequence and do the actual cutting.
     *
     * @param referenceSequenceID     name of the genomic sequence, e.g. <i>chr1</i>.
     * @param genomicPos              central position of the region for which the CuttingPositionMap is created.
     * @param maxDistToGenomicPosUp   maximal distance to 'genomicPos' in upstream direction.
     * @param maxDistToGenomicPosDown maximal distance to 'genomicPos' in downstream direction.
     */
    public ViewPoint(String referenceSequenceID, Integer genomicPos, Integer maxDistToGenomicPosUp, Integer maxDistToGenomicPosDown) {

        /* Set fields */

        setReferenceID(referenceSequenceID);
        setGenomicPos(genomicPos);
        setStartPos(genomicPos - maxDistToGenomicPosUp);
        setEndPos(genomicPos + maxDistToGenomicPosDown);
        setDerivationApproach("INITIAL");


    }

    /**
     * TODO -- need to execute this to finish initializing ViewPoint ojects made from GUI
     */
    public void initCuttingPositionMap(String referenceSequenceID, Integer genomicPos, IndexedFastaSequenceFile fastaReader, Integer maxDistToGenomicPosUp, Integer maxDistToGenomicPosDown, String[] cuttingPatterns) {
/* Create cuttingPositionMap */

        // TODO: Exception: genomicPos + maxDistToGenomicPosDown outside genomic range.
        // TODO: Handling: Set maxDistToGenomicPosDown to largest possible value and throw warning.

        // TODO: Exception: genomicPos.
        // TODO: Handling: Discard viewpoint and throw warning.
        cuttingPositionMap = new CuttingPositionMap(referenceSequenceID, genomicPos, fastaReader, maxDistToGenomicPosUp, maxDistToGenomicPosDown, cuttingPatterns);

    }


    /**
     * Initializes {@link #restFragListMap} with the information in
     * cuttingPatterns.
     * <p>
     * TODO -- need to execute this to finish initializing ViewPoint ojects made from GUI
     *
     * @param cuttingPatterns
     */
    private void initRestrictionFragments(String[] cuttingPatterns) {
         /* Retrieve all restriction fragments within the viewpoint */

        this.restFragListMap = new HashMap<String, ArrayList<Fragment>>();
        for (int i = 0; i < cuttingPatterns.length; i++) {
            ArrayList arrList = new ArrayList<Fragment>();
            restFragListMap.put(cuttingPatterns[i], arrList);
        }

        for (int i = 0; i < cuttingPatterns.length; i++) {
            for (int j = 0; j < cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).size() - 1; j++) {
                Fragment restFrag = new Fragment("dirpath", cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j), cuttingPositionMap.getHashMapOnly().get(cuttingPatterns[i]).get(j + 1), false);
                restFragListMap.get(cuttingPatterns[i]).add(restFrag);
            }
        }
    }




    /* getter and setter functions */

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


    public Integer getGenomicPosOfGenomicRelativePos(Integer genomicPos, Integer genomicPosRelPos) {
        return this.genomicPos + genomicPosRelPos;
    }

    public final CuttingPositionMap getCuttingPositionMap() {
        return cuttingPositionMap;
    }

    public final HashMap<String, ArrayList<Fragment>> getRestFragListMap() {
        return restFragListMap;
    }

    ;


    /* wrapper/helper functions */

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
     * This function converts absolute coordinates of the genomic sequence to coordinates relative to <i>genomicPos</i>.
     *
     * @param absPos
     * @return the coordinate relative to <i>genomicPos</i>.
     */
    public Integer absToRelPos(Integer absPos) {
        return absPos - genomicPos;
    }

    /**
     * This function converts coordinates relative to <i>genomicPos</i> to absolute coordinates in the genomic sequence.
     *
     * @param relPos
     * @return
     */
    public Integer relToAbsPos(Integer relPos) {
        return relPos + genomicPos;
    }


    /**
     * This function converts absolute genomic coordinates to absolute coordinates within the viewpoint (<i>startPos</i> corresponds to 0).
     *
     * @param absPos
     * @return
     */
    public Integer absToVpIdxPos(Integer absPos) {
        return absPos - startPos;
    }

    /**
     * This function converts absolute coordinates within the viewpoint to absolute genomic coordinates.
     *
     * @param vpIdx
     * @return
     */
    public Integer vpIdxToAbsPos(Integer vpIdx) {
        return vpIdx + startPos;
    }

    /**
     * This function converts coordinates relative to <i>genomicPos</i> to absolute coordinates within the viewpoint.
     *
     * @param relPos
     * @return
     */
    public Integer relToVpIdxPos(Integer relPos) {
        return relPos - startPos + genomicPos;
    }

    /**
     * This function converts absolute coordinates within the viewpoint to coordinates relative to <i>genomicPos</i>.
     *
     * @param vpIdx
     * @return
     */
    public Integer vpIdxToRelPos(Integer vpIdx) {
        return vpIdx - genomicPos + startPos;
    }


    public void selectOrDeSelectFragment(Integer pos) {

        /* find the fragments that contains 'pos' */

        Object[] cuttingPatterns = restFragListMap.keySet().toArray();

        boolean found = false;
        for (int i = 0; i < cuttingPatterns.length; i++) {
            for (int j = 0; j < restFragListMap.get(cuttingPatterns[i]).size(); j++) {
                Integer sta = restFragListMap.get(cuttingPatterns[i]).get(j).getStartPos();
                Integer end = restFragListMap.get(cuttingPatterns[i]).get(j).getEndPos();
                if (sta < pos && pos < end) {
                    restFragListMap.get(cuttingPatterns[i]).get(j).setSelected(!restFragListMap.get(cuttingPatterns[i]).get(j).getSelected());
                    found = true;
                }
            }
        }
        if (found) {
            System.out.println("hooray!");
        }


/*
        boolean found=false;
        for (int i = 0; i< getRestFragListMap().get(motif).size(); i++ ) {

        }
*/
        //vp.getRestFragListMap().get(motif).
    }


    public String toString() {
        return String.format("%s: [%d-%d]",getReferenceID(),getStartPos(),getEndPos());
    }

}




