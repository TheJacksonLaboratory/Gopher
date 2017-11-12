package vpvgui.io;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeneRegGTFParserTest {

    static GeneRegGTFParser parser;


    @BeforeClass
    public static void init() {
        String path = "/Users/peterrobinson/Documents/data/homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff";
        parser=new GeneRegGTFParser(path);

    }



    @Test
    public void mytest() {
        // for now use local path
        //plan to make small excerpt of the GTF file
        Assert.assertEquals(1,1);
    }


}
