package gopher.gui.gophermain;

import com.google.common.collect.ImmutableList;
import gopher.exception.DownloadFileNotFoundException;
import gopher.exception.GopherException;
import gopher.gui.analysisPane.VPAnalysisPresenter;
import gopher.gui.analysisPane.VPAnalysisView;
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
import gopher.gui.taskprogressbar.TaskProgressBarPresenter;
import gopher.gui.taskprogressbar.TaskProgressBarView;
import gopher.gui.util.WindowCloser;
import gopher.io.*;
import gopher.model.*;
import gopher.model.digest.DigestCreationTask;
import gopher.model.viewpoint.ExtendedViewPointCreationTask;
import gopher.model.viewpoint.SimpleViewPointCreationTask;
import gopher.model.viewpoint.ViewPoint;
import gopher.model.viewpoint.ViewPointCreationTask;
import gopher.util.SerializationManager;
import gopher.util.Utils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import javafx.util.StringConverter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A Java app to help design probes for Capture Hi-C
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.5.5 (2018-09-10)
 */
public class GopherMainPresenter implements Initializable {
    private final static Logger logger = Logger.getLogger(GopherMainPresenter.class.getName());
    /** The Model for the entire analysis.*/
    private Model model = null;
    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc. It is set in the FXML
     * document to refer to the Anchor pane that is the root node of the GUI.
     */
    @FXML
    private Node rootNode;
    /** List of genome builds. Used by genomeChoiceBox */
    @FXML
    private final ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9", "mm10");
    /** List of Design approaches.  */
    @FXML
    private final ObservableList<String> approachList = FXCollections.observableArrayList("Simple", "Extended");
    @FXML
    private ChoiceBox<String> genomeChoiceBox;
    @FXML
    private ChoiceBox<String> approachChoiceBox;
    @FXML
    private Button decompressGenomeButton;
    @FXML
    private Button indexGenomeButton;
    /**
     * Clicking this button will download the genome file if it is not found at the indicated directory.
     */
    @FXML
    private Button downloadGenomeButton;
    /** Button to download RefSeq.tar.gz (transcript/gene definition file*/
    @FXML
    private Button downloadTranscriptsButton;
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
     * Progress indicator for downloading the transcript file
     */
    @FXML
    private ProgressIndicator transcriptDownloadPI;
    /**
     * Progress indicator for downloading alignability file
     */
    @FXML
    private ProgressIndicator alignabilityDownloadPI;

    @FXML
    private Label sizeUpLabel;
    @FXML
    private Label sizeDownLabel;
    @FXML
    private TextField sizeUpTextField;
    @FXML
    private TextField sizeDownTextField;
    @FXML
    private TextField minFragSizeTextField;
    @FXML
    private TextField maxKmerAlignabilityTextField;
    @FXML
    private TextField minGCContentTextField;
    @FXML
    private TextField maxGCContentTextField;
    @FXML
    private TextField minBaitCountTextField;
    @FXML
    private TextField baitLengthTextField;
    @FXML
    private TextField marginSizeTextField;

    @FXML
    private Label patchedViewpointLabel;
    @FXML
    private CheckBox unbalancedMarginCheckbox;
    @FXML
    private CheckBox patchedViewpointCheckbox;

    @FXML
    RadioMenuItem loggingLevelOFF;
    @FXML
    RadioMenuItem loggingLevelTrace;
    @FXML
    RadioMenuItem loggingLevelInfo;
    @FXML
    RadioMenuItem loggingLevelDebug;
    @FXML
    RadioMenuItem loggingLevelWarn;
    @FXML
    RadioMenuItem loggingLevelError;

    @FXML private Label targetGeneLabel;
    @FXML private Label allGenesLabel;
    @FXML private Label bedTargetsLabel;

    /**
     * Show which enzymes the user has chosen.
     */
    @FXML
    private Label restrictionEnzymeLabel;

    @FXML
    private TabPane tabpane;
    @FXML
    private StackPane analysisPane;

    /**
     * The 'second' tab of VPVGui that shows a summary of the analysis and a list of Viewpoints.
     */
    @FXML
    private Tab analysistab;
    /**
     * Click this to choose the restriction enzymes with which to do the capture Hi-C cutting
     */
    @FXML
    private Button chooseEnzymeButton;
    /**
     * Presenter for the second tab.
     */
    private VPAnalysisPresenter vpanalysispresenter;
    /**
     * View for the second tab.
     */
    private VPAnalysisView vpanalysisview;
    /**
     * Reference to the primary stage. We use this to set the title when we switch models (new from File menu).
     */
    private Stage primaryStage = null;

    final transient private IntegerProperty sizeUp = new SimpleIntegerProperty();

    private int getSizeUp() {
        return sizeUp.get();
    }

    private void setSizeUp(int su) {
        sizeUp.set(su);
    }

    private IntegerProperty sizeDownProperty() {
        return sizeDown;
    }

    final transient private IntegerProperty sizeDown = new SimpleIntegerProperty();

    private int getSizeDown() {
        return sizeDown.get();
    }

    private void setSizeDown(int sd) {
        sizeUp.set(sd);
    }

    private IntegerProperty sizeUpProperty() {
        return sizeUp;
    }

    final transient private IntegerProperty minFragSize = new SimpleIntegerProperty();

    private int getMinFragSize() {
        return minFragSize.get();
    }

    private void setMinFragSize(int i) {
        this.minFragSize.set(i);
    }

    private IntegerProperty minFragSizeProperty() {
        return minFragSize;
    }

    final transient private DoubleProperty maxRepeatContent = new SimpleDoubleProperty();

    private double getMaxRepeatContent() {
        return maxRepeatContent.get();
    }

    private void setMaxRepeatContent(double r) {
        this.maxRepeatContent.set(r);
    }

    private DoubleProperty maxRepeatContentProperty() {
        return maxRepeatContent;
    }

    final transient private IntegerProperty maxMeanKmerAlignability = new SimpleIntegerProperty();

    private int getMaxMeanKmerAlignability() {
        return maxMeanKmerAlignability.get();
    }

    private void setMaxMeanKmerAlignability(int mmka) {
        this.maxMeanKmerAlignability.set(mmka);
    }

    private IntegerProperty maxMeanKmerAlignabilityProperty() {
        return maxMeanKmerAlignability;
    }

    final transient private IntegerProperty minBaitCount = new SimpleIntegerProperty();

    private int getMinimumBaitCount() {
        return minBaitCount.get();
    }

    private void setMinimumBaitCount(int bc) {
        this.minBaitCount.set(bc);
    }

    private IntegerProperty minimumBaitCountProperty() {
        return minBaitCount;
    }

    final transient private IntegerProperty baitLength = new SimpleIntegerProperty();

    private int getBaitLength() {
        return baitLength.get();
    }

    private void setBaitLength(int len) {
        this.baitLength.set(len);
    }

    private IntegerProperty baitLengthProperty() {
        return baitLength;
    }

    final transient private IntegerProperty marginLength = new SimpleIntegerProperty();

    private int getMarginLength() {
        return marginLength.get();
    }

    private void setMarginLength(int len) {
        this.marginLength.set(len);
    }

    private IntegerProperty marginLengthProperty() {
        return marginLength;
    }

    final transient private IntegerProperty maxBaitCount = new SimpleIntegerProperty();

    private int getMaximumBaitCount() {
        return maxBaitCount.get();
    }

    private void setMaximumBaitCount(int bc) {
        this.maxBaitCount.set(bc);
    }

    final transient private DoubleProperty minGCcontent = new SimpleDoubleProperty();

    private double getMinGCcontent() {
        return minGCcontent.get();
    }

    private void setMinGCcontent(double mgc) {
        minGCcontent.set(mgc);
    }

    private DoubleProperty minGCcontentProperty() {
        return minGCcontent;
    }

    final transient private DoubleProperty maxGCcontent = new SimpleDoubleProperty();

    private double getMaxGCcontent() {
        return maxGCcontent.get();
    }

    /**
     * Note we expect the user to enter a percentage, and we convert it here to proportion.
     */
    private void setMaxGCcontent(double mgc) {
        maxGCcontent.set(mgc);
    }

    private DoubleProperty maxGCcontentProperty() {
        return maxGCcontent;
    }


    /**
     * Serialize the project data to the default location. Note that the class
     * {@link SerializationManager} will set the model's "clean" variable to true after
     * saving.
     */
    private boolean serialize() {
        String projectname = this.model.getProjectName();
        if (projectname == null) {
            PopupFactory.displayError("Error", "Could not get viewpoint name (should never happen). Will save with default");
            projectname = "default";
        }
        String serializedFilePath = Platform.getAbsoluteProjectPath(projectname);
        return serializeToLocation(serializedFilePath);
    }

    /**
     * Serialialize the project file to the location given as path.
     *
     * @param path absolute path to which the serilaized file should be saved.
     * @return true iff serialization is successful
     */
    private boolean serializeToLocation(String path) {
        if (path == null) {
            PopupFactory.displayError("Error", "Could not get file name for saving project file.");
            return false;
        }
        try {
            SerializationManager.serializeModel(this.model, path);
        } catch (IOException e) {
            PopupFactory.displayException("Error", "Unable to serialize Gopher viewpoint", e);
            return false;
        }
        logger.trace("Serialization successful to file " + path);
        return true;
    }

    private void setLoggingLevel(org.apache.log4j.Level level){
        LogManager.getRootLogger().setLevel(level);
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

        ToggleGroup tGroup = new ToggleGroup();

        tGroup.getToggles().addAll(loggingLevelOFF,loggingLevelTrace,loggingLevelInfo,loggingLevelDebug,loggingLevelWarn,loggingLevelError);
        loggingLevelOFF.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.OFF));
        loggingLevelTrace.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.TRACE));
        loggingLevelInfo.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.INFO));
        loggingLevelDebug.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.DEBUG));
        loggingLevelWarn.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.WARN));
        loggingLevelError.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.ERROR));
        loggingLevelError.setSelected(true);


        setGUItoSimple();

        initializePromptTexts();

        this.vpanalysisview = new VPAnalysisView();
        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
        this.analysisPane.getChildren().add(vpanalysisview.getView());

        this.approachChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, number2) -> {
            String selectedItem = approachChoiceBox.getItems().get((Integer) number2);
            switch (selectedItem) {
                case "Simple":
                    setGUItoSimple();
                    break;
                case "Extended":
                    setGUItoExtended();
                    break;
                default:
                    logger.error(String.format("Did not recognize approach in menu %s", selectedItem));
            }
        });
    }



    /** makes the upstream and downstream size fields invisible because they are irrelevant to the simple approach.*/
    private void setGUItoSimple() {
        this.sizeUpTextField.setDisable(true);
        this.sizeDownTextField.setDisable(true);
        this.sizeDownLabel.setDisable(true);
        this.sizeUpLabel.setDisable(true);
        this.patchedViewpointCheckbox.setDisable(false);
        this.patchedViewpointLabel.setDisable(false);
        this.sizeUpTextField.setVisible(false);
        this.sizeDownTextField.setVisible(false);
    }
    /** makes the upstream and downstream size fields visible because they are needed for the extended approach.*/
    private void setGUItoExtended() {
        this.sizeUpTextField.setDisable(false);
        this.sizeDownTextField.setDisable(false);
        this.sizeDownLabel.setDisable(false);
        this.sizeUpLabel.setDisable(false);
        this.patchedViewpointCheckbox.setDisable(true);
        this.patchedViewpointLabel.setDisable(true);
        this.patchedViewpointCheckbox.setSelected(false);
        this.sizeUpTextField.setVisible(true);
        this.sizeDownTextField.setVisible(true);

    }



    private void setInitializedValuesInGUI() {
        String path_to_downloaded_genome_directory=model.getGenomeDirectoryPath();
        if (path_to_downloaded_genome_directory!= null) {
           // this.downloadedGenomeLabel.setText(path_to_downloaded_genome_directory);
            this.genomeDownloadPI.setProgress(1.00);
        } else {
           // this.downloadedGenomeLabel.setText("...");
            this.genomeDownloadPI.setProgress(0);
        }
        if (model.isGenomeUnpacked()) {
           // this.decompressGenomeLabel.setText("Extraction previously completed");
            this.genomeDecompressPI.setProgress(1.00);
        } else {
           // this.decompressGenomeLabel.setText("...");
            this.genomeDecompressPI.setProgress(0.0);
        }
        String refGenePath=this.model.getRefGenePath();
        if (refGenePath!=null) {
            this.transcriptDownloadPI.setProgress(1.0);
        } else {
            this.transcriptDownloadPI.setProgress(0.0);
        }

        if (model.alignabilityMapPathIncludingFileNameGzExists()) {
            this.alignabilityDownloadPI.setProgress(1.0);
        } else {
            this.alignabilityDownloadPI.setProgress(0.0);
        }

        if (model.isGenomeIndexed()) {
            this.genomeIndexPI.setProgress(1.00);
        } else {
            this.genomeIndexPI.setProgress(0.00);
        }
        if (model.getChosenEnzymelist()!=null && model.getChosenEnzymelist().size()>0) {
            this.restrictionEnzymeLabel.setText(model.getAllSelectedEnzymeString());
        } else {
            this.restrictionEnzymeLabel.setText(null);
        }
        this.unbalancedMarginCheckbox.setSelected(model.getAllowUnbalancedMargins());
        this.patchedViewpointCheckbox.setSelected(model.getAllowPatching());

        this.targetGeneLabel.setText("");
        this.allGenesLabel.setText("");
        this.bedTargetsLabel.setText("");
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
        if (model.getProbeLength()==0) {
            this.baitLengthTextField.setText(String.valueOf(Default.PROBE_LENGTH));
        } else {
            this.baitLengthTextField.setText(String.valueOf(model.getProbeLength()));
        }
        if (model.getMarginSize()==0) {
            this.marginSizeTextField.setText(String.valueOf(Default.MARGIN_SIZE));
        } else {
            this.marginSizeTextField.setText(String.valueOf(model.getMarginSize()));
        }
        if (! model.getTargetType().equals(Model.TargetType.NONE))  {
            int n_targets = model.getN_validGeneSymbols();
            setTargetFeedback(model.getTargetType(),n_targets);
        }

        if (model.useSimpleApproach()) {
            this.approachChoiceBox.setValue("Simple");
        } else if (model.useExtendedApproach()){
            this.approachChoiceBox.setValue("Extended");
        }

        Model.TargetType ttype = model.getTargetType();
        switch (ttype) {
            case TARGET_GENES:
                int count = model.getN_validGeneSymbols();
                this.targetGeneLabel.setText(String.format("%d genes",count));
                break;
            case ALL_GENES:
                int allgenes = model.getN_validGeneSymbols();
                this.allGenesLabel.setText(String.format("%d genes",allgenes));
                break;
            case BED_TARGETS:
                int n_bedtargets = model.getN_validGeneSymbols();
                this.bedTargetsLabel.setText(String.format("%d targets",n_bedtargets));
                break;
        }

        this.genomeChoiceBox.setValue(model.getGenomeBuild());
        // after we have set up the model the first time, mark it as clean. Any changes after this will lead
        // to a confirmation window being opened if the user has changed anything.
        model.setClean(true);
    }


    /** The prompt (gray) values of the text fields in the settings windows get set to their default values here. */
    private void initializePromptTexts() {
        this.sizeUpTextField.setPromptText(String.format("%d",Default.SIZE_UPSTREAM));
        this.sizeDownTextField.setPromptText(String.format("%d",Default.SIZE_DOWNSTREAM));
        this.minGCContentTextField.setPromptText(String.format("%.1f %%",100*Default.MIN_GC_CONTENT));
        this.minBaitCountTextField.setPromptText(String.valueOf(Default.MIN_BAIT_NUMBER));
        this.maxGCContentTextField.setPromptText(String.format("%.1f %%",100*Default.MAX_GC_CONTENT));
        this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        this.maxKmerAlignabilityTextField.setPromptText(String.format("%d",Default.MAXIMUM_KMER_ALIGNABILITY));
        this.marginSizeTextField.setPromptText(String.valueOf(Default.MARGIN_SIZE));
        this.baitLengthTextField.setPromptText(String.valueOf(Default.PROBE_LENGTH));


    }

    /** Remove any previous values from the text fields so that if the user chooses "New" from the File menu, they
     * will not see the values chosen for the previous model, but will instead see the grey prompt text default values.
     */
    private void removePreviousValuesFromTextFields() {
        genomeChoiceBox.getSelectionModel().selectFirst();
        this.sizeUpTextField.setText(null);
        this.sizeDownTextField.setText(null);
        this.minGCContentTextField.setText(null);
        this.minBaitCountTextField.setText(null);
        this.maxGCContentTextField.setText(null);
        this.minFragSizeTextField.setText(null);
        this.maxKmerAlignabilityTextField.setText(null);
        this.marginSizeTextField.setText(null);
        this.baitLengthTextField.setText(null);
    }

    /**
     * This object is used to convert doubles in the bindings. It stops exceptions
     * from being thrown if the user enters non-numbers.
     */
    private final StringConverter<Number> doubleConverter = new StringConverter<Number>() {
        @Override
        public String toString(Number object) {
            return object == null ? "" : object.toString();
        }
        @Override
        public Number fromString(String string) {
            if (string == null) {
                return 0.0;
            } else {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException ex) {
                    return 0.0;
                }
            }
        }
    };

    private final StringConverter<Number> integerConverter = new StringConverter<Number>() {
        @Override
        public String toString(Number object) {
            return object == null ? "" : object.toString();
        }
        @Override
        public Number fromString(String string) {
            if (string == null) {
                return 0;
            } else {
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
        }
    };

    /** Keep the fields in the GUI in synch with the corresponding variables in this class. */
    private void setBindings() {
        Bindings.bindBidirectional(this.sizeDownTextField.textProperty(),sizeDownProperty(),integerConverter);
        Bindings.bindBidirectional(this.sizeUpTextField.textProperty(), sizeUpProperty(),integerConverter);
        Bindings.bindBidirectional(this.minFragSizeTextField.textProperty(),minFragSizeProperty(),integerConverter);
        Bindings.bindBidirectional(this.maxKmerAlignabilityTextField.textProperty(),maxMeanKmerAlignabilityProperty(),doubleConverter);
        Bindings.bindBidirectional(this.minGCContentTextField.textProperty(),minGCcontentProperty(),doubleConverter);
        Bindings.bindBidirectional(this.maxGCContentTextField.textProperty(),maxGCcontentProperty(),doubleConverter);
        Bindings.bindBidirectional(this.minBaitCountTextField.textProperty(),minimumBaitCountProperty(),integerConverter);
        Bindings.bindBidirectional(this.baitLengthTextField.textProperty(),baitLengthProperty(),integerConverter);
        Bindings.bindBidirectional(this.marginSizeTextField.textProperty(),marginLengthProperty(),integerConverter);
        sizeDownTextField.clear();
        sizeUpTextField.clear();
        minFragSizeTextField.clear();
        maxKmerAlignabilityTextField.clear();
        minGCContentTextField.clear();
        maxGCContentTextField.clear();
        minBaitCountTextField.clear();
        baitLengthTextField.clear();
        marginSizeTextField.clear();

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
        double minGCproportion = percentageToProportion(this.minGCContentTextField.getText());
        this.model.setMinGCcontent(minGCproportion>0?minGCproportion:Default.MIN_GC_CONTENT);
        double maxGCproportion = percentageToProportion(this.maxGCContentTextField.getText());
        this.model.setMaxGCcontent(maxGCproportion>0?maxGCproportion:Default.MAX_GC_CONTENT);
        int kmerAlign = getMaxMeanKmerAlignability()>0?getMaxMeanKmerAlignability() : Default.MAXIMUM_KMER_ALIGNABILITY;
        this.model.setMaxMeanKmerAlignability(kmerAlign);
        int minbait = getMinimumBaitCount()>0 ? getMinimumBaitCount() : Default.MIN_BAIT_NUMBER;
        this.model.setMinBaitCount(minbait);
        int baitlen = getBaitLength()>0?getBaitLength() : Default.PROBE_LENGTH;
        this.model.setProbeLength(baitlen);
        int marginsize = getMarginLength()>0 ? getMarginLength() : Default.MARGIN_SIZE;
        this.model.setMarginSize(marginsize);
    }

    /**
     *
     * @param perc a string such as 35%
     * @return The corresponding proportion (e.g., 0.35)
     */
    private double percentageToProportion(String perc) {
        if (perc==null) return 0.0;
        String s = perc.replaceAll("%","");
        try {
            return Double.parseDouble(s)/100.0;
        } catch (NumberFormatException e){
            // do nothing
        }
        return 0.0;
    }


    /** This gets called when the user chooses a new genome build. They need to do download, uncompression, indexing and
     * also get the corresponding transcript file.
     * @param build Name of genome build.
     */
    private void setGenomeBuild(String build) {
        logger.info("Setting genome build to "+build);
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
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        dirChooser.setTitle("Choose directory for genome build " + build + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().equals("")) {
            logger.error("Could not set genome download path from Directory Chooser");
            PopupFactory.displayError("Error","Could not get path to download genome.");
            return;
        }
        logger.info("downloadGenome to directory  "+ file.getAbsolutePath());
        if (this.model.checkDownloadComplete(file.getAbsolutePath())) {
            // we're done!
            //this.downloadedGenomeLabel.setText(String.format("Genome %s was already downloaded",build));
            this.genomeDownloadPI.setProgress(1.0);
        } else {
            gdownloader.downloadGenome(file.getAbsolutePath(), model.getGenomeBasename(), genomeDownloadPI);
            model.setGenomeDirectoryPath(file.getAbsolutePath());
            //this.downloadedGenomeLabel.setText(file.getAbsolutePath());
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
            model.setTranscriptsBasename(transcriptName);
        } catch (DownloadFileNotFoundException dfne) {
            PopupFactory.displayError("Could not identify RefGene file for genome",dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory where RefGene transcripts file for " + genomeBuild + " is located (will" +
                " be downloaded if not found).");
       dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error","Could not get path to download transcript file.");
            return;
        }
        if (! rgd.needToDownload(file.getAbsolutePath())) {
            logger.trace(String.format("Found refGene.txt.gz file at %s. No need to download",file.getAbsolutePath()));
            this.transcriptDownloadPI.setProgress(1.0);
           // this.downloadedTranscriptsLabel.setText(transcriptName);
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.model.setRefGenePath(abspath);
            return;
        }

        Downloader downloadTask = new Downloader(file, url, basename, transcriptDownloadPI);
        downloadTask.setOnSucceeded( event -> {
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.model.setRefGenePath(abspath);
            //this.downloadedTranscriptsLabel.setText(transcriptName);
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
           // decompressGenomeLabel.setText("Chromosome files extracted");
            genomeDecompressPI.setProgress(1.00);
            model.setGenomeUnpacked();
            return;
        }
        GenomeGunZipper genomeGunZipper = new GenomeGunZipper(this.model.getGenome(),
                this.genomeDecompressPI);
        if (! genomeGunZipper.gZippedFileExists()) {
            PopupFactory.displayError("Could not find genome file",
                    "Download genome file before extraction step!");
            return;
        }
        genomeGunZipper.setOnSucceeded( event -> {
           // decompressGenomeLabel.setText(genomeGunZipper.getStatus());
            if (genomeGunZipper.OK()) {
                model.setGenomeUnpacked();
            } else {
                PopupFactory.displayError("Error","Error from Genome g-unzipper");
            }
        });
        genomeGunZipper.setOnFailed(eventh -> {
            //decompressGenomeLabel.setText("Decompression failed");
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
        if (! manager.genomeFileExists()) {
            PopupFactory.displayError("Could not find genome file",
                    "Download and extract genome file before indexing step!");
            return;
        }

        manager.setOnSucceeded(event ->{
            int n_chroms = manager.getContigLengths().size();
            String message = String.format("%d chromosomes in %s successfully indexed.",
                    n_chroms,
                    model.getGenome().getGenomeFastaName());
           // indexGenomeLabel.setText(message);
            logger.debug(message);
            model.setIndexedGenomeFastaIndexFile(manager.getGenomeFastaIndexPath());
           model.setGenomeIndexed();
        } );
        manager.setOnFailed(event-> {
          //  indexGenomeLabel.setText("FASTA indexing failed");
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
        dirChooser.setTitle("Choose directory where the alignability map for " + genomeBuild + " is located (will be" +
                " downloaded if not found).");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
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
            logger.trace(String.format("Found %s. No need to download",alignabilityMapPathIncludingFileNameGz));
            this.alignabilityDownloadPI.setProgress(1.0);
           // this.downloadAlignabilityLabel.setText("Download complete");
            return;
        }

        // prepare download
        String basenameGz = genomeBuild.concat(".50mer.alignabilityMap.bedgraph.gz");
        String url;
        String url2;
        switch (genomeBuild) {
            case "hg19":
                url = "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/wgEncodeCrgMapabilityAlign50mer.hg19.bedGraph.gz";
                //url = "https://www.dropbox.com/s/lxrkpjfwy6xenq5/wgEncodeCrgMapabilityAlign50mer.bedpraph.gz?dl=1";
                url2 = "http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/chromInfo.txt.gz";
                break;
            case "mm9":
                url = "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/wgEncodeCrgMapabilityAlign50mer.mm9.bedGraph.gz";
                url2 = "http://hgdownload.cse.ucsc.edu/goldenPath/mm9/database/chromInfo.txt.gz";
                break;
            case "hg38":
                url = "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/hg38_50.m2.bedGraph.gz";
                url2 = "http://hgdownload.cse.ucsc.edu/goldenPath/hg38/database/chromInfo.txt.gz";
                break;
            case "mm10":
                url = "ftp://ftp.jax.org/robinp/GOPHER/alignability_maps/mm10_50.m2.bedGraph.gz";
                url2 = "http://hgdownload.cse.ucsc.edu/goldenPath/mm10/database/chromInfo.txt.gz";
                break;
            default:
                //this.downloadAlignabilityLabel.setText(("No map available for " + genomeBuild));
                return;
        }
        // also download chromosme file
        Downloader downloadTask0 = new Downloader(file, url2, "chromInfo.txt.gz", alignabilityDownloadPI);
        Thread th = new Thread(downloadTask0);
        th.start();

        Downloader downloadTask = new Downloader(file, url, basenameGz, alignabilityDownloadPI);
        //downloadTask.setOnSucceeded( event -> this.downloadAlignabilityLabel.setText("Download complete")  );
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
    @FXML private void enterGeneList(ActionEvent e) {
        EntrezGeneViewFactory.display(this.model);
        model.setTargetType(Model.TargetType.TARGET_GENES);
        setTargetFeedback(Model.TargetType.TARGET_GENES,model.getN_validGeneSymbols());
        e.consume();
    }

    @FXML private void enterBedFile(ActionEvent e) {
        e.consume();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file == null) {
            logger.error("Could not get name of BED file");
            return;
        } else {
            logger.info("Uploading targets from "+file.getAbsolutePath());
            this.model.setTargetGenesPath(file.getAbsolutePath());
        }
        logger.trace("Entering bed file");
        try {
            BedFileParser parser = new BedFileParser(file.getAbsolutePath());
            List<GopherGene> genelist = parser.getGopherGeneList();
            List<String>  validGeneSymbols = genelist.stream().map(GopherGene::getGeneSymbol).collect(Collectors.toList());
            List<String> invalidGeneSymbols= ImmutableList.of();
            int uniqueTSSpositions = genelist.size();
            int n_genes=genelist.size();
            int chosenGeneCount=genelist.size();
            int uniqueChosenTSS=genelist.size();
            // String html = getValidatedGeneListHTML(validGeneSymbols, invalidGeneSymbols,n_genes, uniqueTSSpositions);
            this.model.setN_validGeneSymbols(validGeneSymbols.size());
            this.model.setUniqueTSScount(uniqueTSSpositions);
            this.model.setUniqueChosenTSScount(uniqueChosenTSS);
            this.model.setChosenGeneCount(chosenGeneCount);
            model.setTotalRefGeneCount(n_genes);
            this.model.setGopherGenes(parser.getGopherGeneList());
            this.model.setUniqueChosenTSScount(genelist.size());
            this.model.setTargetType(Model.TargetType.BED_TARGETS);
            setTargetFeedback(Model.TargetType.BED_TARGETS,validGeneSymbols.size());
        } catch (GopherException ge) {
            PopupFactory.displayException("Error","Could not input BED file",ge);
        }
    }

    @FXML private void allProteinCodingGenes(ActionEvent e) {
        e.consume();
        String path = model.getRefGenePath();

        logger.trace("Getting all protein coding genes");

        if (path==null) {
            logger.error("Attempt to validate gene symbols before refGene.txt.gz file was downloaded");
            PopupFactory.displayError("Error retrieving refGene data","Download refGene.txt.gz file before proceeding.");
            return;
        }
        logger.info("About to parse refGene.txt.gz file to validate uploaded gene symbols. Path at "+ path);
        RefGeneParser parser;
        try {
            parser = new RefGeneParser(path);
            //parser.checkGenes(this.symbols);
        } catch (Exception exc) {
            PopupFactory.displayException("Error while attempting to validate Gene symbols","Could not validate gene symbols",exc);
            return;
        }
        List<String>  validGeneSymbols = parser.getAllProteinCodingGeneSymbols();
        List<String> invalidGeneSymbols= ImmutableList.of();
        int uniqueTSSpositions = parser.getTotalTSScount();
        int n_genes=parser.getTotalNumberOfRefGenes();
        int chosenGeneCount=parser.getNumberOfRefGenesChosenByUser();
        int uniqueChosenTSS=parser.getCountOfChosenTSS();
        // String html = getValidatedGeneListHTML(validGeneSymbols, invalidGeneSymbols,n_genes, uniqueTSSpositions);
        this.model.setN_validGeneSymbols(validGeneSymbols.size());
        this.model.setUniqueTSScount(uniqueTSSpositions);
        this.model.setUniqueChosenTSScount(uniqueChosenTSS);
        this.model.setChosenGeneCount(chosenGeneCount);
        model.setTotalRefGeneCount(n_genes);
        this.model.setGopherGenes(parser.getGopherGeneList());
        this.model.setUniqueChosenTSScount(parser.getCountOfChosenTSS());
        model.setTargetType(Model.TargetType.ALL_GENES);
        setTargetFeedback(Model.TargetType.ALL_GENES,validGeneSymbols.size());
    }


    private void setTargetFeedback(Model.TargetType ttype, int count) {
        this.targetGeneLabel.setText("");
        this.bedTargetsLabel.setText("");
        this.allGenesLabel.setText("");
        switch (ttype) {
            case NONE:
                return;
            case TARGET_GENES:
                this.targetGeneLabel.setText(String.format("%d genes",count));
                return;
            case ALL_GENES:
                this.allGenesLabel.setText(String.format("%d genes",count));
                return;
            case BED_TARGETS:
                this.bedTargetsLabel.setText(String.format("%d targets",count));
        }
    }


    @FXML private void saveDigestFileAs(ActionEvent e) {
        logger.trace("Saving the digest file");
        // get path from chooser
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting digest file.");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (dir==null || dir.getAbsolutePath().equals("")) {
            PopupFactory.displayError("Error","Could not get path to export digest file.");
            return;
        }

        String path = dir.getAbsolutePath();
        path += File.separator;
        DigestCreationTask task = new DigestCreationTask(path,model);

        TaskProgressBarView pbview = new TaskProgressBarView();
        TaskProgressBarPresenter pbpresent = (TaskProgressBarPresenter)pbview.getPresenter();

        pbpresent.titleProperty().bind(task.titleProperty());
        pbpresent.messageProperty().bind(task.messageProperty());
        pbpresent.progressProperty().bind(task.progressProperty());

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
                    task.cancel();
                    window.close();
                    break;
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });

        task.setOnSucceeded(event -> {
            logger.trace("Finished creating digest file");
            window.close();
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

    @FXML public void saveProbeFileAs(ActionEvent e) {
        List<ViewPoint> vplist=this.model.getViewPointList();
        if (vplist==null || vplist.isEmpty()) {
            PopupFactory.displayError("Error","Attempt to save probe file failed. Complete generation and analysis of ViewPoints before saving probes!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for saving probe file");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().equals("")) {
            PopupFactory.displayError("Error","Could not get path to save file.");
            return;
        }
        String prefix=model.getProjectName();
        ProbeFileExporter exporter = new ProbeFileExporter(file.getAbsolutePath(),prefix);
        try {
            logger.trace(model.getGenomeFastaFile());
            logger.trace(model.getIndexedGenomeFastaIndexFile());
            exporter.printProbeFileInAgilentFormat(this.model.getViewPointList(),this.model.getGenomeBuild(), model.getGenomeFastaFile());
        } catch (Exception exc) {
            PopupFactory.displayException("Could not save probes.", exc.getMessage(),exc);
        }
        e.consume();
    }


    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link GopherGene} objects into the {@link Model}
     * object. This function will use the {@link GopherGene} obejcts and other information
     * to create {@link gopher.model.viewpoint.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisPresenter} Tab.
     */
    public void createViewPoints()  {
        String approach = this.approachChoiceBox.getValue();
        this.model.setApproach(approach);
        updateModel();
        if (model.getChosenEnzymelist()==null || model.getChosenEnzymelist().isEmpty() ) {
            PopupFactory.displayError("Data incomplete", "Choose a restriction enzyme before proceding");
            return;
        }
        if (model.getGopherGeneList()==null|| model.getGopherGeneList().size()==0) {
            PopupFactory.displayError("Data incomplete", "Choose target genes/regions before proceding");
            return;
        }
        boolean OK=QCCheckFactory.showQCCheck(model);
        if (! OK ) {
            return;
        }

        ViewPointCreationTask task;

        if (model.useSimpleApproach()) {
            task = new SimpleViewPointCreationTask(model);
        } else {
            task = new ExtendedViewPointCreationTask(model);
        }

        TaskProgressBarView pbview = new TaskProgressBarView();
        TaskProgressBarPresenter pbpresent = (TaskProgressBarPresenter)pbview.getPresenter();
        pbpresent.titleProperty().bind(task.titleProperty());
        pbpresent.messageProperty().bind(task.messageProperty());
        pbpresent.progressProperty().bind(task.progressProperty());


        Stage window = new Stage();
        window.setTitle("Viewpoint creation");
        window.setAlwaysOnTop(true);
        window.setOnCloseRequest( event -> window.close() );
        pbpresent.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                    task.cancel();
                    break;
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
            window.close();
        });
        task.setOnFailed(eh -> {
            if (eh.getSource().getException() instanceof OutOfMemoryError) {
                window.close();
                JOptionPane.showMessageDialog(null,
                        "Out of memory error--see online documentation for how to increase memory",
                        "Out of memory error", JOptionPane.ERROR_MESSAGE);
            } else {
                Exception exc = (Exception) eh.getSource().getException();
                PopupFactory.displayException("Error",
                        "Exception encountered while attempting to create viewpoints",
                        exc);
            }
        });
        task.setOnCancelled( e -> window.close() );
        new Thread(task).start();
        window.setScene(new Scene(pbview.getView()));
        window.showAndWait();
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
        SettingsViewFactory.showSettings(model);
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

    /**
     * This function is called if the user choose the get human example genes from the Help menu, which is intended
     * to give new users an easy way to get a list of genes to try out the software.
     */
    @FXML private void openGeneWindowWithExampleHumanGenes() {
        InputStream is = GopherMainPresenter.class.getResourceAsStream("/data/humangenesymbols.txt");

        if (is == null) {
            logger.warn("Could not open bundled example human gene list at path '/humangenesymbols.txt'");
            PopupFactory.displayError("Could not open example human gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,new InputStreamReader(is));
        model.setTargetType(Model.TargetType.TARGET_GENES);
        setTargetFeedback(Model.TargetType.TARGET_GENES,model.getN_validGeneSymbols());
    }


    @FXML private void openGeneWindowWithExampleMouseGenes() {
        InputStream is = GopherMainPresenter.class.getResourceAsStream("/data/mousegenesymbols.txt");

        if (is == null) {
            logger.warn("Could not open bundled example fly gene list at path '/mousegenesymbols.txt'");
            PopupFactory.displayError("Could not open example mouse gene list","Please report to developers");
            return;
        }
        EntrezGeneViewFactory.displayFromFile(this.model,new InputStreamReader(is));
        model.setTargetType(Model.TargetType.TARGET_GENES);
        setTargetFeedback(Model.TargetType.TARGET_GENES,model.getN_validGeneSymbols());
    }


    @FXML public void exportBEDFiles(ActionEvent e) {
        List<ViewPoint> vplist=this.model.getViewPointList();
        if (vplist==null || vplist.isEmpty()) {
            PopupFactory.displayError("Attempt to export empty BED files","Complete generation and analysis of ViewPoints before exporting to BED!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting BED files.");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
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
    public void showLog(ActionEvent e) {
        LogViewerFactory factory = new LogViewerFactory();
        factory.display();
        e.consume();
    }

    @FXML
    public void about(ActionEvent e) {
        PopupFactory.showAbout(Model.getVersion(), model.getLastChangeDate());
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
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = chooser.showSaveDialog(null);
        if (file==null) return;
        String path = file.getAbsolutePath();
        serializeToLocation(path);
        logger.trace(String.format("Serialized file to %s",path));
        e.consume();
    }

    /** Open a project from a file specified by the user. */
    @FXML
    public void importProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Gopher project file");
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
        vpanalysispresenter.refreshVPTable();
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
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
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

    /**
     * This method is run after user clicks on 'Close' item of File|Menu. User is prompted to confirm the closing and
     * window is closed if 'yes' is selected.
     * @param e Event triggered by close command.
     */
    public void closeWindow(ActionEvent e) {
        if (model.isClean()) {
            boolean answer = PopupFactory.confirmDialog("Alert", "Are you sure you want to quit?");
            if (answer) {
                logger.info("Closing Gopher Gui");
                javafx.application.Platform.exit();
                System.exit(0);
            }
        } else {
//            boolean answer = PopupFactory.confirmDialog("Unsaved work", "Unsaved work. Are you sure you want to quit?");
            WindowCloser closer = new WindowCloser();
            closer.display();
            if (closer.save()) {
                serialize();
            }
            if (closer.quit()) {
                logger.info("Closing Gopher Gui");
                javafx.application.Platform.exit();
                System.exit(0);
            }
        }
    }

    /** A Handler for the event that the user clicks the close box at the left upper corner of the app.
     * We just call {@link #closeWindow(ActionEvent)}, which is the function that is called
     * if the user chooses Quit from the file menu or uses the corresponding keyboard shortcut.
     * Note that we need to transform a WindowEvent to an ActionEvent.
     */
    private final EventHandler<WindowEvent> confirmCloseEventHandler = event -> {
        event.consume();
        closeWindow(new ActionEvent(event.getSource(),event.getTarget()));
    };

    public void setPrimaryStageReference(Stage stage) {

        this.primaryStage=stage;
        this.primaryStage.setOnCloseRequest(confirmCloseEventHandler);
    }

    @FXML public void displayReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.model);
        PopupFactory.showReportListDialog(report.getReportList());
        e.consume();
    }

    @FXML public void exportReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.model);
        String filename =String.format("%s-report.txt",model.getProjectName());
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setInitialFileName(filename);
        File file=chooser.showSaveDialog(this.primaryStage);
        if (file==null) {
            PopupFactory.displayError("Error","Could not get filename for saving report");
            return;
        }
        report.outputRegulatoryReport(file.getAbsolutePath());
        e.consume();
    }


    @FXML private void setUnbalancedMargin(ActionEvent e) {
        if (unbalancedMarginCheckbox.isSelected()) {
            this.model.setAllowUnbalancedMargins(true);
        } else {
            this.model.setAllowUnbalancedMargins(false);
        }
        e.consume();
    }

    @FXML private void setAllowPatching(ActionEvent e) {
        if (patchedViewpointCheckbox.isSelected()) {
            this.model.setAllowPatching(true);
        } else {
            this.model.setAllowPatching(false);
        }
        e.consume();
    }


}




