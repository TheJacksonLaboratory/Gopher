package gopher.service.model.viewpoint;

import gopher.service.model.IntPair;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import gopher.service.model.Default;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SegmentTest {

    private static IndexedFastaSequenceFile FastaReader;
    private static final String referenceSequenceID = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";

    private static Segment segmentA; // no repeats
    private static Segment segmentB; // repeats in 10 of 24 nucleotides
    private static Segment segmentC; // complete repetitive




    @BeforeEach
    public void setup() throws Exception {
        //String testFastaFile="src/test/resources/testgenome/test_genome.fa";
        ClassLoader classLoader = SegmentTest.class.getClassLoader();

        String testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        File fasta = new File(testFastaFile);
        FastaReader = new IndexedFastaSequenceFile(fasta);

        segmentA = buildSegmentA(FastaReader);
        segmentB = buildSegmentB(FastaReader);
        segmentC = buildSegmentC(FastaReader);
    }

    /**
     * This creates a segment from one of the sequences contained in resources/testgenome/test_genome.fa
     * The sequence is called chr_t4_GATC_short_20bp_and_long_24bp_fragments (see {@link #referenceSequenceID}).
     * and we get positions 21-44 of that
     * AACCG-GTGAC-ATGAN-CATTT-G//ATCA-ACCGG-TGACA-TGANC-ATTTG//-ATCAA-CCGGTGACATGAN
     * This is the 1-based sequence returned by FastaReader.getSubsequenceAt(referenceSequenceID,21,44).getBaseString()
     * GATCAACCGGTGACATGANCATTT
     * @return A Segment with no repeat bases
     */
    private static Segment buildSegmentA(IndexedFastaSequenceFile reader) {
        return new Segment(referenceSequenceID,21,44,
                reader,
                Default.MARGIN_SIZE);
    }

    /**
     * replaces new Segment(referenceSequenceID,69,92,false, FastaReader);
     * The sequence returned by reader.getSubsequenceAt(referenceSequenceID, 69, 92).getBaseString()
     * is
     * gatca.accgg.TGACA.TGANC.ATTT
     * It is 24 bases long and has 10 repeat (lower case bases).
     * @param reader Reader with test FASTA sequence
     * @return A Segment with 10/24 repeat bases
     */
    private static Segment buildSegmentB(IndexedFastaSequenceFile reader) {
        return new Segment(referenceSequenceID,69,92,
                reader,
                Default.MARGIN_SIZE);
    }

    /**
     * replaces new Segment(referenceSequenceID,69,92,false, FastaReader);
     * The sequence returned by reader.getSubsequenceAt(referenceSequenceID, 69, 92).getBaseString()
     * is
     * gatca.accgg.tgaca.tganc
     * It is 20 bases long and has 20 repeat (lower case bases).
     * @param reader Reader with test FASTA sequence
     * @return A Segment with 100% repeat bases
     */
    private static Segment buildSegmentC(IndexedFastaSequenceFile reader) {
        return new Segment(referenceSequenceID,93,112,
                reader,
                Default.MARGIN_SIZE);
    }


    /**
     * We are looking at this sequence with a margin size of 5.
     * gatcaaccggTGACATGANCATTT
     * Thus, there is a 100% upstream and a zero percent downstream repeat content
     */
    @Test
    public void testGetSegmentMargins() {
        int marginSize=5; /* margin size for testing */
        Segment  segment = new Segment(referenceSequenceID,69,92,FastaReader,marginSize);
        double expected = 1.0;
        double epsilon=0.0001;
        assertEquals(expected,segment.getRepeatContentMarginUp(),epsilon);
        expected=0.0;
        assertEquals(expected,segment.getRepeatContentMarginDown(),epsilon);
    }

    /**
     * We are looking at this sequence with a margin size of 5.
     * ccggTGACATGANCATTT
     * Thus, there is a 80% upstream and a zero percent downstream repeat content
     */
    @Test
    public void testGetSegmentMargins2() {
        int marginSize = 5; /* margin size for testing */
        Segment  segment = new Segment(referenceSequenceID,75,92,FastaReader,marginSize);
        double expected = 0.8;
        double epsilon=0.0001;
        assertEquals(expected,segment.getRepeatContentMarginUp(),epsilon);
        expected=0.0;
        assertEquals(expected,segment.getRepeatContentMarginDown(),epsilon);
    }


    /**
     * We are looking at this sequence with a margin size of 5.
     * ccggTGACATGANCATTTgat
     * Thus, there is a 80% upstream and a 60% percent downstream repeat content
     */
    @Test
    public void testGetSegmentMargins3() {
        int marginSize = 5; /* margin size for testing */
        Segment  segment = new Segment(referenceSequenceID,75,95,FastaReader, marginSize);
        //System.out.println("D="+FastaReader.getSubsequenceAt(referenceSequenceID, 75, 95).getBaseString());
        double expected = 0.8;
        double epsilon=0.0001;
        assertEquals(expected,segment.getRepeatContentMarginUp(),epsilon);
        expected=0.6;
        assertEquals(expected,segment.getRepeatContentMarginDown(),epsilon);

    }



    @Test
    public void testSegmentCPositions() {
        Integer expectedStart=93;
        Integer expectedEnd=112;
        assertEquals(expectedStart,segmentC.getStartPos());
        assertEquals(expectedEnd,segmentC.getEndPos());
    }

    @Test
    public void testSegmentCSelected() {
        assertFalse(segmentC.isSelected());
    }

    @Test
    public void testSegmentCLength() {
        Integer expected = 112-93+1;
        assertEquals(expected,segmentC.length());
    }

    /* segment C has 20 lower case letters, and is 20 long. */
    @Test
    public void testSegmentCRepeatContent() {
        double expected = 20.0/20;
        double epsilon=0.0000001;
        assertEquals(expected,segmentC.getRepeatContent(),epsilon);
    }

    /* segment B has 10 lower case letters, and is 24 long. It is so short that it consists of one single margin
    * according to our model, i.e., the marginUp and Down repeat content should be the same as the overall repeat
    * content*/
    @Test
    public void testSegmentCRepeatContentMargins() {
        double expected = 1.0;
        double epsilon=0.0000001;
        assertEquals(expected,segmentC.getRepeatContentMarginUp(),epsilon);
        assertEquals(expected,segmentC.getRepeatContentMarginDown(),epsilon);
        assertEquals(expected,segmentC.getMeanMarginRepeatContent(),epsilon);
        String expString=String.format("%.2f%%",100.0); /* should be 41.67% */
        assertEquals(expString,segmentC.getRepeatContentMarginUpAsPercent());
        assertEquals(expString,segmentC.getRepeatContentMarginDownAsPercent());
    }

    /* The margin size cannot be longer than the length of the Segment. */
    @Test
    public void testSegmentCmarginSize() {
        int expected = Math.min(segmentC.length(),Default.MARGIN_SIZE);
        assertEquals(expected,segmentC.getMarginSize());
    }

    @Test
    public void testGetChromosomealPositionAsStringSegmentC() {
        String expected=String.format("%s:%d-%d",referenceSequenceID,93,112);
        assertEquals(expected,segmentC.getChromosomalPositionString());
    }







    @Test
    public void testSegmentBPositions() {
        Integer expectedStart=69;
        Integer expectedEnd=92;
        assertEquals(expectedStart,segmentB.getStartPos());
        assertEquals(expectedEnd,segmentB.getEndPos());
    }

    @Test
    public void testSegmentBSelected() {
        assertFalse(segmentB.isSelected());
    }

    @Test
    public void testSegmentBLength() {
        Integer expected = 92-69+1;
        assertEquals(expected,segmentB.length());
    }

    /* segment B has 10 lower case letters, and is 24 long. */
    @Test
    public void testSegmentBRepeatContent() {
        double expected = 10.0/24;
        double epsilon=0.0000001;
        assertEquals(expected,segmentB.getRepeatContent(),epsilon);
    }

    /* segment B has 10 lower case letters, and is 24 long. It is so short that it consists of one single margin
    * according to our model, i.e., the marginUp and Down repeat content should be the same as the overall repeat
    * content*/
    @Test
    public void testSegmentBRepeatContentMargins() {
        double expected = 10.0/24;
        double epsilon=0.0000001;
        assertEquals(expected,segmentB.getRepeatContentMarginUp(),epsilon);
        assertEquals(expected,segmentB.getRepeatContentMarginDown(),epsilon);
        assertEquals(expected,segmentB.getMeanMarginRepeatContent(),epsilon);
        String expString=String.format("%.2f%%",100*(10.0/24)); /* should be 41.67% */
        assertEquals(expString,segmentB.getRepeatContentMarginUpAsPercent());
        assertEquals(expString,segmentB.getRepeatContentMarginDownAsPercent());
    }

    /* The margin size cannot be longer than the length of the Segment. */
    @Test
    public void testSegmentBmarginSize() {
        int expected = Math.min(segmentB.length(),Default.MARGIN_SIZE);
        assertEquals(expected,segmentB.getMarginSize());
    }

    @Test
    public void testGetChromosomealPositionAsStringSegmentB() {
        String expected=String.format("%s:%d-%d",referenceSequenceID,69,92);
        assertEquals(expected,segmentB.getChromosomalPositionString());
    }

    /** Check repeat content for
     * FastaReader.getSubsequenceAt(referenceSequenceID,seg.getStartPos(),seg.getEndPos()).getBaseString()
     * caacc.ggTGA.CATGA.NCATT.T  7 of 21 bases are lower case (repeat)
     */
    @Test
    public void testRepetitiveContentAlteredSegmentB() {
        // change end position
        Segment seg=new Segment(referenceSequenceID,72,92, FastaReader, Default.MARGIN_SIZE);
        double expected = 7.0/21;
        double epsilon=0.0001;

        assertEquals(expected, seg.getRepeatContent(),epsilon);
       // System.out.println("RC segment 2 after changing starting position and update: " + seg.getRepeatContent() + "\n" + FastaReader.getSubsequenceAt(referenceSequenceID,seg.getStartPos(),seg.getEndPos()).getBaseString() + "\n");

    }



    @Test
    public void testSegmentAPositions() {
        Integer expectedStart=21;
        Integer expectedEnd=44;
        assertEquals(expectedStart,segmentA.getStartPos());
        assertEquals(expectedEnd,segmentA.getEndPos());
    }

    @Test
    public void testSegmentASelected() {
        assertFalse(segmentA.isSelected());
    }

    @Test
    public void testSegmentALength() {
        Integer expected = 44-21+1;
        assertEquals(expected,segmentA.length());
    }

    /* segment A has no lower case letters, i.e., no repeats. */
    @Test
    public void testSegmentARepeatContent() {
        double expected = 0.0;
        double epsilon=0.0000001;
        assertEquals(expected,segmentA.getRepeatContent(),epsilon);
    }

    /* segment A has no lower case letters, i.e., no repeats. */
    @Test
    public void testSegmentARepeatContentMargins() {
        double expected = 0.0;
        double epsilon=0.0000001;
        assertEquals(expected,segmentA.getRepeatContentMarginUp(),epsilon);
        assertEquals(expected,segmentA.getRepeatContentMarginDown(),epsilon);
        assertEquals(expected,segmentA.getMeanMarginRepeatContent(),epsilon);
        String expString="0.00%";
        assertEquals(expString,segmentA.getRepeatContentMarginUpAsPercent());
        assertEquals(expString,segmentA.getRepeatContentMarginDownAsPercent());
    }

    /* The margin size cannot be longer than the length of the Segment. */
    @Test
    public void testSegmentAmarginSize() {
        int expected = Math.min(segmentA.length(),Default.MARGIN_SIZE);
        assertEquals(expected,segmentA.getMarginSize());
    }

    @Test
    public void testGetChromosomealPositionAsStringSegmentA() {
        String expected=String.format("%s:%d-%d",referenceSequenceID,21,44);
        assertEquals(expected,segmentA.getChromosomalPositionString());
    }

    @Test
    public void getBaitsForUpstreamMargin() throws IOException {

        // create segment for testing
        File fasta = new File("src/test/resources/testAlignabilityMap/testAlignabilityMap.fa");
        FastaReader = new IndexedFastaSequenceFile(fasta);
        Segment testSeg = new Segment("chr1",900,2002, FastaReader, 250);
        List<IntPair> ip = testSeg.getSegmentMargins();
        Integer upStreamStaPos = ip.get(0).startPos();
        Integer upStreamEndPos = ip.get(0).endPos();

        String alignabilityPath="src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz";
        String chromInfoPath="src/test/resources/testAlignabilityMap/chromInfo.txt.gz";
        AlignabilityMapIterator apiterator = new AlignabilityMapIterator(alignabilityPath,chromInfoPath,50);
        AlignabilityMap amp=null;
        while (apiterator.hasNext()) {
            AlignabilityMap c2m=apiterator.next();
            if (c2m.getChromName().equals("chr1")) {
                amp=c2m;
                break;
            }
        }
        assertNotNull(amp);
    }
}
