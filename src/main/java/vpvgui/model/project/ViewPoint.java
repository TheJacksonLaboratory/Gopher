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
 * A region, usually at the transcription start site of a gene,
 * that will be enriched in a capture C experiment.
 * The start and end coordinates can be set manually or automatically.
 * In the latter case one of two approaches can be used.
 * Start and end coordinates can be altered after automatic generation.
 * The last approach of derivation will be tracked.
 */

public class ViewPoint {

    /* Usually a chromosome */
    private String referenceSequenceID;

    /* Coordinate of the original TSS */
    private Integer tssPos;

    /* Start and end position of the viewpoint */
    private Integer startPos, endPos;

    /* Symbol of the corresponding gene, e.g. BCL2 */
    private String geneSymbol;  // IN FACT, A VIEWPOINT SHOULD POINT TO A GENE OBJECT!

    /* Derivation approach, either combined (CA), Andrey et al. 2016 (AEA), or manually (M) */
    private StringProperty derivationApproach;

    /* A hash map with restriction cutting sites as key and arrays of integers as values.
     * The integer values are positions relative to the TSS. */
    private static HashMap<String,ArrayList<Integer>> cuttingPositionMap; // = new HashMap<String,ArrayList<Integer>>();


    /* Constructor fuction */
    public ViewPoint(String referenceSequenceID, Integer tssPos, Integer initialRadius, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader) {

        /* Set referenceSequenceID */
        setReferenceID(referenceSequenceID);
        System.out.println(getReferenceID());

        /* Init hash of int arrays */
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

    //----------------------------------------

    public final HashMap<String,ArrayList<Integer>> getCuttingPositionMap() {
        return cuttingPositionMap;
    }





    /* wrapper/helper functions */

    private HashMap<String,ArrayList<Integer>> createCuttingPositionMap(String referenceSequenceID, Integer tssPos, Integer initialRadius, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader) {

        HashMap<String,ArrayList<Integer>> cuttingPositionMap = new HashMap<String,ArrayList<Integer>>();

        for(int i=0;i<cuttingPatterns.length;i++) {

            String tssRegionString = fastaReader.getSubsequenceAt(referenceSequenceID,tssPos-initialRadius,tssPos+initialRadius).getBaseString(); // get sequence around TSS
            Pattern pattern = Pattern.compile(cuttingPatterns[i]);
            Matcher matcher = pattern.matcher(tssRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<Integer>(); //int[] cuttingPositions;

            int count = 0;
            while(matcher.find()) {
                count++;
                cuttingPositionList.add(matcher.start()-initialRadius); // push occurence positions relative to the TSS to list.
            }

            cuttingPositionMap.put(cuttingPatterns[i],cuttingPositionList); // push array list to map
        }

        return cuttingPositionMap;
    }


}
