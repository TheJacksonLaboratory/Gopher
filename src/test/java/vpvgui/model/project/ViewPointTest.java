package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;


public class ViewPointTest {

    @Test
    public void testConstructor() throws Exception {

        /* Create string array for cutting patterns */
        String[] Patterns = new String[]{"ACGT","CTGA"};

        /* Create indexed sequence file */
        final File fasta = new File("src/test/resources/smallgenome/chr4_ctg9_hap1.fa"); // index has to be there
        IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(fasta);

        /* Call Contructor */
        ViewPoint myViewpoint = new ViewPoint("chr4_ctg9_hap1",100, Patterns, fastaReader);
        }
}