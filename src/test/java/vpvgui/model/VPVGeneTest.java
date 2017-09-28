package vpvgui.model;

import org.junit.Assert;
import org.junit.Test;
import vpvgui.model.VPVGene;


/**
 * Created by peterrobinson on 7/11/17.
 */
public class VPVGeneTest {

    private static String symbol="FAM216B";
    private static String geneid="NM_182508";




    @Test
    public void testVPVGeneCTOR() {
        VPVGene gene = new VPVGene(geneid,symbol);
        Assert.assertEquals(symbol,gene.getGeneSymbol());
        Assert.assertEquals(geneid,gene.getRefSeqID());
    }
}