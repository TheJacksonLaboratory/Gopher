package gopher.gui.gophermain;

import gopher.model.digest.DigestCreationTask;
import gopher.model.viewpoint.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.apache.log4j.Logger;
import gopher.exception.DownloadFileNotFoundException;

import gopher.gui.analysisPane.VPAnalysisPresenter;
import gopher.gui.analysisPane.VPAnalysisView;
import gopher.gui.taskprogressbar.TaskProgressBarPresenter;
import gopher.gui.taskprogressbar.TaskProgressBarView;
import gopher.gui.deletepane.delete.DeleteFactory;
import gopher.gui.entrezgenetable.EntrezGeneViewFactory;
import gopher.gui.enzymebox.EnzymeViewFactory;
import gopher.gui.help.HelpViewFactory;
import gopher.gui.logviewer.LogViewerFactory;
import gopher.gui.popupdialog.PopupFactory;
import gopher.gui.progresspopup.ProgressPopup;
import gopher.gui.proxy.SetProxyPresenter;
import gopher.gui.proxy.SetProxyView;
import gopher.gui.qcCheckPane.QCCheckFactory;
import gopher.gui.regulatoryexomebox.RegulatoryExomeBoxFactory;
import gopher.gui.settings.SettingsViewFactory;
import gopher.io.*;
import gopher.model.*;
import gopher.util.SerializationManager;
import gopher.util.Utils;

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
 * @version 0.2.8 (2017-11-12)
 */
public class GopherMainPresenter implements Initializable {
    private final static Logger logger = Logger.getLogger(GopherMainPresenter.class.getName());
    /** The Model for the entire analysis. */
    private Model model = null;
    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc. It is set in the FXML
     * document to refer to the Anchor pane that is the root node of the GUI.
     */
    @FXML private Node rootNode;
    /** List of genome builds. Used by genomeChoiceBox*/
    @FXML private final ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9","mm10");
    /** List of genome builds. Used by genomeChoiceBox*/
    @FXML private final ObservableList<String> approachList = FXCollections.observableArrayList("Simple", "Extended");
    @FXML private ChoiceBox<String> genomeChoiceBox;
    @FXML private ChoiceBox<String> approachChoiceBox;
    /** Clicking this button downloads the genome build and unpacks it.*/
    @FXML private Button downloadGenome;
    @FXML private Button decompressGenomeButton;
    @FXML private Button indexGenomeButton;
    /** Label for the genome build we want to download. */
    @FXML private Label genomeBuildLabel;
    /** Label for the transcripts we want to download.*/
    @FXML private Label transcriptsLabel;
    /** Show which design approach */
    @FXML private Label approachLabel;
    /**Clicking this button will download the genome file if it is not found at the indicated directory. */
    @FXML private Button downloadGenomeButton;
    /** Button to download RefSeq.tar.gz (transcript/gene definition file  */
    @FXML private Button downloadTranscriptsButton;
    @FXML private ProgressIndicator genomeDownloadPI;
    /** Show progress in downloading the Genome and corresponding transcript definition file.  */
    @FXML private ProgressIndicator genomeDecompressPI;
    @FXML private ProgressIndicator genomeIndexPI;
    /** Progress indicator for downloading the transcript file */
    @FXML private ProgressIndicator transcriptDownloadPI;
    /** Progress indicator for downloading alignability file */
    @FXML private ProgressIndicator alignabilityDownloadPI;

    @FXML private Label sizeUpLabel;
    @FXML private Label sizeDownLabel;
    @FXML private TextField sizeUpTextField;
    @FXML private TextField sizeDownTextField;
    @FXML private TextField minFragSizeTextField;
    @FXML private TextField maxKmerAlignabilityTextField;
    @FXML private TextField minGCContentTextField;
    @FXML private TextField maxGCContentTextField;
    @FXML private TextField minBaitCountTextField;
    @FXML private TextField maxBaitCountTextField;

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
    /** Show name of downloaded transcripts file. */
    @FXML private Label downloadAlignabilityLabel;
    @FXML private Label decompressAlignabilityLabel;

    @FXML RadioMenuItem tiling1;
    @FXML RadioMenuItem tiling2;
    @FXML RadioMenuItem tiling3;
    @FXML RadioMenuItem tiling4;
    @FXML RadioMenuItem tiling5;
    @FXML RadioMenuItem singleMarginRadioMenuitem;
    @FXML RadioMenuItem bothMarginsRadioMenuitem;
    @FXML private TabPane tabpane;
    @FXML private StackPane analysisPane;

    /** The 'second' tab of VPVGui that shows a summary of the analysis and a list of Viewpoints. */
    @FXML private Tab analysistab;
    /** Click this to choose the restriction enzymes with which to do the capture Hi-C cutting  */
    @FXML private Button chooseEnzymeButton;
    /** Presenter for the second tab. */
    private VPAnalysisPresenter vpanalysispresenter;
    /** View for the second tab. */
    private VPAnalysisView vpanalysisview;
    /** Reference to the primary stage. We use this to set the title when we switch models (new from File menu). */
    private Stage primaryStage=null;

    transient private IntegerProperty sizeUp = new SimpleIntegerProperty();
    private int getSizeUp() { return sizeUp.get();}
    private void setSizeUp(int su) { sizeUp.set(su);}
    private IntegerProperty sizeDownProperty() { return sizeDown; }

    transient private IntegerProperty sizeDown = new SimpleIntegerProperty();
    private int getSizeDown() { return sizeDown.get();}
    private void setSizeDown(int sd) { sizeUp.set(sd);}
    private IntegerProperty sizeUpProperty() { return sizeUp; }

    transient private IntegerProperty minFragSize = new SimpleIntegerProperty();
    private int getMinFragSize() { return minFragSize.get(); }
    private void setMinFragSize(int i) { this.minFragSize.set(i);}
    private IntegerProperty minFragSizeProperty() { return minFragSize; }

    transient private DoubleProperty maxRepeatContent = new SimpleDoubleProperty();
    private double getMaxRepeatContent() {return maxRepeatContent.get();}
    private void setMaxRepeatContent(double r) { this.maxRepeatContent.set(r);}
    private DoubleProperty maxRepeatContentProperty() { return maxRepeatContent;  }

    transient private IntegerProperty maxMeanKmerAlignability = new SimpleIntegerProperty();
    private int getMaxMeanKmerAlignability(){ return maxMeanKmerAlignability.get();}
    private void setMaxMeanKmerAlignability(int mmka) { this.maxMeanKmerAlignability.set(mmka);}
    private IntegerProperty maxMeanKmerAlignabilityProperty() { return maxMeanKmerAlignability; }

    transient private IntegerProperty minBaitCount = new SimpleIntegerProperty();
    private int getMinimumBaitCount(){ return minBaitCount.get();}
    private void setMinimumBaitCount(int bc) { this.minBaitCount.set(bc);}
    private IntegerProperty minimumBaitCountProperty() { return minBaitCount; }

    transient private IntegerProperty maxBaitCount = new SimpleIntegerProperty();
    private int getMaximumBaitCount(){ return maxBaitCount.get();}
    private void setMaximumBaitCount(int bc) { this.maxBaitCount.set(bc);}
    private IntegerProperty maximumBaitCountProperty() { return maxBaitCount; }

    transient private DoubleProperty minGCcontent = new SimpleDoubleProperty();
    private double getMinGCcontent() { return minGCcontent.get();}
    private void setMinGCcontent(double mgc) { minGCcontent.set(mgc);}
    private DoubleProperty minGCcontentProperty() { return minGCcontent; }

    transient private DoubleProperty maxGCcontent = new SimpleDoubleProperty();
    private double getMaxGCcontent() { return maxGCcontent.get();}
    /** Note we expect the user to enter a percentage, and we convert it here to proportion. */
    private void setMaxGCcontent(double mgc) { maxGCcontent.set(mgc);}
    private DoubleProperty maxGCcontentProperty() { return maxGCcontent; }

    @FXML
    void exitButtonClicked(ActionEvent e) {
        e.consume();
        logger.info("Closing VPV Gui");
        serialize();
        javafx.application.Platform.exit();
    }

    /** Serialize the project data to the default location. */
    private boolean serialize() {
        String projectname=this.model.getProjectName();
        if (projectname==null) {
            PopupFactory.displayError("Error","Could not get viewpoint name (should never happen). Will save with default");
            projectname="default";
        }
        String serializedFilePath=Platform.getAbsoluteProjectPath(projectname);
        return serializeToLocation(serializedFilePath);
    }

    /** Serialialize the project file to the location given as path.
     * @param path absolute path to which the serilaized file should be saved.
     * @return true iff serialization is successful
     */
    private boolean serializeToLocation(String path) {
        if (path==null) {
            PopupFactory.displayError("Error","Could not get file name for saving project file.");
            return false;
        }
        try {
            SerializationManager.serializeModel(this.model, path);
        } catch (IOException e) {
            PopupFactory.displayException("Error","Unable to serialize VPV viewpoint",e);
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
        genomeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> setGenomeBuild(newValue) );
        // The following will initialize the GUI to the simple approach
        approachChoiceBox.setItems(approachList);
        approachChoiceBox.getSelectionModel().selectFirst();
        setGUItoSimple();
        approachChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
            this.approachLabel.setText(newValue) );
        this.approachLabel.setText(approachChoiceBox.getValue());

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
        tiling2.setSelected(true);

        ToggleGroup marginToggleGroup = new ToggleGroup();
        marginToggleGroup.getToggles().addAll(singleMarginRadioMenuitem,bothMarginsRadioMenuitem);
        singleMarginRadioMenuitem.setOnAction(e -> {this.model.setAllowSingleMargin(true); e.consume();});
        bothMarginsRadioMenuitem.setOnAction(e -> {this.model.setAllowSingleMargin(false); e.consume();});
        singleMarginRadioMenuitem.setSelected(true);

        this.approachChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                String selectedItem=approachChoiceBox.getItems().get((Integer) number2);
                if (selectedItem.equals("Simple")) {
                    setGUItoSimple();
                } else if (selectedItem.equals("Extended")) {
                    setGUItoExtended();
                } else {
                    logger.error(String.format("Did not recognize approach in menu %s",selectedItem ));
                }
            }
        });

    }

    /** makes the upstream and downstream size fields invisible because they are irrelevant to the simple approach.*/
    private void setGUItoSimple() {
        this.sizeUpTextField.setVisible(false);
        this.sizeDownTextField.setVisible(false);
        this.sizeDownLabel.setVisible(false);
        this.sizeUpLabel.setVisible(false);
    }
    /** makes the upstream and downstream size fields visible because they are needed for the extended approach.*/
    private void setGUItoExtended() {
        this.sizeUpTextField.setVisible(true);
        this.sizeDownTextField.setVisible(true);
        this.sizeDownLabel.setVisible(true);
        this.sizeUpLabel.setVisible(true);
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
            this.decompressGenomeLabel.setText("Extraction previously completed");
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
            this.transcriptDownloadPI.setProgress(0.0);
        }

        if (model.alignabilityMapPathIncludingFileNameGzExists()) {
            this.downloadAlignabilityLabel.setText("Download complete");
            this.alignabilityDownloadPI.setProgress(1.0);
        } else {
            this.downloadAlignabilityLabel.setText("...");
            this.alignabilityDownloadPI.setProgress(0.0);
        }

        if (model.isGenomeIndexed()) {
            this.indexGenomeLabel.setText("Genome files successfully indexed");
            this.genomeIndexPI.setProgress(1.00);
        } else {
            this.indexGenomeLabel.setText("...");
            this.genomeIndexPI.setProgress(0.00);
        }
        if (model.getChosenEnzymelist()!=null && model.getChosenEnzymelist().size()>0) {
            this.restrictionEnzymeLabel.setText(model.getAllSelectedEnzymeString());
        } else {
            this.restrictionEnzymeLabel.setText(null);
        }
        if (this.model.getVPVGeneList()!=null && this.model.getVPVGeneList().size()>0) {
            this.nValidGenesLabel.setText(String.format("%d valid target genes",this.model.getVPVGeneList().size() ));
        } else {
            this.nValidGenesLabel.setText(null);
        }
        if (model.getAllowSingleMargin()) {
            this.bothMarginsRadioMenuitem.setSelected(false);
            this.singleMarginRadioMenuitem.setSelected(true);
        } else {
            this.bothMarginsRadioMenuitem.setSelected(true);
            this.singleMarginRadioMenuitem.setSelected(false);
        }
    }

    /**
     * This allows a caller to set the {@link Model} object for this presenter (for instance, a default
     * {@link Model} object is set if the user chooses a new viewpoint. If the user chooses to open a previous
     * viewpoint from a serialized file, then a  {@link Model} object is initialized from the file and set here.
     * This method calls {@link #setInitializedValuesInGUI()} in order to show relevant data in the GUI.
     * @param mod A {@link Model} object.
     */
    private void setModel(Model mod) {
        this.model=mod;
        logger.trace(String.format("Setting model to %s",mod.getProjectName()));
        setInitializedValuesInGUI();
        setBindings();
    }


    public void setModelInMainAndInAnalysisPresenter(Model mod) {
        setModel(mod);
        this.vpanalysispresenter.setModel(mod);
        logger.trace(String.format("setModelInMainAndInAnalysisPresenter for genome build %s and basename %s",mod.getGenome().getGenomeBuild(),model.getGenome().getGenomeBasename()));
        if (model.getMaxGCcontent()>0){
            this.maxGCContentTextField.setText(String.format("%.1f%%",model.getMaxGCContentPercent()));
        } else {
            this.maxGCContentTextField.setPromptText(String.format("%.1f%%",100*Default.MAX_GC_CONTENT));
        }
        if (model.getMinGCcontent()>0) {
            this.minGCContentTextField.setText(String.format("%.1f%%",model.getMinGCContentPercent()));
        } else {
            this.minGCContentTextField.setPromptText(String.format("%.1f%%",100*Default.MIN_GC_CONTENT));
        }
        if (model.getMinBaitCount()>0) {
            this.minBaitCountTextField.setText(String.valueOf(model.getMinBaitCount()));
        } else {
            this.minBaitCountTextField.setText(String.valueOf(Default.MIN_BAIT_NUMBER));
        }
        if (model.getMaxBaitCount()>0) {
            this.maxBaitCountTextField.setText(String.valueOf(model.getMaxBaitCount()));
        } else {
            this.maxBaitCountTextField.setText(String.valueOf(Default.MAX_BAIT_NUMBER));
        }
        if (model.getMinFragSize()>0) {
            this.minFragSizeTextField.setText(String.format("%d",model.getMinFragSize()));
        } else {
            this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        }
        if (model.getMaxMeanKmerAlignability()>0) {
            this.maxKmerAlignabilityTextField.setText(String.format("%d",model.getMaxMeanKmerAlignability()));
        } else {
            this.maxKmerAlignabilityTextField.setPromptText(String.format("%d",Default.MAXIMUM_KMER_ALIGNABILITY));
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
        if (model.getChosenEnzymelist()!=null && model.getChosenEnzymelist().size()>0) {
            this.restrictionEnzymeLabel.setText(model.getAllSelectedEnzymeString());
        } else {
            this.restrictionEnzymeLabel.setText("not initialized");
        }
        if (model.getVPVGeneList()!=null && model.getVPVGeneList().size()>0) {
            this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",
                    this.model.getChosenGeneCount(),
                    this.model.getUniqueChosenTSScount()));
        } else {
            this.nValidGenesLabel.setText("not initialized");
        }
        if (model.useSimpleApproach()) {
            this.approachChoiceBox.setValue("Simple");
        } else if (model.useExtendedApproach()){
            this.approachChoiceBox.setValue("Extended");
        }
    }


    /** The prompt (gray) values of the text fields in the settings windows get set to their default values here. */
    private void initializePromptTextsToDefaultValues() {
        this.sizeUpTextField.setPromptText(String.format("%d",Default.SIZE_UPSTREAM));
        this.sizeDownTextField.setPromptText(String.format("%d",Default.SIZE_DOWNSTREAM));
        this.minGCContentTextField.setPromptText(String.format("%.1f %%",100*Default.MIN_GC_CONTENT));
        this.minBaitCountTextField.setPromptText(String.valueOf(Default.MIN_BAIT_NUMBER));
        this.maxBaitCountTextField.setPromptText(String.valueOf(Default.MAX_BAIT_NUMBER));
        this.maxGCContentTextField.setPromptText(String.format("%.1f %%",100*Default.MAX_GC_CONTENT));
        this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        this.maxKmerAlignabilityTextField.setPromptText(String.format("%d",Default.MAXIMUM_KMER_ALIGNABILITY));
    }

    /** Remove any previous values from the text fields so that if the user chooses "New" from the File menu, they
     * will not see the values chosen for the previous model, but will instead see the grey prompt text default values.
     */
    private void removePreviousValuesFromTextFields() {
        this.sizeUpTextField.setText(null);
        this.sizeDownTextField.setText(null);
        this.minGCContentTextField.setText(null);
        this.minBaitCountTextField.setText(null);
        this.maxBaitCountTextField.setText(null);
        this.maxGCContentTextField.setText(null);
        this.minFragSizeTextField.setText(null);
        this.maxKmerAlignabilityTextField.setText(null);
    }

    /** Keep the six fields in the GUI in synch with the corresponding variables in this class. */
    private void setBindings() {
        StringConverter<Number> converter = new NumberStringConverter();
        Bindings.bindBidirectional(this.sizeDownTextField.textProperty(),sizeDownProperty(),converter);
        Bindings.bindBidirectional(this.sizeUpTextField.textProperty(), sizeUpProperty(),converter);
        Bindings.bindBidirectional(this.minFragSizeTextField.textProperty(),minFragSizeProperty(),converter);
        Bindings.bindBidirectional(this.maxKmerAlignabilityTextField.textProperty(),maxMeanKmerAlignabilityProperty(),converter);
        Bindings.bindBidirectional(this.minGCContentTextField.textProperty(),minGCcontentProperty(),converter);
        Bindings.bindBidirectional(this.maxGCContentTextField.textProperty(),maxGCcontentProperty(),converter);
        Bindings.bindBidirectional(this.minBaitCountTextField.textProperty(),minimumBaitCountProperty(),converter);
        Bindings.bindBidirectional(this.maxBaitCountTextField.textProperty(),maximumBaitCountProperty(),converter);
        sizeDownTextField.clear();
        sizeUpTextField.clear();
        minFragSizeTextField.clear();
        maxKmerAlignabilityTextField.clear();
        minGCContentTextField.clear();
        maxGCContentTextField.clear();
        minBaitCountTextField.clear();
        maxBaitCountTextField.clear();

    }

    /** This method should be called before we create viewpoints. It updates all of the variables in our model object
     * to have the values specified in the user for the GUI, including the values of the six fields we show in the GUI
     * and that are bound in {@link #setBindings()}. Note that we store GC and repeat content as a proportion in
     * {@link Model} but confirmDialog it as a proportion in the GUI. The default values are used for any fields that have not
     * been filled in by the user.
     */
    private void updateModel() {
        this.model.setSizeDown(getSizeDown()>0?getSizeDown():Default.SIZE_DOWNSTREAM);
        this.model.setSizeUp(getSizeUp()>0?getSizeUp():Default.SIZE_UPSTREAM);
        this.model.setMinFragSize(getMinFragSize()>0?getMinFragSize():Default.MINIMUM_FRAGMENT_SIZE);
        double repeatProportion=getMaxRepeatContent()/100;
        this.model.setMaxRepeatContent(repeatProportion>0?repeatProportion:Default.MAXIMUM_KMER_ALIGNABILITY);
        double minGCproportion = getMinGCcontent()/100;
        this.model.setMinGCcontent(minGCproportion>0?minGCproportion:Default.MIN_GC_CONTENT);
        double maxGCproportion = getMaxGCcontent()/100;
        this.model.setMaxGCcontent(maxGCproportion>0?maxGCproportion:Default.MAX_GC_CONTENT);
        int kmerAlign = getMaxMeanKmerAlignability()>0?getMaxMeanKmerAlignability() : Default.MAXIMUM_KMER_ALIGNABILITY;
        this.model.setMaxMeanKmerAlignability(kmerAlign);
        int minbait = getMinimumBaitCount()>0 ? getMinimumBaitCount() : Default.MIN_BAIT_NUMBER;
        this.model.setMinBaitCount(minbait);
        int maxbait = getMaximumBaitCount()>0?getMaximumBaitCount() : Default.MAX_BAIT_NUMBER;
        this.model.setMaxBaitCount(maxbait);
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
        this.alignabilityDownloadPI.setProgress(0.0);
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
    @FXML public void downloadGenome(ActionEvent e) {
        e.consume();
        String build = this.model.getGenomeBuild();
        logger.info("About to download genome for "+build +" (if necessary)");
        GenomeDownloader gdownloader = new GenomeDownloader(build);
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + build + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().equals("")) {
            logger.error("Could not set genome download path from Directory Chooser");
            PopupFactory.displayError("Error","Could not get path to download genome.");
            return;
        }
        logger.info("downloadGenome to directory  "+ file.getAbsolutePath());
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
        String url;
        try {
            url = rgd.getURL();
        } catch (DownloadFileNotFoundException dfne) {
            PopupFactory.displayError("Could not identify RefGene file for genome",dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for " + genomeBuild + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error","Could not get path to download transcript file.");
            return;
        }
        if (! rgd.needToDownload(file.getAbsolutePath())) {
            logger.trace(String.format("Found refGene.txt.gz file at %s. No need to download",file.getAbsolutePath()));
            this.transcriptDownloadPI.setProgress(1.0);
            this.downloadedTranscriptsLabel.setText(transcriptName);
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.model.setRefGenePath(abspath);
            return;
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
            decompressGenomeLabel.setText("Chromosome files extracted");
            genomeDecompressPI.setProgress(1.00);
            model.setGenomeUnpacked();
            return;
        }
        GenomeGunZipper genomeGunZipper = new GenomeGunZipper(this.model.getGenome(),
                this.genomeDecompressPI);
        genomeGunZipper.setOnSucceeded( event -> {
            decompressGenomeLabel.setText(genomeGunZipper.getStatus());
            if (genomeGunZipper.OK()) {
                model.setGenomeUnpacked();
            } else {
                PopupFactory.displayError("Error","Error from Genome g-unzipper");
            }
        });
        genomeGunZipper.setOnFailed(eventh -> {
            decompressGenomeLabel.setText("Decompression failed");
            PopupFactory.displayError("Could not decompress genome file" ,genomeGunZipper.getException().getMessage());
        });
        Thread th = new Thread(genomeGunZipper);
        th.setDaemon(true);
        th.start();
    }

    /** Create genome fai (fasta index file)
     * @param e Event triggered by index genome command.
     * */
    @FXML public void indexGenome(ActionEvent e) {
        e.consume();
        logger.trace("Indexing genome files...");
        Faidx manager = new Faidx(this.model,this.genomeIndexPI);
        manager.setOnSucceeded(event ->{
            int n_chroms = manager.getContigLengths().size();
            String message = String.format("%d chromosomes in %s successfully indexed.",
                    n_chroms,
                    model.getGenome().getGenomeFastaName());
            indexGenomeLabel.setText(message);
            logger.debug(message);
            model.setIndexedGenomeFastaIndexFile(manager.getGenomeFastaIndexPath());
           model.setGenomeIndexed();
        } );
        manager.setOnFailed(event-> {
            indexGenomeLabel.setText("FASTA indexing failed");
            PopupFactory.displayError("Failure to index Genome FASTA file.",
                    manager.getException().getMessage());
        });
        Thread th = new Thread(manager);
        th.setDaemon(true);
        th.start();
    }


    /**
     * @param e event triggered by command to download appropriate {@code refGene.txt.gz} file.
     */
    @FXML public void downloadAlignabilityMap(ActionEvent e) {

        String genomeBuild = genomeChoiceBox.getValue(); // e.g. hg19 or mm9

        DirectoryChooser dirChooser = new DirectoryChooser(); // choose directory to which the map will be downloaded
        dirChooser.setTitle("Choose directory for " + genomeBuild + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error","Could not get path to download alignabilty file.");
            return;
        }

        // assemble file paths including file names and save in model
        String alignabilityMapPathIncludingFileNameGz = file.getAbsolutePath();
        alignabilityMapPathIncludingFileNameGz += File.separator;
        alignabilityMapPathIncludingFileNameGz += genomeBuild;
        alignabilityMapPathIncludingFileNameGz += ".50mer.alignabilityMap.bedgraph.gz";
        model.setAlignabilityMapPathIncludingFileNameGz(alignabilityMapPathIncludingFileNameGz);

        String chromInfoPathIncludingFileNameGz = file.getAbsolutePath();
        chromInfoPathIncludingFileNameGz += File.separator;
        chromInfoPathIncludingFileNameGz += "chromInfo.txt.gz";
        model.setChromInfoPathIncludingFileNameGz(chromInfoPathIncludingFileNameGz);


        // check if the file that is going to be downloaded already exists
        if (model.alignabilityMapPathIncludingFileNameGzExists()) {
            logger.trace(String.format("Found " +  alignabilityMapPathIncludingFileNameGz + ". No need to download"));
            this.alignabilityDownloadPI.setProgress(1.0);
            this.downloadAlignabilityLabel.setText("Download complete");
            return;
        }

        // prepare download
        String basenameGz = genomeBuild.concat(".50mer.alignabilityMap.bedgraph.gz");
        String url;
        String url2;
        if(genomeBuild.equals("hg19")) {
            url = "https://www.dropbox.com/s/lxrkpjfwy6xenq5/wgEncodeCrgMapabilityAlign50mer.bedpraph.gz?dl=1"; // this is 50-mer
            url2 = "http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/chromInfo.txt.gz";

        } else if (genomeBuild.equals("mm9")) {
            url = "https://www.dropbox.com/s/nqq1c8vzuh5o4ky/wgEncodeCrgMapabilityAlign100mer.bedgraph.gz?dl=1"; // this is still 100-mer
            url2 = "http://hgdownload.cse.ucsc.edu/goldenPath/mm9/database/chromInfo.txt.gz";
        } else {
            this.downloadAlignabilityLabel.setText(("No map available for " + genomeBuild));
            return;
        }
        // also download chromosme file
        Downloader downloadTask0 = new Downloader(file, url2, "chromInfo.txt.gz", alignabilityDownloadPI);
        Thread th = new Thread(downloadTask0);
        th.start();

        Downloader downloadTask = new Downloader(file, url, basenameGz, alignabilityDownloadPI);
        downloadTask.setOnSucceeded( event -> {
            this.downloadAlignabilityLabel.setText("Download complete");
        });
        th = new Thread(downloadTask);
        th.setDaemon(true);
        th.start();
        e.consume();
    }

    /** This function is called after the user has chosen restriction enzymes in the
     * corresponding popup window. It passes a list of the {@link RestrictionEnzyme}
     * objects to the {@link Model}.*/
    @FXML public void chooseEnzymes() {
        List<RestrictionEnzyme> chosenEnzymes = EnzymeViewFactory.getChosenEnzymes(this.model);
        if (chosenEnzymes==null || chosenEnzymes.size()==0) {
            PopupFactory.displayError("Warning","Warning -- no restriction enzyme chosen!");
            return;
        }
        this.model.setChosenRestrictionEnzymes(chosenEnzymes);
        this.restrictionEnzymeLabel.setText(this.model.getAllSelectedEnzymeString());
    }

    /**
     * Open a new dialog where the user can upload gene symbols or Entrez Gene IDs.
     * The effect of the command <pre>EntrezGeneViewFactory.confirmDialog(this.model);</pre>
     * is to pass a list of {@link GopherGene} objects to the {@link Model}.
     * These objects are used with other information in the Model to create {@link gopher.model.viewpoint.ViewPoint}
     * objects when the user clicks on {@code Create ViewPoints}.
     * See {@link EntrezGeneViewFactory} for logic.
     *
     * @param e event triggered by enter gene command.
     */
    @FXML public void enterGeneList(ActionEvent e) {
        EntrezGeneViewFactory.display(this.model);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",
                this.model.getChosenGeneCount(),
                this.model.getUniqueChosenTSScount()));
        e.consume();
    }


    @FXML private void saveDigestFileAs(ActionEvent e) {
        logger.trace("Saving the digest file");
        // get path from chooser
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose file path to save digest file");
        File file = chooser.showSaveDialog(null);
        if (file==null) {
            return;
        }
        String path = file.getAbsolutePath();
        StringProperty sp=new SimpleStringProperty();
        DigestCreationTask task = new DigestCreationTask(path,model,sp);

        TaskProgressBarView pbview = new TaskProgressBarView();
        TaskProgressBarPresenter pbpresent = (TaskProgressBarPresenter)pbview.getPresenter();
        pbpresent.setTitle("Creating Digest file");
        pbpresent.initBindings(task,sp);
        Stage window = new Stage();
        String windowTitle = "Digest file creation";
        window.setOnCloseRequest( event -> window.close() );
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
            logger.trace("Finished creating digest file");
            pbpresent.closeWindow();
        });
        task.setOnFailed(eh -> {
            Exception exc = (Exception)eh.getSource().getException();
            PopupFactory.displayException("Error",
                    "Exception encountered while attempting to create digest file",
                    exc);
        });
        new Thread(task).start();
        window.setScene(new Scene(pbview.getView()));
        window.showAndWait();
        e.consume();
    }

    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link GopherGene} objects into the {@link Model}
     * object. This function will use the {@link GopherGene} obejcts and other information
     * to create {@link gopher.model.viewpoint.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisPresenter} Tab.
     */
    public void createViewPoints() throws IOException {
        String approach = this.approachChoiceBox.getValue();
        this.model.setApproach(approach);
        updateModel();
        boolean OK=QCCheckFactory.showQCCheck(model);
        if (! OK ) {
            return;
        }
        StringProperty sp=new SimpleStringProperty();
        ViewPointCreationTask task;

        // TODO use boolean var allowSingleMargin

        logger.trace("Reading alignability map to memory...");
        AlignabilityMap alignabilityMap = new AlignabilityMap(model.getChromInfoPathIncludingFileNameGz(),model.getAlignabilityMapPathIncludingFileNameGz(),50);
        logger.trace("...done.");

        if (model.useSimpleApproach()) {
            task = new SimpleViewPointCreationTask(model,sp,alignabilityMap);
        } else {
            task = new ExtendedViewPointCreationTask(model,sp,alignabilityMap);
        }

        TaskProgressBarView pbview = new TaskProgressBarView();
        TaskProgressBarPresenter pbpresent = (TaskProgressBarPresenter)pbview.getPresenter();
        pbpresent.setTitle("Creating Viewpoints ...4");
        pbpresent.initBindings(task,sp);

        Stage window = new Stage();
        String windowTitle = "Viewpoint creation";
        window.setOnCloseRequest( event -> window.close() );
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
            logger.trace("Finished createViewPoints()");
            pbpresent.closeWindow();
        });
        task.setOnFailed(eh -> {
            Exception exc = (Exception)eh.getSource().getException();
            PopupFactory.displayException("Error",
                    "Exception encountered while attempting to create viewpoints",
                    exc);
        });
        new Thread(task).start();
        window.setScene(new Scene(pbview.getView()));
        window.showAndWait();
    }



    /**
     * This method is run after user clicks on 'Close' item of File|Menu. User is prompted to confirm the closing and
     * window is closed if 'yes' is selected.
     * @param e Event triggered by close command.
     */
    public void closeWindow(ActionEvent e) {
        boolean answer = PopupFactory.confirmDialog("Alert", "Are you sure you want to quit?");
        if (answer) {
            logger.info("Closing VPV Gui");
            serialize();
            javafx.application.Platform.exit();
        }
    }

    public void refreshViewPoints() {
        if (this.vpanalysispresenter==null) {
            logger.error("Could not refresh viewpoint table, since vpanalysispresenter was null");
            return;
        }
        this.vpanalysispresenter.refreshVPTable();
    }

    /**
     * This is called when the user starts a new viewpoint. It erases everything from
     * the GUI as well
     * @param e Event triggered by new viewpoint command.
     */
    @FXML public void startNewProject(ActionEvent e) {
        PopupFactory factory = new PopupFactory();
        String projectname = factory.getProjectName();
        if (factory.wasCancelled())
            return; // do nothing, the user cancelled!
        if (projectname == null || projectname.length() <1) {
            PopupFactory.displayError("Could not get valid project name", "enter a valid name starting with a letter, character or underscore!");
            return;
        }

        ObservableList<Tab> panes = this.tabpane.getTabs();
        /* collect tabs first then remove them -- avoids a ConcurrentModificationException */
        List<Tab> tabsToBeRemoved=new ArrayList<>();
        /* close all tabs except setup and analysis. */
        for (Tab tab : panes) {
            String id=tab.getId();
            if (id != null && (id.equals("analysistab") || id.equals("setuptab") )) { continue; }
            tabsToBeRemoved.add(tab);
        }
        this.tabpane.getTabs().removeAll(tabsToBeRemoved);
        this.model=new Model();
        this.model.setProjectName(projectname);
        if (this.primaryStage!=null)
            this.primaryStage.setTitle(String.format("GOPHER: %s",projectname));
        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setModel(this.model);
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        this.analysisPane.getChildren().add(vpanalysisview.getView());
        setInitializedValuesInGUI();
        removePreviousValuesFromTextFields();
        e.consume();
    }

    /** Display the settings (parameters) of the current viewpoint. */
    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(model.getProperties());
    }

    /**
     * Content of {@link Model} is written to platform-dependent default location.
     */
    @FXML private void saveProject(ActionEvent e) {
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
     * Save all of the data about the current analysis (project) to a serialized file.
     */
    @FXML private void saveProjectAndClose() {
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
        window.setOnCloseRequest( event -> window.close() );
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
            PopupFactory.displayError("Error obtaining Proxy","Proxy string could not be obtained. Please try again");
            return;
        }
        this.model.setHttpProxy(proxy);
        this.model.setHttpProxyPort(port);
        logger.info(String.format("Set proxy to %s[%s]",proxy,port));
        Utils.setSystemProxyAndPort(proxy,port);
    }


    @FXML public void openGeneWindowWithExampleHumanGenes() {
        File file = new File(getClass().getClassLoader().getResource("humangenesymbols.txt").getFile());
        if (! file.exists()) {
            PopupFactory.displayError("Could not open example human gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",
                this.model.getChosenGeneCount(),
                this.model.getUniqueChosenTSScount()));
    }
    @FXML public void openGeneWindowWithExampleFlyGenes() {
        File file = new File(getClass().getClassLoader().getResource("flygenesymbols.txt").getFile());
        if (! file.exists()) {
            PopupFactory.displayError("Could not open example fly gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",
                this.model.getChosenGeneCount(),
                this.model.getUniqueChosenTSScount()));
    }
    @FXML public void openGeneWindowWithExampleMouseGenes() {
        File file = new File(getClass().getClassLoader().getResource("mousegenesymbols.txt").getFile());
        if (! file.exists()) {
            PopupFactory.displayError("Could not open example mouse gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",
                this.model.getChosenGeneCount(),
                this.model.getUniqueChosenTSScount()));
    }
    @FXML public void openGeneWindowWithExampleRatGenes() {
        File file = new File(getClass().getClassLoader().getResource("ratgenesymbols.txt").getFile());
        if (! file.exists()) {
            PopupFactory.displayError("Could not open example rat gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,file);
        this.nValidGenesLabel.setText(String.format("%d valid genes with %d viewpoint starts",
                this.model.getChosenGeneCount(),
                this.model.getUniqueChosenTSScount()));
    }

    @FXML public void exportBEDFiles(ActionEvent e) {
        List<ViewPoint> vplist=this.model.getViewPointList();
        if (vplist==null || vplist.isEmpty()) {
            PopupFactory.displayError("Attempt to export empty BED files","Complete generation and analysis of ViewPoints before exporting to BED!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting BED files.");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().equals("")) {
            PopupFactory.displayError("Error","Could not get path to export BED files.");
            return;
        }
        String prefix=model.getProjectName();
        BEDFileExporter exporter = new BEDFileExporter(file.getAbsolutePath(),prefix);
        try {
            exporter.printRestFragsToBed(this.model.getViewPointList(),this.model.getGenomeBuild());
        } catch (Exception exc) {
            PopupFactory.displayException("Could not save data to BED files", exc.getMessage(),exc);
        }
        e.consume();
    }

    @FXML
    public void setProbeLength(ActionEvent e) {
        PopupFactory factory = new PopupFactory();
        Integer len= factory.setProbeLength(model.getProbeLength());
        if (factory.wasCancelled())
            return; // do nothing, the user cancelled!
        if (len == null || len <=0) {
            PopupFactory.displayError("Could not get probe length", "enter a positive integer value!");
            return;
        }
        this.model.setProbeLength(len);
        this.vpanalysispresenter.refreshVPTable();
        logger.trace(String.format("probe length set to %d", model.getProbeLength()));
    }


    @FXML
    public void setMarginSize(ActionEvent e) {
        PopupFactory factory = new PopupFactory();
        Integer len= factory.setMarginSize(model.getMarginSize());
        if (factory.wasCancelled())
            return; // do nothing, the user cancelled!
        if (len == null || len <=0) {
            PopupFactory.displayError("Could not get margin size length", "enter a positive integer value!");
            return;
        }
        this.model.setMarginSize(len);
        this.vpanalysispresenter.refreshVPTable();
        logger.trace(String.format("MarginSize set to %d", model.getMarginSize()));
    }

    @FXML
    public void showLog(ActionEvent e) {
        LogViewerFactory factory = new LogViewerFactory();
        factory.display();
        e.consume();
    }

    @FXML
    public void about(ActionEvent e) {
        PopupFactory.showAbout(model.getVersion(), model.getLastChangeDate());
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
     * which is the .gopher directory). */
    @FXML
    public void exportProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        String initFileName=String.format("%s.ser",this.model.getProjectName());
        chooser.setInitialFileName(initFileName);
        chooser.setTitle("Choose file path to save project file");
        File file = chooser.showSaveDialog(null);
        String path = file.getAbsolutePath();
        serializeToLocation(path);
        logger.trace(String.format("Serialized file to %s",path));
        e.consume();
    }

    /** Open a project from a file specified by the user. */
    @FXML
    public void importProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open VPV project file");
        File file = chooser.showOpenDialog(null);
        if (file==null) { //Null pointer returned if user clicks on cancel. In this case, just do nothing.
            return;
        }
        try {
            removePreviousValuesFromTextFields();
            this.model = SerializationManager.deserializeModel(file.getAbsolutePath());
            if (this.primaryStage!=null)
                this.primaryStage.setTitle(String.format("GOPHER: %s",
                        model.getProjectName()));

        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setModel(this.model);
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        this.analysisPane.getChildren().add(vpanalysisview.getView());
        setInitializedValuesInGUI();

            setModelInMainAndInAnalysisPresenter(this.model);
            logger.trace(String.format("Opened model %s from file %s",model.getProjectName(), file.getAbsolutePath()));
        } catch (IOException ex) {
            PopupFactory.displayException("Error","I/O Error opening project file", ex);
        } catch (ClassNotFoundException clnf) {
            PopupFactory.displayException("Error","Deserialization error",clnf);
        }
        e.consume();
    }

    @FXML
    public void downloadRegulationData(ActionEvent event) {
        String genomeBuild=genomeChoiceBox.getValue();
        RegulatoryBuildDownloader regbuildDownloader = new RegulatoryBuildDownloader(genomeBuild);
        String basename=regbuildDownloader.getBaseName();
        String url;
        try {
            url = regbuildDownloader.getURL();
        } catch (DownloadFileNotFoundException dfne) {
            PopupFactory.displayError(String.format("Cannot generate Regulatory Exome for genome: %s",genomeBuild),dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory to download regulatory build for " + genomeBuild + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error","Could not get path to download regulatory build file.");
            return;
        }
        if (! regbuildDownloader.needToDownload(file.getAbsolutePath())) {
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            PopupFactory.displayMessage("Regulatory Build",String.format("Found regulatory build file at %s. No need to download",file.getAbsolutePath()));
            model.setRegulatoryBuildPath(abspath);
            return;
        }

        ProgressPopup popup = new ProgressPopup("Downloading...", "Downloading Ensembl regulatory build file");
        ProgressIndicator progressIndicator = popup.getProgressIndicator();

        Downloader downloadTask = new Downloader(file, url, basename, progressIndicator);
        downloadTask.setOnSucceeded( e -> {
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            logger.trace("Setting regulatory build path in model to "+abspath);
            model.setRegulatoryBuildPath(abspath);

            popup.close();
        });
        downloadTask.setOnFailed(e -> logger.error("Download of regulatory build failed") );
        try {
            popup.startProgress(downloadTask);
        } catch (InterruptedException e) {
            PopupFactory.displayException("Error","Could not download regulatory build", e);
        }
        event.consume();
    }

    @FXML
    public void buildRegulatoryExome(ActionEvent event) {
        event.consume();
        if (!model.viewpointsInitialized()) {
            PopupFactory.displayError("Viewpoints not initialized",
                    "Please initialize viewpoints before exporting regulatory exome");
            return;
        }
        if (! model.regulatoryBuildPathInitialized()) {
            PopupFactory.displayError("Regulatory build path not initialized",
                    "Please download the regulatory build file before exporting regulatory exome");
            return;
        }
        try {
            final File regulatoryExomeDirectory = RegulatoryExomeBoxFactory.getDirectoryForExport(this.rootNode);
            logger.info("downloadGenome to directory  " + regulatoryExomeDirectory.getAbsolutePath());
            javafx.application.Platform.runLater(() ->
                        RegulatoryExomeBoxFactory.exportRegulatoryExome(model, regulatoryExomeDirectory));
        } catch (Exception e) {
            PopupFactory.displayException("Error", "Could not create regulatory exome panel data", e);
        }
        logger.trace("buildRegulatoryExome");
    }

    public void setPrimaryStageReference(Stage stage) {
        this.primaryStage=stage;
    }

    @FXML public void displayReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.model);
        PopupFactory.showSummaryDialog(report.getReport());
        e.consume();
    }

    @FXML public void exportReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.model);
        String filename =String.format("%s-report.txt",model.getProjectName());
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(filename);
        File file=chooser.showSaveDialog(this.primaryStage);
        if (file==null) {
            PopupFactory.displayError("Error","Could not get filename for saving report");
            return;
        }
        report.outputRegulatoryReport(file.getAbsolutePath());
        e.consume();
    }

    @FXML public void createProbes(ActionEvent event) {
        event.consume();
        if (!model.viewpointsInitialized()) {
            PopupFactory.displayError("Viewpoints not initialized",
                    "Please initialize viewpoints before creating probes");
            return;
        }
        try {
            //ProbeFactory probeFactory = new ProbeFactory(model);
            /*
            final File regulatoryExomeDirectory = RegulatoryExomeBoxFactory.getDirectoryForExport(this.rootNode);
            logger.info("downloadGenome to directory  " + regulatoryExomeDirectory.getAbsolutePath());
            javafx.application.Platform.runLater(() ->
                    RegulatoryExomeBoxFactory.exportRegulatoryExome(model, regulatoryExomeDirectory));*/
        } catch (Exception e) {
            PopupFactory.displayException("Error", "Could not create probes", e);
        }
        logger.trace("buildRegulatoryExome");
    }

    @FXML public void exportProbes(ActionEvent e) {
        GopherReport report = new GopherReport(this.model);
        String filename =String.format("%s-probes.txt",model.getProjectName());
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(filename);
        File file=chooser.showSaveDialog(this.primaryStage);
        if (file==null) {
            PopupFactory.displayError("Error","Could not get filename for saving report");
            return;
        }
        report.outputRegulatoryReport(file.getAbsolutePath());
        e.consume();
    }

}




