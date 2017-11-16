package vpvgui.model;

import org.junit.Assert;
import org.junit.Test;


/**
 * Created by peterrobinson on 7/11/17.
 */
public class DataSourceTest {


    @Test
    public void testCreateUCSCmm9(){
        DataSource ds = DataSource.createUCSCmm9();
        String expected="UCSC-mm9";
        Assert.assertEquals(expected,ds.getGenomeName());
        expected = "chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeBasename());
        expected="http://hgdownload.soe.ucsc.edu/goldenPath/mm9/bigZips/chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeURL());
    }

    @Test
    public void testCreateUCSCmm10(){
        DataSource ds = DataSource.createUCSCmm10();
        String expected="UCSC-mm10";
        Assert.assertEquals(expected,ds.getGenomeName());
        expected = "chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeBasename());
        expected="http://hgdownload.soe.ucsc.edu/goldenPath/mm10/bigZips/chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeURL());
    }

    @Test
    public void testCreateUCSChg19(){
        DataSource ds = DataSource.createUCSChg19();
        String expected="UCSC-hg19";
        Assert.assertEquals(expected,ds.getGenomeName());
        expected = "chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeBasename());
        expected="http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeURL());
    }

    @Test
    public void testCreateUCSChg38(){
        DataSource ds = DataSource.createUCSChg38();
        String expected="UCSC-hg38";
        Assert.assertEquals(expected,ds.getGenomeName());
        expected = "chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeBasename());
        expected="http://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/hg38.chromFa.tar.gz";
        Assert.assertEquals(expected,ds.getGenomeURL());
    }

}
