package gopher.model.viewpoint;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by hansep on 6/15/18.
 */
public class AlignabilityMapTest {
    private static Logger logger = Logger.getLogger(AlignabilityMap.class.getName());

    private static AlignabilityMap testMap =null;

    @BeforeClass
    public static void setup() throws Exception {
        testMap = new AlignabilityMap("src/test/resources/testAlignabilityMap/chromInfo.txt.gz", "src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz");
    }

    @Test
    public void testGetScoreFromTo() {

        ArrayList<Double> scoreArray = testMap.getScoreFromTo("chr1", 100,101);
        assertEquals(-1.0, scoreArray.get(0),0.001);
        assertEquals(0.25, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr1", 200,201);
        assertEquals(0.25, scoreArray.get(0),0.001);
        assertEquals(-1.0, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr1", 500,501);
        assertEquals(-1.0, scoreArray.get(0),0.001);
        assertEquals(0.5, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr1", 1000,1001);
        assertEquals(0.5, scoreArray.get(0),0.001);
        assertEquals(1.0, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr1", 2000,2001);
        assertEquals(1.0, scoreArray.get(0),0.001);
        assertEquals(-1.0, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr2", 300,301);
        assertEquals(0.5, scoreArray.get(0),0.001);
        assertEquals(0.333333, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr2", 1000,1001);
        assertEquals(0.333333, scoreArray.get(0),0.001);
        assertEquals(0.2, scoreArray.get(1),0.001);

        scoreArray = testMap.getScoreFromTo("chr3", 2000,2001);
        assertEquals(1.0, scoreArray.get(0),0.001);
        assertEquals(-1.0, scoreArray.get(1),0.001);
    }

    @Ignore("Test is ignored because it is only for manual checking of specified regions in real data.")
    @Test
    public void testGetScoreFromToRealData() throws IOException {

        AlignabilityMap testMap2 = new AlignabilityMap("/home/peter/storage_1/VPV_data/hg19/chromInfo.txt.gz", "/home/peter/storage_1/VPV_data/hg19/hg19.100mer.alignabilityMap.bedgraph.gz");
        String chromosome = "chr3";
        int interval_start = 856715;
        int interval_length = 50;

        logger.trace("");
        logger.trace(chromosome + ":" + interval_start + "-" + (interval_start + interval_length));
        logger.trace("");
        ArrayList<Double> scoreArray = testMap2.getScoreFromTo("chr3", interval_start,interval_start + interval_length);

        for(int i = 0; i < scoreArray.size(); i++) {
            logger.trace("chr3" + "\t" + (interval_start + i -1) + "\t" + scoreArray.get(i));
        }
        logger.trace("");
    }
}