package gopher.model.digest;
import gopher.io.RestrictionEnzymeParser;
import gopher.model.RestrictionEnzyme;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;



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

        String restrictionEnzymeFile = classLoader.getResource("testdata/enzymelist.tab").getFile();
        RestrictionEnzymeParser parser = new RestrictionEnzymeParser(restrictionEnzymeFile);
        relist = parser.getEnzymes();
    }


    @Test
    public void test1() {
        assertFalse(1==2);
    }



}

