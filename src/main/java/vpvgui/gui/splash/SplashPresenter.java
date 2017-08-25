package vpvgui.gui.splash;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import org.apache.log4j.Logger;
import vpvgui.framework.Signal;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.Popups;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static vpvgui.io.Platform.getVPVDir;


/**
 * This will show the startup screen that will include a button for creating a new project as well as
 * a list of buttons for opening previous projects.
 */
public class SplashPresenter implements Initializable {
    static Logger logger = Logger.getLogger(SplashPresenter.class.getName());
    @FXML
    private ChoiceBox projectBox;
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

        /*
        .getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
      @Override
         */
        projectBox.getSelectionModel().
                selectedItemProperty().
                addListener((observable,  oldValue,  newValue)
                        -> {
                    System.out.println(newValue);
                });
        Image newFileImage = new Image(SplashPresenter.class.getResourceAsStream("/img/newFileIcon.png"));
        Image openFileImage = new Image(SplashPresenter.class.getResourceAsStream("/img/openFileIcon.png"));
        this.openFileView.setImage(openFileImage);
        this.newFileView.setImage(newFileImage);

    }

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    public void newProject(ActionEvent e) {
        String projectname=Popups.getStringFromUser("New Project Name","name of project","Enter name of new project");
        if (projectname==null) {
            ErrorWindow.display("Error","Please enter a valid project name!");
            return;
        } else {
            this.switchscreen.createNewProject(projectname);
        }
        e.consume();
    }



    private ObservableList<String> getExistingProjectNames() {
        File dir=getVPVDir();
       /* todo find all settings files */
       File[] files = dir.listFiles(new FileFilter() {
           @Override
           public boolean accept(File pathname) {
               if (pathname.getAbsolutePath().endsWith(".settings")) return true;
               return false;
           }
       });
        ObservableList<String> lst = FXCollections.observableArrayList();
       for (File f : files) {
           lst.add(f.getName());
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
