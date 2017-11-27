package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.model.RestrictionEnzyme;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * This test class used the sequence
 * {@code chr_t4_GATC_short_20bp_and_long_24bp_fragments_copy} that has
 * GATC sites at positions {21,45,69,93,113,137,161,185,209,229,259,279}
 * The code identifies the position as follows (where matcher is looking for a pattern such as GATC and
 * the offset refers to the location of the cut offset for patterns such as ^GATC or GA^TC
 * int pos = matcher.start() - maxDistToGenomicPosUp + cuttingPositionMapOffsets.get(cutpat) - 1;
 * Note that maxDistToGenomicPositition up is defined as the distance from the center point of the
 * fragment (genomicPos). The subsequence is extracted as
 * <pre>genomicPos - maxDistToGenomicPosUp, genomicPos + maxDistToGenomicPosDown</pre>
 * Note that these are all one-based inclusive numbers. So if maxDistToGenomicPosUp and
 * maxDistToGenomicPosDown are both equal to 1, then the resulting fragment is 3 nucleotides long.
 * Thus, we would get zero for a restriction site that begins exactly at genomicPos, negative numbers for
 * sites that begin 5' to genomicPos, and positive numbers for sites that begin 3' to genomicPos.
 * The offset will indicate where in the cutting site the restriction enzyme actually cuts.
 * ^GATC would have an offset of zero, G^ATC would have an offset of one, etc.
 * MaxDistToGenomicPosUp is the number of bases 3' to genomic pos that our fragment goes.
 * Here is the sequence
 * <pre>
 * >chr_t4_GATC_veryshort
 * AGATCACCGG.TGACATGACA.TTTGATCAAC.CGGTGACATG.ANCATTTGAT.CAACCG
 *</pre>
 * The sequence is 56 nucleotides long. There is a GATC at position 2,24,48 (one-based numbering)
 */
public class SegmentFactoryTest {

    private static String testFastaFile = null;
    private static String refSeqID1 = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";
    private static Integer genomicPos_1 = 125;
    private static Integer maxDistToGenomicPosUp = 115;
    private static Integer maxDistToGenomicPosDown = 115;
    private static IndexedFastaSequenceFile testFastaReader;
    static List<RestrictionEnzyme> chosenEnzymeList;
    static SegmentFactory segmentFactory =null;

    private static List<Integer> gatcsites;


    @BeforeClass
    public static void setup() throws Exception {
        RestrictionEnzyme re1 = new RestrictionEnzyme("HindIII", "A^AGCTT");
        RestrictionEnzyme re2 = new RestrictionEnzyme("DpnII", "^GATC");
        Map remap = new HashMap<>();
        remap.put("AAGCTT", re1);
        remap.put("GATC", re2);
        SegmentFactory.setRestrictionEnzymeMap(remap);
        chosenEnzymeList = new ArrayList<>();
        chosenEnzymeList.add(re1);
        chosenEnzymeList.add(re2);
        ClassLoader classLoader = SegmentFactoryTest.class.getClassLoader();
        testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        final File fasta = new File(testFastaFile);
        testFastaReader = new IndexedFastaSequenceFile(fasta);
        segmentFactory = new SegmentFactory(refSeqID1,
                genomicPos_1,
                testFastaReader,
                maxDistToGenomicPosUp,
                maxDistToGenomicPosDown,
                chosenEnzymeList);
        // The following are the first bases of the GATC fragments in the sequence with ^GATC, i.e.,
        //offset zero.
        gatcsites=new ArrayList<>(Arrays.asList(21,45,69,93,113,137,161,185,209,229,259,279));
    }





    @Test
    public void testWeCouldReadFASTAFile() {
        Assert.assertNotNull(testFastaReader);
    }


    @Test
    public void testCuttingPositionMapConstructor() {
        Assert.assertNotNull(segmentFactory);
    }

    @Test
    public void testGenomicPosition() {
        Integer expected = genomicPos_1;
        Assert.assertEquals(expected, segmentFactory.getGenomicPos());
    }

    @Test
    public void testMaxDistToGenomicPos() {
        Integer expected = maxDistToGenomicPosUp;
        Assert.assertEquals(expected, segmentFactory.getMaxDistToGenomicPosUp());
        expected = maxDistToGenomicPosDown;
        Assert.assertEquals(expected, segmentFactory.getMaxDistToGenomicPosDown());
    }

    /*
    * Segment factory where the downstream distance is longer than the extent of the chromosome
    * Make sure we still get the right DpnII sites.
     */
    @Test
    public void testVeryShortSegmentFactory (){
        List<Integer> adjustedGatcSitesOffsetZero=new ArrayList<>();
        RestrictionEnzyme re = new RestrictionEnzyme("DpnII", "^GATC");
        Map remap = new HashMap<>();
        remap.put(re.getPlainSite(), re);
        SegmentFactory.setRestrictionEnzymeMap(remap);
        chosenEnzymeList = new ArrayList<>();
        chosenEnzymeList.add(re);
        ClassLoader classLoader = SegmentFactoryTest.class.getClassLoader();
        testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        final File fasta = new File(testFastaFile);
        String refID="veryshort";
        //refID="chr_t4_GATC_short_20bp_and_long_24bp_fragments";
        Integer genomicPos=42;
        int maxDistUp=20;
        int maxDistDown=15;
        SegmentFactory cpm=null;
        try {
            testFastaReader = new IndexedFastaSequenceFile(fasta);
            String seq=testFastaReader.getSequence(refID).getBaseString();
            cpm = new SegmentFactory(refID,
                    genomicPos,
                    testFastaReader,
                    maxDistUp,
                    maxDistDown,
                    chosenEnzymeList);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // The following are the first bases of the GATC fragments in the sequence with ^GATC, i.e.,
        //offset zero.
        gatcsites=new ArrayList<>(Arrays.asList(2,24,48));

        int offset=0; // for ^GATC
        for (Integer pos:gatcsites) {
           // System.out.println("^GATC sites: " +pos);
            if (pos >= genomicPos - maxDistUp) {
                adjustedGatcSitesOffsetZero.add(pos);
            }
        }
        Assert.assertTrue(adjustedGatcSitesOffsetZero.equals(cpm.getAllCuts()));
    }





}
