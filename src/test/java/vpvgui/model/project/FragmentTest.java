package vpvgui.model.project;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;


/**
 * Created by peter on 29.05.17.
 */
public class FragmentTest {

    private static Fragment fragment;

    @BeforeClass
    public static void setup() throws Exception {
        ClassLoader classLoader = FragmentTest.class.getClassLoader();
        String dirpath = classLoader.getResource("smallgenome").getFile();
        fragment = new Fragment(dirpath);
    }

    /* On my machine, fragment.getDirectoryPath() returns
    * /home/peter/IdeaProjects/VPV/target/test-classes/smallgenome
     */
    @Test
    public void testDirectory() {
        String expected="smallgenome";
        Assert.assertTrue(fragment.getDirectoryPath().contains(expected));
    }

    /**
     * The directory
     * test/resources/smallgenome
     * has the files
     * chr4_ctg9_hap1.fa  chr4_ctg9_hap1.fa.fai
     * This is a small scaffold from the hg38 genome but it could have
     * been chr1 etc.
     * Therefore,we set the chromosome to "chr4_ctg9_hap1"
     * In general, set it to the same name as on the ">" line, e.g., chr1, chr2, ...
     */
    @Test
    public void testSetChromosome() {
        String chromosome="chr4_ctg9_hap1";
        fragment.setCurrentChromosome(chromosome);
        String chr = fragment.getCurrentChromosome();
        Assert.assertEquals(chromosome,chr);
        String expectedFASTA="chr4_ctg9_hap1.fa";
        // Something like
        // /home/peter/IdeaProjects/VPV/target/test-classes/smallgenome/chr4_ctg9_hap1.fa
        File expectedFASTApath=new File(fragment.getDirectoryPath() + File.separatorChar
                + expectedFASTA);
        Assert.assertEquals(expectedFASTApath.getAbsolutePath(),fragment.getFastaAbsolutePath());
    }

    /*
    * The first two lines of the FASTA file are
    * >chr4_ctg9_hap1
    * GAATTCTTCACATTTCCTGGCTTTTAAAAGTTCTCCTTCCACAAATCTTC
    * Positions 6-10 are : CTTCA
    * Test whether we can retrieve this.
     */
    @Test
    public void testGetDNAfragment1() {
        String chromosome="chr4_ctg9_hap1";
        fragment.setCurrentChromosome(chromosome);
        String expected = "CTTCA";
        String dna = fragment.getGenomicReferenceSequence(chromosome, 6, 10);
        Assert.assertEquals(expected,dna);
    }
}
