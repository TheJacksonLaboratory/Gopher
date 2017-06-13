package vpvgui.io;

import javafx.collections.ObservableList;
import org.junit.Test;
import vpvgui.model.Model;
import vpvgui.model.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static vpvgui.io.SettingsIO.*;
import static vpvgui.io.Platform.getVPVDir;

/**
 * @author Hannah Blau (blauh)
 *         created on 6/12/17.
 */
public class SettingsIOTest {
    private static Model m = new Model();

    // Test findExistingProjects when the default vpv directory is empty (test will fail if that
    // directory contains any files that look like project settings files).
    @Test
    public void test0() throws Exception {
        List<String> expected = new ArrayList<>();
        assert (expected.equals(findExistingProjects()));
    }

/*
    // Project name does not correspond to any existing project file, should cause system exit with error msg.
    @Test
    public void test1() throws Exception {
        loadSettings("mysteryProject");
    }
*/

/*
    // Project name is the empty string, should cause system exit with error msg.
    @Test
    public void test2() throws Exception {
        saveSettings(m);
    }
*/

    // Fill in the settings object, then save to file.
    @Test
    public void test3() throws Exception {
        Settings s = m.getSettings();
        s.setProjectName("SettingsIOTestProject");
        s.setGenomeFileFrom("genomeFile.txt.gz");
        s.setTranscriptsFileFrom("transcriptsFile.txt.gz");
        s.setRepeatsFileFrom("repeatsFile.txt.gz");
        s.setGenomeFileTo("/Users/blauh/vpv/mygenome");
        s.setTranscriptsFileTo("/Users/blauh/vpv/mytranscripts");
        s.setRepeatsFileTo("/Users/blauh/vpv/myrepeats");
        ObservableList<String> rel = s.getRestrictionEnzymesList();
        ObservableList<String> tgl = s.getTargetGenesList();
        for (int i = 0; i < 7; i++) {
            rel.add("RestrictionEnzyme" + i);
            tgl.add("TargetGene" + i);
        }
        saveSettings(m);
    }

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
        assert(t.equals(m.getSettings()));
    }

    // Look for project settings files in the default vpv directory. Create the files you want to find, and
    // throw in one extra file that should not be on the list of project names.
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
        assert(expected.equals(findExistingProjects()));
    }
}