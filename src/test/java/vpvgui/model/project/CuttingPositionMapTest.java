package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * Created by phansen on 6/19/17.
 */
public class CuttingPositionMapTest {


    /* test fields */

    String testReferenceSequenceID="chr4_ctg9_hap1";;
    Integer testGenomicPos=20000;;
    Integer testMaxDistToTssUp=150;
    Integer testMaxDistToTssDown=50;


    /* create CuttingPositionMap object for testing */

    private String[] testCuttingPatterns = new String[]{"ACTTT^TA","AAAC^CACTTAC","G^AG"};

    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);

    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);

    CuttingPositionMap testCuttingPositionMap = new CuttingPositionMap(testFastaReader, testReferenceSequenceID, testGenomicPos, testCuttingPatterns, testMaxDistToTssUp, testMaxDistToTssDown);

    public CuttingPositionMapTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?

    /* test constructor */

    @Test
    public void testFields() throws Exception {
        assertEquals(testReferenceSequenceID,testCuttingPositionMap.getReferenceID());
        assertEquals(testGenomicPos,testCuttingPositionMap.getGenomicPos());
        assertEquals(testMaxDistToTssUp,testCuttingPositionMap.getMaxDistToTssUp());
        assertEquals(testMaxDistToTssDown,testCuttingPositionMap.getMaxDistToTssDown());
    }

    @Test
    public void testHashMap() throws Exception {

        // print the initial sequence
        System.out.println();
        System.out.println("Sequence around 'genomicPos':");
        String tssRegionString = testFastaReader.getSubsequenceAt(testReferenceSequenceID, testGenomicPos - testMaxDistToTssUp, testGenomicPos + testMaxDistToTssDown).getBaseString(); // get sequence around genomic position
        System.out.println(tssRegionString);
        String tssRegionStringUpper = tssRegionString.toUpperCase();
        System.out.println();
        System.out.println("Sequence around 'genomicPos' uppercas only:");
        System.out.println(tssRegionStringUpper);

        // print genomic position
        String s = new String("");
        for (int k = 0; k < testMaxDistToTssUp; k++) { s += " "; }
        s += "|";
        System.out.println(s);
        s = "";
        for (int k = 0; k < testMaxDistToTssUp-1; k++) { s += " "; }
        s += "TSS";
        System.out.println(s);

        // print cutting motif occurrences only for individual motifs
        for (int i = 0; i < testCuttingPatterns.length; i++) {
            ArrayList<Integer> relPosIntArray = testCuttingPositionMap.getCuttingPositionMap().get(testCuttingPatterns[i]);
            for (int j = 0; j < relPosIntArray.size(); j++) {
                s = "";
                for (int k = 0; k < relPosIntArray.get(j)+testMaxDistToTssUp; k++) {
                    s += " ";
                }
                s += testFastaReader.getSubsequenceAt(testReferenceSequenceID, testGenomicPos + relPosIntArray.get(j), testGenomicPos + relPosIntArray.get(j) + testCuttingPatterns[i].length() - 1).getBaseString();
                s += " ";
                s += relPosIntArray.get(j);
                System.out.println(s);
            }
        }

        // print out all cutting sites
        s = "";
        for (int i = 0; i < testCuttingPositionMap.getCuttingPositionMap().get("ALL").size(); i++) {
            s = "";
            for (int j = 0; j < testCuttingPositionMap.getCuttingPositionMap().get("ALL").get(i)+testMaxDistToTssUp; j++) {
                s += " ";
            }
            s += "|";
            s += testCuttingPositionMap.getCuttingPositionMap().get("ALL").get(i);
            System.out.println(s);
        }
    }




    /* setter and getter functions */

    @Test
    public void testSetAndGetReferenceID() throws Exception {
        testCuttingPositionMap.setReferenceID(testReferenceSequenceID);
        assertEquals(testReferenceSequenceID,testCuttingPositionMap.getReferenceID());
    }

    @Test
    public void testSetAndGetGenomicPos() throws Exception {
        testCuttingPositionMap.setGenomicPos(testGenomicPos);
        assertEquals(testGenomicPos,testCuttingPositionMap.getGenomicPos());
    }

    @Test
    public void testSeAndGetMaxDistToTssUp() throws Exception {
        testCuttingPositionMap.setMaxDistToTssUp(testMaxDistToTssUp);
        assertEquals(testMaxDistToTssUp,testCuttingPositionMap.getMaxDistToTssUp());
    }

    @Test
    public void testSetAndGetMaxDistToTssDown() throws Exception {
        testCuttingPositionMap.setMaxDistToTssDown(testMaxDistToTssDown);
        assertEquals(testMaxDistToTssDown,testCuttingPositionMap.getMaxDistToTssDown());
    }

    @Test
    public void testGetNextCutUp() throws Exception {
    }

    @Test
    public void testGetNextCutDown() throws Exception {
    }

}