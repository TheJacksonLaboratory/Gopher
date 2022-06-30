package gopher.gui.util;

import gopher.configuration.GopherConfig;
import gopher.gui.factories.PopupFactory;
import javafx.application.Preloader;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.controlsfx.dialog.CommandLinksDialog;

import java.io.File;
import java.util.*;

/**
 * JavaFX preloader class that sets the project name/project file.
 * The user can either enter the name of a new project or choose an existing project from
 * a list.
 * @author  Peter N Robinson
 */
public final class MyPreloader extends Preloader {
    private Stage preloaderStage;

    private static String projectName;

    private static boolean isNewProject = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;
        ClassLoader classLoader = MyPreloader.class.getClassLoader();
        GopherConfig gopherConfig = new GopherConfig();
        File appHomeDir = gopherConfig.appHomeDir();
        // get all *.ser files
        Map<String, String> serFiles = new HashMap<>();
        if (appHomeDir.isDirectory()) {
            for (final File fileEntry : appHomeDir.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".ser")) {
                    String bname = fileEntry.getName();
                    serFiles.put(bname, fileEntry.getAbsolutePath());
                }
            }
        }
        var newGopherProject = new CommandLinksDialog.CommandLinksButtonType("New Project", "Start a new GOPHER project", true);
        List<CommandLinksDialog.CommandLinksButtonType> commands = new ArrayList<>();
        commands.add(newGopherProject);
        for (var e: serFiles.entrySet()) {
            var projectCommand = new CommandLinksDialog.CommandLinksButtonType(e.getKey(), e.getValue(), false);
            commands.add(projectCommand);
        }
        CommandLinksDialog dialog = new CommandLinksDialog(commands);

        dialog.setTitle("Get started");
        dialog.setHeaderText("Select project");
        dialog.setContentText("Start a new project or open an existing one.");
        Optional<ButtonType> opt = dialog.showAndWait();
        if (opt.isPresent()) {
            ButtonType btype = opt.get();
            if ("New Project".equals(btype.getText())) {
                getNewProjectName();
                isNewProject = true;
            } else {
                MyPreloader.projectName = btype.getText();
            }
        }
    }

    private void getNewProjectName() {
        MyPreloader.projectName =
               PopupFactory.getStringFromUser("New Project", "project name", "Enter new project name");
    }


    public static String getProjectName() {
        return projectName;
    }

    public static boolean isIsNewProject() { return isNewProject; }

    @Override
    public void handleStateChangeNotification(StateChangeNotification
                                                      stateChangeNotification) {
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_START) {
            preloaderStage.hide();
        }
    }
}