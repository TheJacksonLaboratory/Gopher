package vpvgui.htsjdk_snippets;

import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;
import static org.junit.Assert.*;



/**
 * Created by phansen on 6/2/17.
 */
public class htsjdk_snippetsTest {

    // learn how to use junit testing
    // ------------------------------

    private htsjdk_snippets get_name = new htsjdk_snippets("Klaus");

    @Test
    public void getName() throws Exception {

        assertEquals("Klaus",get_name.getName());
    }

    @Test
    public void testHappyAfterPlay() throws Exception {
        get_name.playWithRock();
        assertTrue(get_name.isHappy());
    }

    // learn how to use htsjdk API
    // ---------------------------

    // open FASTA file
    @Test
    public void testOpenFastaFile() throws Exception {
        final IndexedFastaSequenceFile fastaReader = get_name.openFastaFile("src/test/resources/smallgenome/chr4_ctg9_hap1.fa");
        //ReferenceSequence referenceSequence = fastaReader.getSubsequenceAt("chr4_ctg9_hap1",1,5);

        // FASTA file exists
        assertNotNull(fastaReader);

        // FASTA file is indexed
        assertTrue(fastaReader.isIndexed());

    }

    // find pattern in given FASTA file
    @Test
    public void testFindPattern() throws Exception {

    }

}
