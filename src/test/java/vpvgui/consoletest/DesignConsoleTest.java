package vpvgui.consoletest;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.model.Design;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.viewpoint.*;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DesignConsoleTest {

    private static Model testModel;
    private static String testFastaFile = "src/test/resources/testgenome/test_genome.fa";
    private static String referenceSequenceID_1 = "chr_t4_GATC_short_20bp_and_long_24bp_fragments";
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
    static List<RestrictionEnzyme> lst;

    @BeforeClass
    public static void setup() throws Exception {
        RestrictionEnzyme re1 = new RestrictionEnzyme("HindIII", "A^AGCTT");
        RestrictionEnzyme re2 = new RestrictionEnzyme("DpnII", "^GATC");
        Map remap=new HashMap<>();
        remap.put("AAGCTT",re1);
        remap.put("GATC",re2);
        SegmentFactory.setRestrictionEnzymeMap(remap);

        lst=new ArrayList<>();
        lst.add(re1);
        lst.add(re2);
        ViewPoint.setChosenEnzymes(lst);
        // create a model for testing
        testModel = new Model();
        testModel.setProbeLength(120);
        testModel.setMaximumAllowedRepeatOverlap(20);
        testModel.setTilingFactor(2);

        // add a list of two overlapping viewpoints to the model
        final File fasta = new File(testFastaFile);
        testFastaReader = new IndexedFastaSequenceFile(fasta);


        List<ViewPoint> viewPointList = new ArrayList<>();

        // first viewpoint
        ViewPoint testViewpointLupianez_1 = new ViewPoint.Builder(referenceSequenceID_1, genomicPos_1).
                maxDistToGenomicPosUp(maxDistToGenomicPosUp).
                maxDistToGenomicPosDown(maxDistToGenomicPosDown).
              //  cuttingPatterns(testCuttingPatterns).
                fastaReader(testFastaReader).
                minimumSizeUp(minSizeUp).
                minimumSizeDown(minSizeDown).
                maximumSizeUp(maxSizeUp).
                maximumSizeDown(maxSizeDown).
                minimumFragmentSize(minFragSize).
                maximumRepeatContent(minRepFrag).
                marginSize(marginSize).
                build();
        testViewpointLupianez_1.generateViewpointLupianez(fragNumUp,fragNumDown,"ALL",maxSizeUp,maxSizeDown);
        viewPointList.add(testViewpointLupianez_1);

        // second viewpoint with identical parameters
        ViewPoint testViewpointLupianez_2 = new ViewPoint.Builder(referenceSequenceID_1, genomicPos_1).
                maxDistToGenomicPosUp(maxDistToGenomicPosUp).
                maxDistToGenomicPosDown(maxDistToGenomicPosDown).
               // cuttingPatterns(testCuttingPatterns).
                fastaReader(testFastaReader).
                minimumSizeUp(minSizeUp).
                minimumSizeDown(minSizeDown).
                maximumSizeUp(maxSizeUp).
                maximumSizeDown(maxSizeDown).
                minimumFragmentSize(minFragSize).
                maximumRepeatContent(minRepFrag).
                marginSize(marginSize).
                build();
        testViewpointLupianez_2.generateViewpointLupianez(fragNumUp,fragNumDown,"ALL",maxSizeUp,maxSizeDown);
        viewPointList.add(testViewpointLupianez_2);

        testModel.setViewPoints(viewPointList);
    }

    @Test
    public void testGetTotalNumberOfProbeNucleotides() throws Exception {

        List<ViewPoint> viewPointList = testModel.getViewPointList();

        for (ViewPoint vp : viewPointList) {
            System.out.println("--------------------------------------------------------");
            ArrayList<Segment> selectedSegments = vp.getSelectedRestSegList("ALL");

            for (Segment ss : selectedSegments) {
                System.out.println("start: " + ss.getStartPos() + "\t" + "end: " + ss.getEndPos() + "\t" + "selected: " + ss.isSelected());
            }
        }
    }


    @Test
    public void testCuttingPositionMapTwice() throws Exception {
        final File fasta = new File(testFastaFile);
        testFastaReader = new IndexedFastaSequenceFile(fasta);
        System.out.println("testCuttingPositionMapTwice");
        SegmentFactory cpm = new SegmentFactory(referenceSequenceID_1,
                genomicPos_1,
                testFastaReader,
                 maxDistToGenomicPosUp,
                 maxDistToGenomicPosDown,
                lst);
    }

}
