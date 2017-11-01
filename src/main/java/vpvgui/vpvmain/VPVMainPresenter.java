package vpvgui.vpvmain;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.apache.log4j.Logger;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ConfirmWindow;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.Popups;
import vpvgui.gui.analysisPane.VPAnalysisPresenter;
import vpvgui.gui.analysisPane.VPAnalysisView;
import vpvgui.gui.createviewpointpb.CreateViewpointPBPresenter;
import vpvgui.gui.createviewpointpb.CreateViewpointPBView;
import vpvgui.gui.deletepane.delete.DeleteFactory;
import vpvgui.gui.entrezgenetable.EntrezGeneViewFactory;
import vpvgui.gui.enzymebox.EnzymeViewFactory;
import vpvgui.gui.help.HelpViewFactory;
import vpvgui.gui.logviewer.LogViewerFactory;
import vpvgui.gui.proxy.SetProxyPresenter;
import vpvgui.gui.proxy.SetProxyView;
import vpvgui.gui.qcCheckPane.QCCheckFactory;
import vpvgui.gui.settings.SettingsViewFactory;
import vpvgui.io.*;
import vpvgui.model.*;
import vpvgui.model.viewpoint.SimpleViewPointCreationTask;
import vpvgui.model.viewpoint.ViewPoint;
import vpvgui.model.viewpoint.ExtendedViewPointCreationTask;
import vpvgui.model.viewpoint.ViewPointCreationTask;
import vpvgui.util.SerializationManager;
import vpvgui.util.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


/**
 * A Java app to help design probes for Capture Hi-C
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.0.6 (2017-10-14)
 */
public class VPVMainPresenter implements Initializable {
    static Logger logger = Logger.getLogger(VPVMainPresenter.class.getName());
    /** The Model for the entire analysis. */
    private Model model = null;
    /** Convenience class that knows about the required order of operations and dependencies.*/
    private Initializer initializer=null;
    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc. It is set in the FXML
     * document to refer to the Anchor pane that is the root node of the GUI.
     */
    @FXML
    private Node rootNode;
    /** List of genome builds. Used by genomeChoiceBox*/
    @FXML
    private ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9","mm10");
    /** List of genome builds. Used by genomeChoiceBox*/
    @FXML
    private ObservableList<String> approachList = FXCollections.observableArrayList("Simple", "Extended");
    @FXML
    private ChoiceBox<String> genomeChoiceBox;
    @FXML
    private ChoiceBox<String> approachChoiceBox;
    /** Clicking this button downloads the genome build and unpacks it.*/
    @FXML
    private Button downloadGenome;
    @FXML
    private Button decompressGenomeButton;
    @FXML
    private Button indexGenomeButton;
    /** Label for the genome build we want to download. */
    @FXML
    private Label genomeBuildLabel;
    /** Label for the transcripts we want to download.*/
    @FXML
    private Label transcriptsLabel;
    /** Show which design approach */
    @FXML private Label approachLabel;
    /**
     * Clicking this button will download some combination of genome and (compatible) gene definition files.
     * A file chooser will appear and the user can decide where to download everything. The paths will be stored
     * in the viewpoint settings file.
     */
    @FXML private Button downloadGenomeButton;
    /** Show progress in downloading the Genome and corresponding transcript definition file.  */
    /** Button to download RefSeq.tar.gz (transcript/gene definition file  */
    @FXML private Button downloadTranscriptsButton;
    @FXML private ProgressIndicator genomeDownloadPI;
    /** Show progress in downloading the Genome and corresponding transcript definition file.  */
    @FXML private ProgressIndicator genomeDecompressPI;
    @FXML private ProgressIndicator genomeIndexPI;
    /** Progress indicator for downloading the transcript file */
    @FXML private ProgressIndicator transcriptDownloadPI;

    //@FXML private TextField fragNumUpTextField;
    //@FXML private TextField fragNumDownTextField;
    @FXML private TextField sizeUpTextField;
    //@FXML private TextField maxSizeUpTextField;
    @FXML private TextField sizeDownTextField;
//    @FXML private TextField maxSizeDownTextField;
    @FXML private TextField minFragSizeTextField;
    @FXML private TextField maxRepContentTextField;
    @FXML private TextField minGCContentTextField;
    @FXML private TextField maxGCContentTextField;

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
    @FXML RadioMenuItem tiling1;
    @FXML RadioMenuItem tiling2;
    @FXML RadioMenuItem tiling3;
    @FXML RadioMenuItem tiling4;
    @FXML RadioMenuItem tiling5;
    @FXML private Button showButton;
    @FXML private Button exitButton;
    @FXML private Button enterGeneListButton;
    @FXML private Button createCaptureProbesButton;

    @FXML private TabPane tabpane;
    @FXML private StackPane analysisPane;
    @FXML private Tab setuptab;
    @FXML private Menu helpMenu;
    @FXML private MenuItem showSettingsCurrentProject;
    @FXML private MenuItem helpMenuItem;
    @FXML private MenuItem openHumanGeneWindow;
    @FXML private MenuItem openMouseGeneWindow;
    @FXML private MenuItem openRatGeneWindow;
    @FXML private MenuItem openFlyGeneWindow;
    @FXML private MenuItem exportBEDFilesMenuItem;
    /** The 'second' tab of VPVGui that shows a summary of the analysis and a list of Viewpoints. */
    @FXML private Tab analysistab;
    /** Click this to choose the restriction enzymes with which to do the capture Hi-C cutting  */
    @FXML private Button chooseEnzymeButton;
    /** Presenter for the second tab. */
    private VPAnalysisPresenter vpanalysispresenter;
    /** View for the second tab. */
    private VPAnalysisView vpanalysisview;

    transient private IntegerProperty sizeUp = new SimpleIntegerProperty();
    public final int getSizeUp() { return sizeUp.get();}
    public final void setSizeUp(int su) { sizeUp.set(su);}
    public IntegerProperty sizeDownProperty() { return sizeDown; }

    transient private IntegerProperty sizeDown = new SimpleIntegerProperty();
    public final int getSizeDown() { return sizeDown.get();}
    public final void setSizeDown(int sd) { sizeUp.set(sd);}
    public IntegerProperty sizeUpProperty() { return sizeUp; }

    transient private IntegerProperty minFragSize = new SimpleIntegerProperty();
    public int getMinFragSize() { return minFragSize.get(); }
    public void setMinFragSize(int i) { this.minFragSize.set(i);}
    public IntegerProperty minFragSizeProperty() { return minFragSize; }

    transient private DoubleProperty maxRepeatContent = new SimpleDoubleProperty();
    public final double getMaxRepeatContent() {return maxRepeatContent.get();}
    public final void setMaxRepeatContent(double r) { this.maxRepeatContent.set(r);}
    public DoubleProperty maxRepeatContentProperty() { return maxRepeatContent;  }

    transient private DoubleProperty minGCcontent = new SimpleDoubleProperty();
    public final double getMinGCcontent() { return minGCcontent.get();}
    public final void setMinGCcontent(double mgc) { minGCcontent.set(mgc);}
    public DoubleProperty minGCcontentProperty() { return minGCcontent; }

    transient private DoubleProperty maxGCcontent = new SimpleDoubleProperty();
    public final double getMaxGCcontent() { return maxGCcontent.get();}
    public final void setMaxGCcontent(double mgc) { maxGCcontent.set(mgc);}
    public DoubleProperty maxGCcontentProperty() { return maxGCcontent; }

    @FXML
    void exitButtonClicked(ActionEvent e) {
        e.consume();
        logger.info("Closing VPV Gui");
        serialize();
        javafx.application.Platform.exit();
    }

    /** Serialize the project data to the default location. */
    public boolean serialize() {
        String projectname=this.model.getProjectName();
        if (projectname==null) {
            ErrorWindow.display("Error","Could not get viewpoint name (should never happen). Will save with default");
            projectname="default";
        }
        String serializedFilePath=Platform.getAbsoluteProjectPath(projectname);
        return serializeToLocation(serializedFilePath);
    }

    /** Serialialize the project file to the location given as path.
     * @param path absolute path to which the serilaized file should be saved.
     * @return
     */
    private boolean serializeToLocation(String path) {
        if (path==null) {
            ErrorWindow.display("Error","Could not get file name for saving project file.");
            return false;
        }
        try {
            SerializationManager.serializeModel(this.model, path);
        } catch (IOException e) {
            ErrorWindow.displayException("Error","Unable to serialize VPV viewpoint",e);
            return false;
        }
        logger.trace("Serialization successful to file "+path);
        return true;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.trace("initialize() called");
        genomeChoiceBox.setItems(genomeTranscriptomeList);
        genomeChoiceBox.getSelectionModel().selectFirst();
        genomeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            setGenomeBuild(newValue);
        });
        approachChoiceBox.setItems(approachList);
        approachChoiceBox.getSelectionModel().selectFirst();
        approachChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.approachLabel.setText(newValue);
        });
        this.approachLabel.setText(approachChoiceBox.getValue());
        this.initializer=new Initializer(model);
        initializePromptTextsToDefaultValues();



        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        this.analysisPane.getChildren().add(vpanalysisview.getView());
        ToggleGroup tGroup = new ToggleGroup();
        tGroup.getToggles().addAll(tiling1,tiling2,tiling3,tiling4,tiling5);
        tiling1.setOnAction(e->{this.model.setTilingFactor(1);this.vpanalysispresenter.refreshVPTable();e.consume(); });
        tiling2.setOnAction(e->{this.model.setTilingFactor(2);this.vpanalysispresenter.refreshVPTable();e.consume(); });
        tiling3.setOnAction(e->{this.model.setTilingFactor(3);this.vpanalysispresenter.refreshVPTable();e.consume(); });
        tiling4.setOnAction(e->{this.model.setTilingFactor(4);this.vpanalysispresenter.refreshVPTable();e.consume(); });
        tiling5.setOnAction(e->{this.model.setTilingFactor(5);this.vpanalysispresenter.refreshVPTable();e.consume(); });

    }

    private void setInitializedValuesInGUI() {
        String genomebuild=model.getGenomeBuild();
        if (genomebuild!=null)
            this.genomeBuildLabel.setText(genomebuild);
        String path_to_downloaded_genome_directory=model.getGenomeDirectoryPath();
        if (path_to_downloaded_genome_directory!= null) {
            this.downloadedGenomeLabel.setText(path_to_downloaded_genome_directory);
            this.genomeDownloadPI.setProgress(1.00);
        } else {
            this.downloadedGenomeLabel.setText("...");
            this.genomeDownloadPI.setProgress(0);
        }
        if (model.isGenomeUnpacked()) {
            this.decompressGenomeLabel.setText("extraction previously completed");
            this.genomeDecompressPI.setProgress(1.00);
        } else {
            this.decompressGenomeLabel.setText("...");
            this.genomeDecompressPI.setProgress(0.0);
        }
        String refGenePath=this.model.getRefGenePath();
        if (refGenePath!=null) {
            this.downloadedTranscriptsLabel.setText(refGenePath);
            this.transcriptDownloadPI.setProgress(1.0);
        } else {
            this.downloadedTranscriptsLabel.setText("...");
        }
    }

    /**
     * This allows a caller to set the {@link Model} object for this presenter (for instance, a default
     * {@link Model} object is set if the user chooses a new viewpoint. If the user chooses to open a previous
     * viewpoint from a serialized file, then a  {@link Model} object is initialized from the file and set here.
     * This method calls {@link #setInitializedValuesInGUI()} in order to show relevant data in the GUI.
     * @param mod A {@link Model} object.
     */
    public void setModel(Model mod) {
        this.model=mod;
        logger.trace(String.format("Setting model to %s",mod.getProjectName()));
        setInitializedValuesInGUI();
        setBindings();
    }


    public void setModelInMainAndInAnalysisPresenter(Model mod) {
        setModel(mod);
        this.vpanalysispresenter.setModel(mod);
        if (model.getMaxGCcontent()>0){
            this.maxGCContentTextField.setText(String.format("%.2f",model.getMaxGCcontent()));
        } else {
            this.maxGCContentTextField.setPromptText(String.format("%.2f",Default.MAX_GC_CONTENT));
        }
        if (model.getMinGCcontent()>0) {
            this.minGCContentTextField.setText(String.format("%.2f",model.getMinGCcontent()));
        } else {
            this.minGCContentTextField.setPromptText(String.format("%.2f",Default.MIN_GC_CONTENT));
        }
        if (model.getMinFragSize()>0) {
            this.minFragSizeTextField.setText(String.format("%d",model.getMinFragSize()));
        } else {
            this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        }
        if (model.getMaxRepeatContent()>0) {
            this.maxRepContentTextField.setText(String.format("%.2f",model.getMaxRepeatContent()));
        } else {
            this.maxRepContentTextField.setPromptText(String.format("%.2f",Default.MAXIMUM_REPEAT_CONTENT));
        }
        if (model.getSizeUp()>0) {
            this.sizeUpTextField.setText(String.format("%d",model.getSizeUp()));
        } else {
            this.sizeUpTextField.setPromptText(String.format("%d",Default.SIZE_UPSTREAM));
        }
        if (model.getSizeDown()>0) {
            this.sizeDownTextField.setText(String.format("%d",model.getSizeDown()));
        } else {
            this.sizeDownTextField.setPromptText(String.format("%d",Default.SIZE_DOWNSTREAM));
        }
    }


    /** The prompt (gray) values of the text fields in the settings windows get set to their default values here. */
    private void initializePromptTextsToDefaultValues() {
        this.sizeUpTextField.setPromptText(String.format("%d",Default.SIZE_UPSTREAM));
        this.sizeDownTextField.setPromptText(String.format("%d",Default.SIZE_DOWNSTREAM));
        this.minGCContentTextField.setPromptText(String.format("%.1f %%",Default.MIN_GC_CONTENT));
        this.maxGCContentTextField.setPromptText(String.format("%.1f %%",Default.MAX_GC_CONTENT));
        this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        this.maxRepContentTextField.setPromptText(String.format("%.1f %%",Default.MAXIMUM_REPEAT_CONTENT));
    }

    /** Keep the six fields in the GUI in synch with the corresponding variables in this class. */
    private void setBindings() {
        StringConverter<Number> converter = new NumberStringConverter();
        Bindings.bindBidirectional(this.sizeDownTextField.textProperty(),sizeDownProperty(),converter);
        Bindings.bindBidirectional(this.sizeUpTextField.textProperty(), sizeUpProperty(),converter);
        Bindings.bindBidirectional(this.minFragSizeTextField.textProperty(),minFragSizeProperty(),converter);
        Bindings.bindBidirectional(this.maxRepContentTextField.textProperty(),maxRepeatContentProperty(),converter);
        Bindings.bindBidirectional(this.minGCContentTextField.textProperty(),minGCcontentProperty(),converter);
        Bindings.bindBidirectional(this.maxRepContentTextField.textProperty(),maxGCcontentProperty(),converter);
    }

    /** This method should be called before we create viewpoints. It updates all of the variables in our model object
     * to have the values specified in the user for the GUI, including the values of the six fields we show in the GUI
     * and that are bound in {@link #setBindings()}.
     */
    private void updateModel() {
        this.model.setSizeDown(getSizeDown());
        this.model.setSizeUp(getSizeUp());
        this.model.setMinFragSize(getMinFragSize());
        this.model.setMaxRepeatContent(getMaxRepeatContent());
        this.model.setMinGCcontent(getMinGCcontent());
        this.model.setMaxGCcontent(getMaxGCcontent());
    }


    /** This gets called when the user chooses a new genome build. They need to do download, uncompression, indexing and
     * also get the corresponding transcript file.
     * @param build Name of genome build.
     */
    private void setGenomeBuild(String build) {
        logger.info("Setting genome build to "+build);
        this.genomeBuildLabel.setText(build);
        this.model.setGenomeBuild(build);
        this.transcriptDownloadPI.setProgress(0.0);
        this.genomeDownloadPI.setProgress(0.0);
        this.genomeIndexPI.setProgress(0.0);
        this.genomeDecompressPI.setProgress(0.0);
    }

    public Model getModel() { return this.model; }

    /** This downloads the tar-gzip genome file as chosen by the user from the UCSC download site.
     * It places the compressed file into the directory chosen by the user. The path to the directory
     * is stored in the {@link Model} object using the {@link Model#setGenomeDirectoryPath} function.
     * Following this the user needs to uncompress and index the genome files using the function
     * {@link #decompressGenome(ActionEvent)} which is called after the corresponding button
     * is clicked in the GUI.
     */
    @FXML public void downloadGenome() {
        String build = this.model.getGenomeBuild();
        logger.info("About to download genome for "+build +" (if necessary)");
        GenomeDownloader gdownloader = new GenomeDownloader(build);
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + build + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath()=="") {
            logger.error("Could not set genome download path from Directory Chooser");
            ErrorWindow.display("Error","Could not get path to download genome.");
            return;
        }
        logger.info("downloadGenome to directory  "+file.getAbsolutePath());
        if (this.model.checkDownloadComplete(file.getAbsolutePath())) {
            // we're done!
            this.downloadedGenomeLabel.setText(String.format("Genome %s was already downloaded",build));
            this.genomeDownloadPI.setProgress(1.0);
        } else {
            gdownloader.downloadGenome(file.getAbsolutePath(), model.getGenomeBasename(), genomeDownloadPI);
            model.setGenomeDirectoryPath(file.getAbsolutePath());
            this.downloadedGenomeLabel.setText(file.getAbsolutePath());
        }
    }

    /**
     * @param e event triggered by command to download appropriate {@code refGene.txt.gz} file.
     */
   @FXML public void downloadRefGeneTranscripts(ActionEvent e) {

        String genomeBuild=genomeChoiceBox.getValue();
        RefGeneDownloader rgd = new RefGeneDownloader(genomeBuild);
        String transcriptName = rgd.getTranscriptName();
        String basename=rgd.getBaseName();
        String url=null;
        try {
            url = rgd.getURL();
        } catch (DownloadFileNotFoundException dfne) {
            ErrorWindow.display("Could not identify RefGene file for genome",dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + genomeBuild + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().isEmpty()) {
            ErrorWindow.display("Error","Could not get path to download transcript file.");
            return;
        }
        if (! rgd.needToDownload(file.getAbsolutePath())) {
            logger.trace(String.format("Found refGene.txt.gz file at %s. No need to download",file.getAbsolutePath()));
            this.transcriptDownloadPI.setProgress(1.0);
            this.downloadedTranscriptsLabel.setText(transcriptName);
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.model.setRefGenePath(abspath);
        }

        Downloader downloadTask = new Downloader(file, url, basename, transcriptDownloadPI);
        downloadTask.setOnSucceeded( event -> {
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.model.setRefGenePath(abspath);
            this.downloadedTranscriptsLabel.setText(transcriptName);
        });
       Thread th = new Thread(downloadTask);
       th.setDaemon(true);
       th.start();
       e.consume();
    }


    /** G-unzip and un-tar the downloaded chromFa.tar.gz file.
     * @param e  Event triggered by decompress genome command
     */
    @FXML public void decompressGenome(ActionEvent e) {
        e.consume();
        if (this.model.getGenome().isIndexingComplete()) {
            decompressGenomeLabel.setText("chromosome files extracted");
            genomeDecompressPI.setProgress(1.00);
            model.setGenomeUnpacked();
            return;
        }
        GenomeGunZipper gindexer = new GenomeGunZipper(this.model.getGenome(),
                this.genomeDecompressPI);
        gindexer.setOnSucceeded( event -> {
            decompressGenomeLabel.setText(gindexer.getStatus());
            if (gindexer.OK())
                model.setGenomeUnpacked();
        });
        gindexer.setOnFailed(eventh -> {
            decompressGenomeLabel.setText("Decompression failed");
            ErrorWindow.display("Could not decompress genome file" ,gindexer.getException().getMessage());
        });
        Thread th = new Thread(gindexer);
        th.setDaemon(true);
        th.start();
    }

    /** Create fai (fasta index files)
     * @param e Event triggered by index genome command.
     * */
    @FXML public void indexGenome(ActionEvent e) {
        e.consume();
        logger.trace("Indexing genome files...");
        FASTAIndexManager manager = new FASTAIndexManager(this.model,this.genomeIndexPI);
        manager.setOnSucceeded(event ->{

            indexGenomeLabel.setText("FASTA files successfully indexed.");
            logger.debug("Number of FASTA files retrieved> "+manager.getIndexedFastaFiles().size());
            model.setIndexedFastaFiles(manager.getIndexedFastaFiles());
            model.setGenomeIndexed();
            model.setContigLengths(manager.getContigLengths());
        } );
        manager.setOnFailed(event-> {
            indexGenomeLabel.setText("FASTA indexing failed");
            ErrorWindow.display("Failure to extract FASTA files.",
                    manager.getException().getMessage());
        });
        Thread th = new Thread(manager);
        th.setDaemon(true);
        th.start();
    }



    /** This function is called after the user has chosen restriction enzymes in the
     * corresponding popup window. It passes a list of the {@link RestrictionEnzyme}
     * objects to the {@link Model}.*/
    @FXML public void chooseEnzymes() {
        List<RestrictionEnzyme> chosenEnzymes = EnzymeViewFactory.getChosenEnzymes(this.model);
        this.model.setChosenRestrictionEnzymes(chosenEnzymes);
        this.restrictionEnzymeLabel.setText(this.model.getRestrictionEnzymeString());
    }

    /**
     * Open a new dialog where the user can paste gene symbols or Entrez Gene IDs.
     * The effect of the command <pre>EntrezGeneViewFactory.display(this.model);</pre>
     * is to pass a list of {@link VPVGene} objects to the {@link Model}.
     * These objects are used with other information in the Model to create {@link vpvgui.model.viewpoint.ViewPoint}
     * objects when the user clicks on {@code Create ViewPoints}.
     * See {@link EntrezGeneViewFactory} for logic.
     *
     * @param e event triggered by enter gene command.
     */
    @FXML public void enterGeneList(ActionEvent e) {
        EntrezGeneViewFactory.display(this.model);
        /** The following command is just for debugging. We now have all VPVGenes, but still need to
         * add information about the restriction enzymes and the indexed FASTA file.
         */
        //this.model.debugPrintVPVGenes();
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
        e.consume();
    }






    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link VPVGene} objects into the {@link Model}
     * object. This function will use the {@link VPVGene} obejcts and other information
     * to create {@link vpvgui.model.viewpoint.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisPresenter} Tab.
     */
    public void createViewPoints() {
        logger.trace("Entering createViewPoints");
        updateModel();
        boolean OK=QCCheckFactory.showQCCheck(model);
        if (! OK ) {
            return;
        }


        String approach = this.approachChoiceBox.getValue();
        boolean doSimple=false;
        if (approach.equals("Simple")) {
            doSimple=true;
        } else if (approach.equals("Extended")) {
            doSimple=false;
        } else {
            logger.error("Could not retrieve approach, I got "+approach);
            return;
        }


        StringProperty sp=new SimpleStringProperty();
        ViewPointCreationTask task =null;
        if (doSimple) {
            task = new SimpleViewPointCreationTask(model,sp);
        } else {
            task = new ExtendedViewPointCreationTask(model,sp);
        }

        CreateViewpointPBView pbview = new CreateViewpointPBView();
        CreateViewpointPBPresenter pbpresent = (CreateViewpointPBPresenter)pbview.getPresenter();
        pbpresent.initBindings(task,sp);

        Stage window;
        String windowTitle = "Viewpoint creation";
        window = new Stage();
        window.setOnCloseRequest( event -> {
            window.close();
        } );
        window.setTitle(windowTitle);
        pbpresent.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });

        task.setOnSucceeded(event -> {
            SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
            this.vpanalysispresenter.setModel(this.model);
            this.vpanalysispresenter.showVPTable();
            selectionModel.select(this.analysistab);
            logger.trace("Finished factory.createViewPoints()");
            pbpresent.closeWindow();
        });
        task.setOnFailed(eh -> {
            Exception exc = (Exception)eh.getSource().getException();
            ErrorWindow.displayException("Error",
                    "Exception encountered while attempting to create viewpoints",
                    exc);
        });
        logger.trace("About to run task");
        new Thread(task).start();
        logger.trace("Past");
        window.setScene(new Scene(pbview.getView()));
        window.showAndWait();
    }



    /**
     * @param e Event triggered by close command.
     */
    public void closeWindow(ActionEvent e) {
        boolean answer = ConfirmWindow.display("Alert", "Are you sure you want to quit?");
        if (answer) {
            logger.info("Closing VPV Gui");
            serialize();
        }
        javafx.application.Platform.exit();
    }

    public void refreshViewPoints() {
        if (this.vpanalysispresenter==null) {
            logger.error("Could not refresh viewpoint table, since vpanalysispresenter was null");
            return;
        }
        this.vpanalysispresenter.refreshVPTable();
    }

    /**
     * This is called when the user starts a new viewpoint. It should erase everything from
     * the GUI as well (TODO check this!)
     * @param e Event triggered by new viewpoint command.
     */
    @FXML public void startNewProject(ActionEvent e) {
        logger.trace("Start new viewpoint");
        ObservableList<Tab> panes = this.tabpane.getTabs();
        /* collect tabs first then remove them -- avoids a ConcurrentModificationException */
        List<Tab> tabsToBeRemoved=new ArrayList<>();
        /* close all tabs except setup and analysis. */
        for (Tab tab : panes) {
            String id=tab.getId();
            if (id != null && (id.equals("analysistab") || id.equals("setuptab") )) { continue; }
            logger.trace("Closing tab "+id);
            tabsToBeRemoved.add(tab);
        }
        this.tabpane.getTabs().removeAll(tabsToBeRemoved);
        this.model=new Model();
        this.model.setDefaultValues();
        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setModel(this.model);
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        setInitializedValuesInGUI();
        e.consume();
        /* TODO */
        logger.error("TODO -- also re-initialize first tab");
    }

    /** Display the settings (parameters) of the current viewpoint. */
    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(model.getProperties());
    }

    /**
     * Content of {@link Model} is written to platform-dependent default location.
     *  @throws IOException caused by an error in serialization
     */
    @FXML private void saveProject(ActionEvent e) throws IOException {
       // Model.writeSettingsToFile(this.model);
        boolean result=serialize();
        if (result) { /* if it didnt work, the serialize method will show an error dialog. */
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(String.format("Successfully saved viewpoint data to %s", Platform.getAbsoluteProjectPath(model.getProjectName())));
            alert.show();
        }
        e.consume();
    }

    /**
     * @throws IOException caused by an error in serialization
     */
    @FXML private void saveProjectAndClose() throws IOException {
        serialize();
        javafx.application.Platform.exit();
    }


    /**
     * @param e event triggered by show help command.
     */
    @FXML public void showHelpWindow(ActionEvent e) {
        HelpViewFactory.display();
        e.consume();
    }

    /**
     * @param e event triggered by set proxy command.
     */
    @FXML void setProxyDialog(ActionEvent e) {
        Stage window;
        String windowTitle = "Proxy Settings";
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
        if (model.getHttpProxy()!=null) {
            presenter.setProxyProperty(model.getHttpProxy());
        }
        if (model.getHttpProxyPort()!=null) {
            logger.trace(String.format("http proxy port: %s",model.getHttpProxyPort()));
            presenter.setPort(model.getHttpProxyPort());
        }
        window.setScene(new Scene(view.getView()));
        window.showAndWait();
        String port=presenter.getPort();
        String proxy=presenter.getProxy();
        if (proxy==null) {
            ErrorWindow.display("Error obtaining Proxy","Proxy string could not be obtained. Please try again");
            return;
        }
        this.model.setHttpProxy(proxy);
        this.model.setHttpProxyPort(port);
        logger.info(String.format("Set proxy to %s[%s]",proxy,port));
        Utils.setSystemProxyAndPort(proxy,port);
    }


    @FXML public void openGeneWindowWithExampleHumanGenes() {
        File file = new File(getClass().getClassLoader().getResource("humangenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example human gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }
    @FXML public void openGeneWindowWithExampleFlyGenes() {
        File file = new File(getClass().getClassLoader().getResource("flygenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example fly gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }
    @FXML public void openGeneWindowWithExampleMouseGenes() {
        File file = new File(getClass().getClassLoader().getResource("mousegenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example mouse gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }
    @FXML public void openGeneWindowWithExampleRatGenes() {
        File file = new File(getClass().getClassLoader().getResource("ratgenesymbols.txt").getFile());
        if (file==null) {
            ErrorWindow.display("Could not open example rat gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",this.model.n_valid_genes(),this.model.n_viewpointStarts()));
    }

    @FXML public void exportBEDFiles(ActionEvent e) {
        List<ViewPoint> vplist=this.model.getViewPointList();
        if (vplist==null || vplist.isEmpty()) {
            ErrorWindow.display("Attempt to export empty BED files","Complete generation and analysis of ViewPoints before exporting to BED!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting BED files.");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath()=="") {
            ErrorWindow.display("Error","Could not get path to export BED files.");
            return;
        }
        String prefix=model.getProjectName();
        BEDFileExporter exporter = new BEDFileExporter(file.getAbsolutePath(),prefix);
        try {
            exporter.printRestFragsToBed(this.model.getViewPointList(),this.model.getGenomeBuild());
        } catch (Exception exc) {
            ErrorWindow.displayException("Could not save data to BED files", exc.getMessage(),exc);
        }
        e.consume();
    }

    @FXML
    public void setProbeLength(ActionEvent e) {
        int probelen;
        if (model.getProbeLength()>0) {
            probelen=model.getProbeLength();
        } else {
            probelen=Default.PROBE_LENGTH;
        }

        Integer len = Popups.getIntegerFromUser("Enter Probe Length",
                probelen,
                "Enter probe length:");
        if (len == null) {
            ErrorWindow.display("Could not get probe length", "enter an integer value!");
            return;
        }
        this.model.setProbeLength(len);
        this.vpanalysispresenter.refreshVPTable();
        logger.trace(String.format("We just set probe length to %d", model.getProbeLength()));
    }


    @FXML
    public void setMarginSize(ActionEvent e) {
        Integer msize = Popups.getIntegerFromUser("Enter margin size",
                Default.MARGIN_SIZE,
                "Margin size for calculating repeat content");
        if (msize==null) {
            ErrorWindow.display("Could not get Margin size", "enter an integer value!");
            return;
        }
        this.model.setMarginSize(msize);
        this.vpanalysispresenter.refreshVPTable();
        logger.trace(String.format("We just set MarginSize to %d", model.getMarginSize()));
    }

    @FXML
    public void showLog(ActionEvent e) {
        LogViewerFactory factory = new LogViewerFactory();
        factory.display();
        e.consume();
    }

    @FXML
    public void about(ActionEvent e) {
        Popups.showAbout(model.getVersion(), model.getLastChangeDate());
        e.consume();
    }

    /** Open a window that will allow the user to delete unwanted project files. Do not allow the
     * user to delete the file that is currently opened.
     * @param e action event.*/
    @FXML
    public void deleteProjectFiles(ActionEvent e) {
        DeleteFactory.display(this.model);
        e.consume();
    }

    /** Export the project ser file to a location chosen by the user (instead of the default location,
     * which is the .vpvgui directory). */
    @FXML
    public void exportProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        String initFileName=String.format("%s.ser",this.model.getProjectName());
        chooser.setInitialFileName(initFileName);
        chooser.setTitle("Choose file path to save project file");
        File file = chooser.showSaveDialog(null);
        String path = file.getAbsolutePath();
        if (path==null) {
            ErrorWindow.display("Error","Could not retrieve path to export project file");
            return;
        }
        serializeToLocation(path);
        logger.trace(String.format("Serialized file to %s",path));
        e.consume();
    }

    /** Open a project from a file specified by the user. */
    @FXML
    public void openProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open VPV project file");
        File file = chooser.showOpenDialog(null);
        try {
            this.model = SerializationManager.deserializeModel(file.getAbsolutePath());
            setModelInMainAndInAnalysisPresenter(this.model);
            logger.trace(String.format("Opened model %s from file %s",model.getProjectName(), file.getAbsolutePath()));
        } catch (IOException ex) {
            ErrorWindow.displayException("Error","I/O Error opening project file", ex);
        } catch (ClassNotFoundException clnf) {
            ErrorWindow.displayException("Error","Deserialization error",clnf);
        }
        e.consume();
    }


}





