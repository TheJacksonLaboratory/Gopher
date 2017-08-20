package vpvgui.io;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test that we get this information for chr4_ctg9_hap1.fa:
 * <pre>chr4_ctg9_hap1 590426 16 50 51</pre>
 * Created by peterrobinson on 7/22/17.
 */
public class FASTAIndexerTest {

    private static FASTAIndexer faidx;

    @BeforeClass
    public static void setup() throws Exception {
        ClassLoader classLoader = FASTAIndexerTest.class.getClassLoader();
        String fasta = classLoader.getResource("smallgenome/chr4_ctg9_hap1.fa").getFile();
        faidx = new FASTAIndexer(fasta);
        faidx.createFASTAindex();
    }

    @Test
    public void testContigname() {
        String expected="chr4_ctg9_hap1";
        Assert.assertEquals(expected,faidx.getContigname());
    }

    @Test
    public void testBaseOffset() {
        long expected=16;
        Assert.assertEquals(expected,faidx.getByte_index());
    }

    @Test
    public void testTotalBytes() {
        long expected = 590426;
        Assert.assertEquals(expected,faidx.getN_bases());
    }

    @Test
    public void testBasesPerLine() {
        long expected=50;
        Assert.assertEquals(expected,faidx.getBases_per_line());
    }

    @Test
    public void testBytesPerLine() {
        long expected=51;
        Assert.assertEquals(expected,faidx.getBytes_per_line());
    }

}
