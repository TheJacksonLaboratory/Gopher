package vpvgui.gui.analysisPane;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import org.apache.log4j.Logger;
import vpvgui.gui.viewpointpanel.ViewPointPresenter;
import vpvgui.gui.viewpointpanel.ViewPointView;
import vpvgui.model.Model;
import vpvgui.model.project.ViewPoint;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * TODO - do we need to update entries in the TableView? Are they read-only?
 * Created by peterrobinson on 7/6/17.
 */
public class VPAnalysisPresenter implements Initializable {

    static Logger logger = Logger.getLogger(VPAnalysisPresenter.class.getName());

    private static final String INITIAL_HTML_CONTENT = "<html><body><h3>View Point Viewer</h3><p>Please set up and " +
            "initialize analysis using the Set Up Tab.</p></body></html>";

    private static final String UPDATE_HTML_CONTENT = "<html><body><h3>View Point Viewer</h3><p>Number of viewpoints:" +
            " %d.</p></body></html>";



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
    private TableColumn<ViewPoint, String> refSeqIdTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> genPositionTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> nSelectedTableColumn;



    private BooleanProperty editingStarted;

    private Model model;
    /** A reference to the main TabPane of the GUI. We will add new tabs to this that will show viewpoints in the
     * UCSC browser.
     */
    private TabPane tabpane;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.setProperty("jsse.enableSNIExtension", "false");
        contentWebEngine = contentWebView.getEngine();
        contentWebEngine.loadContent(INITIAL_HTML_CONTENT);

        initTable();
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
        // TODO - do we need editable table?
        targetTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getTargetName()));
        targetTableColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow())
                .setTargetName(e.getNewValue()));

        // the third column
        refSeqIdTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getReferenceID()));
        refSeqIdTableColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow())
                .setReferenceID(e.getNewValue()));

        // the fourth column
        genPositionTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue()
                .getGenomicPos())));
        genPositionTableColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow())
                .setGenomicPos(Integer.parseInt(e.getNewValue())));

        // fifth column--number of selected fragments
        nSelectedTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getActiveSegments().size())));
        nSelectedTableColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow())
                .setReferenceID(e.getNewValue()));
    }


    /**
     * This method creates a new {@link Tab} populated with
     */
    private void openViewPointInTab(ViewPoint vp) {
        final Tab tab = new Tab("Viewpoint: " + vp.getTargetName());
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


    public void showVPTable() {
        if (! this.model.viewpointsInitialized()) {
            System.out.println("[View Points not initialized");
            return;
        }

        // update WebView with N loaded ViewPoints
        contentWebEngine.loadContent(String.format(UPDATE_HTML_CONTENT, model.getViewPointList().size()));

        ObservableList<VPRow> viewpointlist = FXCollections.observableArrayList();
        if (model==null) {
            System.err.println("[ERROR] VPAnalysisPresenter -- model null, should never happen" );
            return;
        }
        List<ViewPoint> vpl = this.model.getViewPointList();
        logger.trace("In showVPTable: got a total of "+vpl.size() + " VPVGenes");
        for (ViewPoint v : vpl) {
            viewpointlist.add(new VPRow(v,this.model));
        }
        viewPointTableView.getItems().addAll(vpl);
        viewPointTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }


    private TableColumn createTextColumn(String name, String caption) {
        TableColumn column = new TableColumn(caption);
        appendEditListeners(column);
        column.setCellValueFactory(new PropertyValueFactory<VPRow, String>(name));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        return column;
    }

    private void appendEditListeners(TableColumn column) {
        column.setOnEditStart(new EventHandler() {
            @Override
            public void handle(Event t) {
                editingStarted.set(true);
            }
        });
        column.setOnEditCancel(new EventHandler() {
            @Override
            public void handle(Event t) {
                editingStarted.set(false);
            }
        });

    }
}
