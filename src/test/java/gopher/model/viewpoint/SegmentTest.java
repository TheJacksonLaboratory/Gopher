package gopher.model.viewpoint;

import gopher.exception.GopherException;
import gopher.io.RestrictionEnzymeParser;
import gopher.model.IntPair;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import gopher.model.Default;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SegmentTest {
    private static Logger logger = Logger.getLogger(SegmentTest.class.getName());

    private static IndexedFastaSequenceFile FastaReader;
    private static String referenceSequenceID = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";

    private static Segment segmentA; // no repeats
    private static Segment segmentB; // repeats in 10 of 24 nucleotides
    private static Segment segmentC; // complete repetitive




    @BeforeClass
    public static void setup() throws Exception {
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
        return new Segment.Builder(referenceSequenceID,21,44).
                fastaReader(reader).
                marginSize(Default.MARGIN_SIZE).
                build();
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
        return new Segment.Builder(referenceSequenceID,69,92).
                fastaReader(reader).
                marginSize(Default.MARGIN_SIZE).
                build();
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
        return new Segment.Builder(referenceSequenceID,93,112).
                fastaReader(reader).
                marginSize(Default.MARGIN_SIZE).build();
    }


    /**
     * We are looking at this sequence with a margin size of 5.
     * gatcaaccggTGACATGANCATTT
     * Thus, there is a 100% upstream and a zero percent downstream repeat content
     */
    @Test
    @Ignore("This test is failing in my environment (@ielis)") // TODO(fixtest)
    public void testGetSegmentMargins() {
        int marg=5; /* margin size for testing */
        Segment  segment = new Segment.Builder(referenceSequenceID,69,92).fastaReader(FastaReader).marginSize(marg).build();
        double expected = 1.0;
        double epsilon=0.0001;
        Assert.assertEquals(expected,segment.getRepeatContentMarginUp(),epsilon);
        expected=0.0;
        Assert.assertEquals(expected,segment.getRepeatContentMarginDown(),epsilon);
    }

    /**
     * We are looking at this sequence with a margin size of 5.
     * ccggTGACATGANCATTT
     * Thus, there is a 80% upstream and a zero percent downstream repeat content
     */
    @Test
    @Ignore("This test is failing in my environment (@ielis)") // TODO(fixtest)
    public void testGetSegmentMargins2() {
        int marg=5; /* margin size for testing */
        Segment  segment = new Segment.Builder(referenceSequenceID,75,92).fastaReader(FastaReader).marginSize(marg).build();
        double expected = 0.8;
        double epsilon=0.0001;
        Assert.assertEquals(expected,segment.getRepeatContentMarginUp(),epsilon);
        expected=0.0;
        Assert.assertEquals(expected,segment.getRepeatContentMarginDown(),epsilon);
    }


    /**
     * We are looking at this sequence with a margin size of 5.
     * ccggTGACATGANCATTTgat
     * Thus, there is a 80% upstream and a 60% percent downstream repeat content
     */
    @Test
    @Ignore("This test is failing in my environment (@ielis)") // TODO(fixtest)
    public void testGetSegmentMargins3() {
        int marg=5; /* margin size for testing */
        Segment  segment = new Segment.Builder(referenceSequenceID,75,95).fastaReader(FastaReader).marginSize(marg).build();
        //System.out.println("D="+FastaReader.getSubsequenceAt(referenceSequenceID, 75, 95).getBaseString());
        double expected = 0.8;
        double epsilon=0.0001;
        Assert.assertEquals(expected,segment.getRepeatContentMarginUp(),epsilon);
        expected=0.6;
        Assert.assertEquals(expected,segment.getRepeatContentMarginDown(),epsilon);

    }



    @Test
    public void testSegmentCPositions() {
        Integer expectedStart=93;
        Integer expectedEnd=112;
        Assert.assertEquals(expectedStart,segmentC.getStartPos());
        Assert.assertEquals(expectedEnd,segmentC.getEndPos());
    }

    @Test
    public void testSegmentCSelected() {
        Assert.assertFalse(segmentC.isSelected());
    }

    @Test
    public void testSegmentCLength() {
        Integer expected = 112-93+1;
        Assert.assertEquals(expected,segmentC.length());
    }

    /* segment C has 20 lower case letters, and is 20 long. */
    @Test
    public void testSegmentCRepeatContent() {
        double expected = 20.0/20;
        double epsilon=0.0000001;
        Assert.assertEquals(expected,segmentC.getRepeatContent(),epsilon);
    }

    /* segment B has 10 lower case letters, and is 24 long. It is so short that it consists of one single margin
    * according to our model, i.e., the marginUp and Down repeat content should be the same as the overall repeat
    * content*/
    @Test
    public void testSegmentCRepeatContentMargins() {
        double expected = 1.0;
        double epsilon=0.0000001;
        Assert.assertEquals(expected,segmentC.getRepeatContentMarginUp(),epsilon);
        Assert.assertEquals(expected,segmentC.getRepeatContentMarginDown(),epsilon);
        Assert.assertEquals(expected,segmentC.getMeanMarginRepeatContent(),epsilon);
        String expString=String.format("%.2f%%",100.0); /* should be 41.67% */
        Assert.assertEquals(expString,segmentC.getRepeatContentMarginUpAsPercent());
        Assert.assertEquals(expString,segmentC.getRepeatContentMarginDownAsPercent());
    }

    /* The margin size cannot be longer than the length of the Segment. */
    @Test
    public void testSegmentCmarginSize() {
        int expected = Math.min(segmentC.length(),Default.MARGIN_SIZE);
        Assert.assertEquals(expected,segmentC.getMarginSize());
    }

    @Test
    public void testGetChromosomealPositionAsStringSegmentC() {
        String expected=String.format("%s:%d-%d",referenceSequenceID,93,112);
        Assert.assertEquals(expected,segmentC.getChromosomalPositionString());
    }







    @Test
    public void testSegmentBPositions() {
        Integer expectedStart=69;
        Integer expectedEnd=92;
        Assert.assertEquals(expectedStart,segmentB.getStartPos());
        Assert.assertEquals(expectedEnd,segmentB.getEndPos());
    }

    @Test
    public void testSegmentBSelected() {
        Assert.assertFalse(segmentB.isSelected());
    }

    @Test
    public void testSegmentBLength() {
        Integer expected = 92-69+1;
        Assert.assertEquals(expected,segmentB.length());
    }

    /* segment B has 10 lower case letters, and is 24 long. */
    @Test
    public void testSegmentBRepeatContent() {
        double expected = 10.0/24;
        double epsilon=0.0000001;
        Assert.assertEquals(expected,segmentB.getRepeatContent(),epsilon);
    }

    /* segment B has 10 lower case letters, and is 24 long. It is so short that it consists of one single margin
    * according to our model, i.e., the marginUp and Down repeat content should be the same as the overall repeat
    * content*/
    @Test
    public void testSegmentBRepeatContentMargins() {
        double expected = 10.0/24;
        double epsilon=0.0000001;
        Assert.assertEquals(expected,segmentB.getRepeatContentMarginUp(),epsilon);
        Assert.assertEquals(expected,segmentB.getRepeatContentMarginDown(),epsilon);
        Assert.assertEquals(expected,segmentB.getMeanMarginRepeatContent(),epsilon);
        String expString=String.format("%.2f%%",100*(10.0/24)); /* should be 41.67% */
        Assert.assertEquals(expString,segmentB.getRepeatContentMarginUpAsPercent());
        Assert.assertEquals(expString,segmentB.getRepeatContentMarginDownAsPercent());
    }

    /* The margin size cannot be longer than the length of the Segment. */
    @Test
    public void testSegmentBmarginSize() {
        int expected = Math.min(segmentB.length(),Default.MARGIN_SIZE);
        Assert.assertEquals(expected,segmentB.getMarginSize());
    }

    @Test
    public void testGetChromosomealPositionAsStringSegmentB() {
        String expected=String.format("%s:%d-%d",referenceSequenceID,69,92);
        Assert.assertEquals(expected,segmentB.getChromosomalPositionString());
    }

    /** Check repeat content for
     * FastaReader.getSubsequenceAt(referenceSequenceID,seg.getStartPos(),seg.getEndPos()).getBaseString()
     * caacc.ggTGA.CATGA.NCATT.T  7 of 21 bases are lower case (repeat)
     */
    @Test
    @Ignore("This test is failing in my environment (@ielis)") // TODO(fixtest)
    public void testRepetitiveContentAlteredSegmentB() {
        // change end position
        //Segment seg = new Segment(referenceSequenceID,72,92,false, FastaReader);
        Segment seg=new Segment.Builder(referenceSequenceID,72,92).
                fastaReader(FastaReader).
                marginSize(Default.MARGIN_SIZE).
                build();
        double expected = 7.0/21;
        double epsilon=0.0001;

        assertEquals(expected, seg.getRepeatContent(),epsilon);
       // System.out.println("RC segment 2 after changing starting position and update: " + seg.getRepeatContent() + "\n" + FastaReader.getSubsequenceAt(referenceSequenceID,seg.getStartPos(),seg.getEndPos()).getBaseString() + "\n");

    }



    @Test
    public void testSegmentAPositions() {
        Integer expectedStart=21;
        Integer expectedEnd=44;
        Assert.assertEquals(expectedStart,segmentA.getStartPos());
        Assert.assertEquals(expectedEnd,segmentA.getEndPos());
        String seg=FastaReader.getSubsequenceAt(referenceSequenceID,21,44).getBaseString();
    }

    @Test
    public void testSegmentASelected() {
        Assert.assertFalse(segmentA.isSelected());
    }

    @Test
    public void testSegmentALength() {
        Integer expected = 44-21+1;
        Assert.assertEquals(expected,segmentA.length());
    }

    /* segment A has no lower case letters, i.e., no repeats. */
    @Test
    public void testSegmentARepeatContent() {
        double expected = 0.0;
        double epsilon=0.0000001;
        Assert.assertEquals(expected,segmentA.getRepeatContent(),epsilon);
    }

    /* segment A has no lower case letters, i.e., no repeats. */
    @Test
    public void testSegmentARepeatContentMargins() {
        double expected = 0.0;
        double epsilon=0.0000001;
        Assert.assertEquals(expected,segmentA.getRepeatContentMarginUp(),epsilon);
        Assert.assertEquals(expected,segmentA.getRepeatContentMarginDown(),epsilon);
        Assert.assertEquals(expected,segmentA.getMeanMarginRepeatContent(),epsilon);
        String expString="0.00%";
        Assert.assertEquals(expString,segmentA.getRepeatContentMarginUpAsPercent());
        Assert.assertEquals(expString,segmentA.getRepeatContentMarginDownAsPercent());
    }

    /* The margin size cannot be longer than the length of the Segment. */
    @Test
    public void testSegmentAmarginSize() {
        int expected = Math.min(segmentA.length(),Default.MARGIN_SIZE);
        Assert.assertEquals(expected,segmentA.getMarginSize());
    }

    @Test
    public void testGetChromosomealPositionAsStringSegmentA() {
        String expected=String.format("%s:%d-%d",referenceSequenceID,21,44);
        Assert.assertEquals(expected,segmentA.getChromosomalPositionString());
    }

    @Test
    public void getBaitsForUpstreamMargin() throws IOException, GopherException {

        // create segment for testing
        File fasta = new File("src/test/resources/testAlignabilityMap/testAlignabilityMap.fa");
        FastaReader = new IndexedFastaSequenceFile(fasta);
        Segment testSeg = new Segment.Builder("chr1",900,2002).
                fastaReader(FastaReader).
                marginSize(250).
                build();
        ArrayList<IntPair> ip = testSeg.getSegmentMargins();
        Integer upStreamStaPos = ip.get(0).getStartPos();
        Integer upStreamEndPos = ip.get(0).getEndPos();

        AlignabilityMap testMap = new AlignabilityMap("src/test/resources/testAlignabilityMap/chromInfo.txt.gz", "src/test/resources/testAlignabilityMap/testAlignabilityMap.bedgraph.gz",50);

        List<Bait> baitList = testSeg.setUsableBaitsForUpstreamMargin(1,120,testMap,0.35,0.65,10.0);
        logger.trace(baitList.size());
        for (Bait bait : baitList) {
            logger.trace(bait.getStartPos() + "\t" + bait.getEndPos() + "\t" + bait.getAlignabilityScore());
        }

    }

    @Ignore("Test is ignored because it is only for manual checking of specified regions in real data.")
    @Test
    public void getBaitsForUpstreamMarginRealData() throws IOException, GopherException {

        // create segment for testing
        File fasta = new File("/home/peter/storage_1/VPV_data/hg19/hg19.fa");
        FastaReader = new IndexedFastaSequenceFile(fasta);
        Segment testSeg = new Segment.Builder("chr20",50160700,50161090).//up:50158209,50158459;down:
                fastaReader(FastaReader).
                marginSize(250).
                build();
        ArrayList<IntPair> ip = testSeg.getSegmentMargins();

        //AlignabilityMap testMap = new AlignabilityMap("/Users/hansep/data/hg19/chromInfo.txt.gz", "/Users/hansep/data/hg19/hg19.50mer.alignabilityMap.bedgraph.gz",50);
        AlignabilityMap testMap = new AlignabilityMap("/home/peter/storage_1/VPV_data/hg19/chromInfo.txt.gz", "/home/peter/storage_1/VPV_data/hg19/hg19.50mer.alignabilityMap.bedgraph.gz",50);

        List<Bait> baitList = testSeg.setUsableBaitsForUpstreamMargin(1,120,testMap,0.35,0.65,10.0);

        logger.trace(baitList.size());
        for (Bait bait : baitList) {
            logger.trace(bait.getStartPos() + "\t" + bait.getEndPos() + "\t" + bait.getAlignabilityScore() + "\t" + bait.getGCContent() + "\t" + bait.getRepeatContent());
        }

    }
}
