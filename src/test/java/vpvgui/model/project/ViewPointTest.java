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
    private Integer testMaxUpstreamTssPos=100; // replace initial radius with two separate max values for up and downstream. This is more flexible.
    private Integer testMaxDownstreamPos=100;
    private Integer testStartPos=testTssPos-testInitialRadius;
    private Integer testEndPos=testTssPos+testInitialRadius;
    private String testGeneSymbol = "SOX9";
    private String testDerivationApproach = "INITIAL";

    private String[] testCuttingPatterns = new String[]{"TCCG","CA","AAA"};

    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);
    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);

    ViewPoint testViewpoint = new ViewPoint(testReferenceSequenceID, testTssPos, testInitialRadius, testMaxUpstreamTssPos, testMaxDownstreamPos, testCuttingPatterns, testFastaReader, testGeneSymbol);

    public ViewPointTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?


    /* test getter and setter functions */

    @Test
    public void testSetAndGetReferenceID() throws Exception {
        testViewpoint.setReferenceID(testReferenceSequenceID);
        assertEquals(testReferenceSequenceID,testViewpoint.getReferenceID());
    }

    @Test
    public void testSetAndGetTssPos() throws Exception {
        testViewpoint.setTssPos(testTssPos);
        assertEquals(testTssPos,testViewpoint.getTssPos());
    }

    @Test
    public void testSetAndGetMaxUpstreamTssPos() throws Exception {
        testViewpoint.setMaxUpstreamTssPos(testMaxUpstreamTssPos);
        assertEquals(testMaxUpstreamTssPos,testViewpoint.getMaxUpstreamTssPos());
    }

    @Test
    public void testSetAndGetMaxDownstreamPos() throws Exception {
        testViewpoint.setMaxDownstreamTssPos(testMaxDownstreamPos);
        assertEquals(testMaxDownstreamPos,testViewpoint.getMaxDownstreamTssPos());
    }
    @Test
    public void testSetAndGetStartAndEndPos() throws Exception {
        testViewpoint.setStartPos(testStartPos);
        assertEquals(testStartPos,testViewpoint.getStartPos());
        testViewpoint.setEndPos(testEndPos);
        assertEquals(testEndPos,testViewpoint.getEndPos());
    }

    @Test
    public void testSetAndGetGeneSymbol() throws Exception {
        testViewpoint.setGeneSymbol(testGeneSymbol);
        assertEquals(testGeneSymbol,testViewpoint.getGeneSymbol());
    }

    @Test
    public void testSetAndGetDerivationApproach() throws Exception {
        testViewpoint.setDerivationApproach(testDerivationApproach);
        assertEquals(testDerivationApproach,testViewpoint.getDerivationApproach());
    }

    @Test
    public void testGetGenomicPosFromTssRelativePos() throws Exception {

        for(int i=0;i<testCuttingPatterns.length;i++) {

            ArrayList<Integer> relPosIntArray = testViewpoint.getCuttingPositionMap().get(testCuttingPatterns[i]);

            for(int j=0;j<relPosIntArray.size();j++) {
                Integer genomicPosition = testTssPos + relPosIntArray.get(j);
                assertEquals(genomicPosition,
                 testViewpoint.getGenomicPosFromTssRelativePos(testTssPos,relPosIntArray.get(j)));
            }
        }
    }
}