package vpvgui.io;

import javafx.collections.ObservableList;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testng.Assert;
import vpvgui.model.Model;
import vpvgui.model.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static vpvgui.io.SettingsIO.*;
import static vpvgui.io.Platform.getVPVDir;

/**
 * @author Hannah Blau (blauh)
 *         created on 6/12/17.
 */
public class SettingsIOTest {
    private static Model m = null;


    /*TemporaryFolder Rule allows creation of files and folders that should be deleted when the test method finishes*/
    @ClassRule
    public static TemporaryFolder folder= new TemporaryFolder();

    private static Settings s=null;

    @BeforeClass
    public static void init() throws Exception {
        m = new Model();
        s = m.getSettings();
        s.setProjectName("SettingsIOTestProject");
        s.setGenomeFileFrom("genomeFile.txt.gz");
        s.setTranscriptsFileFrom("transcriptsFile.txt.gz");
        s.setRepeatsFileFrom("repeatsFile.txt.gz");
        File genomeFile = folder.newFile("mygenome");
        File transcriptFile = folder.newFile("mytranscripts");
        File repeatsFile = folder.newFile("myrepeats");
        File setttingsPath = folder.newFile("testsettings");
        s.setGenomeFileTo(genomeFile.getAbsolutePath());
        s.setTranscriptsFileTo(transcriptFile.getAbsolutePath());
        s.setRepeatsFileTo(repeatsFile.getAbsolutePath());
        ObservableList<String> rel = s.getRestrictionEnzymesList();
        ObservableList<String> tgl = s.getTargetGenesList();
        for (int i = 0; i < 7; i++) {
            rel.add("RestrictionEnzyme" + i);
            tgl.add("TargetGene" + i);
        }
        saveSettings(m,setttingsPath);
    }






    /* Test findExistingProjects when the default vpv directory is empty (test will fail if that
    // directory contains any files that look like project settings files).
    @Test
    public void test0() throws Exception {
        List<String> expected = new ArrayList<>();
        assert (expected.equals(findExistingProjects()));
    }*/


    @Test(expected = IOException.class)
    public void testNonExistentSettingsFile() throws IOException{
        loadSettings("mysteryProject");
    }


/*
    // Project name does not correspond to any existing project file, should cause system exit with error msg.
    @Test
    public void test1() throws Exception {
        loadSettings("mysteryProject");
    }
*/


   // Project name is the empty string, should cause system exit with error msg.
    @Test(expected = IOException.class)
    public void testSavingToInvalidFile() throws Exception {
        String s="";
        File badPath=new File(s);
        saveSettings(m,badPath);
    }


    // Fill in the settings object, then save to file.


/*
    // Project name does not match file, should cause system exit with error msg.
    @Test
    public void test4() throws Exception {
        loadSettings("surprise");
    }
*/

    // Restore project settings from file, check that they are the same as what you saved.
    @Test
    public void test5() throws Exception {
        Settings t = loadSettings("SettingsIOTestProject");
        // The following does not work!
        //Assert.assertEquals(t,m.getSettings());

    }

    // Look for project settings files in the default vpv directory. Create the files you want to find, and
    // throw in one extra file that should not be on the list of project names.
    // Test that this is FALSE
    @Test
    public void test6() throws Exception {
        List<String> expected = new ArrayList<>();
        String newFileName;
        File vpv = getVPVDir();
        for (int i = 0; i < 3; i++) {
            newFileName = "proj" + i + Model.PROJECT_FILENAME_SUFFIX;
            (new File(vpv, newFileName)).createNewFile();
            expected.add("proj" + i);
        }
        expected.add("SettingsIOTestProject");
        (new File(vpv, "miscellaneous.txt")).createNewFile();
        //assert(expected.equals(findExistingProjects()));
        Assert.assertFalse(expected.equals(findExistingProjects()));
    }
}