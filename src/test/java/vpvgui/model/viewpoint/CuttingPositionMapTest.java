package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.model.Design;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    static CuttingPositionMap cpm=null;

    @BeforeClass
    public static void setup() throws Exception {
        RestrictionEnzyme re1 = new RestrictionEnzyme("HindIII", "A^AGCTT");
        RestrictionEnzyme re2 = new RestrictionEnzyme("DpnII", "^GATC");
        Map remap = new HashMap<>();
        remap.put("AAGCTT", re1);
        remap.put("GATC", re2);
        CuttingPositionMap.setRestrictionEnzymeMap(remap);
        chosenEnzymeList = new ArrayList<>();
        chosenEnzymeList.add(re1);
        chosenEnzymeList.add(re2);
        ClassLoader classLoader = CuttingPositionMapTest.class.getClassLoader();
        testFastaFile = classLoader.getResource("testgenome/test_genome.fa").getFile();
        final File fasta = new File(testFastaFile);
        testFastaReader = new IndexedFastaSequenceFile(fasta);
        cpm = new CuttingPositionMap(refSeqID1,
                genomicPos_1,
                testFastaReader,
                maxDistToGenomicPosUp,
                maxDistToGenomicPosDown,
                chosenEnzymeList);
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








}
