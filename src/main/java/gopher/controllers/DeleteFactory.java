package gopher.controllers;

import gopher.gui.factories.PopupFactory;
import gopher.gui.factories.ProjectFile;
import gopher.service.GopherService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import gopher.io.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
@Scope("prototype")
public class DeleteFactory implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DeleteFactory.class.getName());
    public TableColumn projectFileColumn;

    /** This causes the gene upload window to be displayed with an introductory text. */
    public static void display(GopherService model) {
        Stage window;
        String windowTitle = "Delete unwanted project files";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close());
        window.setTitle(windowTitle);

        ClassPathResource deleteResource = new ClassPathResource("fxml/delete.fxml");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(deleteResource.getURL());
            Parent parent = fxmlLoader.load();
            window.setScene(new Scene(parent));
            window.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Label deleteLabel;
        Label undoneLabel;
        TableView<ProjectFile> tableView = new TableView<>();
        TableColumn<ProjectFile, Button> deleteButtonColumn = new TableColumn<>();
        TableColumn<ProjectFile, String> projectFileColumn = new TableColumn<>();
        Button closeButton;
        // The first column with buttons that open new tab for the ViewPoint
        deleteButtonColumn.setSortable(false);
        deleteButtonColumn.setCellValueFactory((cdf -> {
            ProjectFile pf = cdf.getValue();
            // create Button here & set the action
            Button btn = new Button("Delete");
            btn.setOnAction(e -> {
                pf.deleteFile();
                tableView.getItems().removeAll(pf);
                tableView.refresh();
            });
            // wrap it so it can be displayed in the TableView
            return new ReadOnlyObjectWrapper<>(btn);
        }));

        // the second column
        projectFileColumn.setSortable(false);
        projectFileColumn.setEditable(false);
        projectFileColumn.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getProjectName()));
        List<String> files = getProjectFiles();
        List<ProjectFile> projectFiles = files.stream().map(ProjectFile::new).toList();
        ObservableList<ProjectFile> obsProjs = FXCollections.observableArrayList();
        List<ProjectFile> projects = getProjectFiles(model);
        obsProjs.addAll(projects);
        tableView.getItems().clear();
        tableView.getItems().addAll(obsProjs);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }






    private static List<ProjectFile> getProjectFiles(GopherService model) {
        List<ProjectFile> projects = new ArrayList<>();
        String activeProject = model.getProjectName();
        List<String> projectfiles = getProjectFiles();
        for (String pf:projectfiles) {
            ProjectFile proj = new ProjectFile(pf);
            logger.trace(String.format("Project file %s; activeProject \"%s\"",pf,activeProject));
            if (pf.contains(activeProject)) {
                proj.setActive();
            } else {
                projects.add(proj);
            }
        }
        logger.trace(String.format("Retrieved %d project files",projects.size()));
        return projects;
    }


    private static List<String> getProjectFiles() {
        List<String> files = new ArrayList<>();
        File projectDir = Platform.getGopherDir();
        if (!projectDir.isDirectory()) {
            PopupFactory.displayError("Error",
                    String.format("Could not get project directory. %s was not a directory", projectDir.getAbsolutePath()));

        } else {
            for (final File fileEntry : projectDir.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".ser")) {
                    files.add(fileEntry.getAbsolutePath());
                }
            }
        }
        return files;
    }

    public void closeButtonClicked(ActionEvent e) {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.setProperty("jsse.enableSNIExtension", "false");


    }
}
