package gopher.model.digest;

import gopher.io.RestrictionEnzymeParser;
import gopher.model.RestrictionEnzyme;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;


public class FragmentTest {


    private static IndexedFastaSequenceFile FastaReader;

    private static String referenceSequenceID = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";


    private static List<RestrictionEnzyme> relist;


    @BeforeClass
    public static void setup() throws Exception {
        //String testFastaFile="src/test/resources/testgenome/test_genome.fa";
        ClassLoader classLoader = FragmentTest.class.getClassLoader();
        String testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        File fasta = new File(testFastaFile);
        FastaReader = new IndexedFastaSequenceFile(fasta);

        relist = RestrictionEnzymeParser.getEnzymes(classLoader.getResourceAsStream("testdata/enzymelist.tab"));
    }


    @Test
    public void test1() {
        assertFalse(1 == 2);
    }

}

