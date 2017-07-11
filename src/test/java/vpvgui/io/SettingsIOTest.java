package vpvgui.io;

import javafx.collections.ObservableList;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import vpvgui.model.Model;
import vpvgui.model.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static vpvgui.io.SettingsIO.*;
import static vpvgui.io.Platform.getVPVDir;

/**
 * @author Hannah Blau (blauh), peter
 * @version 0.0.2
 */
public class SettingsIOTest {
    private static Model m = null;


    /*TemporaryFolder Rule allows creation of files and folders that should be deleted when the test method finishes*/
    @ClassRule
    public static TemporaryFolder folder= new TemporaryFolder();

    private static File settingsPath=null;

    private static Settings s=null;

    @BeforeClass
    public static void init() throws Exception {
        m = new Model();
        s = m.getSettings();
        s.setProjectName("SettingsIOTestProject");
        s.setGenomeFileFrom("genomeFile.txt.gz");
        s.setTranscriptsFileFrom("transcriptsFile.txt.gz");
        File genomeFile = folder.newFile("mygenome");
        File transcriptFile = folder.newFile("mytranscripts");
        settingsPath = folder.newFile("testsettings");
        s.setGenomeFileTo(genomeFile.getAbsolutePath());
        s.setTranscriptsFileTo(transcriptFile.getAbsolutePath());
        ObservableList<String> rel = s.getRestrictionEnzymesList();
        ObservableList<String> tgl = s.getTargetGenesList();
        for (int i = 0; i < 7; i++) {
            rel.add("RestrictionEnzyme" + i);
            tgl.add("TargetGene" + i);
        }
        SettingsIO.saveSettings(m,settingsPath);
    }



    @Test(expected = IOException.class)
    public void testNonExistentSettingsFile() throws IOException{
        SettingsIO.loadSettings("mysteryProject");
    }



   // Project name is the empty string, should throw exception.
    @Test(expected = IOException.class)
    public void testSavingToInvalidFile() throws Exception {
        String s="";
        File badPath=new File(s);
        saveSettings(m,badPath);
    }

    /* Restore project settings from file, check that they are the same as what you saved.
    TODO why doesnt this work?
    @Test
    public void test5() throws Exception {
        Settings t = loadSettings(settingsPath.getAbsolutePath());
        // The following does not work!
        String expected=m.getSettings().getDefaultPath();
        Assert.assertEquals(expected,settingsPath);
        Assert.assertEquals(t,m.getSettings());

    }*/

    /* Look for project settings files in a temp directory. Create the files you want to find, and
     * throw in one extra file that should not be on the list of project names.
     * Test that this is FALSE
     * TODO CANNOT HAVE TEST USE THE DEFAULT .VPVGUI directory -- this "pollutes" the user directory
     * with test files. I cannot get the TemporaryFolder to work here, unsure why.
    @Test
    public void test6() throws Exception {
        List<String> expected = new ArrayList<>();
        String newFileName;
        TemporaryFolder tmp= new TemporaryFolder();
        File vpv = tmp.newFile();
        for (int i = 0; i < 3; i++) {
            newFileName = "proj" + i + Model.PROJECT_FILENAME_SUFFIX;
            (new File(vpv, newFileName)).createNewFile();
            expected.add("proj" + i);
        }
        expected.add("SettingsIOTestProject");
        (new File(vpv, "miscellaneous.txt")).createNewFile();
        Assert.assertFalse(expected.equals(findExistingProjects()));
    }
    */

}