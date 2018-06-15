package gopher.model.viewpoint;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by hansep on 6/15/18.
 */
public class AlignabilityMapTest {

    private static AlignabilityMap testMap =null;

    @BeforeClass
    public static void setup() throws Exception {
        testMap = new AlignabilityMap("/Users/hansep/data/hg19/hg19.100mer.alignabilityMap.bedgraph.gz");
    }

    @Test
    public void testIfBedGraphIsSorted() throws IOException {
        testMap.bedGraphFileIsSorted();
    }

}