package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.model.Design;
import vpvgui.model.Model;
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
public class CuttingPositionMapTest {

    private static Model testModel;
    private static Design testDesign;
    private static String testFastaFile = null;
    private static String refSeqID1 = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";
    private static Integer genomicPos_1 = 125;
    private static Integer genomicPos_2 = 150; // position on the next fragment in downstream direction -> overlapping viewpoints
    private static String referenceSequenceID_2 = "chr_t4_GATC_short_20bp_and_long_24bp_fragments_copy"; // same sequence, but different name
    private static Integer genomicPos_3 = 125; // non overlapping viewpoint
    private static Integer maxDistToGenomicPosUp = 115;
    private static Integer maxDistToGenomicPosDown = 115;

    private static String[] testCuttingPatterns = new String[]{"^GATC", "A^AGCTT"};


    // parameters for Lupianez-Funktion
    private static Integer fragNumUp = 1;
    private static Integer fragNumDown = 2;
    private static Integer minSizeUp = 20;
    private static Integer minSizeDown = 20;
    private static Integer maxSizeUp = 95;
    private static Integer maxSizeDown = 150;
    private static Integer marginSize = 10;
    private static Integer minFragSize = 20;
    private static Double minRepFrag = 0.6;
    private static String motif = "GATC";

    private static IndexedFastaSequenceFile testFastaReader;
    static List<RestrictionEnzyme> chosenEnzymeList;
    static SegmentFactory cpm=null;

    private static List<Integer> gatcsites;
    private static List<Integer> adjustedGatcSitesOffsetZero;

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
        ClassLoader classLoader = CuttingPositionMapTest.class.getClassLoader();
        testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        final File fasta = new File(testFastaFile);
        testFastaReader = new IndexedFastaSequenceFile(fasta);
        cpm = new SegmentFactory(refSeqID1,
                genomicPos_1,
                testFastaReader,
                maxDistToGenomicPosUp,
                maxDistToGenomicPosDown,
                chosenEnzymeList);
        // The following are the first bases of the GATC fragments in the sequence with ^GATC, i.e.,
        //offset zero.
        gatcsites=new ArrayList<>(Arrays.asList(21,45,69,93,113,137,161,185,209,229,259,279));
        adjustedGatcSitesOffsetZero=new ArrayList<>();
        int offset=0; // for ^GATC
        for (Integer pos:gatcsites) {
            Integer adjustedPos=genomicPos_1-maxDistToGenomicPosUp-1+offset;
            System.out.println("genomePos_1 ="+genomicPos_1 + ", pos="+pos);
            Integer j=genomicPos_1-pos;
            adjustedGatcSitesOffsetZero.add(j);
        }
    }





    @Test
    public void testWeCouldReadFASTAFile() {
        Assert.assertNotNull(testFastaReader);
    }


    @Test
    public void testCuttingPositionMapConstructor() {
        Assert.assertNotNull(cpm);
    }

    @Test
    public void testGenomicPosition() {
        Integer expected = genomicPos_1;
        Assert.assertEquals(expected,cpm.getGenomicPos());
    }

    @Test
    public void testMaxDistToGenomicPos() {
        Integer expected = maxDistToGenomicPosUp;
        Assert.assertEquals(expected,cpm.getMaxDistToGenomicPosUp());
        expected = maxDistToGenomicPosDown;
        Assert.assertEquals(expected,cpm.getMaxDistToGenomicPosDown());
    }

    @Test
    public void testCalculateGATC() {
        List<Integer> gatc = cpm.getAllCutsForGivenMotif("GATC");
        for (Integer i:gatc) {
            System.out.println("gatc :"+i);
        }
        for (Integer i:adjustedGatcSitesOffsetZero) {
            System.out.println("adjustedGatkSites :"+i);
        }
        Assert.assertTrue(gatcsites.equals(gatc));
    }

    public SegmentFactory createVeryShortCPM() {
        RestrictionEnzyme re = new RestrictionEnzyme("DpnII", "^GATC");
        Map remap = new HashMap<>();
        remap.put(re.getPlainSite(), re);
        SegmentFactory.setRestrictionEnzymeMap(remap);
        chosenEnzymeList = new ArrayList<>();
        chosenEnzymeList.add(re);
        ClassLoader classLoader = CuttingPositionMapTest.class.getClassLoader();
        testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        final File fasta = new File(testFastaFile);
        String refID="veryshort";
        //refID="chr_t4_GATC_short_20bp_and_long_24bp_fragments";
        Integer genomicPos=42;
        int maxDistUp=20;
        int maxDistDown=15;
        try {
            testFastaReader = new IndexedFastaSequenceFile(fasta);
            String seq=testFastaReader.getSequence(refID).getBaseString();
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println(seq);
            System.out.println("length="+seq.length());
            System.out.println("genomicPos="+genomicPos);
            System.out.println("maxDistUp="+maxDistUp);
            System.out.println("maxDistDown="+maxDistDown);
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            cpm = new SegmentFactory(refID,
                    genomicPos,
                    testFastaReader,
                    maxDistUp,
                    maxDistDown,
                    chosenEnzymeList);
            return cpm;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testVeryShortCPM (){

        RestrictionEnzyme re = new RestrictionEnzyme("DpnII", "^GATC");
        Map remap = new HashMap<>();
        remap.put(re.getPlainSite(), re);
        SegmentFactory.setRestrictionEnzymeMap(remap);
        chosenEnzymeList = new ArrayList<>();
        chosenEnzymeList.add(re);
        ClassLoader classLoader = CuttingPositionMapTest.class.getClassLoader();
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
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println(seq);
            System.out.println("length="+seq.length());
            System.out.println("genomicPos="+genomicPos);
            System.out.println("maxDistUp="+maxDistUp);
            System.out.println("maxDistDown="+maxDistDown);
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
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
        adjustedGatcSitesOffsetZero=new ArrayList<>();
        int offset=0; // for ^GATC
        for (Integer pos:gatcsites) {
            Integer adjustedPos=pos-maxDistUp-1+offset;
            System.out.println("genomePos ="+genomicPos + ", adjustedPos="+adjustedPos);
            adjustedGatcSitesOffsetZero.add(adjustedPos);
        }
        System.out.println("Calculated sites");
        List<Integer> sites = cpm.getAllCuts();
        for (Integer i:sites) {
            System.out.println("\t site="+i);
        }
      //  Assert.assertTrue(adjustedGatcSitesOffsetZero.equals(cpm.getAllCuts()));

    }








}
