package vpvgui.vpvmain;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ConfirmWindow;
import vpvgui.gui.EnzymeCheckBoxWindow;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.analysisPane.VPAnalysisPresenter;
import vpvgui.gui.analysisPane.VPAnalysisView;
import vpvgui.gui.entrezgenetable.EntrezGeneViewFactory;
import vpvgui.gui.help.HelpPresenter;
import vpvgui.gui.help.HelpViewFactory;
import vpvgui.gui.settings.SettingsViewFactory;
import vpvgui.io.*;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.Settings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


/**
 * A Java app to help design probes for Capture Hi-C
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.0.2 (July 1, 2017)
 */
public class VPVMainPresenter implements Initializable {

    /**
     * The Model for the entire analysis.
     */
    private Model model = null;

    private Stage primaryStage;

    public void initStage(Stage stage) {
        primaryStage = stage;
    }


    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc.
     */
    @FXML
    private Node rootNode;

    /**
     * List of genome+gene files for download. Used by genomeChoiceBox
     */
    @FXML
    private ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("UCSC-hg19", "UCSC-hg38", "UCSC-mm9","UCSC-mm10");

    @FXML
    private ChoiceBox<String> genomeChoiceBox;

    //@FXML
    //private ChoiceBox<String> geneDefinitionsChoiceBox;
    /**
     * Clicking this button downloads the genome build and unpacks it.
     */
    @FXML
    private Button downloadGenome;
    /**
     * Label for the genome build we want to download.
     */
    @FXML
    private Label genomeBuildLabel;
    /**
     * Label for the transcripts we want to download.
     */
    @FXML
    private Label transcriptsLabel;

    /**
     * Clicking this button will download some combination of genome and (compatible) gene definition files.
     * A file chooser will appear and the user can decide where to download everything. The paths will be stored
     * in the project settings file.
     */
    @FXML
    private Button downloadGenomeButton;
    /**
     * Show progress in downloading the Genome and corresponding transcript definition file.
     */
    @FXML
    private ProgressIndicator genomeDownloadPI;
    /**
     * Button to download RefSeq.tar.gz (transcript/gene definition file
     */
    @FXML
    private Button downloadTranscriptsButton;
    /**
     * Progress indicator for downloading the transcript file
     */
    @FXML
    private ProgressIndicator transcriptDownloadPI;


    @FXML
    private TextField textTextField;

    @FXML
    private Button showButton;

    @FXML
    private Button exitButton;

    @FXML
    private Button enterGeneListButton;

    @FXML
    Button createCaptureProbesButton;

    @FXML
    private Label textLabel;

    @FXML
    private TabPane tabpane;

    @FXML
    private AnchorPane analysisPane;

    @FXML
    private Tab setuptab;

    @FXML
    MenuItem showSettingsCurrentProject;

    @FXML MenuItem helpMenuItem;


    @FXML
    private Tab analysistab;
    /** Tab for the viewpoints (will show statistics and a table with individual viewpoints. */
    @FXML private Tab viewpointTableTab;
    /** Tab to show a selected viewpoint in a browser. */
    @FXML private Tab singleviewpointTab;

    /**
     * Click this to choose the restriction enzymes with which to do the capture Hi-C cutting
     */
    @FXML
    private Button chooseEnzymeButton;


    private VPAnalysisPresenter vpanalysispresenter;
    private VPAnalysisView vpanalysisview;

    @FXML
    void exitButtonClicked(ActionEvent e) {
        e.consume();
        closeWindow();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.model = new Model();
        initializeBindings();
        // initialize settings. First check if already exists
        //TODO For now we are using just one default project name. Later extend this so the
        //user can store multiple project settings file under names chosen by them.
        String defaultProjectName = "vpvsettings";
        Settings set = null;
        try {
            set = loadSettings(defaultProjectName);
        } catch (IOException i) {
            set = new Settings();
        }
        set.setProjectName(defaultProjectName);
        model.setSettings(set);

        genomeChoiceBox.setItems(genomeTranscriptomeList);
        genomeChoiceBox.getSelectionModel().selectFirst();
        genomeChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setGenomeBuild(newValue);
        }));


        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();

        createPanes();


        // this.genomeChoiceBox.getItems().addAll(genomeTranscriptomeList);
        // textLabel.textProperty().bind(textTextField.textProperty());*/
    }

    private void createPanes() {
        this.analysisPane.getChildren().add(vpanalysisview.getView());
        /* todo -- analogous for other tabs/p[anes.*/
    }



    /**
     * Initialize the bindings to Java bean properties in the model with
     * the GUI elements.
     */
    private void initializeBindings() {
        genomeChoiceBox.valueProperty().bindBidirectional(model.genomeBuildProperty());
        //genomeBuildLabel.textProperty().bind(genomeChoiceBox.valueProperty());

    }

    private void setGenomeBuild(String newValue) {
        //System.out.println("I am getting newValue=" + newValue);
        //System.out.println("The model should have been set to this by binding, " + model.getGenomeBuild());
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            ErrorWindow.display("Error getting genome path",e.toString());
            return;
        }
        /* The model directly updates the settings object to reflect
        the genome build etc, and the following command will save the settings to disk.
         */
        try {
            saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadGenome() {

        String genome = this.model.getGenomeURL();
        genome = this.genomeChoiceBox.getValue();
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            ErrorWindow.display("Error", e.getMessage());
            return;
        }
        /** The Model should now be set to hg19, hg38, mm9, or mm10 etc.*/

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + genome + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        Operation op = new GenomeDownloadOperation(file.getPath());
        if (file==null || file.getAbsolutePath()=="") {
            ErrorWindow.display("Error","Could not get path to download genome.");
            return;
        }
        if (model.getGenomeURL()==null || model.getGenomeURL().isEmpty()) {
            ErrorWindow.display("Error","Genome URL (UCSC address) not initialized");
            return;
        }
        if (model.getGenomeBasename()==null || model.getGenomeBasename().isEmpty()) {
            ErrorWindow.display("Error","Genome Basename (usually chromFa.tar.gz) not initialized");
            return;
        }
        Downloader downloadTask = new Downloader(file, model.getGenomeURL(), model.getGenomeBasename(), genomeDownloadPI);
        if (downloadTask.needToDownload(op)) {
            Thread th = new Thread(downloadTask);
            th.setDaemon(true);
            th.start();
        }
    }

    /**
     * Use {@link JannovarTranscriptFileBuilder} to download the transcript file definitions.
     */
    public void downloadTranscripts() {
        String genome = this.genomeChoiceBox.getValue();
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            ErrorWindow.display("Error", e.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for transcripts file for " + genome + " (will be created by Jannovar if not found).");
        File destinationdirectory = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (destinationdirectory == null) {
            return; // assume the user clicked the cancel button, we do not need to show an error message
        }
        JannovarTranscriptFileBuilder builder = new JannovarTranscriptFileBuilder(genome, destinationdirectory);
        try {
            builder.runJannovar();
        } catch (Exception e) {
            ErrorWindow.display("Error downloading Jannovar transcript file",
                    String.format("Could not download Jannovar transcript file. Are you online?\n%s ",e.toString()));
            return;
        }
        String jannovarSerializedFilePath = builder.getSerializedFilePath();
        System.out.println("PATH=" + jannovarSerializedFilePath);
        this.model.getSettings().setTranscriptsFileTo(jannovarSerializedFilePath);
        try {
            saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    /* TODO -- complete me */
    public void chooseEnzymes() {
        List<RestrictionEnzyme> enzymes = this.model.getRestrictionEnymes();
        List<RestrictionEnzyme> chosenEnzymes = EnzymeCheckBoxWindow.display(enzymes);

    }

    /**
     * Open a new dialog where the user can paste gene symbols or Entrez Gene IDs.
     * See {@link EntrezGeneViewFactory} for logic.
     *
     * @param e
     */
    public void enterGeneList(ActionEvent e) {
        EntrezGeneViewFactory.display(this.model);
        /** The following command is just for debugging. We now have all VPVGenes, but still need to
         * add information about the restriction enzymes and the indexed FASTA file.
         */
        this.model.debugPrintVPVGenes();
        e.consume();
    }

    public void createCaptureProbes() {
        SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
        System.out.println("createCaptureProbes setting tab");
        this.vpanalysispresenter.setModel(this.model);
        this.vpanalysispresenter.showVPTable();
        selectionModel.select(this.analysistab);
    }

    public void closeWindow() {
        boolean answer = ConfirmWindow.display("Alert", "Are you sure you want to quit?");
        if (answer)
            System.exit(0);
    }

    /**
     * Update model with new settings object that will be gradually filled in by user via gui.
     */
    private void startNewProject() {
        model.setSettings(Settings.factory());
    }

    /**
     * Parse settings file from standard location and return as {@link Settings} bean.
     *
     * @return Settings for specified project
     */
    private Settings loadSettings(String projectName) throws IOException {
        return SettingsIO.loadSettings(projectName);
    }


    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(model.getSettings());
    }

    /**
     * This method gets called when user chooses to close Gui. Content of
     * {@link Settings} bean is written to platform-dependent default location.
     */
    private void saveSettings() throws IOException {
        SettingsIO.saveSettings(model);
    }


    @FXML public void showHelpWindow(ActionEvent e) {
        System.out.println("Calling help");
        HelpViewFactory.display();
    }


}





