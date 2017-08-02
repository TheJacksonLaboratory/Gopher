package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SegmentTest {

    private static File fasta;
    private static IndexedFastaSequenceFile FastaReader;
    private static String referenceSequenceID = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";

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

        assertEquals(0.0, segment_1.getRepetitiveContent(),0.0);
        System.out.println("RC segment 1: " + segment_1.getRepetitiveContent() + "\n" + FastaReader.getSubsequenceAt(referenceSequenceID,segment_1.getStartPos(),segment_1.getEndPos()).getBaseString() + "\n");

        assertEquals(0.416, segment_2.getRepetitiveContent(),0.01);
        System.out.println("RC segment 2: " + segment_2.getRepetitiveContent() + "\n" + FastaReader.getSubsequenceAt(referenceSequenceID,segment_2.getStartPos(),segment_2.getEndPos()).getBaseString() + "\n");

        // change end position
        segment_2.setStartPos(72);
        // update RC
        segment_2.setRepetitiveContent(FastaReader);
        assertEquals(0.33, segment_2.getRepetitiveContent(),0.01);
        System.out.println("RC segment 2 after changing starting position and update: " + segment_2.getRepetitiveContent() + "\n" + FastaReader.getSubsequenceAt(referenceSequenceID,segment_2.getStartPos(),segment_2.getEndPos()).getBaseString() + "\n");

        segment_3.setRepetitiveContent(FastaReader);
        assertEquals(1.00, segment_3.getRepetitiveContent(),0.0);
        System.out.println("RC segment 3: " + segment_3.getRepetitiveContent() + "\n" + FastaReader.getSubsequenceAt(referenceSequenceID,segment_3.getStartPos(),segment_3.getEndPos()).getBaseString() + "\n");

    }


    @Test
    public void testGetSegmentMargins() throws Exception {
        double rc_upstream_margin = segment_2.getSegmentMargins(5).get(0).getRepetitiveContent();
        System.out.println("RC upstream: " + rc_upstream_margin);
        double rc_downstream_margin = segment_2.getSegmentMargins(5).get(1).getRepetitiveContent();
        System.out.println("RC downstream: " + rc_downstream_margin);
    }

}
