package gopher.model.viewpoint;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by hansep on 6/15/18.
 */
public class AlignabilityMapTest {
    private static Logger logger = Logger.getLogger(AlignabilityMap.class.getName());

    private static AlignabilityMap testMap =null;

    @BeforeClass
    public static void setup() throws Exception {
        //testMap = new AlignabilityMap("/Users/hansep/data/hg19/hg19.100mer.alignabilityMap.bedgraph.gz");
        //testMap = new AlignabilityMap("/home/peter/storage_1/VPV_data/hg19/chromInfo.txt.gz", "/home/peter/storage_1/VPV_data/hg19/hg19.100mer.alignabilityMap.bedgraph.gz");
    }

    @Test
    public void testMapConstruction() throws IOException {
        testMap = new AlignabilityMap("/home/peter/IdeaProjects/Gopher/src/test/resources/testAlignabilityMap/chromInfo.txt.gz", "/home/peter/IdeaProjects/Gopher/src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz");



        for(int i = 1; i < 2300 - 100; i++) {
            logger.trace("chr1" + "\t" + i + "\t" + testMap.getScoreAtPos("chr1", i));
        }
    }

}