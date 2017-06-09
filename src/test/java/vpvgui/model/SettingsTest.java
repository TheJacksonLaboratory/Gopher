package vpvgui.model;

import javafx.collections.ObservableList;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

/**
 * @author Hannah Blau (blauh)
 * @version last modified 6/9/17
 */
public class SettingsTest {
    private static Settings s;

    @BeforeClass
    public static void setup() throws Exception {
        s = Settings.factory();
        s.setProjectName("FancyProject");
        s.setGenomeFileFrom("http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz");
        s.setTranscriptsFileFrom("http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/refGene.txt.gz");
        s.setRepeatsFileFrom("http://hgdownload.soe.ucsc.edu/goldenPath/hg19/repeats.tar.gz");
        s.setGenomeFileTo("/Users/blauh/vpv/mygenome");
        s.setTranscriptsFileTo("/Users/blauh/vpv/mytranscripts");
        s.setRepeatsFileTo("/Users/blauh/vpv/myrepeats");
        ObservableList<String> rel = s.getRestrictionEnzymesList();
        ObservableList<String> tgl = s.getTargetGenesList();
        for (int i = 0; i < 10; i++) {
            rel.add("RestrictionEnzyme" + i);
            tgl.add("TargetGene" + i);
        }
    }

    @Test
    public void test0ToString() throws Exception {
        Settings t = Settings.factory();

        System.out.print(t);
        System.out.print(s);
    }

    @Test
    public void test1SaveToFile() throws Exception {
        File f = new File("/Users/blauh/vpv/settingsTestFile.txt");

        Settings.saveToFile(s, f);
        System.out.println("\nSettings saved to file");
    }

    @Test
    public void test2Factory() throws Exception {
        Settings t = Settings.factory("/Users/blauh/vpv/settingsTestFile.txt");

        System.out.print("\nSettings restored from file\n" + t);
    }

    @Test
    public void test3writeRead() throws Exception {
        Settings t = Settings.factory();
        File f = new File("/Users/blauh/vpv/test3File.txt");

        t.setProjectName("ANewFancyProject");
        t.setGenomeFileFrom("/Users/blauh/vpv/genomeFile");
        // leave transcriptFileFrom, repeatsFileFrom unspecified
        t.setGenomeFileTo("/Users/somebody/mygenome");
        // leave transcriptsFileTo unspecified
        t.setRepeatsFileTo("/Users/somebody/myrepeats");
        // leave restrictionEnzymesList empty
        ObservableList<String> tgl = t.getTargetGenesList();
        for (int i = 0; i < 8; i++) {
            tgl.add("NewTargetGene" + i);
        }

        System.out.print(t);
        Settings.saveToFile(t, f);
        System.out.println("\nSettings saved to file");
        Settings u = Settings.factory("/Users/blauh/vpv/test3File.txt");
        System.out.print("\nSettings restored from file\n" + u);
    }
}