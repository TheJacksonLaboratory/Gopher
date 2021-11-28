package gopher.controllers;

import com.google.common.collect.ImmutableList;
import gopher.exception.DownloadFileNotFoundException;
import gopher.exception.GopherException;
import gopher.gui.factories.DeleteFactory;
import gopher.gui.entrezgenetable.EntrezGeneViewFactory;
import gopher.gui.factories.EnzymeViewFactory;
import gopher.gui.help.HelpViewFactory;
import gopher.gui.logviewer.LogViewerFactory;
import gopher.gui.popupdialog.PopupFactory;
import gopher.gui.progresspopup.ProgressPopup;
import gopher.gui.regulatoryexomebox.RegulatoryExomeBoxFactory;
import gopher.gui.webpopup.SettingsViewFactory;
import gopher.gui.taskprogressbar.TaskProgressBarPresenter;
import gopher.gui.taskprogressbar.TaskProgressBarView;
import gopher.gui.util.WindowCloser;
import gopher.io.*;
import gopher.service.model.*;
import gopher.service.model.dialog.ProxyResults;
import gopher.service.model.digest.DigestCreationTask;
import gopher.service.model.viewpoint.ExtendedViewPointCreationTask;
import gopher.service.model.viewpoint.SimpleViewPointCreationTask;
import gopher.service.model.viewpoint.ViewPoint;
import gopher.service.model.viewpoint.ViewPointCreationTask;
import gopher.service.GopherService;
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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;


import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

/**
 * A Java app to help design probes for Capture Hi-C
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.5.7 (2018-09-10)
 */
@Component
public class GopherMainController implements Initializable {
    private final static Logger logger = LoggerFactory.getLogger(GopherMainController.class.getName());

    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc. It is set in the FXML
     * document to refer to the Anchor pane that is the root node of the GUI.
     */
    @FXML
    private Node rootNode;
    /** List of genome builds. Used by genomeChoiceBox */
    @FXML
    private final ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9", "mm10", "xenTro9", "danRer10");
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
//    private VPAnalysisView vpanalysisview;
    /**
     * Reference to the primary stage. We use this to set the title when we switch models (new from File menu).
     */
    private Stage primaryStage = null;

    @Autowired
    GopherService gopherService;


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
//        loggingLevelOFF.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.OFF));
//        loggingLevelTrace.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.TRACE));
//        loggingLevelInfo.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.INFO));
//        loggingLevelDebug.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.DEBUG));
//        loggingLevelWarn.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.WARN));
//        loggingLevelError.setOnAction(e-> setLoggingLevel(org.apache.log4j.Level.ERROR));
        loggingLevelError.setSelected(true);

        setGUItoSimple();
        initializePromptTexts();

//        this.vpanalysisview = new VPAnalysisView();
//        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
//        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
//        this.analysisPane.getChildren().add(vpanalysisview.getView());

        this.approachChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, number2) -> {
            String selectedItem = approachChoiceBox.getItems().get((Integer) number2);
            switch (selectedItem) {
                case "Simple" -> setGUItoSimple();
                case "Extended" -> setGUItoExtended();
                default -> logger.error(String.format("Did not recognize approach in menu %s", selectedItem));
            }
        });
        File userDir = Platform.getGopherDir();
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

    /**
     * This method gets called by SwitchScreens when the user wants to start a new project.
     */
    public void initializeNewModelInGui() {
        setInitializedValuesInGUI();
        removePreviousValuesFromTextFields();
    }

    private void setInitializedValuesInGUI() {
        String path_to_downloaded_genome_directory = gopherService.getGenomeDirectoryPath();
        if (path_to_downloaded_genome_directory!= null) {
           // this.downloadedGenomeLabel.setText(path_to_downloaded_genome_directory);
            this.genomeDownloadPI.setProgress(1.00);
        } else {
           // this.downloadedGenomeLabel.setText("...");
            this.genomeDownloadPI.setProgress(0);
        }
        if (gopherService.isGenomeUnpacked()) {
           // this.decompressGenomeLabel.setText("Extraction previously completed");
            this.genomeDecompressPI.setProgress(1.00);
        } else {
           // this.decompressGenomeLabel.setText("...");
            this.genomeDecompressPI.setProgress(0.0);
        }
        String refGenePath = gopherService.getRefGenePath();
        if (refGenePath!=null) {
            this.transcriptDownloadPI.setProgress(1.0);
        } else {
            this.transcriptDownloadPI.setProgress(0.0);
        }

        if (gopherService.alignabilityMapPathIncludingFileNameGzExists()) {
            this.alignabilityDownloadPI.setProgress(1.0);
        } else {
            this.alignabilityDownloadPI.setProgress(0.0);
        }

        if (gopherService.isGenomeIndexed()) {
            this.genomeIndexPI.setProgress(1.00);
        } else {
            this.genomeIndexPI.setProgress(0.00);
        }
        if (!gopherService.getChosenEnzymelist().isEmpty()) {
            this.restrictionEnzymeLabel.setText(gopherService.getAllSelectedEnzymeString());
        } else {
            this.restrictionEnzymeLabel.setText(null);
        }
        this.unbalancedMarginCheckbox.setSelected(gopherService.getAllowUnbalancedMargins());
        this.patchedViewpointCheckbox.setSelected(gopherService.getAllowPatching());

        this.targetGeneLabel.setText("");
        this.allGenesLabel.setText("");
        this.bedTargetsLabel.setText("");
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
        gopherService.setProjectName(projectname);
        if (this.primaryStage!=null)
            this.primaryStage.setTitle(String.format("GOPHER: %s",projectname));
//        this.vpanalysisview = new VPAnalysisView();
//        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
//        this.vpanalysispresenter.setModel(this.model);
//        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
//        this.analysisPane.getChildren().add(vpanalysisview.getView());
        initializeNewModelInGui();
        e.consume();
    }

    /**
     * This allows a caller to set the {@link GopherModel} object for this presenter (for instance, a default
     * {@link GopherModel} object is set if the user chooses a new viewpoint. If the user chooses to open a previous
     * viewpoint from a serialized file, then a  {@link GopherModel} object is initialized from the file and set here.
     * This method calls {@link #setInitializedValuesInGUI()} in order to show relevant data in the GUI.
     * @param mod A {@link GopherModel} object.
     */
    private void setModel(GopherModel mod) {
        this.gopherService.setModel(mod);
        setInitializedValuesInGUI();
        setBindings();
    }


    public void setModelInMainAndInAnalysisPresenter(GopherModel mod) {
        setModel(mod);
        this.vpanalysispresenter.setModel(mod);
        logger.trace(String.format("setModelInMainAndInAnalysisPresenter for genome build %s and basename %s",
                mod.getGenome().getGenomeBuild(),
                gopherService.getGenome().getGenomeBasename()));
        if (gopherService.getMaxGCcontent()>0){
            this.maxGCContentTextField.setText(String.format("%.1f%%",gopherService.getMaxGCContentPercent()));
        } else {
            this.maxGCContentTextField.setPromptText(String.format("%.1f%%",100*Default.MAX_GC_CONTENT));
        }
        if (gopherService.getMinGCcontent()>0) {
            this.minGCContentTextField.setText(String.format("%.1f%%",gopherService.getMinGCContentPercent()));
        } else {
            this.minGCContentTextField.setPromptText(String.format("%.1f%%",100*Default.MIN_GC_CONTENT));
        }
        if (gopherService.getMinBaitCount()>0) {
            this.minBaitCountTextField.setText(String.valueOf(gopherService.getMinBaitCount()));
        } else {
            this.minBaitCountTextField.setText(String.valueOf(Default.MIN_BAIT_NUMBER));
        }
        if (gopherService.getMinFragSize()>0) {
            this.minFragSizeTextField.setText(String.format("%d",gopherService.getMinFragSize()));
        } else {
            this.minFragSizeTextField.setPromptText(String.format("%d",Default.MINIMUM_FRAGMENT_SIZE));
        }
        if (gopherService.getMaxMeanKmerAlignability()>0) {
            this.maxKmerAlignabilityTextField.setText(String.format("%d",gopherService.getMaxMeanKmerAlignability()));
        } else {
            this.maxKmerAlignabilityTextField.setPromptText(String.format("%d",Default.MAXIMUM_KMER_ALIGNABILITY));
        }
        if (gopherService.getSizeUp()>0) {
            this.sizeUpTextField.setText(String.format("%d",gopherService.getSizeUp()));
        } else {
            this.sizeUpTextField.setPromptText(String.format("%d",Default.SIZE_UPSTREAM));
        }
        if (gopherService.getSizeDown()>0) {
            this.sizeDownTextField.setText(String.format("%d",gopherService.getSizeDown()));
        } else {
            this.sizeDownTextField.setPromptText(String.format("%d",Default.SIZE_DOWNSTREAM));
        }
        if (! gopherService.getChosenEnzymelist().isEmpty()) {
            this.restrictionEnzymeLabel.setText(gopherService.getAllSelectedEnzymeString());
        } else {
            this.restrictionEnzymeLabel.setText("not initialized");
        }
        if (gopherService.getProbeLength()==0) {
            this.baitLengthTextField.setText(String.valueOf(Default.PROBE_LENGTH));
        } else {
            this.baitLengthTextField.setText(String.valueOf(gopherService.getProbeLength()));
        }
        if (gopherService.getMarginSize()==0) {
            this.marginSizeTextField.setText(String.valueOf(Default.MARGIN_SIZE));
        } else {
            this.marginSizeTextField.setText(String.valueOf(gopherService.getMarginSize()));
        }
        if (! gopherService.getTargetType().equals(GopherModel.TargetType.NONE))  {
            int n_targets = gopherService.getN_validGeneSymbols();
            setTargetFeedback(gopherService.getTargetType(),n_targets);
        }

        if (gopherService.useSimpleApproach()) {
            this.approachChoiceBox.setValue("Simple");
        } else if (gopherService.useExtendedApproach()){
            this.approachChoiceBox.setValue("Extended");
        }

        GopherModel.TargetType ttype = gopherService.getTargetType();
        switch (ttype) {
            case TARGET_GENES -> {
                int count = gopherService.getN_validGeneSymbols();
                this.targetGeneLabel.setText(String.format("%d genes", count));
            }
            case ALL_GENES -> {
                int allgenes = gopherService.getN_validGeneSymbols();
                this.allGenesLabel.setText(String.format("%d genes", allgenes));
            }
            case BED_TARGETS -> {
                int n_bedtargets = gopherService.getN_validGeneSymbols();
                this.bedTargetsLabel.setText(String.format("%d targets", n_bedtargets));
            }
        }

        this.genomeChoiceBox.setValue(gopherService.getGenomeBuild());
        // after we have set up the model the first time, mark it as clean. Any changes after this will lead
        // to a confirmation window being opened if the user has changed anything.
        gopherService.setClean(true);
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
        approachChoiceBox.getSelectionModel().selectFirst();
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
    private final StringConverter<Number> doubleConverter = new StringConverter<>() {
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

    private final StringConverter<Number> integerConverter = new StringConverter<>() {
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
     * {@link GopherModel} but confirmDialog it as a proportion in the GUI. The default values are used for any fields that have not
     * been filled in by the user.
     */
    private void updateModel() {
        this.gopherService.setSizeDown(getSizeDown()>0?getSizeDown():Default.SIZE_DOWNSTREAM);
        this.gopherService.setSizeUp(getSizeUp()>0?getSizeUp():Default.SIZE_UPSTREAM);
        this.gopherService.setMinFragSize(getMinFragSize()>0?getMinFragSize():Default.MINIMUM_FRAGMENT_SIZE);
        double repeatProportion=getMaxRepeatContent()/100;
        this.gopherService.setMaxRepeatContent(repeatProportion>0?repeatProportion:Default.MAXIMUM_KMER_ALIGNABILITY);
        double minGCproportion = percentageToProportion(this.minGCContentTextField.getText());
        this.gopherService.setMinGCcontent(minGCproportion>0?minGCproportion:Default.MIN_GC_CONTENT);
        double maxGCproportion = percentageToProportion(this.maxGCContentTextField.getText());
        this.gopherService.setMaxGCcontent(maxGCproportion>0?maxGCproportion:Default.MAX_GC_CONTENT);
        int kmerAlign = getMaxMeanKmerAlignability()>0?getMaxMeanKmerAlignability() : Default.MAXIMUM_KMER_ALIGNABILITY;
        this.gopherService.setMaxMeanKmerAlignability(kmerAlign);
        int minbait = getMinimumBaitCount()>0 ? getMinimumBaitCount() : Default.MIN_BAIT_NUMBER;
        this.gopherService.setMinBaitCount(minbait);
        int baitlen = getBaitLength()>0?getBaitLength() : Default.PROBE_LENGTH;
        this.gopherService.setProbeLength(baitlen);
        int marginsize = getMarginLength()>0 ? getMarginLength() : Default.MARGIN_SIZE;
        this.gopherService.setMarginSize(marginsize);
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
        logger.info("Setting genome build to "+ build);
        this.gopherService.setGenomeBuild(build);
        this.transcriptDownloadPI.setProgress(0.0);
        this.alignabilityDownloadPI.setProgress(0.0);
        this.genomeDownloadPI.setProgress(0.0);
        this.genomeIndexPI.setProgress(0.0);
        this.genomeDecompressPI.setProgress(0.0);
    }

    /** This downloads the tar-gzip genome file as chosen by the user from the UCSC download site.
     * It places the compressed file into the directory chosen by the user. The path to the directory
     * is stored in the {@link GopherModel} object using the {@link GopherModel#setGenomeDirectoryPath} function.
     * Following this the user needs to uncompress and index the genome files using the function
     * {@link #decompressGenome(ActionEvent)} which is called after the corresponding button
     * is clicked in the GUI.
     */
    @FXML public void downloadGenome(ActionEvent e) {
        e.consume();
        String build = this.gopherService.getGenomeBuild();
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
        if (this.gopherService.checkDownloadComplete(file.getAbsolutePath())) {
            // we're done!
            //this.downloadedGenomeLabel.setText(String.format("Genome %s was already downloaded",build));
            this.genomeDownloadPI.setProgress(1.0);
        } else {
            gdownloader.downloadGenome(file.getAbsolutePath(), gopherService.getGenomeBasename(), genomeDownloadPI);
            gopherService.setGenomeDirectoryPath(file.getAbsolutePath());
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
            gopherService.setTranscriptsBasename(transcriptName);
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
            this.gopherService.setRefGenePath(abspath);
            return;
        }

        Downloader downloadTask = new Downloader(file, url, basename, transcriptDownloadPI);
        downloadTask.setOnSucceeded( event -> {
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.gopherService.setRefGenePath(abspath);
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
        if (this.gopherService.getGenome().isIndexingComplete()) {
           // decompressGenomeLabel.setText("Chromosome files extracted");
            genomeDecompressPI.setProgress(1.00);
            gopherService.setGenomeUnpacked();
            return;
        }
        GenomeGunZipper genomeGunZipper = new GenomeGunZipper(this.gopherService.getGenome(),
                this.genomeDecompressPI);
        if (! genomeGunZipper.gZippedFileExists()) {
            PopupFactory.displayError("Could not find genome file",
                    "Download genome file before extraction step!");
            return;
        }
        genomeGunZipper.setOnSucceeded( event -> {
           // decompressGenomeLabel.setText(genomeGunZipper.getStatus());
            if (genomeGunZipper.OK()) {
                gopherService.setGenomeUnpacked();
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
       gopherService.indexGenome(this.genomeIndexPI);
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
        gopherService.setAlignabilityMapPathIncludingFileNameGz(alignabilityMapPathIncludingFileNameGz);

        String chromInfoPathIncludingFileNameGz = file.getAbsolutePath();
        chromInfoPathIncludingFileNameGz += File.separator;
        chromInfoPathIncludingFileNameGz += "chromInfo.txt.gz";
        gopherService.setChromInfoPathIncludingFileNameGz(chromInfoPathIncludingFileNameGz);
        // check if the file that is going to be downloaded already exists
        if (gopherService.alignabilityMapPathIncludingFileNameGzExists()) {
            logger.trace(String.format("Found %s. No need to download",alignabilityMapPathIncludingFileNameGz));
            this.alignabilityDownloadPI.setProgress(1.0);
            return;
        }
        // prepare download
        String basenameGz = genomeBuild.concat(".50mer.alignabilityMap.bedgraph.gz");
        String url = gopherService.getAlignabilityFtp(genomeBuild);
        String url2 = gopherService.getAlignabilityHttp(genomeBuild);
        // also download chromosome file
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
     * objects to the {@link GopherModel}.*/
    @FXML public void chooseEnzymes() {
        List<RestrictionEnzyme> allEnzymes = gopherService.getAllEnyzmes();
        List<RestrictionEnzyme> previouslySelectedEnzymes = gopherService.getChosenEnzymelist();
        EnzymeViewFactory factory = new EnzymeViewFactory();
        List<RestrictionEnzyme> chosenEnzymes = factory.getChosenEnzymes(allEnzymes, previouslySelectedEnzymes);
        if (chosenEnzymes.isEmpty()) {
            PopupFactory.displayError("Warning","Warning -- no restriction enzyme chosen!");
            return;
        } else {
            logger.info("We retrieved {} enzymes", chosenEnzymes.size());
        }
        this.gopherService.setChosenRestrictionEnzymes(chosenEnzymes);
        this.restrictionEnzymeLabel.setText(this.gopherService.getAllSelectedEnzymeString());
    }

    /**
     * Open a new dialog where the user can upload gene symbols or Entrez Gene IDs.
     * The effect of the command <pre>EntrezGeneViewFactory.confirmDialog(this.model);</pre>
     * is to pass a list of {@link GopherGene} objects to the {@link GopherModel}.
     * These objects are used with other information in the Model to create {@link gopher.service.model.viewpoint.ViewPoint}
     * objects when the user clicks on {@code Create ViewPoints}.
     * See {@link EntrezGeneViewFactory} for logic.
     *
     * @param e event triggered by enter gene command.
     */
    @FXML private void enterGeneList(ActionEvent e) {
        //EntrezGeneViewFactory.display(this.model);
        gopherService.setTargetType(GopherModel.TargetType.TARGET_GENES);
        setTargetFeedback(GopherModel.TargetType.TARGET_GENES,gopherService.getN_validGeneSymbols());
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
            this.gopherService.setTargetGenesPath(file.getAbsolutePath());
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
            this.gopherService.setN_validGeneSymbols(validGeneSymbols.size());
            this.gopherService.setUniqueTSScount(uniqueTSSpositions);
            this.gopherService.setUniqueChosenTSScount(uniqueChosenTSS);
            this.gopherService.setChosenGeneCount(chosenGeneCount);
            this.gopherService.setTotalRefGeneCount(n_genes);
            this.gopherService.setGopherGenes(parser.getGopherGeneList());
            this.gopherService.setUniqueChosenTSScount(genelist.size());
            this.gopherService.setTargetType(GopherModel.TargetType.BED_TARGETS);
            setTargetFeedback(GopherModel.TargetType.BED_TARGETS,validGeneSymbols.size());
        } catch (GopherException ge) {
            PopupFactory.displayException("Error","Could not input BED file",ge);
        }
    }

    @FXML private void allProteinCodingGenes(ActionEvent e) {
        e.consume();
        String path = gopherService.getRefGenePath();

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
        this.gopherService.setN_validGeneSymbols(validGeneSymbols.size());
        this.gopherService.setUniqueTSScount(uniqueTSSpositions);
        this.gopherService.setUniqueChosenTSScount(uniqueChosenTSS);
        this.gopherService.setChosenGeneCount(chosenGeneCount);
        this.gopherService.setTotalRefGeneCount(n_genes);
        this.gopherService.setGopherGenes(parser.getGopherGeneList());
        this.gopherService.setUniqueChosenTSScount(parser.getCountOfChosenTSS());
        this.gopherService.setTargetType(GopherModel.TargetType.ALL_GENES);
        setTargetFeedback(GopherModel.TargetType.ALL_GENES,validGeneSymbols.size());
    }


    private void setTargetFeedback(GopherModel.TargetType ttype, int count) {
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
        DigestCreationTask task = new DigestCreationTask(path,gopherService);

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
                case DONE -> window.close();
                case CANCEL -> {
                    task.cancel();
                    window.close();
                }
                case FAILED -> throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
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
        List<ViewPoint> vplist=this.gopherService.getViewPointList();
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
        String prefix=gopherService.getProjectName();
        ProbeFileExporter exporter = new ProbeFileExporter(file.getAbsolutePath(),prefix);
        try {
            logger.trace(gopherService.getGenomeFastaFile());
            logger.trace(gopherService.getIndexedGenomeFastaIndexFile());
            exporter.printProbeFileInAgilentFormat(this.gopherService.getViewPointList(),
                    this.gopherService.getGenomeBuild(), gopherService.getGenomeFastaFile());
        } catch (Exception exc) {
            PopupFactory.displayException("Could not save probes.", exc.getMessage(),exc);
        }
        e.consume();
    }


    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link GopherGene} objects into the {@link GopherModel}
     * object. This function will use the {@link GopherGene} obejcts and other information
     * to create {@link gopher.service.model.viewpoint.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisPresenter} Tab.
     */
    public void createViewPoints()  {
        String approach = this.approachChoiceBox.getValue();
        this.gopherService.setApproach(approach);
        updateModel();
        if (gopherService.getChosenEnzymelist().isEmpty()) {
            PopupFactory.displayError("Data incomplete", "Choose a restriction enzyme before proceding");
            return;
        }
        if (gopherService.getGopherGeneList().isEmpty()) {
            PopupFactory.displayError("Data incomplete", "Choose target genes/regions before proceding");
            return;
        }

        try {
            ClassPathResource gopherResource = new ClassPathResource("fxml/qccheck.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(gopherResource.getURL());
            //fxmlLoader.setControllerFactory(applicationContext::getBean);
            Parent parent = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(parent, 1200, 900));
            stage.setResizable(true);
            stage.setTitle("Quality assessment");
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO return boolean value
//        boolean OK=QCCheckFactory.showQCCheck(model);
//        if (! OK ) {
//            return;
//        }

        ViewPointCreationTask task;

        if (gopherService.useSimpleApproach()) {
            task = new SimpleViewPointCreationTask(gopherService);
        } else {
            task = new ExtendedViewPointCreationTask(gopherService);
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
                case DONE -> window.close();
                case CANCEL -> task.cancel();
                case FAILED -> throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });

        task.setOnSucceeded(event -> {
            SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
            //this.vpanalysispresenter.setModel(this.model);
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


    /** Display the settings (parameters) of the current viewpoint. */
    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(gopherService);
    }

    /**
     * Content of {@link GopherModel} is written to platform-dependent default location.
     */
    @FXML private void saveProject(ActionEvent e) {
       // Model.writeSettingsToFile(this.model);
        boolean result = gopherService.serialize();
        if (result) { /* if it didnt work, the serialize method will show an error dialog. */
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(String.format("Successfully saved viewpoint data to %s", Platform.getAbsoluteProjectPath(gopherService.getProjectName())));
            alert.show();
        }
        e.consume();
    }

    /**
     * Save all of the data about the current analysis (project) to a serialized file.
     */
    @FXML private void saveProjectAndClose() {
        gopherService.serialize();
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
        Dialog<ProxyResults> dialog = new Dialog<>();
        dialog.setTitle("Set Proxy");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Label proxyLabel = new Label("Proxy");
        TextField textField = new TextField();
        if (gopherService.getHttpProxy()!=null) {
            textField.setText(gopherService.getHttpProxy());
        }
        textField.setTooltip(new Tooltip("http proxy"));
        Label proxyPortLabel = new Label("Proxy Port");
        TextField portTextField = new TextField();
        portTextField.setTooltip(new Tooltip("http proxy port"));
        if (gopherService.getHttpProxyPort()!=null) {
            logger.trace(String.format("http proxy port: %s",gopherService.getHttpProxyPort()));
            portTextField.setText(gopherService.getHttpProxyPort());
        }
        dialogPane.setContent(new VBox(8, proxyLabel, textField, proxyPortLabel, portTextField));
        runLater(textField::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new ProxyResults(textField.getText(),
                        portTextField.getText());
            }
            return null;
        });
        Optional<ProxyResults> optionalResult = dialog.showAndWait();
        optionalResult.ifPresent((ProxyResults results) -> {
            String port=results.getPort();
            String proxy=results.getProxy();
            if (proxy==null) {
                PopupFactory.displayError("Error obtaining Proxy","Proxy string could not be obtained. Please try again");
                return;
            }
            this.gopherService.setHttpProxy(proxy);
            this.gopherService.setHttpProxyPort(port);
            logger.info(String.format("Set proxy to %s[%s]",proxy,port));
            Utils.setSystemProxyAndPort(proxy,port);
        });
    }

    /**
     * This function is called if the user choose the get human example genes from the Help menu, which is intended
     * to give new users an easy way to get a list of genes to try out the software.
     */
    @FXML private void openGeneWindowWithExampleHumanGenes() {
        URL url = GopherMainController.class.getResource("/data/humangenesymbols.txt");
        if (url == null) {
            PopupFactory.displayError("Could not find human gene list", "error");
            return;
        }
        List<String> humanGeneSymbols = gopherService.initializeEntrezGene(url.getFile());
        openGeneWithExamples(humanGeneSymbols);
    }

    @FXML private void openGeneWindowWithExampleMouseGenes() {
        URL url = GopherMainController.class.getResource("/data/mousegenesymbols.txt");
        if (url == null) {
            PopupFactory.displayError("Could not find human gene list", "error");
            return;
        }
        List<String> mouseGeneSymbols = gopherService.initializeEntrezGene(url.getFile());
        openGeneWithExamples(mouseGeneSymbols);
    }

    private void openGeneWithExamples(List<String> geneSymbols) {
        EntrezGeneViewFactory.displayWithGenes(geneSymbols);
        gopherService.setTargetType(GopherModel.TargetType.TARGET_GENES);
        setTargetFeedback(GopherModel.TargetType.TARGET_GENES,gopherService.getN_validGeneSymbols());
    }





    @FXML public void exportBEDFiles(ActionEvent e) {
        List<ViewPoint> vplist=this.gopherService.getViewPointList();
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
        String prefix = gopherService.getProjectName();
        BEDFileExporter exporter = new BEDFileExporter(file.getAbsolutePath(),prefix);
        try {
            exporter.printRestFragsToBed(this.gopherService.getViewPointList(),this.gopherService.getGenomeBuild());
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
        PopupFactory.showAbout(GopherModel.getVersion(), gopherService.getLastChangeDate());
        e.consume();
    }

    /** Open a window that will allow the user to delete unwanted project files. Do not allow the
     * user to delete the file that is currently opened.
     * @param e action event.*/
    @FXML
    public void deleteProjectFiles(ActionEvent e) {
        DeleteFactory.display(this.gopherService);
        e.consume();
    }

    /** Export the project ser file to a location chosen by the user (instead of the default location,
     * which is the .gopher directory). */
    @FXML
    public void exportProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        String initFileName=String.format("%s.ser",this.gopherService.getProjectName());
        chooser.setInitialFileName(initFileName);
        chooser.setTitle("Choose file path to save project file");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = chooser.showSaveDialog(null);
        if (file==null) return;
        String path = file.getAbsolutePath();
        gopherService.serializeToLocation(path);
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
        removePreviousValuesFromTextFields();
        gopherService.importProtjectFromFile(file);
        if (this.primaryStage!=null)
            this.primaryStage.setTitle(String.format("GOPHER: %s",
                    gopherService.getProjectName()));

//        this.vpanalysisview = new VPAnalysisView();
//        this.vpanalysispresenter = (VPAnalysisPresenter) this.vpanalysisview.getPresenter();
//        this.vpanalysispresenter.setModel(this.model);
//        this.vpanalysispresenter.setTabPaneRef(this.tabpane);
//        this.analysisPane.getChildren().add(vpanalysisview.getView());
        setInitializedValuesInGUI();
        //setModelInMainAndInAnalysisPresenter(this.model);
        vpanalysispresenter.refreshVPTable();
        logger.trace(String.format("Opened model %s from file %s",gopherService.getProjectName(), file.getAbsolutePath()));
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
            gopherService.setRegulatoryBuildPath(abspath);
            return;
        }

        ProgressPopup popup = new ProgressPopup("Downloading...", "Downloading Ensembl regulatory build file");
        ProgressIndicator progressIndicator = popup.getProgressIndicator();

        Downloader downloadTask = new Downloader(file, url, basename, progressIndicator);
        downloadTask.setOnSucceeded( e -> {
            String abspath=(new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            logger.trace("Setting regulatory build path in model to "+abspath);
            gopherService.setRegulatoryBuildPath(abspath);

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
        if (!gopherService.viewpointsInitialized()) {
            PopupFactory.displayError("Viewpoints not initialized",
                    "Please initialize viewpoints before exporting regulatory exome");
            return;
        }
        if (! gopherService.regulatoryBuildPathInitialized()) {
            PopupFactory.displayError("Regulatory build path not initialized",
                    "Please download the regulatory build file before exporting regulatory exome");
            return;
        }
        try {
            final File regulatoryExomeDirectory = RegulatoryExomeBoxFactory.getDirectoryForExport(this.rootNode);
            logger.info("downloadGenome to directory  " + regulatoryExomeDirectory.getAbsolutePath());
            runLater(() ->
                        RegulatoryExomeBoxFactory.exportRegulatoryExome(gopherService, regulatoryExomeDirectory));
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
        if (gopherService.isClean()) {
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
                gopherService.serialize();
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
        GopherReport report = new GopherReport(this.gopherService);
        PopupFactory.showReportListDialog(report.getReportList());
        e.consume();
    }

    @FXML public void exportReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.gopherService);
        String filename =String.format("%s-report.txt",gopherService.getProjectName());
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
        this.gopherService.setAllowUnbalancedMargins(unbalancedMarginCheckbox.isSelected());
        e.consume();
    }

    @FXML private void setAllowPatching(ActionEvent e) {
        this.gopherService.setAllowPatching(patchedViewpointCheckbox.isSelected());
        e.consume();
    }


}




