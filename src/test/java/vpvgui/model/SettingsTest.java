package vpvgui.model;

import javafx.collections.ObservableList;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.testng.Assert;

import java.io.File;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

/**
 * @author Hannah Blau (blauh)
 * @version last modified 6/9/17
 */
public class SettingsTest {
    private static Settings s;

    /*TemporaryFolder Rule allows creation of files and folders that should be deleted when the test method finishes*/
    @ClassRule
    public static TemporaryFolder folder= new TemporaryFolder();

    public static File fileToSaveTo=null;

    @BeforeClass
    public static void setup() throws Exception {

        fileToSaveTo = folder.newFile("fileToSaveTo");

        s = Settings.factory();
        s.setProjectName("FancyProject");
        s.setGenomeFileFrom("http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz");
        s.setTranscriptsFileFrom("http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/refGene.txt.gz");
        s.setRepeatsFileFrom("http://hgdownload.soe.ucsc.edu/goldenPath/hg19/repeats.tar.gz");
        File genomeFile = folder.newFile("mygenome");
        File transcriptFile = folder.newFile("mytranscripts");
        File repeatsFile = folder.newFile("myrepeats");
        File setttingsPath = folder.newFile("testsettings");
        s.setGenomeFileTo(genomeFile.getAbsolutePath());
        s.setTranscriptsFileTo(transcriptFile.getAbsolutePath());
        s.setRepeatsFileTo(repeatsFile.getAbsolutePath());

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

        //System.out.print(t);
        //System.out.print(s);
        Assert.assertTrue(s.isComplete());
    }

    /* Note: if Settings.saveToFile is successful it returns true. */
    @Test
    public void test1SaveToFile() throws Exception {
       boolean result = Settings.saveToFile(s, fileToSaveTo);
       Assert.assertTrue(result);
    }

    @Test
    public void test2Factory() throws Exception {
        Settings t = Settings.factory(fileToSaveTo.getAbsolutePath());
        Assert.assertNotNull(t);
    }

    @Test
    public void test3writeRead() throws Exception {
        File f = folder.newFile("test3file.txt");
        File genomeFrom = folder.newFile("genomeFrom");
        File genomeTo = folder.newFile("genomeTo");
        File repeatsTo = folder.newFile("repeatsTo");
        Settings t = Settings.factory();

        t.setProjectName("ANewFancyProject");
        t.setGenomeFileFrom(genomeFrom.getAbsolutePath());
        // leave transcriptFileFrom, repeatsFileFrom unspecified
        t.setGenomeFileTo(genomeTo.getAbsolutePath());
        // leave transcriptsFileTo unspecified
        t.setRepeatsFileTo(repeatsTo.getAbsolutePath());
        // leave restrictionEnzymesList empty
        ObservableList<String> tgl = t.getTargetGenesList();
        for (int i = 0; i < 8; i++) {
            tgl.add("NewTargetGene" + i);
        }
        assertFalse(t.isComplete());

        //System.out.print(t);
        boolean results = Settings.saveToFile(t, f);
        Assert.assertTrue(results);

        Settings u = Settings.factory(f.getAbsolutePath());
        Assert.assertEquals(t,u);
    }
}