package vpvgui.gui.viewpointpanel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.project.Segment;
import vpvgui.model.project.ViewPoint;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class acts as a controller of the TabPanes which display individual ViewPoints. Created by peter on 16.07.17.
 */
public class ViewPointPresenter implements Initializable {

    private static final Logger logger = Logger.getLogger(ViewPointPresenter.class.getName());

    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>View Point Viewer</h3><p><i>Connecting to UCSC " +
            "Browser to visualized view point...</i></p></body></html>";

    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private static final int offset = 200;

    final private static String colors[] = {"F08080", "ABEBC6", "FFA07A", "C39BD3", "F7DC6F"};

    /**
     * This is the top-level Pane which contains all other graphical elements of this controller.
     */
    @FXML
    private ScrollPane contentScrollPane;

    /**
     * The graphical element where the UCSC browser content is displayed.
     */
    @FXML
    private WebView ucscContentWebView;

    /**
     * The backend behind the UCSC browser content. The non-visual object capable of managing one Web page at a time.
     */
    private WebEngine ucscWebEngine;

    /**
     * Observable list of ViewPoints (entries of {@link #viewPointsTableView}), the backend behind the TableView.
     */
    private ObservableList<ViewPoint> viewPoints;

    /**
     * The features of {@link ViewPoint} are presented in this TableView.
     */
    @FXML
    private TableView<ViewPoint> viewPointsTableView;

    @FXML
    private TableColumn<ViewPoint, CheckBoxTableCell<Boolean, ViewPoint>> isSelectedTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> refSeqIdTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> targetIdTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> genomicPosTableColumn;

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

    @FXML
    void closeButtonAction() {
        tab.getTabPane().getTabs().remove(tab);
    }

    @FXML
    void refreshUCSCButtonAction() {
//        TODO - refresh UCSC action here
    }

    @FXML
    void saveButtonAction() {
//        TODO - save action here
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewPoints = FXCollections.observableArrayList();
        ucscWebEngine = ucscContentWebView.getEngine();
        ucscWebEngine.loadContent(INITIAL_HTML_CONTENT);
        /* The following line is needed to avoid a SSL handshake alert
         * when opening the UCSC Browser.
         */
        System.setProperty("jsse.enableSNIExtension", "false");


    }

    public void setModel(Model m) {
        this.model = m;
    }

    /**
     * Set the ViewPoint that will be presented. Loads UCSC view.
     * @param vp {@link ViewPoint} which will be presented.
     */
    public void setViewPoint(ViewPoint vp) {
        this.vp = vp;
        setURL(generateURL());

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
     * TODO needs more customization!
     *
     * @return
     */
    private String generateURL() {
        String genome = this.model.getGenomeBuild();
        if (genome.startsWith("UCSC-"))
            genome = genome.substring(5);
        int posFrom, posTo;
        posFrom = vp.getStartPos() - offset;
        posTo = vp.getEndPos() + offset;
        String chrom = vp.getReferenceID();
        if (!chrom.startsWith("chr"))
            chrom = "chr" + chrom; /* TODO MAKE THIS ROBUST! */
        String targetItem = vp.getTargetName();
        String highlights = getHighlightRegions(genome, chrom);
        String url = String.format("http://genome.ucsc.edu/cgi-bin/hgRenderTracks?db=%s&position=%s%%3A%d-%d&hgFind.matches=%s&%s&pix=1400", genome, chrom, posFrom, posTo, targetItem, highlights);
        System.out.println(url);
        return url;
    }


    /** @return something like this highlight=<DB>.<CHROM>:<START>-<END>#<COLOR> for the active fragments. */
    private String getHighlightRegions(String db, String chrom) {
        StringBuilder sb = new StringBuilder();
        List<Segment> seglst = this.vp.getActiveSegments();
        logger.trace("getHighlightRegions: got number Of Active segments " + seglst.size());
        sb.append("highlight=");
        int i = 0;
        // highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>
        for (Segment s : seglst) {
            Integer start = s.getStartPos();
            Integer end = s.getEndPos();
            String color = getNextColor();
            String part = String.format("%s.%s%%3A%d-%d%s", db, chrom, start, end, color);
            if (i > 0) {
                sb.append("%7C");
            } else {
                i = 1;
            }
            sb.append(part);
        }

        return sb.toString();
    }

    /** @return a rotating list of colors for the fragment highlights */
    private String getNextColor() {
        String color = colors[this.coloridx];
        this.coloridx = (this.coloridx + 1) % (colors.length);
        return String.format("%%23%s", color);
    }
}
