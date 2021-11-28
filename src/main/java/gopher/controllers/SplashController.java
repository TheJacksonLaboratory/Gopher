package gopher.controllers;

import javafx.application.Preloader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import gopher.gui.factories.PopupFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Optional;


import static gopher.io.Platform.getGopherDir;


/**
 * This will show the startup screen that will include a button for creating a new viewpoint as well as
 * a list of buttons for opening previous projects.
 */
public class SplashController extends Preloader {
    static Logger logger = LoggerFactory.getLogger(SplashController.class.getName());
    @FXML
    private ChoiceBox<String> projectBox;
    @FXML private Button newProjectButton;
    @FXML private Button openProjectButton;
    @FXML private AnchorPane pane;
    @FXML private ImageView openFileView;
    @FXML private ImageView newFileView;



    /** This object is responsible for closing the splash screen and opening the main analysis GUI. */



    private ObservableList<String> existingProjectNames;

    private ProgressBar progressBar;
    private Stage stage;

    public void start(Stage stage) {
        existingProjectNames = getExistingProjectNames();
        projectBox.setItems(existingProjectNames);
        projectBox.getSelectionModel().selectFirst();
        Image newFileImage = new Image(SplashController.class.getResourceAsStream("/img/newFileIcon.png"));
        Image openFileImage = new Image(SplashController.class.getResourceAsStream("/img/openFileIcon.png"));
        this.openFileView.setImage(openFileImage);
        this.newFileView.setImage(newFileImage);
        this.stage = stage;
        stage.setScene(buildScene());
        stage.show();
    }


    @Override
    public void handleStateChangeNotification(StateChangeNotification stateChangeNotification)
    {
        if (stateChangeNotification.getType() == StateChangeNotification.Type.BEFORE_START)
        {
            stage.hide();
        }
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification preloaderNotification)
    {
        progressBar.setProgress(((ProgressNotification) preloaderNotification).getProgress());
    }

    private Scene buildScene() {
        try {
            URL url = getClass().getResource("fxml/splash.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            progressBar = (ProgressBar) loader.getNamespace().get("progressBar"); // Using the ProgressBar's "fx:id" to get it from AppPreloader.fxml
            return new Scene(root);
        } catch (Exception ex) {
            System.err.println("Unable to load FXML from 'fxml/splash.fxml'. Building a backup scene instead.");
            Label loadingLabel = new Label("Loading...");
            progressBar = new ProgressBar(0.0);
            BorderPane root = new BorderPane();
            root.setCenter(progressBar);
            root.setTop(loadingLabel);
            root.setAlignment(loadingLabel, Pos.CENTER);
            return new Scene(root, 300, 100);
        }
    }




    public void newProject(ActionEvent e) {
        Optional<String> opt = PopupFactory.getProjectName();
        if (opt.isEmpty()) {
            PopupFactory.displayError("Could not get valid project name",
                    "Enter a valid name with letters, numbers, or underscore or space!");
            return; // do nothing, the user cancelled!
        }
        String projectname = opt.get();
        if (existingProjectNames.contains(projectname)) {
            PopupFactory.displayError("Error creating new project",
                        String.format("Project name %s already exists",projectname));
                return;
        }
          // TODO  this.switchscreen.createNewProject(projectname);

        e.consume();
    }


    /**
     * @return list of project serialized files in the user's gopher directory.
     */
    private ObservableList<String> getExistingProjectNames() {
        File dir = getGopherDir();
        ObservableList<String> lst = FXCollections.observableArrayList();
        if (dir==null) return lst;
        File[] files = dir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".ser"));
        if (files==null) return lst;
        for (File f : files) {
            /* We want to show just the base name without "ser". Also, transform underscores to spaces */
            String basename = f.getName();
            basename = basename.replaceAll(".ser", "");
            //basename = basename.replaceAll(" ", "_");
            lst.add(basename);
        }
        return lst;
    }


   public Pane getRootPane() { return this.pane; }




    public void openExistingProject(ActionEvent e){
        String selected = this.projectBox.getSelectionModel().getSelectedItem();
        e.consume();
    }
}

