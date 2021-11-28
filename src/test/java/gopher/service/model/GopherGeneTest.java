package gopher.service.model;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by peterrobinson on 7/11/17.
 */
public class GopherGeneTest {

    private static final String symbol="FAM216B";
    private static final String geneid="NM_182508";


    @Test
    public void testGopherGeneCTOR() {
        boolean isNoncoding=false;
        String contig="chr2";
        String strand="+";
        GopherGene gene = new GopherGene(geneid,symbol,isNoncoding,contig,strand);
        assertEquals(symbol,gene.getGeneSymbol());
        assertEquals(geneid,gene.getRefSeqID());
    }
}
