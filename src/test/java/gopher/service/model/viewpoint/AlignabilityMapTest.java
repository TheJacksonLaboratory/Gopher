package gopher.service.model.viewpoint;



import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Created by hansep on 6/15/18.
 */
public class AlignabilityMapTest {

    //private static AlignabilityMap testMap = null;

    private static Map<String,AlignabilityMap> chr2alMap;

    @BeforeAll
    public static void setup() throws Exception {
        //testMap = new AlignabilityMap("src/test/resources/testAlignabilityMap/chromInfo.txt.gz", "src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz",50);
        String alignabilitypath="src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz";
        String chromInfoPath="src/test/resources/testAlignabilityMap/chromInfo.txt.gz";
        int kmerlen=50;
        chr2alMap=new HashMap<>();
        AlignabilityMapIterator iterator = new AlignabilityMapIterator(alignabilitypath,chromInfoPath,kmerlen);
        while (iterator.hasNext()) {
            AlignabilityMap c2amap=iterator.next();
            chr2alMap.put(c2amap.getChromName(),c2amap);
        }
    }

    @Test
    public void testGetScoreFromTo() {
        AlignabilityMap chr1map = chr2alMap.get("chr1");
        assertNotNull(chr1map);

        ArrayList<Integer> scoreArray = chr1map.getScoreFromTo( 100,101);
        assertEquals(-1, scoreArray.get(0),0.001);
        assertEquals(4, scoreArray.get(1),0.001);

        scoreArray = chr1map.getScoreFromTo( 200,201);
        assertEquals(4, scoreArray.get(0),0.001);
        assertEquals(-1, scoreArray.get(1),0.001);

        scoreArray = chr1map.getScoreFromTo( 500,501);
        assertEquals(-1, scoreArray.get(0),0.001);
        assertEquals(2, scoreArray.get(1),0.001);

        scoreArray = chr1map.getScoreFromTo(1000,1001);
        assertEquals(2, scoreArray.get(0),0.001);
        assertEquals(1, scoreArray.get(1),0.001);

        scoreArray = chr1map.getScoreFromTo(2000,2001);
        assertEquals(1, scoreArray.get(0),0.001);
        assertEquals(-1, scoreArray.get(1),0.001);

        AlignabilityMap chr2map = chr2alMap.get("chr2");
        assertNotNull(chr2map);

        scoreArray = chr2map.getScoreFromTo( 300,301);
        assertEquals(2, scoreArray.get(0),0.001);
        assertEquals(3, scoreArray.get(1),0.001);

        scoreArray = chr2map.getScoreFromTo( 1000,1001);
        assertEquals(3, scoreArray.get(0),0.001);
        assertEquals(5, scoreArray.get(1),0.001);

        AlignabilityMap chr3map = chr2alMap.get("chr3");
        assertNotNull(chr3map);

        scoreArray = chr3map.getScoreFromTo(2000,2001);
        assertEquals(1, scoreArray.get(0),0.001);
        assertEquals(-1, scoreArray.get(1),0.001);
    }

}