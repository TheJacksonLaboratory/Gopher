package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


public class ViewPointTest {

    /* Create data and ViewPoint object for testing */

    private String testReferenceSequenceID="chr4_ctg9_hap1";
    private Integer testTssPos=10000;
    private Integer testInitialRadius=100;
    private Integer testStartPos=testTssPos-testInitialRadius;
    private Integer testEndPos=testTssPos+testInitialRadius;

    private String[] testCuttingPatterns = new String[]{"TG","CA"};

    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);
    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);

    ViewPoint testViewpoint = new ViewPoint(testReferenceSequenceID, testTssPos, testInitialRadius, testCuttingPatterns, testFastaReader);

    public ViewPointTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?


    /* test getter and setter functions */

    @Test
    public void testGetAndSetReferenceID() throws Exception {
        testViewpoint.setReferenceID(testReferenceSequenceID);
        assertEquals(testReferenceSequenceID,testViewpoint.getReferenceID());
    }

    @Test
    public void testGetAndSetTssPos() throws Exception {
        testViewpoint.setTssPos(testTssPos);
        assertEquals(testTssPos,testViewpoint.getTssPos());
    }

    @Test
    public void testGetAndStartAndEndPos() throws Exception {
        testViewpoint.setStartPos(testStartPos);
        assertEquals(testStartPos,testViewpoint.getStartPos());
        testViewpoint.setEndPos(testEndPos);
        assertEquals(testEndPos,testViewpoint.getEndPos());
    }

    @Test
    public void testCreateCuttingPositionMap() throws Exception {

        /* This test is intended to test if the generation of the cuttingPositionMap works correct.
         * Furthermore, the usage of the data structure is demonstrated here.
         * Strucures used for testing including the viewpoint are created at the top of this class.
         */

        /* Print the initial sequence */

        System.out.println();
        String tssRegionString = testFastaReader.getSubsequenceAt("chr4_ctg9_hap1",10000-100,10000+100).getBaseString(); // get sequence around TSS
        System.out.println(tssRegionString);

        for(int i=0;i<testCuttingPatterns.length;i++) {
            //System.out.println();
            ArrayList<Integer> arr1 = testViewpoint.getCuttingPositionMap().get(testCuttingPatterns[i]);
            for(int j=0;j<arr1.size();j++) {
                String s = new String("");
                for(int k=0;k<100+arr1.get(j);k++) {
                    s += " ";
                }
                s += testFastaReader.getSubsequenceAt("chr4_ctg9_hap1",10000+arr1.get(j),10000+arr1.get(j)+testCuttingPatterns[i].length()-1).getBaseString();
                s += " ";
                s += arr1.get(j);
                System.out.println(s);
            }
            //System.out.print('\n');
        }
    }
}