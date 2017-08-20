package vpvgui.gui.analysisPane;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import vpvgui.gui.viewpointpanel.ViewPointPresenter;
import vpvgui.gui.viewpointpanel.ViewPointView;
import vpvgui.model.Model;
import vpvgui.model.project.ViewPoint;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * Created by peterrobinson on 7/6/17.
 */
public class VPAnalysisPresenter implements Initializable {
    static Logger logger = Logger.getLogger(VPAnalysisPresenter.class.getName());
    /** This is the message users will see if they open the analysis tab before they have entered the genes
     * and started the analysis of the viewpoints. */
    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>View Point Viewer</h3><p>Please set up and " +
            "initialize analysis using the Set Up Tab.</p></body></html>";

    @FXML
    private WebView contentWebView;

    private WebEngine contentWebEngine;

    @FXML
    private TableView<ViewPoint> viewPointTableView;

    @FXML
    private TableColumn<ViewPoint, Button> actionTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> targetTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> genomicLocationColumn;

    //@FXML
    //private TableColumn<ViewPoint, String> genPositionTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> nSelectedTableColumn;

    @FXML private TableColumn<ViewPoint,String> viewpointScoreColumn;


    private BooleanProperty editingStarted;

    private Model model;
    /** A reference to the main TabPane of the GUI. We will add new tabs to this that will show viewpoints in the
     * UCSC browser.*/
    private TabPane tabpane;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.setProperty("jsse.enableSNIExtension", "false");
        init();
    }


    public void init() {
        this.contentWebEngine = contentWebView.getEngine();
        this.contentWebEngine.loadContent(INITIAL_HTML_CONTENT);
        initTable();

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
    /**
     * Class for sorting items like chr3:4325 and chrY:762
     */
    class GenomicLocationComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (!s1.startsWith("chr"))
                return 0;
            if (!s2.startsWith("chr"))
                return 0; /* should never happen */
            logger.trace("s1 before=" + s1);
            s1 = s1.substring(3);
            s2 = s2.substring(3);
            logger.trace("s1 after=" + s1);
            String chromStr1, chromStr2;
            Integer chr1, chr2;
            Integer pos1, pos2;
            try {
                int i = s1.indexOf(":");
                if (i > 0) {
                    chromStr1 = s1.substring(0, i);
                    pos1=Integer.parseInt(s1.substring(1+i));
                    if (chromStr1.equals("X")){
                        chr1=100;
                    } else if (chromStr1.equals("Y")) {
                        chr1 = 101;
                    } else if (chromStr1.startsWith("M")){
                        chr1=102;
                    } else {
                        chr1=Integer.parseInt(chromStr1);
                    }
                } else {
                    return 0;
                }
                i = s2.indexOf(":");
                if (i > 0) {
                    chromStr2 = s2.substring(0, i);
                    pos2=Integer.parseInt(s2.substring(1+i));
                    if (chromStr2.equals("X")){
                        chr2=100;
                    } else if (chromStr2.equals("Y")) {
                        chr2 = 101;
                    } else if (chromStr2.startsWith("M")){
                        chr2=102;
                    } else {
                        chr2=Integer.parseInt(chromStr2);
                    }
                } else {
                    return 0;
                }
                if (! chr1.equals(chr2)) {
                    return chr1.compareTo(chr2);
                } else {
                    return pos1.compareTo(pos2);
                }
            } catch (Exception e) {
                logger.error(String.format("Error encounted while sorting chromosome locations %s and %s", s1, s2));
                logger.error(e, e);
                return 0;
            }
        }
    }

    /**
     * Set up the table that will show the ViewPoints.
     */
    private void initTable() {
        // The first column with buttons that open new tab for the ViewPoint
        actionTableColumn.setSortable(false);
        actionTableColumn.setCellValueFactory((cdf -> {
            ViewPoint vp = cdf.getValue();
            // create Button here & set the action
            Button btn = new Button("Show viewpoint");
            btn.setOnAction(e -> {
                logger.trace(String.format("Adding tab for row with Target: %s, Chromosome: %s, Genomic pos: %d n selected %d ",
                        vp.getTargetName(), vp.getReferenceID(), vp.getGenomicPos(),vp.getActiveSegments().size()));
                openViewPointInTab(vp);
            });
            // wrap it so it can be displayed in the TableView
            return new ReadOnlyObjectWrapper<>(btn);
        }));

        // the second column
        targetTableColumn.setSortable(true);
        targetTableColumn.setEditable(false);

        targetTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getTargetName()));


        // the third column--position, e.g.,chr4:622712
        genomicLocationColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getGenomicLocationString()));
        genomicLocationColumn.setComparator(new GenomicLocationComparator());


        // fourth column--number of selected fragments
        nSelectedTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getActiveSegments().size())));


        // fifth column--score of fragments.
        viewpointScoreColumn.setCellValueFactory(cdf-> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getScoreAsPercentString())));
        viewpointScoreColumn.setComparator(new PercentComparator());
    }


    /**
     * This method creates a new {@link Tab} populated with a viewpoint!
     */
    private void openViewPointInTab(ViewPoint vp) {
        final Tab tab = new Tab("Viewpoint: " + vp.getTargetName());
        tab.setId(vp.getTargetName());
        tab.setClosable(true);
        tab.setOnClosed(event -> {
            if (tabpane.getTabs()
                    .size() == 2) {
                event.consume();
            }
        });

        ViewPointView view = new ViewPointView();
        ViewPointPresenter presenter = (ViewPointPresenter) view.getPresenter();
        presenter.setModel(this.model);
        presenter.setTab(tab);
        presenter.setViewPoint(vp);
        tab.setContent(presenter.getPane());

        this.tabpane.getTabs().add(tab);
        this.tabpane.getSelectionModel().select(tab);
    }

    public void setModel(Model m) { this.model=m; }

    public void setTabPaneRef(TabPane tabp) {
        this.tabpane=tabp;
    }

    /** This method gets called right after the user has created a set of Viewpoints. It should
     * show the table and also present a summary of the quality of the set of viewpoints in the
     * upper half of the tab.
     */
    public void showVPTable() {
        if (! this.model.viewpointsInitialized()) {
            System.out.println("[View Points not initialized");
            return;
        }

        // update WebView with N loaded ViewPoints
        ViewPointAnalysisSummaryHTMLGenerator htmlgen = new ViewPointAnalysisSummaryHTMLGenerator(model);
        contentWebEngine.loadContent(htmlgen.getHTML());

        ObservableList<ViewPoint> viewpointlist = FXCollections.observableArrayList(); /* todo Do we need this? */
        if (model==null) {
            System.err.println("[ERROR] VPAnalysisPresenter -- model null, should never happen" );
            return;
        }
        List<ViewPoint> vpl = this.model.getViewPointList();
        logger.trace("In showVPTable: got a total of "+vpl.size() + " VPVGenes");
        for (ViewPoint v : vpl) {
            viewpointlist.add(v);
        }
        viewPointTableView.getItems().addAll(vpl);
        viewPointTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }


    private TableColumn createTextColumn(String name, String caption) {
        TableColumn column = new TableColumn(caption);
        //appendEditListeners(column);
        column.setCellValueFactory(new PropertyValueFactory<ViewPoint, String>(name));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        return column;
    }

}
