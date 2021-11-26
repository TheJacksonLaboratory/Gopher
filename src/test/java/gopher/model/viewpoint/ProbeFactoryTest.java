package gopher.model.viewpoint;

import gopher.model.IntPair;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ProbeFactoryTest {

    private static Segment testSeg = null;
    private static IndexedFastaSequenceFile FastaReader;
    private static AlignabilityMap alignMap;

    @BeforeAll
    public static void setup() throws Exception {

        // create AlignabilityMap object for testing
        //testMap = new AlignabilityMap(, ,50);
        String alignabilitypath="src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz";
        String chromInfoPath="src/test/resources/testAlignabilityMap/chromInfo.txt.gz";
        int kmerlen=50;
        AlignabilityMapIterator iterator = new AlignabilityMapIterator(alignabilitypath,chromInfoPath,kmerlen);
        while (iterator.hasNext()) {
            AlignabilityMap c2amap=iterator.next();
            if (c2amap.getChromName().equals("chr1")) {
                alignMap = c2amap;
                break;
            }
        }

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
        assertNotNull(upStreamEndPos); // todo -- need to test something

    }



}