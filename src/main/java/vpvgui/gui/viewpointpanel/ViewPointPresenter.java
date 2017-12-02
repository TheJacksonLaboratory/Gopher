package vpvgui.gui.viewpointpanel;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
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
import org.apache.log4j.Logger;
import vpvgui.gui.analysisPane.VPAnalysisPresenter;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * This class acts as a controller of the TabPanes which confirmDialog individual ViewPoints.
 * @author Peter Robinson
 * @version 0.2.7 (2017-11-12)
 */
public class ViewPointPresenter implements Initializable {

    private static final Logger logger = Logger.getLogger(ViewPointPresenter.class.getName());

    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>View Point Viewer</h3><p><i>Connecting to UCSC " +
            "Browser to visualize view point...</i></p></body></html>";



    private final static String colors[] = {"F08080", "ABEBC6", "FFA07A", "C39BD3", "FEA6FF","F7DC6F", "CFFF98", "A1D6E2",
            "EC96A4", "E6DF44", "F76FDA","E4EA8C", "F1F76F", "FDD2D6", "F76F7F", "DAF7A6","FFC300" ,"F76FF5"};

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
    private static double ZOOMFACTOR =1.5;
    /**  Individual {@link Segment}s of {@link ViewPoint} are presented in this TableView.   */
    @FXML
    private TableView<ColoredSegment> segmentsTableView;
    @FXML private TableColumn<ColoredSegment, String> colorTableColumn;
    @FXML private TableColumn<ColoredSegment, CheckBox> isSelectedTableColumn;
    @FXML private TableColumn<ColoredSegment, String> locationTableColumn;
    @FXML private TableColumn<ColoredSegment, String> inRepetitiveTableColumn;
    @FXML private TableColumn<ColoredSegment, String> repeatContentUpColumn;
    @FXML private TableColumn<ColoredSegment, String> repeatContentDownColumn;
    @FXML private TableColumn<ColoredSegment, String> gcContentUpColumn;
    @FXML private TableColumn<ColoredSegment, String> gcContentDownColumn;
    @FXML private TableColumn<ColoredSegment, String> gcContentTableColumn;
    @FXML private TableColumn<ColoredSegment,String> segmentLengthColumn;

    @FXML private Button deleteButton;
    @FXML private Button copyToClipboardButton;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;


    /**
     * Reference to the {@link Tab} where this content is placed.
     */
    private Tab tab;

    private Model model;
    /** A link back to the analysis tab that allows us to refresh the statistics if the user deletes "this" ViewPoint.*/
    private VPAnalysisPresenter analysisPresenter=null;

    /**
     * Instance of {@link ViewPoint} presented by this presenter.
     */
    private ViewPoint vp;

    private int coloridx = 0;
    /** This is a kind of wrapper for the segments that keeps track of how they should be colored in the UCSC view as
     * well as in the table.
     */
    private List<ColoredSegment> coloredsegments;


    /** Remove the current tab from the App.  */
    @FXML void closeButtonAction() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                tab.getTabPane().getTabs().remove(tab);
            }
        });
    }

    /** Copy the UCSC URL to the clipboard (interactive instead of image only) */
    @FXML private void copyToClipboard(Event e) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        URLMaker urlmaker = new URLMaker(this.model);
        String url= urlmaker.getURL(vp,getHighlightRegions());
        content.putString(url);
        clipboard.setContent(content);
        e.consume();
    }

    @FXML private void deleteThisViewPoint(Event e) {
        this.model.deleteViewpoint(this.vp);
        this.tab.getTabPane().getTabs().remove(this.tab);
        this.analysisPresenter.refreshVPTable();
        e.consume();
    }

    @FXML void refreshUCSCButtonAction() {
        URLMaker urlmaker = new URLMaker(this.model);
        String url= urlmaker.getImageURL(vp,getHighlightRegions());
        StackPane sproot = new StackPane();
        final ProgressIndicator progress = new ProgressIndicator(); // or you can use ImageView with animated gif instead
        this.ucscWebEngine = ucscContentWebView.getEngine();
        this.ucscWebEngine.load(url);

        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        sproot.getChildren().addAll(progress);

        Scene scene = new Scene(sproot, 75, 75);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        this.ucscWebEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState) {
                        if (newState == Worker.State.SUCCEEDED) {
                            // hide progress bar then page is ready
                            progress.setVisible(false);
                            stage.close();
                        }
                    }
                });
    }

//  TODO - save action here
    @FXML
    void saveButtonAction() {
        // choose Segments that were selected by user.
        List<ColoredSegment> ss = segmentsTableView.getItems().stream()
                .filter(ColoredSegment::isSelected)
                .collect(Collectors
                        .toList());
        logger.trace(String.format("Selected segments: %s", ss.stream().map(ColoredSegment::toString).collect
                (Collectors.joining(","))));
    }

    public void setCallback(VPAnalysisPresenter vpAnalysisPresenter) {
        this.analysisPresenter=vpAnalysisPresenter;
    }

    /** class for sorting chromosome locations like chr5:43679423 */
    class FormattedChromosomeComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            Integer i1 = null;
            Integer i2 = null;
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
                logger.error(String.format("Error while sorting chromosome strings s1=%s s2=%s ", s1, s2));
                logger.error(e,e);
                return 0;
            }
        }
    }

    /** Class for sorting items like 2.3% and 34.5% */
    class PercentComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            int i=s1.indexOf("%");
            if (i>0)
                s1=s1.substring(0,i);
            i=s2.indexOf("%");
            if (i>0)
                s2=s2.substring(0,i);
            try {
                Double d1 = Double.parseDouble(s1);
                Double d2=Double.parseDouble(s2);
                return d1.compareTo(d2);
            } catch (Exception e) {
                logger.error(String.format("Error encounted while sorting percentage values %s and %s",s1,s2));
                logger.error(e,e);
                return 0;
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ucscWebEngine = ucscContentWebView.getEngine();
        ucscWebEngine.loadContent(INITIAL_HTML_CONTENT);
        /* The following line is needed to avoid an SSL handshake alert
         * when opening the UCSC Browser. */
        System.setProperty("jsse.enableSNIExtension", "false");
        // This is a hack when by using dummy column a color for the cell's TableRow is set.
        colorTableColumn.setCellFactory(col -> new TableCell<ColoredSegment, String>() {
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
            Segment segment = cdf.getValue().getSegment();
            CheckBox checkBox = cdf.getValue().getCheckBox();
            if (segment.isSelected()) // inspect state of the segment and initialize CheckBox state accordingly
                checkBox.setSelected(true);
            checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                public void changed(ObservableValue<? extends Boolean> ov,
                                    Boolean old_val, Boolean new_val) {
                    cdf.getValue().getSegment().setSelected(new_val); // changes the selected value of the Segment
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            updateScore();
                            refreshUCSCButtonAction();
                            colorTableColumn.setCellFactory(col -> new TableCell<ColoredSegment, String>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (item != null && !empty) {
                                        getTableRow().setStyle(String.format("-fx-background-color: #%s;", item.substring(3)));
                                    } else {
                                        getTableRow().setStyle(String.format("-fx-background-color: transparent;"));
                                    }
                                }
                            });
                        }
                    });

                }
            });
            return new ReadOnlyObjectWrapper<>(cdf.getValue().getCheckBox()); // the same checkbox
        });

        locationTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getSegment()
                .getChromosomalPositionString())));
        locationTableColumn.setComparator(new FormattedChromosomeComparator());

        segmentLengthColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getSegment().length())));
        segmentLengthColumn.setCellFactory( column -> new TableCell<ColoredSegment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item);
                    Integer len = Integer.parseInt(item);
                    if (len < model.getMinFragSize()) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        inRepetitiveTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue()
                .getSegment().getRepeatContentAsPercent())));
        inRepetitiveTableColumn.setComparator(new PercentComparator());
        // the following causes the contents of the repeat cell to be shown in red if the repeat threshold is surpassed.
        inRepetitiveTableColumn.setCellFactory(column -> new TableCell<ColoredSegment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty);
                if (item != null && !empty) {
                    setText(item);
                    double rp = 0.01 * ((item.endsWith("%")) ? Double.parseDouble(item.substring(0, item.length() -1)): Double.parseDouble(item));
                    if (rp > model.getMaxRepeatContent()) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        inRepetitiveTableColumn.setEditable(false);

        repeatContentUpColumn.setCellValueFactory(cdf ->new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getRepeatContentMarginUpAsPercent())));
        repeatContentUpColumn.setComparator(new PercentComparator());
        repeatContentUpColumn.setCellFactory(column -> new TableCell<ColoredSegment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty);
                if (item != null && !empty) {
                    setText(item);
                    double rp = 0.01 * ((item.endsWith("%")) ? Double.parseDouble(item.substring(0, item.length() -1)): Double.parseDouble(item));
                    if (rp > model.getMaxRepeatContent()) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        repeatContentDownColumn.setCellValueFactory(cdf ->new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getRepeatContentMarginDownAsPercent())));
        repeatContentDownColumn.setComparator(new PercentComparator());
        repeatContentDownColumn.setCellFactory(column -> new TableCell<ColoredSegment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty);
                if (item != null && !empty) {
                    setText(item);
                    double rp = 0.01 * ((item.endsWith("%")) ? Double.parseDouble(item.substring(0, item.length() -1)): Double.parseDouble(item));
                    if (rp > model.getMaxRepeatContent()) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        gcContentUpColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getGCcontentUpAsPercent())));
        gcContentUpColumn.setComparator(new PercentComparator());
        gcContentUpColumn.setCellFactory(column -> new TableCell<ColoredSegment, String>() {
            // this code highlights GC content that outside of GC boundaries set in 'Set up' pane
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) { // here, item is String like '40.00%'
                    setText(item);
                    double gc = 0.01 * ((item.endsWith("%")) ? Double.parseDouble(item.substring(0, item.length() -1)): Double.parseDouble(item));
                    if (gc < model.getMinGCcontent() || gc > model.getMaxGCcontent()) { // GC content is like 0.25
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        gcContentDownColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getGCcontentDownAsPercent())));
        gcContentDownColumn.setComparator(new PercentComparator());
        gcContentDownColumn.setCellFactory(column -> new TableCell<ColoredSegment, String>() {
            // this code highlights GC content that outside of GC boundaries set in 'Set up' pane
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) { // here, item is String like '40.00%'
                    setText(item);
                    double gc = 0.01 * ((item.endsWith("%")) ? Double.parseDouble(item.substring(0, item.length() -1)): Double.parseDouble(item));
                    if (gc < model.getMinGCcontent() || gc > model.getMaxGCcontent()) { // GC content is like 0.25
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });
        /*
        gcContentTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getGCcontentAsPercent())));
        gcContentTableColumn.setComparator(new PercentComparator());
        gcContentTableColumn.setCellFactory(column -> new TableCell<ColoredSegment, String>() {
            // this code highlights GC content that outside of GC boundaries set in 'Set up' pane
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) { // here, item is String like '40.00%'
                    setText(item);
                    double gc = 0.01 * ((item.endsWith("%")) ? Double.parseDouble(item.substring(0, item.length() -1)): Double.parseDouble(item));
                    if (gc < model.getMinGCcontent() || gc > model.getMaxGCcontent()) { // GC content is like 0.25
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal");
                    }
                }
            }
        });*/
        vpScoreProperty=new SimpleStringProperty();
        vpExplanationProperty=new SimpleStringProperty();
        viewpointScoreLabel.textProperty().bindBidirectional(vpScoreProperty);
        viewpointExplanationLabel.textProperty().bindBidirectional(vpExplanationProperty);
        this.segmentsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        /* the following will start us off with a different color each time. */
        this.coloridx = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, colors.length);
    }

    private void updateScore() {
        this.vp.calculateViewpointScore();
        this.vpScoreProperty.setValue(String.format("%s [%s] - Score: %.2f%% [%s], Length: %s",
                vp.getTargetName(),
                vp.getAccession(),
                100*vp.getScore(),
                vp.getGenomicLocationString(),
                vp.getTotalAndActiveLengthAsString()));
        if (vp.hasNoActiveSegment()) {
            this.vpExplanationProperty.setValue("No selected fragments");
        } else {
            int upstreamSpan=vp.getUpstreamSpan();
            int downstreamSpan=vp.getDownstreamSpan();
            int total = vp.getTotalPromoterCount();
            String promoterCount="";
            if (total==1) {
                promoterCount=String.format("Only promoter of %s gene",vp.getTargetName());
            } else {
                 promoterCount = String.format("Promoter %d of %d of %s gene",
                        vp.getPromoterNumber(),
                        total, vp.getTargetName());
            }
            this.vpExplanationProperty.setValue(String.format("Upstream: %d bp; Downstream: %d bp. %s (%s)",
                    upstreamSpan,downstreamSpan,promoterCount,vp.getStrandAsString()));
        }
    }

    public void setModel(Model m) {
        this.model = m;
    }

    /**
     * Set the ViewPoint that will be presented. Load UCSC view and populate tableview with ViewPoint segments.
     *
     * @param vp {@link ViewPoint} which will be presented.
     */
    public void setViewPoint(ViewPoint vp) {
        this.vp = vp;
        updateScore();
        this.coloredsegments = vp.getAllSegments().stream()
                .map(s -> new ColoredSegment(s, getNextColor(s.isSelected())))
                .collect(Collectors.toList());
        segmentsTableView.getItems().addAll(coloredsegments);

        // prepare url parts
        String genome = this.model.getGenomeBuild();
        // create url & load content from UCSC
        URLMaker maker = new URLMaker(this.model);
        String re=this.model.getFirstRestrictionEnzymeString().replaceAll("^","");
        String url= maker.getImageURL(vp,getHighlightRegions());
        ucscWebEngine.load(url);

    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    /**
     * Provide URL pointing to the genomic region of displayed ViewPoint in the UCSC browser. The URL will be loaded by
     * the WebEngine.
     *
     * @param url String with URL
     */
    public void setURL(String url) {
        ucscWebEngine.load(url);
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
     * This function returns a rotating list of colors for the fragment highlights designed to be displayed on the
     * UCSC browser. If a segment is not selected, it returns "" (an emtpy string), which basically causes the corresponding
     * segment to show as not color-highlighted in the UCSC image or in the table.
     * @return a rotating list of colors for the fragment highlights.
     */
    private String getNextColor(boolean isSelected) {
        String color = colors[this.coloridx];
        this.coloridx = (this.coloridx + 1) % (colors.length);
        return String.format("%%23%s", color);
    }




    private void zoom(double factor) {
        String path=this.model.getIndexFastaFilePath(vp.getReferenceID());
        try {
            IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(path));
            ViewPoint newVP = new ViewPoint(this.vp,factor,fastaReader);
            int maxSizeUp = (int) (vp.getUpstreamNucleotideLength() * factor);
            int maxSizeDown = (int) (vp.getDownstreamNucleotideLength() * factor);
            newVP.generateViewpointExtendedApproach(maxSizeUp,maxSizeDown,model.getAllowSingleMargin());
            segmentsTableView.getItems().clear();
            this.coloredsegments.clear();
            setViewPoint(newVP);
            refreshUCSCButtonAction();
            updateScore();
        } catch (FileNotFoundException e) {
            logger.error("Could not zoom for "+vp.getTargetName());
            logger.error(e,e);
        }
    }


    @FXML private void zoomIn() {
        if (ZOOMFACTOR ==0) {
            logger.error("Attempt to zoom in with LOGFACTOR zero");
            return;
        }
        zoom(1/ ZOOMFACTOR);
    }

    @FXML private void zoomOut() {
        if (ZOOMFACTOR ==0) {
            logger.error("Attempt to zoom out with LOGFACTOR zero");
            return;
        }
        zoom(ZOOMFACTOR);

    }

    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
     * . */
    private String getHighlightRegions() {
        String genome = this.model.getGenomeBuild();
        String chromosome = this.vp.getReferenceID();
        List<String> colorsegmentlist = coloredsegments.stream().
                filter(c -> c.isSelected()).
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
    private class ColoredSegment {
        /** Color for highlighting an active segment. */
        private String color;

        private Segment segment;

        private CheckBox checkBox;

        ColoredSegment(Segment segment, String color) {
            this.segment = segment;
            this.color = color;
            this.checkBox = new CheckBox();
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public String getColor() {
            if (this.segment.isSelected())
                return color;
            else
                return null;

        }

        public Segment getSegment() {
            return segment;
        }

        public boolean isSelected() {
            return segment.isSelected();
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ColoredSegment{");
            sb.append("color='").append(color).append('\'');
            sb.append(", segment=").append(segment);
            sb.append('}');
            return sb.toString();
        }
    }





}
