package vpvgui.model.project;

import org.junit.Test;
import org.testng.Assert;

/**
 * Created by peterrobinson on 7/11/17.
 */
public class VPVGeneTest {

    private static String symbol="YFG42";
    private static String geneid="42";



    @Test
    public void testVPVGeneCTOR() {
        VPVGene gene = new VPVGene(geneid,symbol);
        Assert.assertEquals(symbol,gene.getGeneSymbol());
        Integer expected = 42;
        Assert.assertEquals(expected,gene.getGeneID());
    }
}
