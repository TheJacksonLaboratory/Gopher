package vpvgui.model;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.model.project.Segment;
import vpvgui.model.project.ViewPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DesignTest {

    private static Model testModel;
    private static Design testDesign;
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



    @BeforeClass
    public static void setup() throws Exception {

        // create a model for testing
        testModel = new Model();
        testModel.setProbeLength(120);
        testModel.setMaximumAllowedRepeatOverlapProperty(20);
        testModel.setTilingFactor(2.0);

        // add a list of two overlapping viewpoints to the model
        final File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);


        List<ViewPoint> viewPointList = new ArrayList<>();

        // first viewpoint
        ViewPoint testViewpointLupianez_1 = new ViewPoint.Builder(referenceSequenceID_1, genomicPos_1).
                maxDistToGenomicPosUp(maxDistToGenomicPosUp).
                maxDistToGenomicPosDown(maxDistToGenomicPosDown).
                cuttingPatterns(testCuttingPatterns).
                fastaReader(testFastaReader).
                minimumSizeUp(minSizeUp).
                minimumSizeDown(minSizeDown).
                maximumSizeUp(maxSizeUp).
                maximumSizeDown(maxSizeDown).
                minimumFragmentSize(minFragSize).
                maximumRepeatContent(minRepFrag).
                marginSize(marginSize).
                build();
        testViewpointLupianez_1.generateViewpointLupianez(fragNumUp,fragNumDown,motif,maxSizeUp,maxSizeDown);
        viewPointList.add(testViewpointLupianez_1);

        // third viewpoint on a different sequence
        ViewPoint testViewpointLupianez_3 = new ViewPoint.Builder(referenceSequenceID_1, genomicPos_1).
                maxDistToGenomicPosUp(maxDistToGenomicPosUp).
                maxDistToGenomicPosDown(maxDistToGenomicPosDown).
                cuttingPatterns(testCuttingPatterns).
                fastaReader(testFastaReader).
                minimumSizeUp(minSizeUp).
                minimumSizeDown(minSizeDown).
                maximumSizeUp(maxSizeUp).
                maximumSizeDown(maxSizeDown).
                minimumFragmentSize(minFragSize).
                maximumRepeatContent(minRepFrag).
                marginSize(marginSize).
                build();
        testViewpointLupianez_3.generateViewpointLupianez(fragNumUp,fragNumDown,motif,maxSizeUp,maxSizeDown);
        viewPointList.add(testViewpointLupianez_3);


        testModel.setViewPoints(viewPointList);

        // use model to create a design for testing
        testDesign = new Design(testModel);

    }

    @Test
    public void testGetTotalNumberOfProbeNucleotides() throws Exception {

        List<ViewPoint> viewPointList = testModel.getViewPointList();
        for (ViewPoint vp : viewPointList) {
            System.out.println("--------------------------------------------------------");
            ArrayList<Segment> selectedSegments = vp.getSelectedRestSegList("GATC");

            for (Segment ss : selectedSegments) {
                System.out.println(ss.getStartPos() + "\t" + ss.getEndPos());
            }
        }

        //int x = testDesign.getTotalNumberOfProbeNucleotides();
        //System.out.println(x);
    }

}
