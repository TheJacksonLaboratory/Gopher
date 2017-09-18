package vpvgui.consoletest;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.viewpoint.SegmentFactory;

import static org.junit.Assert.assertEquals;

/**
 * This test class tests an instance of the <i>CuttingPositionMapTest</i> class. The settings for the call of the constructor are specified at the head of this test class.
 * <p>
 * The two functions <i>testHashMap</i> and <i>testGetNextPos</i> print output to the screen for demonstration purposes (see method descriptions for more details).
 * <p>
 * @author Peter Hansen
 */
public class CuttingPositionMapTestConsole {


    /* test fields */

    String testReferenceSequenceID="chr4_ctg9_hap1";
    Integer testGenomicPos=20000;
    Integer testMaxDistToGenomicPosUp=150;
    Integer testMaxDistToGenomicPosDown=50;


    /* create CuttingPositionMapTest object for testing */

    private String[] testCuttingPatterns = new String[]{"ACT^TTTA","AAAC^CACTTAC","^GAG"};
    private String[] testCuttingPatternsCopy = testCuttingPatterns.clone();

    RestrictionEnzyme re1 = new RestrictionEnzyme("re1","ACT^TTTA");
    RestrictionEnzyme re2 = new RestrictionEnzyme("re2","AAAC^CACTTAC");
    RestrictionEnzyme re3 = new RestrictionEnzyme("re2","^GAG");
    List<RestrictionEnzyme> renzymeList;


    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);



    SegmentFactory testCuttingPositionMap;// = new CuttingPositionMap(testReferenceSequenceID, testGenomicPos, testFastaReader, testMaxDistToGenomicPosUp, testMaxDistToGenomicPosDown, testCuttingPatterns);
    IndexedFastaSequenceFile testFastaReader;

    public CuttingPositionMapTestConsole() throws FileNotFoundException {
        Map<String,RestrictionEnzyme> remap =new HashMap<>();
        remap.put(re1.getPlainSite(),re1);
        remap.put(re2.getPlainSite(),re2);
        remap.put(re3.getPlainSite(),re3);
//        List<RestrictionEnzyme> lst=new ArrayList<>();
//        lst.add(re1);
//        lst.add(re2);
//        lst.add(re3);
        renzymeList = new ArrayList<>();
        renzymeList.add(re1);
        renzymeList.add(re2);
        renzymeList.add(re3);
        SegmentFactory.setRestrictionEnzymeMap(remap);
        testFastaReader = new IndexedFastaSequenceFile(fasta);
        testCuttingPositionMap = new SegmentFactory(testReferenceSequenceID, testGenomicPos, testFastaReader, testMaxDistToGenomicPosUp, testMaxDistToGenomicPosDown, renzymeList);


    } // Not nice, but without there will be an error. Why?

    /* test constructor */

    @Test
    public void testFields() throws Exception {
        assertEquals(testGenomicPos,testCuttingPositionMap.getGenomicPos());
        assertEquals(testMaxDistToGenomicPosUp,testCuttingPositionMap.getMaxDistToGenomicPosUp());
        assertEquals(testMaxDistToGenomicPosDown,testCuttingPositionMap.getMaxDistToGenomicPosDown());
    }


    /* test unhandled IntegerOutOfRangeException for function 'testgetNextCutPos' */

    @Test(expected = IntegerOutOfRangeException.class)
    public void testIntegerOutOfRangeExceptionDownstream() throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {
        Integer testPos = testMaxDistToGenomicPosDown+1;
        Integer nextCutPos =  testCuttingPositionMap.getNextCutPosOLD(testPos, "down");
    }

    @Test(expected = IntegerOutOfRangeException.class)
    public void testIntegerOutOfRangeExceptionUpstream() throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {
        Integer testPos = testMaxDistToGenomicPosUp-1;
        Integer nextCutPos =  testCuttingPositionMap.getNextCutPosOLD(testPos, "down");
    }

    /* test unhandled IllegalArgumentException for function 'testgetNextCutPos' */

    @Test(expected = IntegerOutOfRangeException.class)
    public void testIllegalArgumentException() throws IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {
        Integer testPos = testMaxDistToGenomicPosDown+1;
        Integer nextCutPos =  testCuttingPositionMap.getNextCutPosOLD(testPos, "down");
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