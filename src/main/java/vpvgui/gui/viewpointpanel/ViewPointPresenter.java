package vpvgui.gui.viewpointpanel;

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
import vpvgui.model.Model;
import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static vpvgui.util.Utils.join;

/**
 * This class acts as a controller of the TabPanes which display individual ViewPoints. Created by peter on 16.07.17.
 */
public class ViewPointPresenter implements Initializable {

    private static final Logger logger = Logger.getLogger(ViewPointPresenter.class.getName());

    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>View Point Viewer</h3><p><i>Connecting to UCSC " +
            "Browser to visualized view point...</i></p></body></html>";

    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private static final int OFFSET = 250;

    private static final int UCSC_WIDTH = 1600;

    private final static String colors[] = {"F08080", "ABEBC6", "FFA07A", "C39BD3", "F7DC6F", "8D230F", "A1D6E2",
            "EC96A4", "E6DF44", "E4EA8C"};

    /** The top-level Pane which contains all other graphical elements of this controller.*/
    @FXML
    private ScrollPane contentScrollPane;

    /** The graphical element where the UCSC browser content is displayed.*/
    @FXML
    private WebView ucscContentWebView;
    /** This will be dynamically set to the name of the gene and the score of the viewpoint. TODO also set name of promoter */
    @FXML private Label viewpointScoreLabel;
    /** This is use to mediate between {@link #viewpointScoreLabel} and the ViewPoint object and score . */
    private StringProperty vpScoreProperty;
    /** The backend behind the UCSC browser content. */
    private WebEngine ucscWebEngine;
    /**  Individual {@link Segment}s of {@link ViewPoint} are presented in this TableView.   */
    @FXML
    private TableView<ColoredSegment> segmentsTableView;

    @FXML
    private TableColumn<ColoredSegment, String> colorTableColumn;

    @FXML
    private TableColumn<ColoredSegment, CheckBox> isSelectedTableColumn;

    @FXML
    private TableColumn<ColoredSegment, String> locationTableColumn;

    @FXML
    private TableColumn<ColoredSegment, String> inRepetitiveTableColumn;
    @FXML private TableColumn<ColoredSegment, String> repeatContentUp;
    @FXML private TableColumn<ColoredSegment, String> repeatContentDown;

    @FXML private Button deleteButton;
    @FXML private Button copyToClipboardButton;

    /**
     * Reference to the {@link Tab} where this content is placed.
     */
    private Tab tab;

    private Model model;

    /**
     * Instance of {@link ViewPoint} presented by this presenter.
     */
    private ViewPoint vp;

    private int coloridx = 0;
    /** This is a kind of wrapper for the segments that keeps track of how they should be colored in the UCSC view as
     * well as in the table.
     */
    private List<ColoredSegment> coloredsegments;



    @FXML
    void closeButtonAction() {
        tab.getTabPane().getTabs().remove(tab);
    }

    @FXML private void copyToClipboard(Event e) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        URLMaker urlmaker = new URLMaker(this.model.getGenomeBuild());
        String url= urlmaker.getURL(vp,getHighlightRegions());
        content.putString(url);
        clipboard.setContent(content);
        e.consume();
    }

    @FXML private void deleteThisViewPoint(Event e) {
        logger.trace("TODO CODE TO DELETE THIS VIEWPOINT GO BACK TO MODEL");
       // e.g., this.model.deleteThisViewPoint(this.vp);
        // e.g., analysisTab->refreshTable()
        // e.g., close this viewpoint Tab

    }

    @FXML
    void refreshUCSCButtonAction() {
        URLMaker urlmaker = new URLMaker(this.model.getGenomeBuild());
        for (ColoredSegment cseg : coloredsegments) {
            logger.trace("Color segment for " + cseg.segment.toString() +", which is selected?="+cseg.segment.isSelected());
        }
        String url= urlmaker.getImageURL(vp,getHighlightRegions());
        logger.trace(String.format("Refresh: %s",url));

        StackPane sproot = new StackPane();
        final ProgressIndicator progress = new ProgressIndicator(); // or you can use ImageView with animated gif instead
        this.ucscWebEngine = ucscContentWebView.getEngine();
        this.ucscWebEngine.load(url);

        // updating progress bar using binding
        //progress.progressProperty().bind(this.ucscWebEngine.getLoadWorker().progressProperty()); TODO not working
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
         * when opening the UCSC Browser.
         */
        System.setProperty("jsse.enableSNIExtension", "false");

        // This is a hack when by using dummy column a color for the cell's TableRow is set.
        colorTableColumn.setCellFactory(col -> new TableCell<ColoredSegment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    getTableRow().setStyle(String.format("-fx-background-color: #%s;", item.substring(3)));
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
                    cdf.getValue().getSegment().setSelected(new_val); /* changes the selected value of the Segment */
                    Platform.runLater(new Runnable() {
                        @Override public void run() {
                            updateScore();
                        }
                    });

                }
            });
            return new ReadOnlyObjectWrapper<>(cdf.getValue().getCheckBox()); // the same checkbox
        });

        locationTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getSegment()
                .getChromosomalPositionString())));
        locationTableColumn.setComparator(new FormattedChromosomeComparator());
        inRepetitiveTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue()
                .getSegment().getRepeatContentAsPercent())));
        inRepetitiveTableColumn.setComparator(new PercentComparator());




        repeatContentUp.setCellValueFactory(cdf ->new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getRepeatContentMarginUpAsPercent())));
        repeatContentUp.setComparator(new PercentComparator());
        repeatContentDown.setCellValueFactory(cdf ->new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().
                getSegment().getRepeatContentMarginDownAsPercent())));
        repeatContentDown.setComparator(new PercentComparator());
        vpScoreProperty=new SimpleStringProperty();
        viewpointScoreLabel.textProperty().bindBidirectional(vpScoreProperty);
        /* the following will start us off with a different color each time. */
        this.coloridx = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, colors.length);
    }

    private void updateScore() {
        this.vp.calculateViewpointScore();
        this.vpScoreProperty.setValue(String.format("%s - Score: %.2f%%",vp.getTargetName(),100*vp.getScore()));
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
        this.vpScoreProperty.setValue(String.format("%s - Score: %.2f%%",vp.getTargetName(),100*vp.getScore()));
        // generate Colored segments - Segment paired with some color.
        this.coloredsegments = vp.getActiveSegments().stream()
                .map(s -> new ColoredSegment(s, getNextColor()))
                .collect(Collectors.toList());
        segmentsTableView.getItems().addAll(coloredsegments);

        // prepare url parts
        String genome = this.model.getGenomeBuild();
        if (genome.startsWith("UCSC-"))
            genome = genome.substring(5);

        // create url & load content from UCSC
        URLMaker maker = new URLMaker(genome);
        String re=this.model.getRestrictionEnzymeString().replaceAll("^","");
        maker.setEnzyme(re);
        String url= maker.getImageURL(vp,getHighlightRegions());
        logger.trace(String.format("INITIAL: %s",url));
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


    /** @return a rotating list of colors for the fragment highlights */
    private String getNextColor() {
        String color = colors[this.coloridx];
        this.coloridx = (this.coloridx + 1) % (colors.length);
        return String.format("%%23%s", color);
    }




    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
     * . */
    private String getHighlightRegions() {
        String genome = this.model.getGenomeBuild();
        String chromosome=this.vp.getReferenceID();
        List<String> colorsegmentlist=new ArrayList<>();
        for (ColoredSegment cseg: coloredsegments) {
           Segment s=cseg.segment;
            Integer start = s.getStartPos();
            Integer end = s.getEndPos();
            String color = cseg.getColor();
            // highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>
            if (color!=null) { /* Not able to get UCSC to show light gray */
                String part = String.format("%s.%s%%3A%d-%d%s", genome, chromosome, start, end, color);
                colorsegmentlist.add(part);
            }
        }
        return String.format("highlight=%s", join(colorsegmentlist,"%7C"));
    }

    /**
     * Container for binding Segment
     */
    private class ColoredSegment {

        private String color;

        private final static String VERYLIGHTGREY="E8E8EE";

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
            return checkBox.isSelected();
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
