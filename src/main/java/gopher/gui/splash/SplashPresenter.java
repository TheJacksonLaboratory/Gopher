package gopher.gui.splash;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.apache.log4j.Logger;
import gopher.framework.Signal;
import gopher.gui.popupdialog.PopupFactory;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static gopher.io.Platform.getGopherDir;


/**
 * This will show the startup screen that will include a button for creating a new viewpoint as well as
 * a list of buttons for opening previous projects.
 */
public class SplashPresenter implements Initializable {
    static Logger logger = Logger.getLogger(SplashPresenter.class.getName());
    @FXML
    private ChoiceBox<String> projectBox;
    @FXML private Button newProjectButton;
    @FXML private Button openProjectButton;
    @FXML private AnchorPane pane;
    @FXML private ImageView openFileView;
    @FXML private ImageView newFileView;



    /** This object is responsible for closing the splash screen and opening the main analysis GUI. */
    private SwitchScreens switchscreen=null;


    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> filelist = getExistingProjectNames();
        projectBox.setItems(filelist);
        projectBox.getSelectionModel().selectFirst();
        Image newFileImage = new Image(SplashPresenter.class.getResourceAsStream("/img/newFileIcon.png"));
        Image openFileImage = new Image(SplashPresenter.class.getResourceAsStream("/img/openFileIcon.png"));
        this.openFileView.setImage(openFileImage);
        this.newFileView.setImage(newFileImage);
    }

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    public void newProject(ActionEvent e) {
        PopupFactory factory = new PopupFactory();
        String projectname = factory.getProjectName();
        if (factory.wasCancelled())
            return; // do nothing, the user cancelled!
        if (projectname == null || projectname.length() <1) {
            PopupFactory.displayError("Could not get valid project name", "enter a valid name starting with a letter, character or underscore!");
            return;
        } else {
            this.switchscreen.createNewProject(projectname);
        }
        e.consume();
    }


    /** @return list of project serialized files in the user's gopher directory. */
    private ObservableList<String> getExistingProjectNames() {
        File dir= getGopherDir();
       File[] files = dir.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".ser"));//FileFilter
        ObservableList<String> lst = FXCollections.observableArrayList();
       for (File f : files) {
           /* We want to show just the base name without "ser". Also, transform underscores to spaces */
           String basename=f.getName();
           basename = basename.replaceAll(".ser","");
           basename = basename.replaceAll(" ","_");
           lst.add(basename);
       }
        return lst;
    }


   public Pane getRootPane() { return this.pane; }


    public void setSwitchScreen(SwitchScreens screenswitcher) {
        this.switchscreen=screenswitcher;
    }


    public void openExistingProject(ActionEvent e){
        String selected = (String)this.projectBox.getSelectionModel().getSelectedItem();
        switchscreen.openExistingModel(selected);
        e.consume();
    }
}
