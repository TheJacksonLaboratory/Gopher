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
    Integer testMaxDistToTssDown=60;


    /* create CuttingPositionMap object for testing */

    private String[] testCuttingPatterns = new String[]{"ACT^TTTA","AAAC^CACTTAC","G^AG"};
    private String[] testCuttingPatternsCopy = testCuttingPatterns.clone();

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

        // print cutting motifs with '^' characters
        System.out.println();
        System.out.println("Cutting motifs:");
        for(int i = 0; i<testCuttingPatterns.length;i++) {
            System.out.println(testCuttingPatternsCopy[i]);
        }

        // print offsets
        System.out.println();
        System.out.println("Offsets of cutting motifs:");
        for(int i = 0; i<testCuttingPositionMap.getCuttingPositionMapOffsets().size();i++) {
            System.out.println(testCuttingPositionMap.getCuttingPositionMapOffsets().get(testCuttingPatterns[i]));
        }

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
        System.out.println();
        System.out.println("Individual cutting motif occurrences:");
        for (int i = 0; i < testCuttingPatterns.length; i++) {
            ArrayList<Integer> relPosIntArray = testCuttingPositionMap.getCuttingPositionMap().get(testCuttingPatterns[i]);
            for (int j = 0; j < relPosIntArray.size(); j++) {
                s = "";
                Integer offset=testCuttingPositionMap.getCuttingPositionMapOffsets().get(testCuttingPatterns[i]);
                for (int k = 0; k < relPosIntArray.get(j)+testMaxDistToTssUp -offset; k++) { // subtract offset
                    s += " ";
                }
                Integer sta = testGenomicPos + relPosIntArray.get(j) - testCuttingPositionMap.getCuttingPositionMapOffsets().get(testCuttingPatterns[i]);
                Integer end = sta + testCuttingPatterns[i].length() - 1;
                s += testFastaReader.getSubsequenceAt(testReferenceSequenceID, sta, end).getBaseString();
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



    /* utility functions */

    @Test
    public void testgetNextCutPos() throws Exception {
        Integer pos = -15;
        String direction="down";
        System.out.println("pos:  " + pos);
        System.out.println("direction:  " + direction);
        Integer nextCutPos = testCuttingPositionMap.getNextCutPos(pos,direction);
        System.out.println("nextCutPos:  " + nextCutPos);
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



}