package vpvgui.io;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.model.project.VPVGene;


import java.util.Arrays;
import java.util.List;

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
    /** four gene symbols that should be found by the parser, and one that should not. */
    private static String[] symbols={"CUEDC1","DKC1","FAM216B","LINC01010", "FAKE"};

    @BeforeClass
    public static void setup() throws Exception {
        ClassLoader classLoader = RefGeneParserTest.class.getClassLoader();
        String refgene = classLoader.getResource("refGeneSmall.txt.gz").getFile();
        parser=new RefGeneParser(refgene);
    }



    @Test
    public void testTotalNumberOfGenes() {
        int expected=19;
        Assert.assertEquals(expected,parser.n_totalRefGenes());
    }

    @Test
    public void testFindValidGenes() {
        int expected=4;
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<String> valid=parser.getValidGeneSymbols();
        Assert.assertEquals(expected,valid.size());
    }

    @Test
    public void testFindInValidGenes() {
        int expected=1;
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<String> invalid=parser.getInvalidGeneSymbols();
        Assert.assertEquals(expected,invalid.size());
        String name=invalid.get(0);
        Assert.assertEquals("FAKE",name);
    }

    /** The following tests the retrieval and information in one of the VPVGenes that
     * is created from the refGeneSmall.txt.gz file.
     */
    @Test
    public void testVPGenes() {
        int expected=1;
        List<String> list = Arrays.asList(symbols);
        parser.checkGenes(list);
        List<VPVGene> vpvgenes=parser.getVPVGeneList();
        VPVGene gene=null;
        for (VPVGene g:vpvgenes) {
            if (g.getGeneSymbol().equals("FAM216B")){
                gene=g; break;
            }
        }
        Assert.assertNotNull(gene);
        String exp="chr13";
        Assert.assertEquals(exp,gene.getContigID());
        exp="NM_182508";
        Assert.assertEquals(exp,gene.getRefSeqID());
        boolean isForwardStrand=true;
        Assert.assertEquals(isForwardStrand,gene.isForward());
        List<Integer> gPosList = gene.getTSSlist();
        Assert.assertEquals(1,gPosList.size());
        Integer expectedPos=43355685; /* Note: zero based numbering! */
        Assert.assertEquals(expectedPos,gPosList.get(0));
    }

    /** There are 25 lines corresponding to 19 genes and 22 distinct transcription start sites
     * in the refGeneSmall.txt.gz file.
     */
    @Test
    public void testNumberOfStartPoints() {
        int expected=22;
        Assert.assertEquals(expected,parser.n_totalTSSstarts());

    }

}
