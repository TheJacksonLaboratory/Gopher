package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SegmentTest {

    private static File fasta;
    private static IndexedFastaSequenceFile FastaReader;

    private static Segment segment_1; // no repeats
    private static Segment segment_2; // repeats and no repeats
    private static Segment segment_3; // complete repetitive


    @BeforeClass
    public static void setup() throws Exception {

        String testFastaFile="src/test/resources/testgenome/test_genome.fa";
        String referenceSequenceID="chr_t4_GATC_short_20bp_and_long_24bp_fragments";

        /* create IndexedFastaSequenceFile object */

        fasta = new File(testFastaFile);
        FastaReader = new IndexedFastaSequenceFile(fasta);

        /* create Segment objects for testing */

        segment_1 = new Segment(referenceSequenceID,21,44,false, FastaReader);
        segment_2 = new Segment(referenceSequenceID,69,92,false, FastaReader);
        segment_3 = new Segment(referenceSequenceID,93,112,false, FastaReader);
    }


    @Test
    public void testSetRepetitiveContent() throws Exception {

        segment_1.setRepetitiveContent(FastaReader);
        System.out.println(segment_1.getRepetitiveContent());

        segment_2.setRepetitiveContent(FastaReader);
        System.out.println(segment_2.getRepetitiveContent());

        segment_3.setRepetitiveContent(FastaReader);
        System.out.println(segment_3.getRepetitiveContent());
    }
}
