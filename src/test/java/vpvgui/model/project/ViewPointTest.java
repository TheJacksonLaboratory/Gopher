package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.tools.ant.util.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.junit.Assert.*;



/**
 * This test class tests an instance of the <i>ViewPoint</i> class. The settings for the call of the constructor are specified at the head of this test class.
 * <p>
 * @author Peter Hansen
 */
public class ViewPointTest {

    /* Create data and ViewPoint object for testing */

    private String testReferenceSequenceID="chr4_ctg9_hap1";
    private Integer testGenomicPos=10000;
    private Integer testMaxUpstreamGenomicPos=100; // replace initial radius with two separate max values for up and downstream. This is more flexible.
    private Integer testMaxDownstreamPos=100;
    private Integer testStartPos=testGenomicPos-testMaxUpstreamGenomicPos;
    private Integer testEndPos=testGenomicPos+testMaxDownstreamPos;
    private String testDerivationApproach = "INITIAL";

    private String[] testCuttingPatterns = new String[]{"TCCG","CA","AAA"};

    private String testFastaFile="src/test/resources/smallgenome/chr4_ctg9_hap1.fa";

    private final File fasta = new File(testFastaFile);
    IndexedFastaSequenceFile testFastaReader = new IndexedFastaSequenceFile(fasta);

    ViewPoint testViewpoint = new ViewPoint(testReferenceSequenceID, testGenomicPos, testMaxUpstreamGenomicPos, testMaxDownstreamPos, testCuttingPatterns, testFastaReader);

    public ViewPointTest() throws FileNotFoundException {} // Not nice, but without there will be an error. Why?


    /* test getter and setter functions */

    @Test
    public void testSetAndGetReferenceID() throws Exception {
        testViewpoint.setReferenceID(testReferenceSequenceID);
        assertEquals(testReferenceSequenceID,testViewpoint.getReferenceID());
    }

    @Test
    public void testSetAndGetGenomicPos() throws Exception {
        testViewpoint.setGenomicPos(testGenomicPos);
        assertEquals(testGenomicPos,testViewpoint.getGenomicPos());
    }

    @Test
    public void testSetAndGetMaxUpstreamGenomicPos() throws Exception {
        testViewpoint.setMaxUpstreamGenomicPos(testMaxUpstreamGenomicPos);
        assertEquals(testMaxUpstreamGenomicPos,testViewpoint.getMaxUpstreamGenomicPos());
    }

    @Test
    public void testSetAndGetMaxDownstreamPos() throws Exception {
        testViewpoint.setMaxDownstreamGenomicPos(testMaxDownstreamPos);
        assertEquals(testMaxDownstreamPos,testViewpoint.getMaxDownstreamGenomicPos());
    }
    @Test
    public void testSetAndGetStartAndEndPos() throws Exception {
        testViewpoint.setStartPos(testStartPos);
        assertEquals(testStartPos,testViewpoint.getStartPos());
        testViewpoint.setEndPos(testEndPos);
        assertEquals(testEndPos,testViewpoint.getEndPos());
    }

    @Test
    public void testSetAndGetDerivationApproach() throws Exception {
        testViewpoint.setDerivationApproach(testDerivationApproach);
        assertEquals(testDerivationApproach,testViewpoint.getDerivationApproach());
    }


    @Test
    public void testGetGenomicPosFromGenomicRelativePos() throws Exception {

        for(int i=0;i<testCuttingPatterns.length;i++) {

            ArrayList<Integer> relPosIntArray = testViewpoint.getCuttingPositionMap().getArrayListForGivenMotif(testCuttingPatterns[i]);

            for(int j=0;j<relPosIntArray.size();j++) {
                Integer genomicPosition = testGenomicPos + relPosIntArray.get(j);
                assertEquals(genomicPosition, testViewpoint.getGenomicPosOfGenomicRelativePos(testGenomicPos,relPosIntArray.get(j)));
            }
        }
    }

    @Test
    public void testExtendFragmentWise() throws Exception {

        /* create viewpoint for testing */

        Integer genomicPos=80;
        Integer maxDistToGenomicPosUp=75;
        Integer maxDistToGenomicPosDown=75;

        String[] testCuttingPatterns = new String[]{"^GATC","A^AGCTT"};
        String testFastaFile="src/test/resources/testgenome/test_genome.fa";
        File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile FastaReader = new IndexedFastaSequenceFile(fasta);

        ViewPoint testViewpointGATC = new ViewPoint("chr_t1_GATC", genomicPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown, testCuttingPatterns, FastaReader);


         /* print genomic position to screen */

        String s_gPos = new String("");
        s_gPos = "";
        for (int k = 0; k < testViewpointGATC.getGenomicPos(); k++) { s_gPos += " ";}
        s_gPos += "|";


        /* print whole test sequence to screen */
        String genomicPosRegionString = FastaReader.getSubsequenceAt("chr_t1_GATC", 0, FastaReader.getSequence("chr_t1_GATC").length()).getBaseString();
        genomicPosRegionString.replaceAll("[\n\r]", "");


        /* print all cutting positions to screen */

        String s_cSites = new String("");
        ArrayList<Integer> relPosArr = testViewpointGATC.getCuttingPositionMap().getHashMapOnly().get("GATC");

        int l=0;
        for(int k=0; k < genomicPosRegionString.length();k++) {
            if(k != testViewpointGATC.relToAbsPos(relPosArr.get(l))) {
                s_cSites += " ";
            } else {
                s_cSites += "^";
                l++;
            }
            if(l==relPosArr.size()) {break;}
        }

        /* print start and end position of the viewpoint to screen */
        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Complete genomic sequence with 'genomicPos' (|) and cutting sites (^) and initial start and end positions (> sta, < end) of the viewpoint");
        System.out.println(s_gPos);
        System.out.println(genomicPosRegionString);
        System.out.println(s_cSites);
        String s_staEnd = getStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        System.out.println(s_staEnd);

        /* move start and end positions to the center */
        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");

        System.out.println("Set new start and end positions near 'genomicPos'");

        testViewpointGATC.setStartPos(testViewpointGATC.getGenomicPos()-4);
        testViewpointGATC.setEndPos(testViewpointGATC.getGenomicPos()+4);

        System.out.println(s_gPos);
        System.out.println(genomicPosRegionString);
        System.out.println(s_cSites);
        s_staEnd = getStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        System.out.println(s_staEnd);

        /* apply extendFragmentWise function */

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Apply function '' to position in upstream directio of the viewpoint");
        Integer upstream_pos=-6;
        System.out.println("Position upstream is: " + upstream_pos + " (X)");
        testViewpointGATC.extendFragmentWise(upstream_pos);
        System.out.println(s_gPos);
        System.out.println(genomicPosRegionString);
        System.out.println(s_cSites);
        String s = new String("");
        for(int i=0;i<testViewpointGATC.relToAbsPos(upstream_pos);i++) { s += " "; } s += "X";
        System.out.println(s);
        s_staEnd = getStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        System.out.println(s_staEnd);

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Apply function '' to position in downstream direction of the viewpoint");
        upstream_pos=30;
        System.out.println("Position upstream is: " + upstream_pos + " (X)");
        testViewpointGATC.extendFragmentWise(upstream_pos);
        System.out.println(s_gPos);
        System.out.println(genomicPosRegionString);
        System.out.println(s_cSites);
        s = new String("");
        for(int i=0;i<testViewpointGATC.relToAbsPos(upstream_pos);i++) { s += " "; } s += "X";
        System.out.println(s);
        s_staEnd = getStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        System.out.println(s_staEnd);

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Apply function '' to position in upstream direction of the viewpoint");
        upstream_pos=-40;
        System.out.println("Position upstream is: " + upstream_pos + " (X)");
        testViewpointGATC.extendFragmentWise(upstream_pos);
        System.out.println(s_gPos);
        System.out.println(genomicPosRegionString);
        System.out.println(s_cSites);
        s = new String("");
        for(int i=0;i<testViewpointGATC.relToAbsPos(upstream_pos);i++) { s += " "; } s += "X";
        System.out.println(s);
        s_staEnd = getStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        System.out.println(s_staEnd);
    }


    @Test
    public void testFragmentListMap() throws Exception {

        /* create viewpoint for testing */

        Integer genomicPos=80;
        Integer maxDistToGenomicPosUp=75;
        Integer maxDistToGenomicPosDown=75;

        String[] testCuttingPatterns = new String[]{"^GATC","A^AGCTT"};
        String testFastaFile="src/test/resources/testgenome/test_genome.fa";
        File fasta = new File(testFastaFile);
        IndexedFastaSequenceFile FastaReader = new IndexedFastaSequenceFile(fasta);

        ViewPoint testViewpointGATC = new ViewPoint("chr_t1_GATC", genomicPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown, testCuttingPatterns, FastaReader);

  /* print genomic position to screen */

        String s_gPos = new String("");
        s_gPos = "";
        for (int k = 0; k < testViewpointGATC.getGenomicPos(); k++) { s_gPos += " ";}
        s_gPos += "|";


        /* print whole test sequence to screen */
        String genomicPosRegionString = FastaReader.getSubsequenceAt("chr_t1_GATC", 0, FastaReader.getSequence("chr_t1_GATC").length()).getBaseString();
        genomicPosRegionString.replaceAll("[\n\r]", "");


        /* print all cutting positions to screen */

        String s_cSites = new String("");
        ArrayList<Integer> relPosArr = testViewpointGATC.getCuttingPositionMap().getHashMapOnly().get("GATC");

        int l=0;
        for(int k=0; k < genomicPosRegionString.length();k++) {
            if(k < testViewpointGATC.relToAbsPos(relPosArr.get(l))) {
                s_cSites += " ";
            } else {
                s_cSites += "^";
                l++;
            }
            if(l==relPosArr.size()) {break;}
        }

        /* print start and end position of the viewpoint to screen as well as cutting sites and fragments */

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Complete genomic sequence with 'genomicPos' (|) and cutting sites (^) and initial start and end positions (> sta, < end) of the viewpoint");
        System.out.println("and print all restriction fragments of the viewpoint. 'T' means selected and 'F' not selected.");
        System.out.println(s_gPos);
        System.out.println(genomicPosRegionString);
        System.out.println(s_cSites);
        String s_staEnd = getStaEndString(testViewpointGATC.getStartPos(),testViewpointGATC.getEndPos());
        System.out.println(s_staEnd);


        /* print all fragments */

        for (int i = 0; i< testViewpointGATC.getRestFragListMap().get("GATC").size();i++) {
            Integer sta = testViewpointGATC.relToAbsPos(testViewpointGATC.getRestFragListMap().get("GATC").get(i).getStartPos());
            Integer end = testViewpointGATC.relToAbsPos(testViewpointGATC.getRestFragListMap().get("GATC").get(i).getEndPos());
            boolean selected = testViewpointGATC.getRestFragListMap().get("GATC").get(i).getSelected();

            String s_frag = new String("");
            for (int j = 0; j<end; j++) {
                if(j<sta) {
                    s_frag += " ";
                }
                else {
                    if(selected) {
                        s_frag += "T";
                    } else {
                        s_frag += "F";
                    }


                }
            }
            System.out.println(s_frag);
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("If the 'selectOrDeSelectFragment' is called with a position that is within a restriction fragment,");
        System.out.println("the fragment will be swichted from 'selected' to 'deselected' or the other way around.");
        System.out.println();
        System.out.println();

        testViewpointGATC.selectOrDeSelectFragment(-20);
        printFragments(testViewpointGATC);
        testViewpointGATC.selectOrDeSelectFragment(-20);
        printFragments(testViewpointGATC);
        testViewpointGATC.selectOrDeSelectFragment(18);
        printFragments(testViewpointGATC);

    }



    private String getStaEndString(Integer sta, Integer end) {
        String s = new String("");
        s = "";
        for(int k=0; k <=end;k++) {
            if(k==sta) {
                s += "> sta";k=k+4;
            }
            else if(k==end) {
                s += "< end";
                break;
            } else {
                s += " ";
            }
        }
        return s;
    }

    private void printFragments(ViewPoint vp) {
        for (int i = 0; i< vp.getRestFragListMap().get("GATC").size();i++) {
            Integer sta = vp.relToAbsPos(vp.getRestFragListMap().get("GATC").get(i).getStartPos());
            Integer end = vp.relToAbsPos(vp.getRestFragListMap().get("GATC").get(i).getEndPos());
            boolean selected = vp.getRestFragListMap().get("GATC").get(i).getSelected();

            String s_frag = new String("");
            for (int j = 0; j<end; j++) {
                if(j<sta) {
                    s_frag += " ";
                }
                else {
                    if(selected) {
                        s_frag += "T";
                    } else {
                        s_frag += "F";
                    }


                }
            }
            System.out.println(s_frag);
        }
    }
}