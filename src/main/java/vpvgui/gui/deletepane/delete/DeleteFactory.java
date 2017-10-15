package vpvgui.gui.deletepane.delete;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.entrezgenetable.EntrezGenePresenter;
import vpvgui.gui.entrezgenetable.EntrezGeneView;
import vpvgui.io.Platform;
import vpvgui.model.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeleteFactory {
    static Logger logger = Logger.getLogger(DeleteFactory.class.getName());

    /** This causes the gene upload window to be displayed with an introductory text. */
    public static void display(Model model) {
        Stage window;
        String windowTitle = "Delete unwanted project files";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);
        List<ProjectFile> projects = getProjectFiles(model);

        DeleteView view = new DeleteView();
        DeletePresenter presenter = (DeletePresenter) view.getPresenter();
        presenter.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });

        presenter.updateTable(projects);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }


    public static List<ProjectFile> getProjectFiles(Model model) {
        List<ProjectFile> projects = new ArrayList<>();
        String activeProject = model.getProjectName();
        List<String> projectfiles = getProjectFiles();
        for (String pf:projectfiles) {
            ProjectFile proj = new ProjectFile(pf);
            logger.trace(String.format("Project file %s; active %s",pf,activeProject));
            if (pf.contains(activeProject)) {
                proj.setActive();
            } else {
                projects.add(proj);
            }
        }
        logger.trace(String.format("Retrieved %d project files",projects.size()));
        return projects;
    }


    public static List<String> getProjectFiles() {
        List<String> files = new ArrayList<>();
        File projectDir = Platform.getVPVDir();
        if (!projectDir.isDirectory()) {
            ErrorWindow.display("Error",
                    String.format("Could not get project directory. %s was not a directory", projectDir.getAbsolutePath()));

        } else {
            for (final File fileEntry : projectDir.listFiles()) {
                if (fileEntry.isDirectory()) {
                    continue;
                } else if (fileEntry.getAbsolutePath().endsWith(".ser")) {
                    files.add(fileEntry.getAbsolutePath());
                }
            }
        }
        return files;
    }
}
