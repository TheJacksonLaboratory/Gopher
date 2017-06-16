package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*
 A region, usually at the transcription start site of a gene,
 that will be enriched in a capture C experiment.
 The start and end coordinates can be set manually or automatically.
 In the latter case one of two approaches can be used.
 Start and end coordinates can be altered after automatic generation.
 The last approach of derivation will be tracked.
 */

public class ViewPoint {

    /* Usually a chromosome */
    private String referenceSequenceID;

    /* Coordinate of the original TSS */
    private Integer tssPos;

    /* The viewpoint cannot be outside the interval [maxUpstreamTssPos,maxDownPos]. */
    private Integer maxUpstreamTssPos;
    private Integer maxDownstreamTssPos;

    /* Start and end position of the viewpoint */
    private Integer startPos, endPos;

    /* Symbol of the corresponding gene, e.g. BCL2 */
    private String geneSymbol;  // IN FACT, A VIEWPOINT SHOULD POINT TO A GENE OBJECT!

    /* Derivation approach, either combined (CA), Andrey et al. 2016 (AEA), or manually (M) */
    private String derivationApproach;

    /* A hash map with restriction cutting sites as key and arrays of integers as values.
     * The integer values are positions relative to the TSS. */
    private static HashMap<String,ArrayList<Integer>> cuttingPositionMap; // = new HashMap<String,ArrayList<Integer>>();


    /* Constructor fuction */

    public ViewPoint(String referenceSequenceID, Integer tssPos, Integer initialRadius, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader, String geneSymbol) {

        /* Set fields */

        setReferenceID(referenceSequenceID);
        setTssPos(tssPos);
        setStartPos(tssPos - initialRadius);
        setEndPos(tssPos + initialRadius);
        setGeneSymbol(geneSymbol);
        setDerivationApproach("INITIAL");

        /* Create hash of int arrays */

        cuttingPositionMap=createCuttingPositionMap(referenceSequenceID, tssPos, initialRadius, cuttingPatterns, fastaReader);
    }


    /* getter and setter functions */

    public final String getReferenceID() {
            return referenceSequenceID;
    }

    public final void setReferenceID(String setReferenceID) {
        this.referenceSequenceID=setReferenceID;
    }


    public final void setTssPos(Integer tssPos) {
        this.tssPos=tssPos;
    }

    public final Integer getTssPos() {
        return tssPos;
    }


    public final void setMaxUpstreamTssPos(Integer maxUpstreamTssPos) { this.maxUpstreamTssPos=maxUpstreamTssPos; }

    public final Integer getMaxUpstreamTssPos() { return maxUpstreamTssPos; }


    public final void setMaxDownstreamTssPos(Integer maxDownstreamTssPos) {
        this.maxDownstreamTssPos=maxDownstreamTssPos;
    }

    public final Integer getMaxDownstreamTssPos() {
        return maxDownstreamTssPos;
    }


    public final void setStartPos(Integer startPos) {
        this.startPos=startPos;
    }

    public final Integer getStartPos() {
        return startPos;
    }


    public final Integer getEndPos() {
        return endPos;
    }

    public final void setEndPos(Integer endPos) {
        this.endPos=endPos;
    }


    public final String getGeneSymbol() { return geneSymbol;    }

    public final void setGeneSymbol(String geneSymbol) { this.geneSymbol=geneSymbol;    }


    public final String getDerivationApproach() { return derivationApproach;    }

    public final void setDerivationApproach(String derivationApproach) { this.derivationApproach=derivationApproach;    }

    public Integer getGenomicPosFromTssRelativePos(Integer tssPos,Integer tssRelPos) {
        return this.tssPos+tssRelPos;
    }


    //----------------------------------------

    public final HashMap<String,ArrayList<Integer>> getCuttingPositionMap() {
        return cuttingPositionMap;
    }





    /* wrapper/helper functions */

    private HashMap<String,ArrayList<Integer>> createCuttingPositionMap(String referenceSequenceID, Integer tssPos, Integer initialRadius, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader) {

        HashMap<String,ArrayList<Integer>> cuttingPositionMap = new HashMap<String,ArrayList<Integer>>();

        for(int i=0;i<cuttingPatterns.length;i++) {

            String tssRegionString = fastaReader.getSubsequenceAt(referenceSequenceID,tssPos-initialRadius,tssPos+initialRadius).getBaseString().toUpperCase(); // get sequence around TSS

            Pattern pattern = Pattern.compile(cuttingPatterns[i]);
            Matcher matcher = pattern.matcher(tssRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<Integer>();

            while(matcher.find()) {
                cuttingPositionList.add(matcher.start()-initialRadius); // push occurence positions relative to the TSS to list.
            }

            cuttingPositionMap.put(cuttingPatterns[i],cuttingPositionList); // push array list to map
        }

        return cuttingPositionMap;
    }

    private void extendFragmentWise(Integer fromThisPos){

        /* The viewpoint will be extended up to the next cutting site. */

            /* CASE 1: If 'fromThisPos' is upstream of the interval ['tssPos'-'initialRadius','tssPos'+'initialRadius'],
                    the viewpoint extended to the outmost cutting site upstream within this interval. */

            /* CASE 2: If 'fromThisPos' is upstream of 'startPos' and within the ['tssPos'-'initialRadius','tssPos'+'initialRadius'],
                    'startPos' will be set to position of the next cutting site upstream of 'fromThisPos'. */

            /* CASE 3: If 'fromThisPos' within the interval [startPos,endPos],
                    nothing will happen. */

            /* CASE 4: If 'fromThisPos' is downstream of 'endPos' and within the [tssPos-initialRadius,tssPos+initialRadius],
                    'endPos' will be set to position of the next cutting site downstream of 'fromThisPos'. */

            /* CASE 5: If 'fromThisPos' is downstream of the interval ['tssPos'-'initialRadius','tssPos'+'initialRadius'],
                    the viewpoint extended in to the outmost cutting site downstream within this interval. */


    }




}
