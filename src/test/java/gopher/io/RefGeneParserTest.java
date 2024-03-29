package gopher.io;


import gopher.service.model.GopherGene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * We are using an excerpt of the entire refGenefile for hg19 with 25 lines corresponding to the
 * following 19 genes: CD99P1
 CUEDC1
 DKC1
 FAM216B
 LINC01010
 LINC01128
 LINC01655
 LOC100130417
 LOC100133331
 LOC729737
 MIA2
 MYCBP2-AS1
 PERM1
 PPP2R2B-IT1
 SAMD11
 SNORD42A
 SNORD55
 WASH7P
 ZNF283

 */
public class RefGeneParserTest {

    private static RefGeneParser parser;
    /** five gene symbols that should be found by the parser, and one that should not. */
    private static final String[] symbols={"WASH7P","CUEDC1","DKC1","FAM216B","LINC01010", "FAKE"};

    @BeforeAll
    public static void setup() {
        ClassLoader classLoader = RefGeneParserTest.class.getClassLoader();
        String refgene = classLoader.getResource("refGeneSmall.txt.gz").getFile();
        parser=new RefGeneParser(refgene);
    }


    /*
    * Total number of uniq gene symbols
    * $ zcat refGeneSmall.txt.gz | cut -f 13 | sort | uniq | wc -l
    *19
     */
    @Test
    public void testTotalNumberOfGenes() {
        int expected=19;
        assertEquals(expected,parser.getTotalNumberOfRefGenes());
    }

    @Test
    public void testFindValidGenes() {
        int expected=5; /* there are five valid genes in "symbols" */
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<String> valid=parser.getValidGeneSymbols();
        assertEquals(expected,valid.size());
    }

    @Test
    public void testFindInValidGenes() {
        int expected=1;
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<String> invalid=parser.getInvalidGeneSymbols();
        assertEquals(expected,invalid.size());
        String name=invalid.get(0);
        assertEquals("FAKE",name);
    }

    /** The following tests the retrieval and information in one of the VPVGenes that
     * is created from the refGeneSmall.txt.gz file.
     */
    @Test
    public void testVPGenes() {
        int expected=1;
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<GopherGene> gophergenes=parser.getGopherGeneList();
        GopherGene gene=null;
        for (GopherGene g:gophergenes) {
            if (g.getGeneSymbol().equals("FAM216B")){
                gene=g; break;
            }
        }
        assertNotNull(gene);
        String exp="chr13";
        assertEquals(exp,gene.getContigID());
        exp="NM_182508";
        assertEquals(exp,gene.getRefSeqID());
        assertTrue(gene.isForward());
        List<Integer> gPosList = gene.getTSSlist();
        assertEquals(1,gPosList.size());
        Integer expectedPos=43355686; /* Note: one based fully closed numbering! */
        assertEquals(expectedPos,gPosList.get(0));
    }

    /** There are 25 lines corresponding to 19 genes and 22 distinct transcription start sites
     * in the refGeneSmall.txt.gz file.
     *  zcat refGeneSmall.txt.gz | cut -f 5 | sort | uniq | wc -l
     22
     */
    @Test
    public void testNumberOfStartPoints() {
        int expected=22;
        assertEquals(expected,parser.getTotalTSScount());
    }


    /**
     * WASH7P is on the negative strand of chromosome 1. It has one entry in out test file
     * <pre>chr1	-	14361	29370	29370	29370</pre>
     * The TSS is thus 29370 (zero-based half closed). The conversion to the reverse strand for
     * one-based fully closes numbering means that 29730 is the expected TSS for this gene.
     */

    @Test
    public void testGenomicPosNegStrandExample() {
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<GopherGene> vpvgenes=parser.getGopherGeneList();
        GopherGene gene=null;
        for (GopherGene g:vpvgenes) {
            if (g.getGeneSymbol().equals("WASH7P")){
                gene=g; break;
            }
        }
        assertNotNull(gene);
        assertFalse(gene.isForward());
        List<Integer> tssList=gene.getTSSlist();
        assertEquals(1,tssList.size());
        Integer expected=29370;
        assertEquals(expected,tssList.get(0));
    }


    /**
     * LINC01010 is on the positive strand of chromosome 6
     * In our test file there is only one line for the gene
     * <pre>chr6	+	134758853	134825158	134825158	134825158...</pre>
     * There is thus only one TSS at 134758853 (zero based). We are expecting the parser to
     * return 1-start, fully-closed numbers, so we actually expect 134758854.
     */
    @Test
    public void testGenomicPosPlusStrandExample() {
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<GopherGene> vpvgenes=parser.getGopherGeneList();
        GopherGene gene=null;
        for (GopherGene g:vpvgenes) {
            if (g.getGeneSymbol().equals("LINC01010")){
                gene=g; break;
            }
        }
        assertNotNull(gene);
        assertTrue(gene.isForward());
        List<Integer> tssList=gene.getTSSlist();
        //there should be just one TSS for this gene!
        assertEquals(1,tssList.size());
        Integer expected=134758854;
        assertEquals(expected,tssList.get(0));
    }


}
