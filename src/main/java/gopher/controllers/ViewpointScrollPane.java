package gopher.controllers;

import gopher.service.GopherService;
import gopher.service.URLMaker;
import gopher.service.model.viewpoint.Segment;
import gopher.service.model.viewpoint.ViewPoint;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides a Scrollpane that is intended to show a single viewpoint. When the user clicks
 * on a viewpoint, a new object is created and the scroll pane is added to a new tab
 *
 * @author Peter N Robinson
 */
@Component
@Scope("prototype")
public class ViewpointScrollPane extends ScrollPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewpointScrollPane.class.getName());
    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>GOPHER</h3><p><i>Connecting to UCSC " +
            "Browser to visualize view point...</i></p></body></html>";


    private static final String FAILED_HTML_CONTENT = "<html><body><h3>GOPHER</h3><p><i>Could not connect to UCSC " +
            "Browser. Please check your internet connection and proxy</i></p></body></html>";

    private final static String[] colors = {"F08080", "CCE5FF", "ABEBC6", "FFA07A", "C39BD3", "FEA6FF", "F7DC6F", "CFFF98", "A1D6E2",
            "EC96A4", "E6DF44", "F76FDA", "FFCCE5", "E4EA8C", "F1F76F", "FDD2D6", "F76F7F", "DAF7A6", "FFC300", "F76FF5", "FFFF99",
            "FF99FF", "99FFFF", "CCFF99", "FFE5CC", "FFD700", "9ACD32", "7FFFD4", "FFB6C1", "FFFACD",
            "FFE4E1", "F0FFF0", "F0FFFF"};

    /**
     * The graphical element where the UCSC browser content is displayed.
     */

    private final WebView ucscContentWebView;
    /**
     * This will be dynamically set to the name of the gene and the score of the viewpoint.
     */
    private final Label viewpointScoreLabel;
    /**
     * This will be dynamically set with details about the view point.
     */
    private final Label viewpointExplanationLabel;
    /**
     * This is use to mediate between {@link #viewpointScoreLabel} and the ViewPoint object and score.
     */
    private final StringProperty vpScoreProperty;
    /**
     * This is used to mediate between the {@link #viewpointExplanationLabel} and the ViewPoint object.
     */
    private final StringProperty vpExplanationProperty;
    /**
     * The backend behind the UCSC browser content.
     */
    private final WebEngine ucscWebEngine;
    /**
     * By how much do we change the width of the UCSC confirmDialog when zooming?
     */
    private static final double ZOOMFACTOR = 1.5;
    /**  Individual {@link Segment}s of {@link ViewPoint} are presented in this TableView.   */
    /**
     * A link back to the analysis tab that allows us to refresh the statistics if the user deletes "this" ViewPoint.
     */
    private final VPAnalysisController analysisPresenter;
    private final TableView<ColoredSegment> segmentsTableView;
    private final TableColumn<ColoredSegment, String> colorTableColumn;
    private final TableColumn<ColoredSegment, CheckBox> isSelectedTableColumn;
    private final TableColumn<ColoredSegment, String> locationTableColumn;
    private final TableColumn<ColoredSegment, String> repeatContentUpColumn;
    private final TableColumn<ColoredSegment, String> gcContentUpDownColumn;
    private final TableColumn<ColoredSegment, String> numberOfBaitsColumn;
    private final TableColumn<ColoredSegment, String> segmentLengthColumn;

    private final TableColumn<ColoredSegment, String> alignabilityContentColumn;

    private final Button deleteButton;
    private final Button copyToClipboardButton;
    private final Button zoomInButton;
    private final Button zoomOutButton;

    private final Button closeButton;

    private final StackPane mainStackPane;
    /**
     * Reference to the {@link Tab} where this content is placed.
     */
    private Tab tab;
    //@Autowired
    private final GopherService gopherService;


    /**
     * Instance of {@link ViewPoint} presented by this presenter.
     */
    private final ViewPoint viewpoint;
    /**
     * If {@link #startIndexForColor} is set to this, then we know we need to set it to a random number. Otherwise
     * leave if unchanged so that the color remains the same.
     */
    private static final int UNINITIALIZED = -1;
    /**
     * The (random) starting index in our list of colors.
     */
    private int startIndexForColor = UNINITIALIZED;
    /**
     * The current index for color -- this will be updated by the iteration.
     */
    private int idx;
    /**
     * This is a kind of wrapper for the segments that keeps track of how they should be colored in the UCSC view as
     * well as in the table.
     */
    private List<ColoredSegment> coloredsegments;

    /**
     * The amount to zoom a window by. Note that we will limit this to 20%-500% of the original window.
     */
    private double zoomfactor = 1.0d;

    private final VBox listviewVBox;

    private final ListView<String> listViewKey;
    private final ListView<String> listViewValue;

    public ViewpointScrollPane(ViewPoint vp, VPAnalysisController analysisPresenter) {
        super();
        this.viewpoint = vp;
        this.analysisPresenter = analysisPresenter;
        this.gopherService = analysisPresenter.getGopherService();
        setFitToHeight(true);
        setFitToWidth(true);
        mainStackPane = new StackPane();
        mainStackPane.setAlignment(Pos.TOP_CENTER);
        setContent(mainStackPane);
        SplitPane splitPane = new SplitPane();
        splitPane.setMaxWidth(1600.0);
        splitPane.setMinHeight(560.0);
        splitPane.setMinWidth(1100.0);
        splitPane.setPrefWidth(1100.0);
        splitPane.setOrientation(Orientation.VERTICAL);
        mainStackPane.getChildren().add(splitPane);
        listviewVBox = new VBox();
        listviewVBox.setAlignment(Pos.TOP_CENTER);
        listviewVBox.setMaxWidth(1200.0);
        listviewVBox.setMinWidth(1105.0);
        listviewVBox.setMinHeight(600.0);
        listviewVBox.setPrefWidth(1200.0);
        Label lvboxLabel = new Label("Panel design summary");
        ClassLoader classLoader = ViewpointScrollPane.class.getClassLoader();
        URL url = classLoader.getResource("css/gopherstyle.css");
        if (url != null) {
            getStylesheets().add(url.toExternalForm());
        } else {
            // should never happen!
            LOGGER.error("Could not load style sheet: css/gopherstyle.css");
        }
        lvboxLabel.setStyle("toplabel");
        HBox hb1 = new HBox();
        hb1.setAlignment(Pos.TOP_CENTER);
        hb1.setPrefHeight(120.0);
        hb1.setPrefWidth(1105.0);
        listViewKey = new ListView<>();
        listViewKey.setPrefHeight(170.0);
        listViewKey.setPrefWidth(200.0);
        listViewValue = new ListView<>();
        listViewValue.setPrefHeight(170.0);
        listViewValue.setPrefWidth(905.0);
        ucscContentWebView = new WebView();
        ucscContentWebView.setMinHeight(300.0);
        ucscContentWebView.setMinWidth(1100.0);
        ucscContentWebView.setPrefHeight(-1.0);
        ucscContentWebView.setPrefWidth(-1.0);
        ucscWebEngine = ucscContentWebView.getEngine();
        ucscWebEngine.setUserDataDirectory(new File(gopher.io.Platform.getWebEngineUserDataDirectory(), getClass().getCanonicalName()));
        ucscWebEngine.loadContent(INITIAL_HTML_CONTENT);
        // allow content of viewpoint tab to be resized to follow width of UCSC image
        splitPane.prefWidthProperty().bind(ucscContentWebView.widthProperty());

        // Todo -- not catching lack of internet connect error.
        ucscWebEngine.setOnError(event -> System.out.println("BAD ERRL " + event.toString()));
        /* The following line is needed to avoid an SSL handshake alert
         * when opening the UCSC Browser. */
        System.setProperty("jsse.enableSNIExtension", "false");
        // VBox with table
        VBox vb1 = new VBox();
        /*

         */
        GridPane gridPane = new GridPane();
        for (int i = 0; i < 5; i++) { // five identical constacut
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setHgrow(Priority.SOMETIMES);
            col1.setMinWidth(10.0);
            col1.setPrefWidth(100.0);
            gridPane.getColumnConstraints().add(col1);
        }
        RowConstraints row1 = new RowConstraints();
        row1.setVgrow(Priority.NEVER);
        row1.setMinHeight(30.0);
        row1.setPrefHeight(30.0);
        RowConstraints row2 = new RowConstraints();
        row2.setVgrow(Priority.NEVER);
        row2.setMinHeight(30.0);
        row2.setPrefHeight(30.0);
        RowConstraints row3 = new RowConstraints();
        row3.setMinHeight(30.0);
        row3.setPrefHeight(30.0);
        gridPane.getRowConstraints().addAll(row1, row2, row3);
        viewpointScoreLabel = new Label();
        viewpointScoreLabel.setMaxWidth(1600.0);
        viewpointScoreLabel.setStyle("mylabel");
        gridPane.add(viewpointScoreLabel, 0, 0, 4, 1); // colspan 5
        viewpointExplanationLabel = new Label();
        viewpointExplanationLabel.setMaxWidth(1600);
        viewpointExplanationLabel.setStyle("mylabel");
        gridPane.add(viewpointExplanationLabel, 1, 0, 4, 1);
        zoomOutButton = createButton("Zoom out", 30, 90, 30, 90, 10);
        zoomOutButton.setOnAction(e -> zoomOut());
        gridPane.add(zoomOutButton, 0, 2);
        zoomInButton = createButton("Zoom in", 30, 90, 30, 90, 10);
        zoomInButton.setOnAction(e -> zoomIn());
        gridPane.add(zoomInButton, 1, 2);
        deleteButton = createButton("Delete", 30, 90, 30, 90, 10);
        deleteButton.setOnAction(e -> deleteThisViewPoint(e));
        gridPane.add(deleteButton, 2, 2);
        copyToClipboardButton = createButton("Copy", 30, 90, 30, 90, 10);
        copyToClipboardButton.setOnAction(e -> copyToClipboard(e));
        gridPane.add(copyToClipboardButton, 3, 2);
        closeButton = createButton("Close", 30, 90, 30, 90, 10);
        closeButton.setOnAction(e -> closeButtonAction());
        gridPane.add(closeButton, 4, 2);
        colorTableColumn = new TableColumn<>();
        // This is a hack when by using dummy column a color for the cell's TableRow is set.
        colorTableColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    getTableRow().setStyle(String.format("-fx-background-color: #%s;", item.substring(3)));
                } else {
                    getTableRow().setStyle("-fx-background-color: transparent;");
                }
            }
        });

        colorTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getColor()));
        isSelectedTableColumn = new TableColumn<>("selected?");
        isSelectedTableColumn.setCellValueFactory(cdf -> {
            // if we get here, the user has selected or deselected the checkbox
            //this.viewpoint.setManuallyRevised();
            //this.analysisPresenter.refreshVPTable();
            Segment segment = cdf.getValue().getSegment();
            CheckBox checkBox = cdf.getValue().getCheckBox();
            ChangeListener<Boolean> changeListener = cdf.getValue().getChangeListener();
            if (segment.isUnselectable()) {
                checkBox.setDisable(true);
            } else if (segment.isSelected()) {// inspect state of the segment and initialize CheckBox state accordingly
                checkBox.setSelected(true);
            }
            if (changeListener == null) {
                changeListener = new ChangeListener<>() {
                    public void changed(ObservableValue<? extends Boolean> ov,
                                        Boolean old_val, Boolean new_val) {
                        // the following updates the selection in the GUI but does not change the originallySelected state of the segment
                        cdf.getValue().getSegment().setSelected(new_val, false); // changes the selected value of the Segment
                        viewpoint.refreshStartAndEndPos();
                        if (!old_val.equals(new_val)) {
                            // if the user has changed something, record that we have unsaved data
                            // and also refresh the table to show the new score etc.
                            gopherService.setClean(false);
                            analysisPresenter.refreshVPTable();
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                updateScore();
                                refreshUCSCButtonAction();
                                colorTableColumn.setCellFactory(col -> new TableCell<>() {
                                    @Override
                                    protected void updateItem(String item, boolean empty) {
                                        super.updateItem(item, empty);
                                        if (item != null && !empty) {
                                            getTableRow().setStyle(String.format("-fx-background-color: #%s;", item.substring(3)));
                                        } else {
                                            getTableRow().setStyle("-fx-background-color: transparent;");
                                        }
                                    }
                                });
                            }
                        });

                    }
                };
                cdf.getValue().setChangeListener(changeListener);
                checkBox.selectedProperty().addListener(changeListener);
            }
            return new ReadOnlyObjectWrapper<>(cdf.getValue().getCheckBox()); // the same checkbox
        });
        locationTableColumn = new TableColumn<>("Location");
        locationTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getSegment()
                .getChromosomalPositionString())));
        locationTableColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                }

            }
        });
        locationTableColumn.setComparator(new ViewPointController.FormattedChromosomeComparator());
        locationTableColumn.setSortable(false);

        segmentLengthColumn = new TableColumn<>("Segment len");
        segmentLengthColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getSegment().length())));
        segmentLengthColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item);
                    int len = Integer.parseInt(item);
                    if (len < gopherService.getMinFragSize()) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        segmentLengthColumn.setSortable(false);

        alignabilityContentColumn = new TableColumn<>("alignability");

        alignabilityContentColumn.setCellValueFactory(cdf -> {
            double alignability = cdf.getValue().getSegment().getMeanAlignabilityOfBaits();
            return new ReadOnlyStringWrapper(String.valueOf(alignability));
        });
        alignabilityContentColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    double rp = Double.parseDouble(item);
                    if (Double.isNaN(rp)) {
                        setText("n/a");
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setText(String.format("%.1f", rp));
                        if (rp > gopherService.getMaxMeanKmerAlignability()) {
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                        } else {
                            setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                        }
                    }
                }
            }
        });
        alignabilityContentColumn.setSortable(false);

        repeatContentUpColumn = new TableColumn<>("repeat content");
        repeatContentUpColumn.setCellValueFactory(cdf -> {
            String val = cdf.getValue().getSegment().getMeanRepeatContentOfBaitsAsPercent();
            return new ReadOnlyStringWrapper(val);
        });
        // repeatContentUpColumn.setComparator(new PercentComparator());
        repeatContentUpColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item);
                    boolean red = false;
                    if (item.equals("n/a")) red = true;
                    else { // in this case we expect something like 35.2%/34.8%
                        String[] A = item.split("/");
                        for (String a : A) {
                            double rp = 0.01 * ((a.endsWith("%")) ? Double.parseDouble(a.substring(0, a.length() - 1)) : Double.parseDouble(a));
                            if (rp > gopherService.getMaxRepeatContent()) red = true;
                        }
                    }
                    if (red) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        repeatContentUpColumn.setSortable(false);


        gcContentUpDownColumn = new TableColumn<>("GC content");

        gcContentUpDownColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getMeanGCcontentOfBaitsAsPercent())));
        gcContentUpDownColumn.setCellFactory(column -> new TableCell<>() {
            // this code highlights GC content that outside of GC boundaries set in 'Set up' pane
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) { // here, item is String like '40.00%'
                    setText(item);
                    boolean red = false;
                    if (item.equals("n/a")) red = true;
                    else {
                        String[] A = item.split("/");
                        for (String a : A) {
                            // maxGcContent is a proportion (not a percentage) so we need to convert back
                            double rp = 0.01 * ((a.endsWith("%")) ? Double.parseDouble(a.substring(0, a.length() - 1)) : Double.parseDouble(a));
                            // Show red if we are above or below threshold for either threshold
                            if (rp > gopherService.getMaxGCcontent()) red = true;
                            if (rp < gopherService.getMinGCcontent()) red = true;
                        }
                    }
                    if (red) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        gcContentUpDownColumn.setSortable(false);


        numberOfBaitsColumn = new TableColumn<>("n baits");
        numberOfBaitsColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getNumberOfBaitsUpDownAsString())));
        numberOfBaitsColumn.setCellFactory(column -> new TableCell<>() {
            // this code highlights GC content that outside of GC boundaries set in 'Set up' pane
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) { // here, item is String like '40.00%'
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                }
            }
        });
        numberOfBaitsColumn.setSortable(false);

        segmentsTableView = new TableView<>();
        segmentsTableView.getColumns().add(colorTableColumn);
        segmentsTableView.getColumns().add(isSelectedTableColumn);
        segmentsTableView.getColumns().add(locationTableColumn);
        segmentsTableView.getColumns().add(segmentLengthColumn);
        segmentsTableView.getColumns().add(alignabilityContentColumn);
        segmentsTableView.getColumns().add(repeatContentUpColumn);
        segmentsTableView.getColumns().add(gcContentUpDownColumn);
        segmentsTableView.getColumns().add(numberOfBaitsColumn);
        // add to the VBox
        vb1.getChildren().addAll(gridPane, segmentsTableView);

        vpScoreProperty = new SimpleStringProperty();
        vpExplanationProperty = new SimpleStringProperty();
        viewpointScoreLabel.textProperty().bindBidirectional(vpScoreProperty);
        viewpointExplanationLabel.textProperty().bindBidirectional(vpExplanationProperty);

        /* the following will start us off with a different color for each ViewPoint. */
        if (startIndexForColor == UNINITIALIZED) {
            this.startIndexForColor = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, colors.length);
        }
        //getChildren().add(mainStackPane);
        splitPane.getItems().addAll(ucscContentWebView, vb1);
        updateScore();
        showColoredSegmentsInTable();
        showUcscView();
    }

    private void showColoredSegmentsInTable() {
        segmentsTableView.getItems().clear();
        this.idx = this.startIndexForColor; // "reset" the start position for the loop around the colors.
        this.coloredsegments = this.viewpoint.getAllSegments().stream()
                .map(s -> new ColoredSegment(s, getNextColor()))
                .collect(Collectors.toList());
        segmentsTableView.getItems().addAll(coloredsegments);

    }

    private void refreshUCSCButtonAction() {
        URLMaker urlmaker = new URLMaker(this.gopherService);
        String url = urlmaker.getImageURL(viewpoint, this.zoomfactor, getHighlightRegions());
        StackPane sproot = new StackPane();
        final ProgressIndicator progress = new ProgressIndicator(); // or you can use ImageView with animated gif instead
        this.ucscWebEngine.load(url);

        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        sproot.getChildren().addAll(progress);

        Scene scene = new Scene(sproot, 75, 75);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        this.ucscWebEngine.getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        // hide progress bar then page is ready
                        progress.setVisible(false);
                        stage.close();
                    } else if (newState.equals(Worker.State.FAILED)) {
                        progress.setVisible(false);
                        stage.close();
                        ucscWebEngine.loadContent(FAILED_HTML_CONTENT);
                    }
                });
    }


    private Button createButton(String label, int minHeight, int minWidth, int maxHeight, int maxWidth, int insetLen) {
        Button button = new Button(label);
        button.setMinHeight(minHeight);
        button.setMinWidth(minWidth);
        button.setMaxHeight(maxHeight);
        button.setMaxWidth(maxWidth);
        button.setPadding(new Insets(insetLen));
        return button;
    }


    private void updateScore() {
        if (this.viewpoint.getDerivationApproach().equals(ViewPoint.Approach.SIMPLE)) {
            this.viewpoint.calculateViewpointScoreSimple(this.viewpoint.getStartPos(), this.viewpoint.getGenomicPos(), this.viewpoint.getEndPos());
        } else {
            this.viewpoint.calculateViewpointScoreExtended();
        }
        this.vpScoreProperty.setValue(String.format("%s [%s] - Score: %.1f%% [%s], Length: %s",
                viewpoint.getTargetName(),
                viewpoint.getAccession(),
                100 * viewpoint.getScore(),
                viewpoint.getGenomicLocationString(),
                viewpoint.getTotalAndActiveLengthAsString()));
        if (viewpoint.hasNoActiveSegment()) {
            this.vpExplanationProperty.setValue("No selected fragments");
        } else {
            int upstreamSpan = viewpoint.getUpstreamSpan();
            int downstreamSpan = viewpoint.getDownstreamSpan();
            int total = viewpoint.getTotalPromoterCount();
            this.vpExplanationProperty.setValue(String.format("Upstream: %d bp; Downstream: %d bp. %s",
                    upstreamSpan, downstreamSpan, viewpoint.getStrandAsString()));
        }
    }

    private void deleteThisViewPoint(Event e) {
        this.gopherService.deleteViewpoint(this.viewpoint);
        tab.setDisable(true);
        this.tab.getTabPane().getTabs().remove(this.tab);
        this.analysisPresenter.refreshVPTable();
        e.consume();
    }


    /**
     * create url & load content from UCSC
     */
    private void showUcscView() {
        LOGGER.trace("showUcscView with gopherService {}", gopherService);
        URLMaker maker = new URLMaker(this.gopherService);
        LOGGER.trace("Getting URL with zoomfactor=" + zoomfactor);
        String url = maker.getImageURL(this.viewpoint, this.zoomfactor, getHighlightRegions());
        ucscWebEngine.load(url);
    }

    /**
     * Adjust the zoom factor and ensure that it stays within 20%-500% of the original range.
     *
     * @param adjustment Amount to change the zoom factor (1.5 or 0.5).
     */
    private void setZoomFactor(double adjustment) {
        this.zoomfactor *= adjustment;
        this.zoomfactor = Math.max(0.2, zoomfactor);
        this.zoomfactor = Math.min(5.0, zoomfactor);
    }

    private void copyToClipboard(Event e) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        URLMaker urlmaker = new URLMaker(this.gopherService);
        String url = urlmaker.getURL(viewpoint, this.zoomfactor, getHighlightRegions());
        content.putString(url);
        clipboard.setContent(content);
        e.consume();
    }

    void closeButtonAction() {
        Platform.runLater(() -> {
            this.analysisPresenter.removeViewPointTab(this.viewpoint);
            this.analysisPresenter.refreshVPTable();
        });
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    /**
     * Get the top-level Pane which contains all other graphical elements of this controller.
     *
     * @return {@link javafx.scene.control.ScrollPane} object.
     */
    public ScrollPane getPane() {
        return this;
    }


    /**
     * This function returns a rotating list of colors for the digest highlights designed to be displayed on the
     * UCSC browser. If a segment is not selected, it returns "" (an emtpy string), which basically causes the corresponding
     * segment to show as not color-highlighted in the UCSC image or in the table.
     *
     * @return a rotating list of colors for the digest highlights.
     */
    private String getNextColor() {
        String color = colors[this.idx];
        this.idx = (this.idx + 1) % (colors.length);
        return String.format("%%23%s", color);
    }


    /**
     * Zoom in or out with the UCSC display.
     *
     * @param adjustment If we zoom in, factor is {@link #ZOOMFACTOR}; if we zoom out, factor is 1/{@link #ZOOMFACTOR};
     */
    private void zoom(double adjustment) {
        LOGGER.trace(String.format("Before zoom (factor %.2f) start=%d end =%d", adjustment, viewpoint.getStartPos(), viewpoint.getEndPos()));
        setZoomFactor(adjustment);
        LOGGER.trace(String.format("After zoom start=%d end =%d", viewpoint.getStartPos(), viewpoint.getEndPos()));
        showColoredSegmentsInTable();
        showUcscView();
    }

    /**
     * Change the UCSC view by zooming in.
     */
    private void zoomIn() {
        zoom(1 / ZOOMFACTOR);
    }

    /**
     * Change the UCSC view by zooming out.
     */
    private void zoomOut() {
        zoom(ZOOMFACTOR);
    }

    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     *
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
     * .
     */
    private String getHighlightRegions() {
        String genome = this.gopherService.getGenomeBuild();
        String chromosome = this.viewpoint.getReferenceID();
        List<String> colorsegmentlist = coloredsegments.stream().
                filter(ColoredSegment::isSelected).
                map(c -> String.format("%s.%s%%3A%d-%d%s",
                        genome,
                        chromosome,
                        c.getSegment().getStartPos(),
                        c.getSegment().getEndPos(),
                        c.getColor())).toList();
        String highlightregions = colorsegmentlist.stream().collect(Collectors.joining("%7C"));
        return String.format("highlight=%s", highlightregions);
    }


}
