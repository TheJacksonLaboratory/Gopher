package vpvgui.io;

import vpvgui.model.Model;
import vpvgui.model.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static vpvgui.io.Platform.getVPVDir;

/**
 * Provides methods to save project settings to a file and read settings in from previously written file. Also can
 * return a list of the names of existing projects collected from their stored settings files.
 *
 * @author Hannah Blau (blauh)
 * @version last modified 6/5/17
 */
public class SettingsIO {

    /**
     * Look for project settings files in default VPV directory, populate list of project names to be offered
     * so user can re-open existing project.
     *
     * @return list of project names (empty if no settings files found, or VPV directory is null)
     */
    private List<String> findExistingProjects() {
        File settingsDir = getVPVDir();
        String[] filesInDir;
        List<String> projectNames = new ArrayList<>();
        int suffixStart;

        if (!(settingsDir == null || (filesInDir = settingsDir.list()) == null)) {
            for (String s : filesInDir) {
                suffixStart = s.indexOf(Model.PROJECT_FILENAME_SUFFIX);
                if (suffixStart > 0) {
                    projectNames.add(s.substring(0, suffixStart));
                }
            }
        }
        // want names in alphabetical order, not unpredictable order returned by File.list() method
        projectNames.sort(null);
        return projectNames;
    }

    /**
     * Parse settings file from standard location and return as {@link Settings} bean.
     *
     * @return Settings for specified project
     */
    public static Settings loadSettings(String projectName, Model model) {
//        File projectSettingsPath = new File(getVPVDir().getAbsolutePath()
//                + File.separator + projectName + Model.PROJECT_FILENAME_SUFFIX);
        File projectSettingsPath = new File(getVPVDir(),
                model.getSettings().getProjectName() + Model.PROJECT_FILENAME_SUFFIX);
        if (!projectSettingsPath.exists()) {
            System.err.println("Cannot find project settings file. Exiting.");
            System.exit(1);
        }
        return Settings.factory(projectSettingsPath.getAbsolutePath());
    }

    /**
     * This method gets called when user chooses to close Gui. Content of
     * {@link Settings} bean is written to platform-dependent default location.
     */
    public static void saveSettings(Model model) {
        File settingsDir = getVPVDir();
        String projName = model.getSettings().getProjectName();

        if (projName == null || projName.isEmpty()) {
            System.err.println("Cannot save settings without project name. Exiting.");
            System.exit(1);
        }

        // getVPVDir returns null if user's platform is unrecognized.
        if (settingsDir == null) {
            System.err.println("Directory for settings files is null. Exiting.");
            System.exit(1);
        }

        // Check whether directory already exists; if not, create it.
        if (!(settingsDir.exists() || settingsDir.mkdir())) {
            System.err.println("Cannot create directory for settings files. Exiting.");
            System.exit(1);
        }

        File projectSettingsPath = new File(settingsDir,
                projName + Model.PROJECT_FILENAME_SUFFIX);
        try {
            // Create new file if one does not already exist.
            projectSettingsPath.createNewFile();
        } catch (IOException e) {
            System.err.println("Cannot create settings file. Exiting.");
            System.exit(1);
        }

        // If .vpvgui directory previously contained a settings file for this project,
        // it gets overwritten by the new file.
        // TODO: figure out standard way to handle and report IO exceptions, as saveToFile can cause one
        Settings.saveToFile(model.getSettings(), projectSettingsPath);
    }
}
