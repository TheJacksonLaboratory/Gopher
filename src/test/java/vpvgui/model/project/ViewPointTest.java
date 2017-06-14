package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


public class ViewPointTest {

    @Test
    public void testConstructor() throws Exception {

        /* Create string array for cutting patterns */
        String[] cuttingPatterns = new String[]{"TG","CA"};

        /* Create indexed sequence file */
        final File fasta = new File("src/test/resources/smallgenome/chr4_ctg9_hap1.fa"); // index has to be there
        IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(fasta);

        /* Call Contructor */
        ViewPoint myViewpoint = new ViewPoint("chr4_ctg9_hap1",10000, cuttingPatterns, fastaReader);

        // print out whole hash map to show how to access the elements
        System.out.println();
        System.out.println(fastaReader.getSubsequenceAt("chr4_ctg9_hap1",10000-100,10000+100).getBaseString());
        String tssRegionString = fastaReader.getSubsequenceAt("chr4_ctg9_hap1",10000-100,10000+100).getBaseString(); // get sequence around TSS
        for(int i=0;i<cuttingPatterns.length;i++) {
            System.out.println();
            ArrayList<Integer> arr1 = myViewpoint.getCuttingPositionMap().get(cuttingPatterns[i]);
            for(int j=0;j<arr1.size();j++) {
                String s = new String("");
                for(int k=0;k<100+arr1.get(j);k++) {
                    s += " ";
                }
                s += fastaReader.getSubsequenceAt("chr4_ctg9_hap1",10000+arr1.get(j),10000+arr1.get(j)+cuttingPatterns[i].length()-1).getBaseString();
                s += " ";
                s += arr1.get(j);
                System.out.println(s);
            }
            //System.out.print('\n');
        }
        }
}