package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * This test class tests an instance of the <i>ViewPoint</i> class. The settings for the call of the constructor are specified at the head of this test class.
 * <p>
 * @author Peter Hansen
 */
public class ViewPointTest {

    /* Create data and ViewPoint object for testing */

    private String testReferenceSequenceID="chr4_ctg9_hap1";
    private Integer testGenomicPos=10000;
    private Integer testMaxUpstreamGenomicPos=100; // replace initial radius with two separate max values for up and downstream. This is more flexible.
    private Integer testMaxDownstreamPos=100;
    private Integer testStartPos=testGenomicPos-testMaxUpstreamGenomicPos;
    private Integer testEndPos=testGenomicPos+testMaxDownstreamPos;
    private String testDerivationApproach = "INITIAL";

    private String[] testCuttingPatterns = new String[]{"TCCG","CA","AAA"};

    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);
    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);

    ViewPoint testViewpoint = new ViewPoint(testReferenceSequenceID, testGenomicPos, testMaxUpstreamGenomicPos, testMaxDownstreamPos, testCuttingPatterns, testFastaReader);

    public ViewPointTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?


    /* test getter and setter functions */

    @Test
    public void testSetAndGetReferenceID() throws Exception {
        testViewpoint.setReferenceID(testReferenceSequenceID);
        assertEquals(testReferenceSequenceID,testViewpoint.getReferenceID());
    }

    @Test
    public void testSetAndGetGenomicPos() throws Exception {
        testViewpoint.setGenomicPos(testGenomicPos);
        assertEquals(testGenomicPos,testViewpoint.getGenomicPos());
    }

    @Test
    public void testSetAndGetMaxUpstreamGenomicPos() throws Exception {
        testViewpoint.setMaxUpstreamGenomicPos(testMaxUpstreamGenomicPos);
        assertEquals(testMaxUpstreamGenomicPos,testViewpoint.getMaxUpstreamGenomicPos());
    }

    @Test
    public void testSetAndGetMaxDownstreamPos() throws Exception {
        testViewpoint.setMaxDownstreamGenomicPos(testMaxDownstreamPos);
        assertEquals(testMaxDownstreamPos,testViewpoint.getMaxDownstreamGenomicPos());
    }
    @Test
    public void testSetAndGetStartAndEndPos() throws Exception {
        testViewpoint.setStartPos(testStartPos);
        assertEquals(testStartPos,testViewpoint.getStartPos());
        testViewpoint.setEndPos(testEndPos);
        assertEquals(testEndPos,testViewpoint.getEndPos());
    }

    @Test
    public void testSetAndGetDerivationApproach() throws Exception {
        testViewpoint.setDerivationApproach(testDerivationApproach);
        assertEquals(testDerivationApproach,testViewpoint.getDerivationApproach());
    }


    @Test
    public void testGetGenomicPosFromGenomicRelativePos() throws Exception {

        for(int i=0;i<testCuttingPatterns.length;i++) {

            ArrayList<Integer> relPosIntArray = testViewpoint.getCuttingPositionMap().getArrayListForGivenMotif(testCuttingPatterns[i]);

            for(int j=0;j<relPosIntArray.size();j++) {
                Integer genomicPosition = testGenomicPos + relPosIntArray.get(j);
                assertEquals(genomicPosition, testViewpoint.getGenomicPosOfGenomicRelativePos(testGenomicPos,relPosIntArray.get(j)));
            }
        }
    }

    @Test
    public void testExtendFragmentWise() throws Exception {

        /* create viewpoint for testing */

        Integer genomicPos=80;
        Integer maxDistToGenomicPosUp=75;
        Integer maxDistToGenomicPosDown=75;

        String[] testCuttingPatterns = new String[]{"^GATC","A^AGCTT"};
        String testFastaFile="src/test/resources/testgenome/test_genome.fa";
        File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile FastaReader = new IndexedFastaSequenceFile(fasta);

        ViewPoint testViewpointGATC = new ViewPoint("chr_t1_GATC", genomicPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown, testCuttingPatterns, FastaReader);


        /* print initial viewpoint sequence to screen */

        String genomicPosRegionString = FastaReader.getSubsequenceAt("chr_t1_GATC", genomicPos-maxDistToGenomicPosUp, genomicPos+maxDistToGenomicPosDown).getBaseString();
        System.out.println("\nViewpoint sequence:");
        System.out.println(genomicPosRegionString);

        /* print all cutting positions to screen */

        String s = new String("");
        s = "";
        Integer pos_prev = testViewpointGATC.getCuttingPositionMap().getNextCutPos(-maxDistToGenomicPosUp,"down");
        int i;
        Integer pos=0;
        for(i=-maxDistToGenomicPosUp;i<=maxDistToGenomicPosDown;i++) {
            pos = testViewpointGATC.getCuttingPositionMap().getNextCutPos(i,"down");
            if(pos==pos_prev) {
                s += " ";
            } else {
                s += "^";
                pos_prev=pos;
            }
            if(testViewpointGATC.getCuttingPositionMap().getHashMapOnly().get("GATC").get(testViewpointGATC.getCuttingPositionMap().getHashMapOnly().get("GATC").size()-1)==pos){
                break;
            }
        }
        for (i=i;i<pos;i++) {
            s += " ";
        }
        s += "^";
        System.out.println(s);

        /* print genomic position to screen */

        s = "";
        for (int k = 0; k < maxDistToGenomicPosUp; k++) { s += " ";}
        s += "|";
        System.out.println(s);

        /* set and print start and end position of the viewpoint to screen */

        System.out.println();
        testViewpointGATC.setStartPos(maxDistToGenomicPosUp-18);
        testViewpointGATC.setEndPos(maxDistToGenomicPosUp+18);

        s = "";
        for (int k = 0; k < testViewpointGATC.getStartPos(); k++) { s += " "; }
        s += "> sta";
        System.out.println(s);
        s = "";
        for (int k = 0; k < testViewpointGATC.getEndPos(); k++) { s += " "; }
        s += "< end";
        System.out.println(s);

        /* use function 'extendFragmentWise' */
        testViewpointGATC.extendFragmentWise(maxDistToGenomicPosUp-20);

        s = "";
        for (int k = 0; k < testViewpointGATC.getStartPos(); k++) { s += " "; }
        s += "> sta";
        System.out.println(s);
        s = "";
        for (int k = 0; k < testViewpointGATC.getEndPos(); k++) { s += " "; }
        s += "< end";
        System.out.println(s);
    }

}