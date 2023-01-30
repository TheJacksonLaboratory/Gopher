package gopher.controllers;

import com.google.common.collect.ImmutableList;
import gopher.exception.DownloadFileNotFoundException;
import gopher.exception.GopherException;
import gopher.gui.factories.*;
import gopher.gui.factories.PopupFactory;
import gopher.gui.logviewer.LogViewerFactory;
import gopher.gui.progresspopup.ProgressPopup;
import gopher.gui.regulatoryexomebox.RegulatoryExomeBoxFactory;
import gopher.gui.util.MyPreloader;
import gopher.gui.webpopup.ProgressForm;
import gopher.gui.webpopup.SettingsViewFactory;
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
import javafx.beans.property.*;
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
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static gopher.configuration.GopherConfig.GENOME_DOWNLOAD_DIRECTORY;
import static javafx.application.Platform.runLater;

/**
 * A Java app to help design probes for Capture Hi-C
 *
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.7.0 (2021-11-27)
 */
@Component
public class GopherMainController implements Initializable {
    private final static Logger LOGGER = LoggerFactory.getLogger(GopherMainController.class.getName());
    @FXML
    public Label projectNameLabel;
    private final StringProperty projectNameProperty = new SimpleStringProperty();


    /**
     * This is the root node of the GUI and refers to the BorderPane. It can be used to
     * obtain a reference to the primary scene, which is needed by FileChooser, etc. It is set in the FXML
     * document to refer to the Anchor pane that is the root node of the GUI.
     */
    @FXML
    private Node rootNode;
    /**
     * List of genome builds. Used by genomeChoiceBox
     */
    @FXML
    private final ObservableList<String> genomeTranscriptomeList = FXCollections.observableArrayList("hg19", "hg38", "mm9", "mm10", "xenTro9", "danRer10");
    /**
     * List of Design approaches.
     */
    @FXML
    private final ObservableList<String> approachList = FXCollections.observableArrayList("Simple", "Extended");
    @FXML
    private ChoiceBox<String> genomeChoiceBox;
    @FXML
    private ChoiceBox<String> approachChoiceBox;
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

    @FXML
    private Label targetGeneLabel;
    @FXML
    private Label allGenesLabel;
    @FXML
    private Label bedTargetsLabel;

    /**
     * Show which enzymes the user has chosen.
     */
    @FXML
    private Label restrictionEnzymeLabel;

    @FXML
    private TabPane tabpane;
    @FXML
    private ScrollPane analysisPane;
    /**
     * The first table with a summary of parameters for setting up the experiment.
     */
    @FXML
    private Tab setuptab;
    /**
     * The 'second' tab of VPVGui that shows a summary of the analysis and a list of Viewpoints.
     */
    @FXML
    private Tab analysistab;
   /**
     * Presenter for the second tab.
     */
    @Autowired
    private VPAnalysisController vpAnalysisController;
    /**
     * This is a Properties object that corresponds to the .gopher/gopher.properties file and
     * can store information about the download locations.
     */
    @Autowired
    private Properties pgProperties;

    @Autowired
    GopherService gopherService;

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${application.title}")
    private String applicationTitle;

    final transient private IntegerProperty sizeUp = new SimpleIntegerProperty(Default.SIZE_UPSTREAM);

    private IntegerProperty sizeDownProperty() {
        return sizeDown;
    }

    final transient private IntegerProperty sizeDown = new SimpleIntegerProperty(Default.SIZE_DOWNSTREAM);


    private IntegerProperty sizeUpProperty() {
        return sizeUp;
    }

    final transient private IntegerProperty minFragSize = new SimpleIntegerProperty(Default.MINIMUM_FRAGMENT_SIZE);

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

    final transient private IntegerProperty minBaitCount = new SimpleIntegerProperty(Default.MIN_BAIT_NUMBER);

    private IntegerProperty minimumBaitCountProperty() {
        return minBaitCount;
   }

    final transient private IntegerProperty baitLength = new SimpleIntegerProperty(Default.BAIT_LENGTH);

    private IntegerProperty baitLengthProperty() {
        return baitLength;
    }

    final transient private IntegerProperty marginLength = new SimpleIntegerProperty(Default.MARGIN_SIZE);



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
        LOGGER.trace("initialize() called");
        genomeChoiceBox.setItems(genomeTranscriptomeList);
        genomeChoiceBox.getSelectionModel().selectFirst();
        genomeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> setGenomeBuild(newValue));
        // The following will initialize the GUI to the simple approach
        approachChoiceBox.setItems(approachList);
        approachChoiceBox.getSelectionModel().selectFirst();

        ToggleGroup tGroup = new ToggleGroup();
        tGroup.getToggles().addAll(loggingLevelOFF, loggingLevelTrace, loggingLevelInfo, loggingLevelDebug, loggingLevelWarn, loggingLevelError);
        loggingLevelError.setSelected(true);

        setGUItoSimple();
        initializePromptTexts();
        setBindings();
        String cssValue = " -fx-font-size: 18; -fx-text-fill: blue;";
        this.projectNameLabel.setStyle(cssValue);
        this.approachChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observableValue, number, number2) -> {
            String selectedItem = approachChoiceBox.getItems().get((Integer) number2);
            switch (selectedItem) {
                case "Simple" -> setGUItoSimple();
                case "Extended" -> setGUItoExtended();
                default -> LOGGER.error(String.format("Did not recognize approach in menu %s", selectedItem));
            }
        });
        File userDir = Platform.getGopherDir();
        String projectName = MyPreloader.getProjectName();
        if (MyPreloader.isIsNewProject()) {
            startNewProject(projectName);
        } else {
            File f = new File(Objects.requireNonNull(userDir).getAbsolutePath() + File.separator + projectName);
            if (!f.isFile()) {
                PopupFactory.displayError("Could not open project", String.format("Could not open GOPHER project at %s",
                        f.getAbsolutePath()));
                return;
            }
            importProject(f);
        }
        setuptab.setId("setuptab");
        gopherService.setProjectName(projectName);
        LOGGER.trace("Project name " + projectName);
        this.vpAnalysisController.setTabPaneRef(tabpane);
    }


   Stage getPrimaryStageReference() {
        Scene scene = this.genomeChoiceBox.getScene();
        if (scene ==null) {
            LOGGER.error("Could not retrieve scene from genomeChoiceBox");
            return null;
        } else {
            return  (Stage) scene.getWindow();
        }
    }


    /**
     * makes the upstream and downstream size fields invisible because they are irrelevant to the simple approach.
     */
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

    /**
     * makes the upstream and downstream size fields visible because they are needed for the extended approach.
     */
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
        removePreviousValuesFromTextFields();
        setInitializedValuesInGUI();
    }

    private void setInitializedValuesInGUI() {
        LOGGER.trace("setInitializedValuesInGUI (top): genome dir{}", gopherService.getGenome().getPathToGenomeDirectory());
        String path_to_downloaded_genome_directory = gopherService.getGenomeDirectoryPath();
        if (path_to_downloaded_genome_directory != null) {
            LOGGER.trace("Setting GUI display for genome_directory {}", path_to_downloaded_genome_directory);
            this.genomeDownloadPI.setProgress(1.00);
        } else {
            LOGGER.trace("Setting GUI display - genome directory not initialized");
            this.genomeDownloadPI.setProgress(0);
        }
        boolean unpacked = gopherService.isGenomeUnpacked();
        if (gopherService.isGenomeUnpacked()) {
            LOGGER.trace("Setting GUI display - genome is unpacked");
            this.genomeDecompressPI.setProgress(1.00);
        } else {
            LOGGER.trace("Setting GUI display - genome is not unpacked");
            this.genomeDecompressPI.setProgress(0.0);
        }
        String refGenePath = gopherService.getRefGenePath();
        if (refGenePath != null) {
            this.transcriptDownloadPI.setProgress(1.0);
        } else {
            this.transcriptDownloadPI.setProgress(0.0);
        }
        if (gopherService.alignabilityMapPathIncludingFileNameGzExists()) {
            this.alignabilityDownloadPI.setProgress(1.0);
        } else {
            this.alignabilityDownloadPI.setProgress(0.0);
        }
        boolean indexed = gopherService.isGenomeIndexed();
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
        LOGGER.trace("setInitializedValuesInGUI (bottom 2): genome dir{}", gopherService.getGenome().getPathToGenomeDirectory());
        this.unbalancedMarginCheckbox.setSelected(gopherService.getAllowUnbalancedMargins());
        this.patchedViewpointCheckbox.setSelected(gopherService.getAllowPatching());
        String gbuild = gopherService.getGenomeBuild();
        this.genomeChoiceBox.getSelectionModel().select(gbuild);
        this.targetGeneLabel.setText("");
        this.allGenesLabel.setText("");
        this.bedTargetsLabel.setText("");
        LOGGER.trace("setInitializedValuesInGUI (bottom1): genome dir{}", gopherService.getGenome().getPathToGenomeDirectory());
        this.projectNameProperty.set(String.format("GOPHER: %s", gopherService.getProjectName()));
        // Something weird is causing the path to path_to_downloaded_genome_directory to be set to null
        // reset it here -- extensive debugging could not figure out
        if (gopherService.getGenomeDirectoryPath() == null) {
            gopherService.setGenomeDirectoryPath(path_to_downloaded_genome_directory);
        }
        if (unpacked) gopherService.setGenomeUnpacked();
        if (indexed) gopherService.setGenomeIndexed(indexed);
        LOGGER.trace("setInitializedValuesInGUI (bottom): genome dir{}", gopherService.getGenome().getPathToGenomeDirectory());
    }


    /**
     * This is called when the user starts a new viewpoint. It erases everything from
     * the GUI as well
     *
     * @param e Event triggered by new viewpoint command.
     */
    @FXML
    public void startNewProjectFromFileMenu(ActionEvent e) {
        Optional<String> optName = PopupFactory.getProjectName();
        if (optName.isEmpty())
            return; // do nothing, the user cancelled!
        String projectname = optName.get();
        startNewProject(projectname);
        e.consume();
    }

    private void startNewProject(String newProjectName) {
        ObservableList<Tab> panes = this.tabpane.getTabs();
        /* collect tabs first then remove them -- avoids a ConcurrentModificationException */
        List<Tab> tabsToBeRemoved = new ArrayList<>();
        /* close all tabs except setup and analysis. */
        for (Tab tab : panes) {
            String id = tab.getId();
            if (id != null && (id.equals("analysistab") || id.equals("setuptab"))) {
                continue;
            }
            tabsToBeRemoved.add(tab);
        }
        this.tabpane.getTabs().removeAll(tabsToBeRemoved);
        LOGGER.info("Starting new project with name {}", newProjectName);
        gopherService.setProjectName(newProjectName);
        initializeNewModelInGui();
        this.projectNameProperty.set(String.format("GOPHER: %s", newProjectName));
    }


    /**
     * The prompt (gray) values of the text fields in the settings windows get set to their default values here.
     */
    private void initializePromptTexts() {
        this.sizeUpTextField.setPromptText(String.format("%d", Default.SIZE_UPSTREAM));
        this.sizeDownTextField.setPromptText(String.format("%d", Default.SIZE_DOWNSTREAM));
        this.minGCContentTextField.setPromptText(String.format("%.1f %%", 100 * Default.MIN_GC_CONTENT));
        this.minBaitCountTextField.setPromptText(String.valueOf(Default.MIN_BAIT_NUMBER));
        this.maxGCContentTextField.setPromptText(String.format("%.1f %%", 100 * Default.MAX_GC_CONTENT));
        this.minFragSizeTextField.setPromptText(String.format("%d", Default.MINIMUM_FRAGMENT_SIZE));
        this.maxKmerAlignabilityTextField.setPromptText(String.format("%d", Default.MAXIMUM_KMER_ALIGNABILITY));
        this.marginSizeTextField.setPromptText(String.valueOf(Default.MARGIN_SIZE));
        this.baitLengthTextField.setPromptText(String.valueOf(Default.BAIT_LENGTH));
    }

    /**
     * Remove any previous values from the text fields so that if the user chooses "New" from the File menu, they
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

    /**
     * Keep the fields in the GUI in synch with the corresponding variables in this class.
     */
    private void setBindings() {
        Bindings.bindBidirectional(this.sizeDownTextField.textProperty(), sizeDownProperty(), integerConverter);
        Bindings.bindBidirectional(this.sizeUpTextField.textProperty(), sizeUpProperty(), integerConverter);
        Bindings.bindBidirectional(this.minFragSizeTextField.textProperty(), minFragSizeProperty(), integerConverter);
        Bindings.bindBidirectional(this.maxKmerAlignabilityTextField.textProperty(), maxMeanKmerAlignabilityProperty(), doubleConverter);
        Bindings.bindBidirectional(this.minGCContentTextField.textProperty(), minGCcontentProperty(), doubleConverter);
        Bindings.bindBidirectional(this.maxGCContentTextField.textProperty(), maxGCcontentProperty(), doubleConverter);
        Bindings.bindBidirectional(this.minBaitCountTextField.textProperty(), minimumBaitCountProperty(), integerConverter);
        Bindings.bindBidirectional(this.baitLengthTextField.textProperty(), baitLengthProperty(), integerConverter);
        Bindings.bindBidirectional(this.marginSizeTextField.textProperty(), marginLengthProperty(), integerConverter);
        Bindings.bindBidirectional(this.projectNameLabel.textProperty(), projectNameProperty);
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

    /**
     * This method should be called before we create viewpoints. It updates all of the variables in our model object
     * to have the values specified in the user for the GUI, including the values of the six fields we show in the GUI
     * and that are bound in {@link #setBindings()}. Note that we store GC and repeat content as a proportion in
     * {@link GopherModel} but confirmDialog it as a proportion in the GUI. The default values are used for any fields that have not
     * been filled in by the user.
     */
    private void updateModel() {
        this.gopherService.setSizeDown(sizeDown.get() > 0 ? sizeDown.get() : Default.SIZE_DOWNSTREAM);
        this.gopherService.setSizeUp(sizeUp.get() > 0 ? sizeUp.get() : Default.SIZE_UPSTREAM);
        this.gopherService.setMinFragSize(minFragSize.get() > 0 ? minFragSize.get() : Default.MINIMUM_FRAGMENT_SIZE);
        double repeatProportion = getMaxRepeatContent() / 100;
        this.gopherService.setMaxRepeatContent(repeatProportion > 0 ? repeatProportion : Default.MAXIMUM_KMER_ALIGNABILITY);
        double minGCproportion = percentageToProportion(this.minGCContentTextField.getText());
        this.gopherService.setMinGCcontent(minGCproportion > 0 ? minGCproportion : Default.MIN_GC_CONTENT);
        double maxGCproportion = percentageToProportion(this.maxGCContentTextField.getText());
        this.gopherService.setMaxGCcontent(maxGCproportion > 0 ? maxGCproportion : Default.MAX_GC_CONTENT);
        int kmerAlign = getMaxMeanKmerAlignability() > 0 ? getMaxMeanKmerAlignability() : Default.MAXIMUM_KMER_ALIGNABILITY;
        this.gopherService.setMaxMeanKmerAlignability(kmerAlign);
        this.gopherService.setMinBaitCount(minBaitCount.get() > 0 ? minBaitCount.get() : Default.MIN_BAIT_NUMBER);
        int baitlen = baitLength.get() > 0 ? baitLength.get() : Default.BAIT_LENGTH;
        this.gopherService.setProbeLength(baitlen);
        int marginsize = marginLength.get() > 0 ? marginLength.get() : Default.MARGIN_SIZE;
        this.gopherService.setMarginSize(marginsize);

    }

    /**
     * @param perc a string such as 35%
     * @return The corresponding proportion (e.g., 0.35)
     */
    private double percentageToProportion(String perc) {
        if (perc == null) return 0.0;
        String s = perc.replaceAll("%", "");
        try {
            return Double.parseDouble(s) / 100.0;
        } catch (NumberFormatException e) {
            // do nothing
        }
        return 0.0;
    }


    /**
     * This gets called when the user chooses a new genome build. They need to do download, uncompression, indexing and
     * also get the corresponding transcript file.
     *
     * @param build Name of genome build.
     */
    private void setGenomeBuild(String build) {
        // if we are changing the build, then reset the GUI
        String previousBuild = this.gopherService.getGenomeBuild();
        if (previousBuild != null && ! previousBuild.equals(build)) {
            LOGGER.info("Chaging genome build from {} to {}", previousBuild, build);
            this.transcriptDownloadPI.setProgress(0.0);
            this.alignabilityDownloadPI.setProgress(0.0);
            this.genomeDownloadPI.setProgress(0.0);
            this.genomeIndexPI.setProgress(0.0);
            this.genomeDecompressPI.setProgress(0.0);
        }
        LOGGER.info("Setting genome build to " + build);
        this.gopherService.setGenomeBuild(build);
    }

    /**
     * This downloads the tar-gzip genome file as chosen by the user from the UCSC download site.
     * It places the compressed file into the directory chosen by the user. The path to the directory
     * is stored in the {@link GopherModel} object using the {@link GopherModel#setGenomeDirectoryPath} function.
     * Following this the user needs to uncompress and index the genome files using the function
     * {@link #decompressGenome(ActionEvent)} which is called after the corresponding button
     * is clicked in the GUI.
     */
    @FXML
    public void downloadGenome(ActionEvent e) {
        e.consume();
        String build = this.gopherService.getGenomeBuild();
        LOGGER.info("About to download genome for " + build + " (if necessary)");
        GenomeDownloader gdownloader = new GenomeDownloader(build);
        DirectoryChooser dirChooser = new DirectoryChooser();
        String genomeDir = pgProperties.getProperty(GENOME_DOWNLOAD_DIRECTORY);
        if (genomeDir != null) {
            File dir = new File(genomeDir);
            dirChooser.setInitialDirectory(dir);
        } else {
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        dirChooser.setTitle("Choose directory for genome build " + build + " (will be downloaded if not found).");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file == null || file.getAbsolutePath().equals("")) {
            LOGGER.error("Could not set genome download path from Directory Chooser");
            PopupFactory.displayError("Error", "Could not get path to download genome.");
            return;
        }
        LOGGER.info("downloadGenome to directory  " + file.getAbsolutePath());
        String parentDirName = file.getAbsoluteFile().getParent();
        pgProperties.setProperty(GENOME_DOWNLOAD_DIRECTORY, parentDirName);
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
    @FXML
    public void downloadRefGeneTranscripts(ActionEvent e) {
        String genomeBuild = genomeChoiceBox.getValue();
        RefGeneDownloader rgd = new RefGeneDownloader(genomeBuild);
        String transcriptName = rgd.getTranscriptName();
        String basename = rgd.getBaseName();
        String url;
        try {
            url = rgd.getURL();
            gopherService.setTranscriptsBasename(transcriptName);
        } catch (DownloadFileNotFoundException dfne) {
            PopupFactory.displayError("Could not identify RefGene file for genome", dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory where RefGene transcripts file for " + genomeBuild + " is located (will" +
                " be downloaded if not found).");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file == null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error", "Could not get path to download transcript file.");
            return;
        }
        if (!rgd.needToDownload(file.getAbsolutePath())) {
            LOGGER.trace(String.format("Found refGene.txt.gz file at %s. No need to download", file.getAbsolutePath()));
            this.transcriptDownloadPI.setProgress(1.0);
            String abspath = (new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.gopherService.setRefGenePath(abspath);
            return;
        }
        Downloader downloadTask = new Downloader(file, url, basename, transcriptDownloadPI);
        downloadTask.setOnSucceeded(event -> {
            String abspath = (new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            this.gopherService.setRefGenePath(abspath);
        });
        Thread th = new Thread(downloadTask);
        th.setDaemon(true);
        th.start();
        e.consume();
    }


    /**
     * G-unzip and un-tar the downloaded chromFa.tar.gz file.
     *
     * @param e Event triggered by decompress genome command
     */
    @FXML
    public void decompressGenome(ActionEvent e) {
        e.consume();
        if (this.gopherService.getGenome().isIndexingComplete()) {
            genomeDecompressPI.setProgress(1.00);
            gopherService.setGenomeUnpacked();
            return;
        }
        GenomeGunZipper genomeGunZipper = new GenomeGunZipper(this.gopherService.getGenome(),
                this.genomeDecompressPI);
        if (!genomeGunZipper.gZippedFileExists()) {
            PopupFactory.displayError("Could not find genome file",
                    "Download genome file before extraction step!");
            return;
        }
        genomeGunZipper.setOnSucceeded(event -> {
            if (genomeGunZipper.OK()) {
                gopherService.setGenomeUnpacked();
            } else {
                PopupFactory.displayError("Error", "Error from Genome g-unzipper");
            }
        });
        genomeGunZipper.setOnFailed(eventh -> PopupFactory.displayError("Could not decompress genome file", genomeGunZipper.getException().getMessage()));
        Thread th = new Thread(genomeGunZipper);
        th.setDaemon(true);
        th.start();
    }

    /**
     * Create genome fai (fasta index file)
     *
     * @param e Event triggered by index genome command.
     */
    @FXML
    public void indexGenome(ActionEvent e) {
        e.consume();
        LOGGER.trace("Indexing genome files...");
        gopherService.indexGenome(this.genomeIndexPI);
    }


    /**
     * @param e event triggered by command to download appropriate {@code refGene.txt.gz} file.
     */
    @FXML
    public void downloadAlignabilityMap(ActionEvent e) {
        String genomeBuild = genomeChoiceBox.getValue(); // e.g. hg19 or mm9
        DirectoryChooser dirChooser = new DirectoryChooser(); // choose directory to which the map will be downloaded
        dirChooser.setTitle("Choose directory where the alignability map for " + genomeBuild + " is located (will be" +
                " downloaded if not found).");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file == null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error", "Could not get path to download alignabilty file.");
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
            LOGGER.trace(String.format("Found %s. No need to download", alignabilityMapPathIncludingFileNameGz));
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
        th = new Thread(downloadTask);
        th.setDaemon(true);
        th.start();
        e.consume();
    }

    /**
     * This function is called after the user has chosen restriction enzymes in the
     * corresponding popup window. It passes a list of the {@link RestrictionEnzyme}
     * objects to the {@link GopherModel}.
     */
    @FXML
    public void chooseEnzymes() {
        List<RestrictionEnzyme> allEnzymes = gopherService.getAllEnyzmes();
        List<RestrictionEnzyme> previouslySelectedEnzymes = gopherService.getChosenEnzymelist();
        EnzymeViewFactory factory = new EnzymeViewFactory();
        List<RestrictionEnzyme> chosenEnzymes = factory.getChosenEnzymes(allEnzymes, previouslySelectedEnzymes);
        if (chosenEnzymes.isEmpty()) {
            PopupFactory.displayError("Warning", "Warning -- no restriction enzyme chosen!");
            return;
        } else {
            LOGGER.info("We retrieved {} enzymes", chosenEnzymes.size());
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
     *
     * @param e event triggered by enter gene command.
     */
    @FXML
    private void enterTargetGeneList(ActionEvent e) {
        e.consume();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose file with list of gene symbols or Entrez Gene IDs");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = fileChooser.showOpenDialog(getPrimaryStageReference());
        if (file == null) {
            LOGGER.error("Could not get name of BED file");
            return;
        } else {
            LOGGER.info("Uploading targets from " + file.getAbsolutePath());
            this.gopherService.setTargetGenesPath(file.getAbsolutePath());
        }
        enterEntrezGeneListFromFile(file);

        e.consume();
    }

    private void enterEntrezGeneListFromFile(File file) {
        LOGGER.trace("Entering target gene list {}", file.getAbsolutePath());
        gopherService.getTargetGopherGenesFromFile(file);
        gopherService.setTargetType(GopherModel.TargetType.TARGET_GENES);
        setTargetFeedback(GopherModel.TargetType.TARGET_GENES, gopherService.getN_validGeneSymbols());
    }


    @FXML
    private void enterBedFile(ActionEvent e) {
        e.consume();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showOpenDialog(getPrimaryStageReference());
        if (file == null) {
            LOGGER.error("Could not get name of BED file");
            return;
        } else {
            LOGGER.info("Uploading targets from " + file.getAbsolutePath());
            this.gopherService.setTargetGenesPath(file.getAbsolutePath());
        }
        LOGGER.trace("Entering bed file");
        try {
            BedFileParser parser = new BedFileParser(file.getAbsolutePath());
            List<GopherGene> genelist = parser.getGopherGeneList();
            List<String> validGeneSymbols = genelist.stream().map(GopherGene::getGeneSymbol).toList();
            List<String> invalidGeneSymbols = ImmutableList.of();
            int uniqueTSSpositions = genelist.size();
            int n_genes = genelist.size();
            int chosenGeneCount = genelist.size();
            int uniqueChosenTSS = genelist.size();
            this.gopherService.setN_validGeneSymbols(validGeneSymbols.size());
            this.gopherService.setUniqueTSScount(uniqueTSSpositions);
            this.gopherService.setUniqueChosenTSScount(uniqueChosenTSS);
            this.gopherService.setChosenGeneCount(chosenGeneCount);
            this.gopherService.setTotalRefGeneCount(n_genes);
            this.gopherService.setGopherGenes(parser.getGopherGeneList());
            this.gopherService.setUniqueChosenTSScount(genelist.size());
            this.gopherService.setTargetType(GopherModel.TargetType.BED_TARGETS);
            setTargetFeedback(GopherModel.TargetType.BED_TARGETS, validGeneSymbols.size());
        } catch (GopherException ge) {
            PopupFactory.displayException("Error", "Could not input BED file", ge);
        }
    }

    @FXML
    private void allProteinCodingGenes(ActionEvent e) {
        e.consume();
        String path = gopherService.getRefGenePath();
        LOGGER.trace("Getting all protein coding genes");
        if (path == null) {
            LOGGER.error("Attempt to validate gene symbols before refGene.txt.gz file was downloaded");
            PopupFactory.displayError("Error retrieving refGene data", "Download refGene.txt.gz file before proceeding.");
            return;
        }
        LOGGER.info("About to parse refGene.txt.gz file to validate uploaded gene symbols. Path at " + path);
        RefGeneParser parser;
        try {
            parser = new RefGeneParser(path);
        } catch (Exception exc) {
            PopupFactory.displayException("Error while attempting to validate Gene symbols", "Could not validate gene symbols", exc);
            return;
        }
        List<String> validGeneSymbols = parser.getAllProteinCodingGeneSymbols();
        int uniqueTSSpositions = parser.getTotalTSScount();
        int n_genes = parser.getTotalNumberOfRefGenes();
        int chosenGeneCount = parser.getNumberOfRefGenesChosenByUser();
        int uniqueChosenTSS = parser.getCountOfChosenTSS();
        this.gopherService.setN_validGeneSymbols(validGeneSymbols.size());
        this.gopherService.setUniqueTSScount(uniqueTSSpositions);
        this.gopherService.setUniqueChosenTSScount(uniqueChosenTSS);
        this.gopherService.setChosenGeneCount(chosenGeneCount);
        this.gopherService.setTotalRefGeneCount(n_genes);
        this.gopherService.setGopherGenes(parser.getGopherGeneList());
        this.gopherService.setUniqueChosenTSScount(parser.getCountOfChosenTSS());
        this.gopherService.setTargetType(GopherModel.TargetType.ALL_GENES);
        setTargetFeedback(GopherModel.TargetType.ALL_GENES, validGeneSymbols.size());
    }


    private void setTargetFeedback(GopherModel.TargetType ttype, int count) {
        this.targetGeneLabel.setText("");
        this.bedTargetsLabel.setText("");
        this.allGenesLabel.setText("");
        switch (ttype) {
            case NONE -> {
                return;
            }
            case TARGET_GENES -> {
                this.targetGeneLabel.setText(String.format("%d genes", count));
                return;
            }
            case ALL_GENES -> {
                this.allGenesLabel.setText(String.format("%d genes", count));
                return;
            }
            case BED_TARGETS -> this.bedTargetsLabel.setText(String.format("%d targets", count));
        }
    }


    @FXML
    private void saveDigestFileAs(ActionEvent e) {
        LOGGER.trace("Saving the digest file");
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting digest file.");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File dir = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (dir == null || dir.getAbsolutePath().equals("")) {
            PopupFactory.displayError("Error", "Could not get path to export digest file.");
            return;
        }

        String path = dir.getAbsolutePath();
        path += File.separator;
        DigestCreationTask task = new DigestCreationTask(path, gopherService);

        ProgressForm pform = new ProgressForm();
        pform.messageProperty().bind(task.messageProperty());
        pform.titleProperty().bind(task.titleProperty());
        pform.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(event -> {
            LOGGER.error("Finished creating digest file");
            pform.close();
        });
        task.setOnFailed(eh -> {
            Exception exc = (Exception) eh.getSource().getException();
            PopupFactory.displayException("Error",
                    "Exception encountered while attempting to create digest file",
                    exc);
            LOGGER.error("Failed to create digest file");
            pform.close();
        });
        pform.activateProgressBar(task);
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        e.consume();
    }

    @FXML
    public void saveProbeFileAs(ActionEvent e) {
        List<ViewPoint> vplist = this.gopherService.getViewPointList();
        if (vplist == null || vplist.isEmpty()) {
            PopupFactory.displayError("Error", "Attempt to save probe file failed. Complete generation and analysis of ViewPoints before saving probes!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for saving probe file");
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file == null || file.getAbsolutePath().equals("")) {
            PopupFactory.displayError("Error", "Could not get path to save file.");
            return;
        }
        String prefix = gopherService.getProjectName();
        ProbeFileExporter exporter = new ProbeFileExporter(file.getAbsolutePath(), prefix);
        try {
            LOGGER.trace(gopherService.getGenomeFastaFile());
            LOGGER.trace(gopherService.getIndexedGenomeFastaIndexFile());
            exporter.printProbeFileInAgilentFormat(this.gopherService.getProbeLength(),
                    this.gopherService.getViewPointList(),
                    this.gopherService.getGenomeBuild(), gopherService.getGenomeFastaFile());
        } catch (Exception exc) {
            PopupFactory.displayException("Could not save probes.", exc.getMessage(), exc);
        }
        LOGGER.trace("Finished output of probe files");
        e.consume();
    }


    /**
     * When the user clicks this button, they should have uploaded and validated a list of gene symbols;
     * these will have been entered as {@link GopherGene} objects into the {@link GopherModel}
     * object. This function will use the {@link GopherGene} obejcts and other information
     * to create {@link gopher.service.model.viewpoint.ViewPoint} objects that will then be displayed in the
     * {@link VPAnalysisController} Tab.
     */
    public void createViewPoints() {
        this.vpAnalysisController.setTabPane(this.tabpane);
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
        QCCheckFactory qcFactory = new QCCheckFactory(this.gopherService);
        boolean OK = qcFactory.loadQcDialog();
        if (!OK) {
            return;
        }
        LOGGER.trace("User entered OK for parameter check");
        ViewPointCreationTask task;

        if (gopherService.useSimpleApproach()) {
            task = new SimpleViewPointCreationTask(gopherService);
        } else {
            task = new ExtendedViewPointCreationTask(gopherService);
        }
        ProgressForm pform = new ProgressForm();
        pform.messageProperty().bind(task.messageProperty());
        pform.titleProperty().bind(task.titleProperty());
        pform.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(event -> {
            LOGGER.info("View Point Creation Task succeded");
            SingleSelectionModel<Tab> selectionModel = tabpane.getSelectionModel();
            if (this.vpAnalysisController == null) {
                LOGGER.error("vpAnalysisController == null");
                return;
            }
            selectionModel.select(this.analysistab);
            this.vpAnalysisController.showVPTable();
            this.vpAnalysisController.updateListView();

            LOGGER.info("Finished createViewPoints()");
            pform.close();
        });
        task.setOnFailed(eh -> {
            if (eh.getSource().getException() instanceof OutOfMemoryError) {
                pform.close();
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
        task.setOnCancelled(e -> pform.close());
        new Thread(task).start();
        pform.activateProgressBar(task);
    }

    public void refreshViewPoints() {
        if (this.vpAnalysisController == null) {
            LOGGER.error("Could not refresh viewpoint table, since vpanalysispresenter was null");
            return;
        }
        this.vpAnalysisController.refreshVPTable();
    }


    /**
     * Display the settings (parameters) of the current viewpoint.
     */
    public void showSettingsOfCurrentProject() {
        SettingsViewFactory.showSettings(gopherService);
    }

    /**
     * Content of {@link GopherModel} is written to platform-dependent default location.
     */
    @FXML
    private void saveProject(ActionEvent e) {
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
    @FXML
    private void saveProjectAndClose() {
        gopherService.serialize();
        javafx.application.Platform.exit();
    }


    /**
     * @param e event triggered by show help command.
     */
    @FXML
    public void showHelpWindow(ActionEvent e) {
        HelpViewFactory.display();
        e.consume();
    }


    /**
     * @param e event triggered by set proxy command.
     */
    @FXML
    void setProxyDialog(ActionEvent e) {
        e.consume();
        Dialog<ProxyResults> dialog = new Dialog<>();
        dialog.setTitle("Set Proxy");
        dialog.setHeaderText("Please specify…");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Label proxyLabel = new Label("Proxy");
        TextField textField = new TextField();
        if (gopherService.getHttpProxy() != null) {
            textField.setText(gopherService.getHttpProxy());
        }
        textField.setTooltip(new Tooltip("http proxy"));
        Label proxyPortLabel = new Label("Proxy Port");
        TextField portTextField = new TextField();
        portTextField.setTooltip(new Tooltip("http proxy port"));
        if (gopherService.getHttpProxyPort() != null) {
            LOGGER.trace(String.format("http proxy port: %s", gopherService.getHttpProxyPort()));
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
            String port = results.getPort();
            String proxy = results.getProxy();
            if (proxy == null) {
                PopupFactory.displayError("Error obtaining Proxy", "Proxy string could not be obtained. Please try again");
                return;
            }
            this.gopherService.setHttpProxy(proxy);
            this.gopherService.setHttpProxyPort(port);
            LOGGER.info(String.format("Set proxy to %s[%s]", proxy, port));
            Utils.setSystemProxyAndPort(proxy, port);
        });
    }

    /**
     * This function is called if the user choose the get human example genes from the Help menu, which is intended
     * to give new users an easy way to get a list of genes to try out the software.
     */
    @FXML
    private void openGeneWindowWithExampleHumanGenes() {
        URL url = GopherMainController.class.getResource("/data/humangenesymbols.txt");
        if (url == null) {
            PopupFactory.displayError("Could not find human gene list", "error");
            return;
        }
        File file = new File(url.getFile());
        if (! file.isFile()) {
            PopupFactory.displayError("Error", "Could not fine humangenesymbols.txt");
            return;
        }
        enterEntrezGeneListFromFile(file);

    }

    @FXML
    private void openGeneWindowWithExampleMouseGenes() {
        URL url = GopherMainController.class.getResource("/data/mousegenesymbols.txt");
        if (url == null) {
            PopupFactory.displayError("Could not find human gene list", "error");
            return;
        }
        File file = new File(url.getFile());
        if (! file.isFile()) {
            PopupFactory.displayError("Error", "Could not fine mousegenesymbols.txt");
            return;
        }
        enterEntrezGeneListFromFile(file);
    }

    @FXML
    public void exportBEDFiles(ActionEvent e) {
        List<ViewPoint> vplist = this.gopherService.getViewPointList();
        if (vplist == null || vplist.isEmpty()) {
            PopupFactory.displayError("Attempt to export empty BED files", "Complete generation and analysis of ViewPoints before exporting to BED!");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory for exporting BED files.");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file == null || file.getAbsolutePath().equals("")) {
            PopupFactory.displayError("Error", "Could not get path to export BED files.");
            return;
        }
        String prefix = gopherService.getProjectName();
        BEDFileExporter exporter = new BEDFileExporter(file.getAbsolutePath(), prefix);
        try {
            exporter.printRestFragsToBed(this.gopherService.getViewPointList(), this.gopherService.getGenomeBuild());
        } catch (Exception exc) {
            PopupFactory.displayException("Could not save data to BED files", exc.getMessage(), exc);
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
        if (applicationVersion == null) {
            applicationVersion = "0.8.2";
        }
        PopupFactory.showAbout(applicationVersion, gopherService.getLastChangeDate());
        e.consume();
    }

    /**
     * Open a window that will allow the user to delete unwanted project files. Do not allow the
     * user to delete the file that is currently opened.
     *
     * @param e action event.
     */
    @FXML
    public void deleteProjectFiles(ActionEvent e) {
        List<ProjectFile> pfiles = gopher.Platform.getProjectFiles();
        DeleteProjectFilesFactory.dispay(pfiles);
        e.consume();
    }

    /**
     * Export the project ser file to a location chosen by the user (instead of the default location,
     * which is the .gopher directory).
     */
    @FXML
    public void exportProject(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        String initFileName = String.format("%s.ser", this.gopherService.getProjectName());
        chooser.setInitialFileName(initFileName);
        chooser.setTitle("Choose file path to save project file");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        String path = file.getAbsolutePath();
        gopherService.serializeToLocation(path);
        LOGGER.trace(String.format("Serialized file to %s", path));
        e.consume();
    }

    /**
     * Open a project from a file specified by the user.
     */
    @FXML
    public void importProjectFromFileMenu(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Gopher project file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gopher Files", "*.ser"));
        File file = chooser.showOpenDialog(null);
        if (file == null) { //Null pointer returned if user clicks on cancel. In this case, just do nothing.
            return;
        }
        importProject(file);
        e.consume();
    }

    private void importProject(File file) {
        LOGGER.trace("Importing project: {}", file.getAbsolutePath());
        removePreviousValuesFromTextFields();
        gopherService.importProjectFromFile(file);
        setInitializedValuesInGUI();
        LOGGER.trace("importProject: genome dir{}", gopherService.getGenome().getPathToGenomeDirectory());
        vpAnalysisController.refreshVPTable();
        LOGGER.trace("Opened model {} from file {}", gopherService.getProjectName(), file.getAbsolutePath());
        LOGGER.trace("Path to genome dir is {}", gopherService.getGenome().getPathToGenomeDirectory());
    }

    @FXML
    public void downloadRegulationData(ActionEvent event) {
        String genomeBuild = genomeChoiceBox.getValue();
        RegulatoryBuildDownloader regbuildDownloader = new RegulatoryBuildDownloader(genomeBuild);
        String basename = regbuildDownloader.getBaseName();
        String url;
        try {
            url = regbuildDownloader.getURL();
        } catch (DownloadFileNotFoundException dfne) {
            PopupFactory.displayError(String.format("Cannot generate Regulatory Exome for genome: %s", genomeBuild), dfne.getMessage());
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory to download regulatory build for " + genomeBuild + " (will be downloaded if not found).");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = dirChooser.showDialog(this.rootNode.getScene().getWindow());
        if (file == null || file.getAbsolutePath().isEmpty()) {
            PopupFactory.displayError("Error", "Could not get path to download regulatory build file.");
            return;
        }
        if (!regbuildDownloader.needToDownload(file.getAbsolutePath())) {
            String abspath = (new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            PopupFactory.displayMessage("Regulatory Build", String.format("Found regulatory build file at %s. No need to download", file.getAbsolutePath()));
            gopherService.setRegulatoryBuildPath(abspath);
            return;
        }

        ProgressPopup popup = new ProgressPopup("Downloading...", "Downloading Ensembl regulatory build file");
        ProgressIndicator progressIndicator = popup.getProgressIndicator();

        Downloader downloadTask = new Downloader(file, url, basename, progressIndicator);
        downloadTask.setOnSucceeded(e -> {
            String abspath = (new File(file.getAbsolutePath() + File.separator + basename)).getAbsolutePath();
            LOGGER.trace("Setting regulatory build path in model to " + abspath);
            gopherService.setRegulatoryBuildPath(abspath);

            popup.close();
        });
        downloadTask.setOnFailed(e -> LOGGER.error("Download of regulatory build failed"));
        popup.startProgress(downloadTask);
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
        if (!gopherService.regulatoryBuildPathInitialized()) {
            PopupFactory.displayError("Regulatory build path not initialized",
                    "Please download the regulatory build file before exporting regulatory exome");
            return;
        }
        try {
            final File regulatoryExomeDirectory = RegulatoryExomeBoxFactory.getDirectoryForExport(this.rootNode);
            LOGGER.info("downloadGenome to directory  " + regulatoryExomeDirectory.getAbsolutePath());
            runLater(() ->
                    RegulatoryExomeBoxFactory.exportRegulatoryExome(gopherService, regulatoryExomeDirectory));
        } catch (Exception e) {
            PopupFactory.displayException("Error", "Could not create regulatory exome panel data", e);
        }
        LOGGER.trace("buildRegulatoryExome");
    }

    /**
     * This method is run after user clicks on 'Close' item of File|Menu. User is prompted to confirm the closing and
     * window is closed if 'yes' is selected.
     *
     * @param e Event triggered by close command.
     */
    public void closeWindow(ActionEvent e) {
        e.consume();
        if (gopherService.isClean()) {
            boolean answer = PopupFactory.confirmQuitDialog("Alert", "Are you sure you want to quit?");
            if (answer) {
                LOGGER.info("Closing Gopher Gui");
                javafx.application.Platform.exit();
                System.exit(0);
            }
        } else {
            WindowCloser closer = new WindowCloser();
            closer.display();
            if (closer.save()) {
                gopherService.serialize();
            }
            if (closer.quit()) {
                LOGGER.info("Closing Gopher Gui");
                javafx.application.Platform.exit();
                System.exit(0);
            }
        }
    }

    /**
     * A Handler for the event that the user clicks the close box at the left upper corner of the app.
     * We just call {@link #closeWindow(ActionEvent)}, which is the function that is called
     * if the user chooses Quit from the file menu or uses the corresponding keyboard shortcut.
     * Note that we need to transform a WindowEvent to an ActionEvent.
     */
    private final EventHandler<WindowEvent> confirmCloseEventHandler = event -> {
        event.consume();
        closeWindow(new ActionEvent(event.getSource(), event.getTarget()));
    };


    @FXML
    public void displayReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.gopherService);
        PopupFactory.showReportListDialog(report.getReportList());
        e.consume();
    }

    @FXML
    public void exportReport(ActionEvent e) {
        GopherReport report = new GopherReport(this.gopherService);
        String filename = String.format("%s-report.txt", gopherService.getProjectName());
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setInitialFileName(filename);
        File file = chooser.showSaveDialog(getPrimaryStageReference());
        if (file == null) {
            PopupFactory.displayError("Error", "Could not get filename for saving report");
            return;
        }
        report.outputRegulatoryReport(file.getAbsolutePath());
        e.consume();
    }


    @FXML
    private void setUnbalancedMargin(ActionEvent e) {
        this.gopherService.setAllowUnbalancedMargins(unbalancedMarginCheckbox.isSelected());
        e.consume();
    }

    @FXML
    private void setAllowPatching(ActionEvent e) {
        this.gopherService.setAllowPatching(patchedViewpointCheckbox.isSelected());
        e.consume();
    }

    public void exportDesignStats(ActionEvent actionEvent) {
        Design design = new Design(this.gopherService);
        design.calculateDesignParameters();
        var map = design.getDesignStatisticsList();
        String enz = this.gopherService.getChosenEnzymelist().stream()
                .map(RestrictionEnzyme::getName)
                .collect(Collectors.joining("-"));
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        String filename = "gopher-design-" + enz + ".tsv";
        chooser.setInitialFileName(filename);
        File file = chooser.showSaveDialog( getPrimaryStageReference());
        if (file == null) {
            PopupFactory.displayError("Error", "Could not get filename for saving report");
            return;
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            for (var e : map.entrySet()) {
                w.write(String.format("%s\t%s\n", e.getKey(), e.getValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




