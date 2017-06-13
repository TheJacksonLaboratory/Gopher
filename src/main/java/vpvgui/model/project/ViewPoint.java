package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;

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
    private static HashMap<String,int[]> cuttingPositionMap = new HashMap<String,int[]>();

    /* Constructor */
    public ViewPoint(String referenceSequenceID, int tssPos, String[] cuttingPatterns, IndexedFastaSequenceFile fastaReader) {

        //setReferenceSequenceID(referenceSequenceID);
        System.out.println("Constructor!");
    }

    public static void main(String [ ] args) {

        /* create a hash of integer arrays */

        cuttingPositionMap.put("ACGT", new int[]{-4, 99});
        int[] arr1 = cuttingPositionMap.get("ACGT");
        System.out.println("ACGT" + '\t' + arr1[0]);
        System.out.println("ACGT" + '\t' + arr1[1]);

        cuttingPositionMap.put("GAATA", new int[]{5, -100});
        int[] arr2 = cuttingPositionMap.get("GAATA");
        System.out.println("GAATA" + '\t' + arr2[0]);
        System.out.println("GAATA" + '\t' + arr2[1]);
    }


    /* getter and setter functions */

    public StringProperty getReferenceSequenceID() {
        return referenceSequenceID;
    }

    public void setReferenceSequenceID(String referenceSequenceID) {
        this.referenceSequenceID.set(referenceSequenceID);
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
