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
    private StringProperty referenceSequenceID;

    /* Start and end position of the viewpoint */
    private IntegerProperty startPos, endPos;

    /* Symbol of the corresponding gene, e.g. BCL2 */
    private StringProperty geneSymbol;

    /* Coordinate of the original TSS */
    private IntegerProperty tssPos;

    /* Derivation approach, either combined (CA), Andrey et al. 2016 (AEA), or manually (M) */
    private StringProperty derivationApproach;

    /* A hash map with restriction cutting sites as key and arrays of integers as values.
     * The integer values are positions relative to the TSS. */
    private static HashMap<String,ArrayList<Integer>> cuttingPositionMap = new HashMap<String,ArrayList<Integer>>();


    /* Constructor */
    public ViewPoint(String rID, int tssPos, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader) {

        int initialRadius=100;

        /* Set referenceSequenceID */
        setReferenceID(rID);
        System.out.println(getReferenceID());

        /* Init hash of int arrays */

        for(int i=0;i<cuttingPatterns.length;i++) {
            String tssRegionString = fastaReader.getSubsequenceAt(rID,tssPos-initialRadius,tssPos+initialRadius).getBaseString(); // get sequence around TSS
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

    }


    /* getter and setter functions */

    public final String getReferenceID() {
        if (referenceSequenceID != null)
            return referenceSequenceID.get();
        return "x";
    }

    public final void setReferenceID(String rID) {
        this.referenceSequenceIDProperty().set(rID);
    }

    public final StringProperty referenceSequenceIDProperty() {
        if (referenceSequenceID == null) {
            referenceSequenceID = new SimpleStringProperty("");
        }
        return referenceSequenceID;
    }

    public final HashMap<String,ArrayList<Integer>> getCuttingPositionMap() {
        return cuttingPositionMap;
    }

    public IntegerProperty getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos.set(startPos);
    }

    public IntegerProperty getEndPos() {
        return endPos;
    }
}
