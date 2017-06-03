package vpvgui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.DirectoryChooser;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ConfirmWindow;
import vpvgui.gui.CopyPasteGenesWindow;
import vpvgui.gui.EnzymeCheckBoxWindow;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.entrezgenetable.PopupController;
import vpvgui.io.*;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.Settings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import static vpvgui.io.Platform.getVPVDir;

public class Controller implements Initializable {
    /**
     * The Model for the entire analysis.
     */
    private Model model=null;

    private Stage primaryStage;
    public void initStage( Stage stage){ primaryStage = stage;}


    /** This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc.
     */
    @FXML private Node rootNode;

    /** List of genome+gene files for download. Used by genomeChoiceBox */
    @FXML
    private ObservableList<String> genomeTranscriptomeList= FXCollections.observableArrayList("UCSC-hg19","UCSC-hg38", "UCSC-mm10");

    @FXML
    private ChoiceBox<String> genomeChoiceBox;

    @FXML private ChoiceBox<String> geneDefinitionsChoiceBox;
    /** Clicking this button downloads the genome build and unpacks it. */
    @FXML private Button downloadGenome;
    /** Label for the genome build we want to download. */
    @FXML private Label genomeBuildLabel;
    /** Label for the transcripts we want to download. */
    @FXML private Label transcriptsLabel;

    /** Clicking this button will download some combination of genome and (compatible) gene definition files.
     * A file chooser will appear and the user can decide where to download everything. The paths will be stored
     * in the project settings file. */
    @FXML private Button downloadGenomeButton;
    /** Show progress in downloading the Genome and corresponding transcript definition file.*/
    @FXML private ProgressIndicator genomeDownloadPI;
    /** Button to download RefSeq.tar.gz (transcript/gene definition file */
    @FXML private Button downloadTranscriptsButton;
    /** Progress indicator for downloading the transcript file */
    @FXML private ProgressIndicator transcriptDownloadPI;


    @FXML
    private TextField textTextField;

    @FXML
    private Button showButton;

    @FXML
    private Button exitButton;

    @FXML private Button enterGeneListButton;

    @FXML Button createCaptureProbesButton;

    @FXML
    private Label textLabel;

    @FXML private TabPane tabpane;

    @FXML private Tab setuptab;

    @FXML private Tab analysistab;

    /** Click this to choose the restriction enzymes with which to do the captuyre Hi-C cutting */
    @FXML private Button chooseEnzymeButton;

    @FXML
    void exitButtonClicked(ActionEvent e) {
        e.consume();
        System.out.println("clclcl");
        closeWindow();
    }

    @FXML
    void showButtonClicked(ActionEvent event) {
        System.out.println("Content: " + textTextField.getText());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.model = new Model();
        initializeBindings();
        genomeChoiceBox.setItems(genomeTranscriptomeList);
        genomeChoiceBox.getSelectionModel().selectFirst();
        genomeChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setGenomeBuild(newValue);

        }));

               // this.genomeChoiceBox.getItems().addAll(genomeTranscriptomeList);
       // textLabel.textProperty().bind(textTextField.textProperty());
    }

    /** Initialize the bindings to Java bean properties in the model with
     * the GUI elements.
     */
    private void initializeBindings() {
        genomeChoiceBox.valueProperty().bindBidirectional(model.genomeBuildProperty());
        //genomeBuildLabel.textProperty().bind(genomeChoiceBox.valueProperty());

    }

    private void setGenomeBuild(String newValue) {
        System.out.println("I am getting newValue="+newValue);
        System.out.println("The model should have been set to this by binding, "+ model.getGenomeBuild());
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {

        }
    }

    public void downloadGenome() {

        //String genome = this.genomeChoiceBox.getValue();
        String genome = this.model.getGenomeURL();
        genome = this.genomeChoiceBox.getValue();
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            ErrorWindow.display("Error",e.getMessage());
            return;
        }
        /** The Model should now be set to hg19,hg38 or mm10 etc.*/

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + genome + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        Operation op = new UCSChg37Operation(file.getPath());
        Downloader downloadTask = new Downloader(file,model.getGenomeURL(),model.getGenomeBasename(),genomeDownloadPI);
        if (downloadTask.needToDownload(op)) {
            Thread th = new Thread(downloadTask);
            th.setDaemon(true);
            th.start();
        }
    }

    public void downloadTranscripts() {
        // todo -- hook up with Jannovar
        String genome = this.genomeChoiceBox.getValue();
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            ErrorWindow.display("Error",e.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for transcripts file for " + genome + " (will be created by Jannovar if not found).");
        File destinationdirectory = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        JannovarTranscriptFileBuilder builder = new JannovarTranscriptFileBuilder(genome,destinationdirectory);


        /*Operation op = new RefSeqOperation(dir.getPath());
        Downloader downloadTask = new Downloader(dir,model.getTranscriptsURL(),model.getTranscriptsBasename(),transcriptDownloadPI);
        if (downloadTask.needToDownload(op)) {
            Thread th = new Thread(downloadTask);
            th.setDaemon(true);
            th.start();
        }*/
        ErrorWindow.display("TO DO","Implement command to create transcript definition file with Jannovar here");
    }

    public void chooseEnzymes() {
        List<RestrictionEnzyme> enzymes = this.model.getRestrictionEnymes();
        List<RestrictionEnzyme> chosenEnzymes = EnzymeCheckBoxWindow.display(enzymes);
    }


    public void enterGeneList() {
        showPopupWindow();
        return; /*
        String [] targetgenes = CopyPasteGenesWindow.display();
        if (targetgenes == null) {
            System.err.println("[TODO] implement me, targetgenes==null");
        } else {
            System.err.println("[TODO] implement me, targetgenes are:");
            for (String tg : targetgenes) {
                System.err.println("\t"+tg);
            }
        }*/
    }

    public void createCaptureProbes() {
        SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
        selectionModel.select(analysistab);
    }

    public void closeWindow() {
        boolean answer = ConfirmWindow.display("Alert", "Are you sure you want to quit?");
        if (answer)
            System.exit(0);
    }
    /**
     * Parse settings file from standard location and return as {@link Settings} bean.
     * @return Settings for specified project
     */
    private Settings loadSettings(String projectName) {
//        File projectSettingsPath = new File(getVPVDir().getAbsolutePath()
//                + File.separator + projectName + "-settings.txt");
        File projectSettingsPath = new File(getVPVDir(),
                model.getSettings().getProjectName() + "-settings.txt");
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
    private void saveSettings() {
        File settingsDir = getVPVDir();

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
                model.getSettings().getProjectName() + "-settings.txt");
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

    private HashMap<String, Object> showPopupWindow() {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();

        FXMLLoader loader = new FXMLLoader();
        System.out.println("GET CLASS="+getClass().getResource("/fxml/popup.fxml"));

        loader.setLocation(getClass().getResource("/fxml/popup.fxml"));
        // initializing the controller
        PopupController popupController = new PopupController();
        loader.setController(popupController);
        Parent layout;
        try {
            layout = loader.load();
            Scene scene = new Scene(layout);
            // this is the popup stage
            Stage popupStage = new Stage();
            // Giving the popup controller access to the popup stage (to allow the controller to close the stage)
            popupController.setStage(popupStage);
            if(this.rootNode.getScene().getWindow()!=null) {
                popupStage.initOwner(this.rootNode.getScene().getWindow());
            }
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.setScene(scene);
            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return popupController.getResult();

    }
}




