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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * This class acts as a controller of the TabPanes which confirmDialog individual ViewPoints.
 * @author Peter Robinson
 * @version 0.2.8 (2018-07-12)
 */
@Component
public class ViewPointController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ViewPointController.class.getName());

    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>GOPHER</h3><p><i>Connecting to UCSC " +
            "Browser to visualize view point...</i></p></body></html>";


    private static final String FAILED_HTML_CONTENT  = "<html><body><h3>GOPHER</h3><p><i>Could not connect to UCSC " +
            "Browser. Please check your internet connection and proxy</i></p></body></html>";

    private final static String[] colors = {"F08080", "CCE5FF", "ABEBC6", "FFA07A", "C39BD3", "FEA6FF","F7DC6F", "CFFF98", "A1D6E2",
            "EC96A4", "E6DF44", "F76FDA","FFCCE5", "E4EA8C", "F1F76F", "FDD2D6", "F76F7F", "DAF7A6","FFC300" ,"F76FF5" , "FFFF99",
            "FF99FF", "99FFFF","CCFF99","FFE5CC","FFD700","9ACD32","7FFFD4","FFB6C1","FFFACD",
            "FFE4E1","F0FFF0","F0FFFF"};

    @FXML
    private SplitPane viewPointSplitPane;

    /** The top-level Pane which contains all other graphical elements of this controller.*/
    @FXML
    private ScrollPane contentScrollPane;
    /** The graphical element where the UCSC browser content is displayed.*/
    @FXML
    private WebView ucscContentWebView;
    /** This will be dynamically set to the name of the gene and the score of the viewpoint. */
    @FXML private Label viewpointScoreLabel;
    /** This will be dynamically set with details about the view point. */
    @FXML private Label viewpointExplanationLabel;
    /** This is use to mediate between {@link #viewpointScoreLabel} and the ViewPoint object and score. */
    private StringProperty vpScoreProperty;
    /** This is used to mediate between the {@link #viewpointExplanationLabel} and the ViewPoint object. */
    private StringProperty vpExplanationProperty;
    /** The backend behind the UCSC browser content. */
    private WebEngine ucscWebEngine;
    /** By how much do we change the width of the UCSC confirmDialog when zooming? */
    private static final double ZOOMFACTOR =1.5;
    /**  Individual {@link Segment}s of {@link ViewPoint} are presented in this TableView.   */
    @FXML
    private TableView<ColoredSegment> segmentsTableView;
    @FXML private TableColumn<ColoredSegment, String> colorTableColumn;
    @FXML private TableColumn<ColoredSegment, CheckBox> isSelectedTableColumn;
    @FXML private TableColumn<ColoredSegment, String> locationTableColumn;
   // @FXML private TableColumn<ColoredSegment, String> inRepetitiveTableColumn;
    @FXML private TableColumn<ColoredSegment, String> repeatContentUpColumn;
   // @FXML private TableColumn<ColoredSegment, String> repeatContentDownColumn;
    @FXML private TableColumn<ColoredSegment, String> gcContentUpDownColumn;
    @FXML private TableColumn<ColoredSegment, String> numberOfBaitsColumn;
   // @FXML private TableColumn<ColoredSegment, String> gcContentTableColumn;
    @FXML private TableColumn<ColoredSegment,String> segmentLengthColumn;

    @FXML private TableColumn<ColoredSegment, String> alignabilityContentColumn;

    @FXML private Button deleteButton;
    @FXML private Button copyToClipboardButton;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;


    /**
     * Reference to the {@link Tab} where this content is placed.
     */
    private Tab tab;

    private GopherService gopherService;
    /** A link back to the analysis tab that allows us to refresh the statistics if the user deletes "this" ViewPoint.*/
    private VPAnalysisPresenter analysisPresenter=null;

    /** Instance of {@link ViewPoint} presented by this presenter. */
    private ViewPoint viewpoint;
    /** If {@link #startIndexForColor} is set to this, then we know we need to set it to a random number. Otherwise
     * leave if unchanged so that the color remains the same.  */
    private static final int UNINITIALIZED=-1;
    /** The (random) starting index in our list of colors. */
    private int startIndexForColor = UNINITIALIZED;
    /** The current index for color -- this will be updated by the iteration. */
    private int idx;
    /** This is a kind of wrapper for the segments that keeps track of how they should be colored in the UCSC view as
     * well as in the table.
     */
    private List<ColoredSegment> coloredsegments;

    /** The amount to zoom a window by. Note that we will limit this to 20%-500% of the original window. */
    private double zoomfactor=1.0d;


    @Autowired
    public ViewPointController(GopherService gopherService) {
        this.gopherService = gopherService;
    }


    /** Remove the current tab from the App.  */
    @FXML void closeButtonAction() {
        Platform.runLater(() -> {
            this.analysisPresenter.removeViewPointTab(this.viewpoint);
            this.analysisPresenter.refreshVPTable();
        });
    }

    /** Copy the UCSC URL to the clipboard (interactive instead of image only) */
    @FXML private void copyToClipboard(Event e) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        URLMaker urlmaker = new URLMaker(this.gopherService);
        String url= urlmaker.getURL(viewpoint,this.zoomfactor,getHighlightRegions());
        content.putString(url);
        clipboard.setContent(content);
        e.consume();
    }





    @FXML private void deleteThisViewPoint(Event e) {
        this.gopherService.deleteViewpoint(this.viewpoint);
        tab.setDisable(true);
        this.tab.getTabPane().getTabs().remove(this.tab);
        this.analysisPresenter.refreshVPTable();
        e.consume();
    }

    @FXML
    private void refreshUCSCButtonAction() {
        URLMaker urlmaker = new URLMaker(this.gopherService);
        String url= urlmaker.getImageURL(viewpoint,this.zoomfactor,getHighlightRegions());
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

    public void setCallback(VPAnalysisPresenter vpAnalysisPresenter) {
        this.analysisPresenter=vpAnalysisPresenter;
    }

    /** class for sorting chromosome locations like chr5:43679423 */
    static class FormattedChromosomeComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            Integer i1;
            Integer i2;
            try {
                int x1 = s1.indexOf(":") + 1;
                int y1 = s1.indexOf("-");
                s1 = s1.substring(x1, y1);
                s1=s1.replaceAll(",", "");
                int x2 = s2.indexOf(":") + 1;
                int y2 = s2.indexOf("-");
                s2 = s2.substring(x2, y2);
                s2=s2.replaceAll(",", "");
                i1 = Integer.parseInt(s1);
                i2 = Integer.parseInt(s2);
                return i1.compareTo(i2);
            } catch (Exception e) {
                logger.error("Error while sorting chromosome strings s1={} s2={}} ({})", s1, s2, e.getMessage());
                return 0;
            }
        }
    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ucscWebEngine = ucscContentWebView.getEngine();
        ucscWebEngine.setUserDataDirectory(new File(gopher.io.Platform.getWebEngineUserDataDirectory(), getClass().getCanonicalName()));
        ucscWebEngine.loadContent(INITIAL_HTML_CONTENT);

        // allow content of viewpoint tab to be resized to follow width of UCSC image
        viewPointSplitPane.prefWidthProperty().bind(ucscContentWebView.widthProperty());

        // Todo -- not catching lack of internet connect error.
        ucscWebEngine.setOnError(event -> System.out.println("BAD ERRL " + event.toString()));



        /* The following line is needed to avoid an SSL handshake alert
         * when opening the UCSC Browser. */
        System.setProperty("jsse.enableSNIExtension", "false");
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

        }});
        locationTableColumn.setComparator(new FormattedChromosomeComparator());
        locationTableColumn.setSortable(false);

        segmentLengthColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getSegment().length())));
        segmentLengthColumn.setCellFactory( column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item);
                    Integer len = Integer.parseInt(item);
                    if (len < gopherService.getMinFragSize()) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        segmentLengthColumn.setSortable(false);

        alignabilityContentColumn.setCellValueFactory(cdf -> {
            double alignability = cdf.getValue().getSegment().getMeanAlignabilityOfBaits();
            return new ReadOnlyStringWrapper(String.valueOf(alignability));
        });
        alignabilityContentColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty);
                if (item != null && !empty) {
                    Double rp =   Double.parseDouble(item);
                    if (rp.isNaN()) {
                        setText("n/a");
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setText(String.format("%.1f",rp));
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

        repeatContentUpColumn.setCellValueFactory(cdf -> {
                String val = cdf.getValue().getSegment().getMeanRepeatContentOfBaitsAsPercent();
                return new ReadOnlyStringWrapper(val);
        });
       // repeatContentUpColumn.setComparator(new PercentComparator());
        repeatContentUpColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty);
                if (item != null && !empty) {
                    setText(item);
                    boolean red=false;
                    if (item.equals("n/a")) red=true;
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

        gcContentUpDownColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getMeanGCcontentOfBaitsAsPercent())));
        gcContentUpDownColumn.setCellFactory(column -> new TableCell<>() {
            // this code highlights GC content that outside of GC boundaries set in 'Set up' pane
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) { // here, item is String like '40.00%'
                    setText(item);
                    boolean red=false;
                    if (item.equals("n/a")) red=true;
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

        vpScoreProperty=new SimpleStringProperty();
        vpExplanationProperty=new SimpleStringProperty();
        viewpointScoreLabel.textProperty().bindBidirectional(vpScoreProperty);
        viewpointExplanationLabel.textProperty().bindBidirectional(vpExplanationProperty);

        /* the following will start us off with a different color for each ViewPoint. */
        if (startIndexForColor == UNINITIALIZED) {
            this.startIndexForColor = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, colors.length);
        }
    }



    private void updateScore() {
        if(this.viewpoint.getDerivationApproach().equals(ViewPoint.Approach.SIMPLE)) {
            this.viewpoint.calculateViewpointScoreSimple(this.viewpoint.getStartPos(),this.viewpoint.getGenomicPos(), this.viewpoint.getEndPos());
        } else {
            this.viewpoint.calculateViewpointScoreExtended();
        }
        this.vpScoreProperty.setValue(String.format("%s [%s] - Score: %.1f%% [%s], Length: %s",
                viewpoint.getTargetName(),
                viewpoint.getAccession(),
                100* viewpoint.getScore(),
                viewpoint.getGenomicLocationString(),
                viewpoint.getTotalAndActiveLengthAsString()));
        if (viewpoint.hasNoActiveSegment()) {
            this.vpExplanationProperty.setValue("No selected fragments");
        } else {
            int upstreamSpan= viewpoint.getUpstreamSpan();
            int downstreamSpan= viewpoint.getDownstreamSpan();
            int total = viewpoint.getTotalPromoterCount();
            this.vpExplanationProperty.setValue(String.format("Upstream: %d bp; Downstream: %d bp. %s",
                    upstreamSpan,downstreamSpan, viewpoint.getStrandAsString()));
        }
    }
    /**
     * Set the ViewPoint that will be presented. Load UCSC view and populate tableview with ViewPoint segments.
     *
     * @param vp {@link ViewPoint} which will be presented.
     */
    public void setViewPoint(ViewPoint vp) {
        this.viewpoint = vp;
        updateScore();
        showColoredSegmentsInTable();
        showUcscView();
    }


    private void showColoredSegmentsInTable() {
        segmentsTableView.getItems().clear();
        this.idx=this.startIndexForColor; // "reset" the start position for the loop around the colors.
        this.coloredsegments = this.viewpoint.getAllSegments().stream()
                .map(s -> new ColoredSegment(s, getNextColor()))
                .collect(Collectors.toList());
        segmentsTableView.getItems().addAll(coloredsegments);
    }

    /**
     * create url & load content from UCSC
     */
    private void showUcscView() {
        URLMaker maker = new URLMaker(this.gopherService);
        logger.trace("Getting URL with zoomfactor="+zoomfactor);
        String url= maker.getImageURL(this.viewpoint,this.zoomfactor,getHighlightRegions());
        ucscWebEngine.load(url);
    }

    /** Adjust the zoom factor and ensure that it stays within 20%-500% of the original range.
     * @param adjustment Amount to change the zoom factor (1.5 or 0.5).
     * */
    private void setZoomFactor(double adjustment) {
        this.zoomfactor *= adjustment;
        this.zoomfactor = Math.max(0.2,zoomfactor);
        this.zoomfactor = Math.min(5.0,zoomfactor);
    }


    public void setTab(Tab tab) {
        this.tab = tab;
    }

    /**
     * Get the top-level Pane which contains all other graphical elements of this controller.
     *
     * @return {@link ScrollPane} object.
     */
    public ScrollPane getPane() {
        return this.contentScrollPane;
    }


    /**
     * This function returns a rotating list of colors for the digest highlights designed to be displayed on the
     * UCSC browser. If a segment is not selected, it returns "" (an emtpy string), which basically causes the corresponding
     * segment to show as not color-highlighted in the UCSC image or in the table.
     * @return a rotating list of colors for the digest highlights.
     */
    private String getNextColor() {
        String color = colors[this.idx];
        this.idx = (this.idx + 1) % (colors.length);
        return String.format("%%23%s", color);
    }


    /**
     * Zoom in or out with the UCSC display.
     * @param adjustment If we zoom in, factor is {@link #ZOOMFACTOR}; if we zoom out, factor is 1/{@link #ZOOMFACTOR};
     */
    private void zoom(double adjustment) {
        logger.trace(String.format("Before zoom (factor %.2f) start=%d end =%d",adjustment,viewpoint.getStartPos(),viewpoint.getEndPos() ));
        setZoomFactor(adjustment);
        logger.trace(String.format("After zoom start=%d end =%d",viewpoint.getStartPos(),viewpoint.getEndPos() ));
        showColoredSegmentsInTable();
        showUcscView();
    }

    /** Change the UCSC view by zooming in. */
    @FXML private void zoomIn() { zoom(1 / ZOOMFACTOR); }
    /** Change the UCSC view by zooming out. */
    @FXML private void zoomOut() { zoom(ZOOMFACTOR); }

    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
     * . */
    private String getHighlightRegions() {
        String genome = this.gopherService.getGenomeBuild();
        String chromosome = this.viewpoint.getReferenceID();
        List<String> colorsegmentlist = coloredsegments.stream().
                filter(ColoredSegment::isSelected).
                map( c -> String.format("%s.%s%%3A%d-%d%s",
                        genome,
                        chromosome,
                        c.segment.getStartPos(),
                        c.segment.getEndPos(),
                        c.getColor()) ).
                collect(Collectors.toList());
        String highlightregions=colorsegmentlist.stream().collect( Collectors.joining( "%7C" ) );
        return String.format("highlight=%s", highlightregions);
    }

    /**
     * Container for binding Segment
     */
    private static class ColoredSegment {
        /** Color for highlighting an active segment. */
        private final String color;

        private final Segment segment;

        private final CheckBox checkBox;

        private ChangeListener<Boolean> changeListener;

        ColoredSegment(Segment segment, String color) {
            this.segment = segment;
            this.color = color;
            this.checkBox = new CheckBox();
            this.changeListener = null;
        }

        CheckBox getCheckBox() {
            return checkBox;
        }

        String getColor() {
            if (this.segment.isSelected())
                return color;
            else
                return null;

        }

        Segment getSegment() {
            return segment;
        }

        boolean isSelected() {
            return segment.isSelected();
        }

        @Override
        public String toString() {
            return "ColoredSegment{color='" +
                    color +
                    "', segment=" +
                    segment +
                    "}";
        }

        public ChangeListener<Boolean> getChangeListener() { return changeListener; }

        public void setChangeListener(ChangeListener<Boolean> changeListener) { this.changeListener = changeListener; }
    }





}
