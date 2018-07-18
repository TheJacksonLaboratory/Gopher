package gopher.model.viewpoint;

import gopher.model.Default;
import gopher.model.IntPair;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ProbeFactoryTest {
    private static Logger logger = Logger.getLogger(ProbeFactoryTest.class.getName());

    private static AlignabilityMap testMap = null;
    private static Segment testSeg = null;
    private static IndexedFastaSequenceFile FastaReader;

    @BeforeClass
    public static void setup() throws Exception {

        // create AlignabilityMap object for testing
        testMap = new AlignabilityMap("src/test/resources/testAlignabilityMap/chromInfo.txt.gz", "src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz",50);

        // create segments for testing
        ClassLoader classLoader = SegmentTest.class.getClassLoader();
        //String testFastaFile = classLoader.getResource("src/test/resources/testAlignabilityMap/testAlignabilityMap.fa").getFile();
        File fasta = new File("src/test/resources/testAlignabilityMap/testAlignabilityMap.fa");
        FastaReader = new IndexedFastaSequenceFile(fasta);

        testSeg = new Segment.Builder("chr1",900,2002).
                fastaReader(FastaReader).
                marginSize(20).
                build();
    }

    @Test
    public void testGetSegmentMargins() {

        List<IntPair> ip = testSeg.getSegmentMargins();
        Integer upStreamStaPos = ip.get(0).getStartPos();
        Integer upStreamEndPos = ip.get(0).getEndPos();
        Integer downStreamStaPos = ip.get(1).getStartPos();
        Integer downStreamEndPos = ip.get(1).getEndPos();

        logger.trace(upStreamStaPos);
        logger.trace(upStreamEndPos);
        logger.trace(testMap.getScoreFromTo("chr1", upStreamStaPos, upStreamEndPos));
        logger.trace(downStreamStaPos);
        logger.trace(downStreamEndPos);
        logger.trace(testMap.getScoreFromTo("chr1", downStreamStaPos, downStreamEndPos));





    }



}