package gopher.io;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneRegGTFParserTest {

    private static GeneRegGTFParser parser;


    @BeforeAll
    public static void init() {
        String path = "/Users/peterrobinson/Documents/data/homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff";
        parser=new GeneRegGTFParser(path);

    }



    @Test
    public void mytest() {
        // for now use local path
        //plan to make small excerpt of the GTF file
        assertEquals(1,1);
    }


}
