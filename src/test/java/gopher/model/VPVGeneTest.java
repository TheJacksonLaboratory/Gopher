package gopher.model;

import org.junit.Assert;
import org.junit.Test;


/**
 * Created by peterrobinson on 7/11/17.
 */
public class VPVGeneTest {

    private static String symbol="FAM216B";
    private static String geneid="NM_182508";




    @Test
    public void testVPVGeneCTOR() {
        boolean isNoncoding=false;
        String contig="chr2";
        String strand="+";
        GopherGene gene = new GopherGene(geneid,symbol,isNoncoding,contig,strand);
        Assert.assertEquals(symbol,gene.getGeneSymbol());
        Assert.assertEquals(geneid,gene.getRefSeqID());
    }
}
