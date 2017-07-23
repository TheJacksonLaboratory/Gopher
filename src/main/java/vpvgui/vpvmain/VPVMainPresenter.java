package vpvgui.vpvmain;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.apache.log4j.Logger;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ConfirmWindow;
import vpvgui.gui.EnzymeCheckBoxWindow;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.analysisPane.VPAnalysisPresenter;
import vpvgui.gui.analysisPane.VPAnalysisView;
import vpvgui.gui.entrezgenetable.EntrezGeneViewFactory;
import vpvgui.gui.help.HelpViewFactory;
import vpvgui.gui.proxy.SetProxyPresenter;
import vpvgui.gui.proxy.SetProxyView;
import vpvgui.gui.settings.SettingsViewFactory;
import vpvgui.io.*;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.project.ViewPointFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static vpvgui.io.Platform.getDefaultProjectPath;


/**
 * A Java app to help design probes for Capture Hi-C
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.0.5 (2017-07-23)
 */
public class VPVMainPresenter implements Initializable {

    /** The Model for the entire analysis. */
    private Model model = null;

    private Stage primaryStage;

    static Logger logger = Logger.getLogger(VPVMainPresenter.class.getName());



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
    private ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9","mm10");

    @FXML
    private ChoiceBox<String> genomeChoiceBox;

    //@FXML
    //private ChoiceBox<String> geneDefinitionsChoiceBox;
    /**
     * Clicking this button downloads the genome build and unpacks it.
     */
    @FXML
    private Button downloadGenome;
    @FXML
    private Button decompressGenomeButton;
    @FXML
    private Button indexGenomeButton;
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
     * Show progress in downloading the Genome and corresponding transcript definition file.
     */
    @FXML
    private ProgressIndicator genomeDecompressPI;
    @FXML
    private ProgressIndicator genomeIndexPI;
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

    @FXML private TextField fragNumUpTextField;
    @FXML private TextField fragNumDownTextField;
    @FXML private TextField minSizeUpTextField;
    @FXML private TextField maxSizeUpTextField;
    @FXML private TextField minSizeDownTextField;
    @FXML private TextField maxSizeDownTextField;
    @FXML private TextField minFragSizeTextField;
    @FXML private TextField maxRepContentTextField;
    /** Show which enzymes the user has chosen. */
    @FXML private Label restrictionEnzymeLabel;
    /** Show how many valid genes were uploaded by user. */
    @FXML private Label nValidGenesLabel;
    /** Show the name of the downloaded genome we areusing. */
    @FXML private Label downloadedGenomeLabel;
    /** Show status of unpacking the downloaded genome. */
    @FXML private Label decompressGenomeLabel;
    /** Show status of indexing the downloaded genome. */
    @FXML private Label indexGenomeLabel;
    /** Show name of downloaded transcripts file. */
    @FXML private Label downloadedTranscriptsLabel;

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
    //@FXML private Tab viewpointTableTab;
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
        logger.trace("initialize() called");
        this.model = initializeModelFromSettingsIfPossible();
        initializeBindings();
        // initialize settings. First check if already exists
        //TODO For now we are using just one default project name. Later extend this so the
        //user can store multiple project settings file under names chosen by them.
        String defaultProjectName = "vpvsettings";
        /*Settings set = null;
        try {
            set = loadSettings(defaultProjectName);
        } catch (IOException i) {
            set = new Settings();
        }*/
        //set.setProjectName(defaultProjectName);
        //model.setSettings(set);

        genomeChoiceBox.setItems(genomeTranscriptomeList);
        genomeChoiceBox.getSelectionModel().selectFirst();
        genomeChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
            setGenomeBuild(newValue);
        }));


        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setModel(this.model);
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);

        createPanes();


        // this.genomeChoiceBox.getItems().addAll(genomeTranscriptomeList);
        // textLabel.textProperty().bind(textTextField.textProperty());*/
    }

    /** Look for the settings file in the .vpvgui directory.
     * If found, initialize the model and the GUI with the data in the settings/project
     * file as avaiable. Otherwise return an empty {@link Model} object.
     * @return A {@link Model} object initialized if possible from a project file
     * TODO -- now just take the default project file. Later allow user to choose a
     * project file from a dialog.
     */
    private Model initializeModelFromSettingsIfPossible() {
        logger.debug("Initializing model...");
        File defaultProject = getDefaultProjectPath();
        logger.debug("Default project file: "+defaultProject);
        if (!defaultProject.exists()) {
            logger.debug("Default project file did not exist, returning empty Model object.");
            return new Model(); /* return an empty Model object. */
        }
        Model model = Model.initializeModelFromSettingsFile(defaultProject.getAbsolutePath());
        logger.debug("Returning model that was initialized from settings file");
        return model;
    }

    public void initStage(Stage stage) {
        primaryStage = stage;
    }


    /** Add theanalysis pane to the GUI. Since we are starting out with just one additional
     * pane, TODO this could be moved to the initialize method.
     */
    private void createPanes() {
        this.analysisPane.getChildren().add(vpanalysisview.getView());
    }



    /**
     * Initialize the bindings to Java bean properties in the model with
     * the GUI elements.
     */
    private void initializeBindings() {
        genomeChoiceBox.valueProperty().bindBidirectional(model.genomeBuildProperty());
        this.fragNumUpTextField.textProperty().bindBidirectional(model.fragNumUpProperty(),new NumberStringConverter());
        this.fragNumDownTextField.textProperty().bindBidirectional(model.fragNumDownProperty(),new NumberStringConverter());
        this.minSizeUpTextField.textProperty().bindBidirectional(model.minSizeUpProperty(),new NumberStringConverter());
        this.maxSizeUpTextField.textProperty().bindBidirectional(model.maxSizeUpProperty(),new NumberStringConverter());
        this.minSizeDownTextField.textProperty().bindBidirectional(model.minSizeDownProperty(),new NumberStringConverter());
        this.maxSizeDownTextField.textProperty().bindBidirectional(model.maxSizeDownProperty(),new NumberStringConverter());
        this.minFragSizeTextField.textProperty().bindBidirectional(model.minFragSizeProperty(),new NumberStringConverter());
        this.maxRepContentTextField.textProperty().bindBidirectional(model.maxRepeatContentProperty(),new NumberStringConverter());


    }

    private void setGenomeBuild(String build) {
        logger.info("Setting genome build to "+build);
        this.genomeBuildLabel.setText(build);
        this.model.setGenomeBuild(build);
        //System.out.println("I am getting newValue=" + newValue);
        //System.out.println("The model should have been set to this by binding, " + model.getGenomeBuild());
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            logger.error("Not Able to find download URL for genome build "+build);
            logger.error(e,e);
            ErrorWindow.display("Error getting genome path",e.toString());
            return;
        }
    }

    public Model getModel() { return this.model; }

    /** This downloads the tar-gzip genome file as chosen by the user from the UCSC download site.
     * It places the compressed file into the directory chosen by the user. The path to the directory
     * is stored in the {@link Model} object using the {@link Model#setGenomeDirectoryPath} function.
     * Following this the user needs to uncompress and index the genome files using the function
     * {@link #decompressGenome(ActionEvent)} which is called after the corresponding button
     * is clicked in the GUI.
     */
    public void downloadGenome() {
        String genome = this.model.getGenomeURL();
        logger.info("About to download genome from "+genome);
        genome = this.genomeChoiceBox.getValue();
        try {
            this.model.adjustGenomeDownloadPaths();
        } catch (DownloadFileNotFoundException e) {
            logger.error("Unable to download genome from "+genome);
            logger.error(e,e);
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
        } else {
            this.model.setGenomeDirectoryPath(file);
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
        this.genomeBuildLabel.setText("downloaded "+genome +" to "+ file.getAbsolutePath());
        logger.info("downloaded "+genome +" to "+ file.getAbsolutePath());
    }


   @FXML public void downloadRefGeneTranscripts(ActionEvent e) {
        String genome = this.model.getGenomeURL();
        if (genome==null)
            genome=genomeChoiceBox.getValue();
        RefSeqDownloader rsd = new RefSeqDownloader(genome);
        String transcriptName = rsd.getTranscriptName();
        String basename=rsd.getBaseName();
        String url=null;
        try {
            url = rsd.getURL();
        } catch (DownloadFileNotFoundException dfne) {
            ErrorWindow.display("Could not identify RefGene file for genome",dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + genome + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath()=="") {
            ErrorWindow.display("Error","Could not get path to download transcript file.");
            return;
        }
        Operation op = new TranscriptDownloadOperation(file.getPath());
        Downloader downloadTask = new Downloader(file, url, basename, transcriptDownloadPI);
        if (downloadTask.needToDownload(op)) {
            Thread th = new Thread(downloadTask);
            th.setDaemon(true);
            th.start();
        }
        /* ToDo-check for completion of download before setting this variable. */
       String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
       this.model.setRefGenePath(abspath);
       this.downloadedTranscriptsLabel.setText(transcriptName);
       e.consume();
    }


    /**
     * TODO delete this, replaced by parsing RefGene.txt.gz (check it works first)
     * Use {@link JannovarTranscriptFileBuilder} to download the transcript file definitions.

    @FXML public void downloadTranscripts(ActionEvent event) {
        event.consume();
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
        if (model.needsProxy()){
            builder.setProxy(model.getHttpProxy(),model.getHttpProxyPort());
        }
        try {
            builder.runJannovar();
        } catch (Exception e) {
            ErrorWindow.display("Error downloading Jannovar transcript file",
                    String.format("Could not download Jannovar transcript file. Are you online?\n%s ",e.toString()));
            return;
        }
        String jannovarSerializedFilePath = builder.getSerializedFilePath();
        this.model.getSettings().setTranscriptsFileTo(jannovarSerializedFilePath);
        try {
            saveSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } */

    /** ToDo wrap this in a Task! */
    @FXML public void decompressGenome(ActionEvent e) {
        e.consume();
        GenomeGunZipper gindexer = new GenomeGunZipper(this.model.getGenomeDirectoryPath());
        gindexer.extractTarGZ();
        //gindexer.indexFastaFiles();
       // Map<String,String> indexedFa=gindexer.getIndexedFastaFiles();
        //model.setIndexedFastaFiles(indexedFa);

    }

    /** ToDo wrap this in a Task! */
    @FXML public void indexGenome(ActionEvent e) {
        e.consume();
        /*GenomeGunZipper gindexer = new GenomeGunZipper(this.model.getGenomeDirectoryPath());
        gindexer.extractTarGZ();
        gindexer.indexFastaFiles();
        Map<String,String> indexedFa=gindexer.getIndexedFastaFiles();
        model.setIndexedFastaFiles(indexedFa);*/

    }



    /** This function is called after the user has chosen restriction enzymes in the
     * corresponding popup window. It passes a list of the {@link RestrictionEnzyme}
     * objects to the {@link Model}.*/
    public void chooseEnzymes() {
        List<RestrictionEnzyme> enzymes = this.model.getRestrictionEnymes();
        List<RestrictionEnzyme> chosenEnzymes = EnzymeCheckBoxWindow.display(enzymes);
        this.model.setChosenRestrictionEnzymes(chosenEnzymes);
        this.restrictionEnzymeLabel.setText(this.model.getRestrictionEnzymeString());
    }

    /**
     * Open a new dialog where the user can paste gene symbols or Entrez Gene IDs.
     * The effect of the command <pre>EntrezGeneViewFactory.display(this.model);</pre>
     * is to pass a list of {@link vpvgui.model.project.VPVGene} objects to the {@link Model}.
     * These objects are used with other information in the Model to create {@link vpvgui.model.project.ViewPoint}
     * objects when the user clicks on {@code Create ViewPoints}.
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
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
        e.consume();
    }

    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link vpvgui.model.project.VPVGene} objects into the {@link Model}
     * object. This function will use the {@link vpvgui.model.project.VPVGene} obejcts and other information
     * to create {@link vpvgui.model.project.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisPresenter} Tab.
     */
    public void createCaptureProbes() {
        ViewPointFactory factory = new ViewPointFactory(model);
        factory.createViewPoints();
        /* The above puts the created viewpoints into the model. */
        SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
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
     * Todo initialize model and ask user to choose a new name.
     */
    private void startNewProject() {
        model=new Model();
    }

    /** Display the settings (parameters) of the current project. */
    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(model.getProperties());
    }

    /**
     * This method gets called when user chooses to close Gui. Content of
     * {@link Settings} bean is written to platform-dependent default location.
     */
    private void saveSettings() throws IOException {
        SettingsIO.saveSettings(model);
    }


    @FXML public void showHelpWindow(ActionEvent e) {
        HelpViewFactory.display();
    }

    /** Todo open a window to set the Proxy. */
    @FXML void setProxyDialog(ActionEvent e) {
        Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        SetProxyView view = new SetProxyView();
        SetProxyPresenter presenter = (SetProxyPresenter) view.getPresenter();
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
        window.setScene(new Scene(view.getView()));
        window.showAndWait();
        int port=presenter.getPort();
        String proxy=presenter.getProxy();
        this.model.setHttpProxy(proxy);
        this.model.setHttpProxyPort(port);

    }


}





