package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;
import static org.junit.Assert.assertEquals;

/**
 * This test class tests an instance of the <i>CuttingPositionMap</i> class. The settings for the call of the constructor are specified at the head of this test class.
 * <p>
 * The two functions <i>testHashMap</i> and <i>testGetNextPos</i> print output to the screen for demonstration purposes (see method descriptions for more details).
 * <p>
 * @author Peter Hansen
 */
public class CuttingPositionMapTest {


    /* test fields */

    String testReferenceSequenceID="chr4_ctg9_hap1";;
    Integer testGenomicPos=20000;;
    Integer testMaxDistToGenomicPosUp=150;
    Integer testMaxDistToGenomicPosDown=50;


    /* create CuttingPositionMap object for testing */

    private String[] testCuttingPatterns = new String[]{"ACT^TTTA","AAAC^CACTTAC","^GAG"};
    private String[] testCuttingPatternsCopy = testCuttingPatterns.clone();

    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);

    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);

    CuttingPositionMap testCuttingPositionMap = new CuttingPositionMap(testReferenceSequenceID, testGenomicPos, testFastaReader, testMaxDistToGenomicPosUp, testMaxDistToGenomicPosDown, testCuttingPatterns);

    public CuttingPositionMapTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?

    /* test constructor */

    @Test
    public void testFields() throws Exception {
        assertEquals(testGenomicPos,testCuttingPositionMap.getGenomicPos());
        assertEquals(testMaxDistToGenomicPosUp,testCuttingPositionMap.getMaxDistToGenomicPosUp());
        assertEquals(testMaxDistToGenomicPosDown,testCuttingPositionMap.getMaxDistToGenomicPosDown());
    }

    /**
     * For demonstration purposes the contents of various data structures are printed to the screen:
     * <ol>
     * <li>Cutting motifs including the '^' character.</li>
     * <li>The offsets for the cutting positions, which correspond to the position of the '^' character.</li>
     * <li>The genomic sequence of the sequence of the viewpoint.</li>
     * <li>The genomic sequence of the sequence of the viewpoint after uppercase conversion.</li>
     * <li>The position of the central position <i>genomicPos</i> indicated by a '|' character.</li>
     * <li>The individual occurrences of the cutting motifs placed within the viewpoint sequence.</li>
     * <li>The more precise cutting positions within the motif occurrences indicated by a '|' character.</li>
     * </ol>
     * @throws Exception
     */
    @Test
    public void testHashMap() throws Exception {

        System.out.println("===============================================================");
        System.out.println("Test function 'testHashMap' prints to the screen ");
        System.out.println("===============================================================");

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
        String genomicPosRegionString = testFastaReader.getSubsequenceAt(testReferenceSequenceID, testGenomicPos - testMaxDistToGenomicPosUp, testGenomicPos + testMaxDistToGenomicPosDown).getBaseString(); // get sequence around genomic position
        System.out.println(genomicPosRegionString);
        String genomicPosRegionStringUpper = genomicPosRegionString.toUpperCase();
        System.out.println();
        System.out.println("Sequence around 'genomicPos' uppercase only:");
        System.out.println(genomicPosRegionStringUpper);

        // print genomic position
        String s = new String("");
        for (int k = 0; k < testMaxDistToGenomicPosUp; k++) { s += " "; }
        s += "|";
        System.out.println(s);
        s = "";
        for (int k = 0; k < testMaxDistToGenomicPosUp-4; k++) { s += " "; }
        s += "genomicPos";
        System.out.println(s);

        // print cutting motif occurrences only for individual motifs
        System.out.println();
        System.out.println("Individual cutting motif occurrences:");
        for (int i = 0; i < testCuttingPatterns.length; i++) {
            ArrayList<Integer> relPosIntArray = testCuttingPositionMap.getHashMapOnly().get(testCuttingPatterns[i]);
            for (int j = 0; j < relPosIntArray.size(); j++) {
                s = "";
                Integer offset=testCuttingPositionMap.getCuttingPositionMapOffsets().get(testCuttingPatterns[i]);
                for (int k = 0; k < relPosIntArray.get(j)+testMaxDistToGenomicPosUp -offset; k++) { // subtract offset
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
        for (int i = 0; i < testCuttingPositionMap.getHashMapOnly().get("ALL").size(); i++) {
            s = "";
            for (int j = 0; j < testCuttingPositionMap.getHashMapOnly().get("ALL").get(i)+testMaxDistToGenomicPosUp; j++) {
                s += " ";
            }
            s += "|";
            s += testCuttingPositionMap.getHashMapOnly().get("ALL").get(i);
            System.out.println(s);
        }
        System.out.println("===============================================================");
        System.out.println("Test function 'testHashMap' END");
        System.out.println("===============================================================");
    }


    /* test utility functions */

    /**
     * In this function some usage applications for the function <i>getNextCutPos</i> are given.
     * Furthermore, the correctness of the function is tested.
     * <p>
     * At first a test position between two cutting sites is generated.
     * This position is used to perform alternate calls of the function <i>getNextCutPos</i> for up and downstream direction.
     * For checking purposes compare the output to the output of the test <i>testHashMap</i>.
     * <p>
     * In addition, the handled exception <i>NoCuttingSiteFoundUpOrDownstreamException</i> is tested for up and downstream direction.
     * <p>
     * @throws Exception
     */
    @Test
    public void testGetNextCutPos() throws Exception {

        System.out.println("===============================================================");
        System.out.println("Test function 'testgetNextCutPos' prints to the screen ");
        System.out.println("===============================================================");

        /* test usual function calls for positions between two cutting sites */

        if(1<testCuttingPositionMap.getHashMapOnly().get("ALL").size()) {

            Integer posA = testCuttingPositionMap.getHashMapOnly().get("ALL").get(0);
            Integer posB = testCuttingPositionMap.getHashMapOnly().get("ALL").get(1);
            Integer testPos=posA+((posB-posA)/2);

            System.out.println("---------------------------------------------------------------");
            System.out.println("Testing position " + testPos + " for downstream direction...");
            Integer nextCutPos = testCuttingPositionMap.getNextCutPos(testPos,"down");
            System.out.println("Next cutting site in downstream direction is: " +  nextCutPos);

            System.out.println("---------------------------------------------------------------");
            System.out.println("Testing position " + testPos + " for upstream direction...");
            nextCutPos = testCuttingPositionMap.getNextCutPos(testPos,"up");
            System.out.println("Next cutting site in upstream direction is: " +  nextCutPos);

            System.out.println("---------------------------------------------------------------");
            System.out.println("Testing position " + testPos + " for downstream direction...");
            nextCutPos = testCuttingPositionMap.getNextCutPos(testPos,"down");
            System.out.println("Next cutting site in downstream direction is: " +  nextCutPos);

            System.out.println("---------------------------------------------------------------");
            System.out.println("Testing position " + testPos + " for upstream direction...");
            nextCutPos = testCuttingPositionMap.getNextCutPos(testPos,"up");
            System.out.println("Next cutting site in upstream direction is: " +  nextCutPos);

        } else {
            System.out.println("There need to be at least two cutting sites to perform this test. Will skip this test.");
        }

        /* test handled NoCuttingSiteFoundUpOrDownstreamException */

        // last cutting position, downstream
        Integer testPos = testCuttingPositionMap.getHashMapOnly().get("ALL").get(testCuttingPositionMap.getHashMapOnly().get("ALL").size()-1) + 1;
        System.out.println("---------------------------------------------------------------");
        System.out.println("Testing position " + testPos + " for downstream direction...");
        Integer nextCutPos = testCuttingPositionMap.getNextCutPos(testPos, "down");
        System.out.println("Returned value: " + nextCutPos);

        // first cutting position, upstream
        testPos = testCuttingPositionMap.getHashMapOnly().get("ALL").get(0) - 1;
        System.out.println("---------------------------------------------------------------");
        System.out.println("Testing position " + testPos + " for upstream direction...");
        nextCutPos = testCuttingPositionMap.getNextCutPos(testPos, "up");
        System.out.println("Returned value: " + nextCutPos);

        System.out.println("===============================================================");
        System.out.println("Test function 'testgetNextCutPos' END");
        System.out.println("===============================================================");
    }

    /* test unhandled IntegerOutOfRangeException for function 'testgetNextCutPos' */

    @Test(expected = IntegerOutOfRangeException.class)
    public void testIntegerOutOfRangeExceptionDownstream() throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {
        Integer testPos = testMaxDistToGenomicPosDown+1;
        Integer nextCutPos =  testCuttingPositionMap.getNextCutPos(testPos, "down");
    }

    @Test(expected = IntegerOutOfRangeException.class)
    public void testIntegerOutOfRangeExceptionUpstream() throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {
        Integer testPos = testMaxDistToGenomicPosUp-1;
        Integer nextCutPos =  testCuttingPositionMap.getNextCutPos(testPos, "down");
    }

    /* test unhandled IllegalArgumentException for function 'testgetNextCutPos' */

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException() throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {
        Integer testPos = testMaxDistToGenomicPosDown+1;
        Integer nextCutPos =  testCuttingPositionMap.getNextCutPos(testPos, "illegal argument");
    }


    /* setter and getter functions */

    @Test
    public void testSetAndGetGenomicPos() throws Exception {
        testCuttingPositionMap.setGenomicPos(testGenomicPos);
        assertEquals(testGenomicPos,testCuttingPositionMap.getGenomicPos());
    }

    @Test
    public void testSetAndGetMaxDistToGenomicPosUp() throws Exception {
        testCuttingPositionMap.setMaxDistToGenomicPosUp(testMaxDistToGenomicPosUp);
        assertEquals(testMaxDistToGenomicPosUp,testCuttingPositionMap.getMaxDistToGenomicPosUp());
    }

    @Test
    public void testSetAndGetMaxDistToGenomicPosDown() throws Exception {
        testCuttingPositionMap.setMaxDistToGenomicPosDown(testMaxDistToGenomicPosDown);
        assertEquals(testMaxDistToGenomicPosDown,testCuttingPositionMap.getMaxDistToGenomicPosDown());
    }

}