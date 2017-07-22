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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import vpvgui.gui.viewpointpanel.ViewPointPresenter;
import vpvgui.gui.viewpointpanel.ViewPointView;
import vpvgui.model.Model;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by peterrobinson on 7/6/17.
 */
public class VPAnalysisPresenter implements Initializable {

    @FXML
    private WebView wview;

    @FXML
    private TableView tview;

    @FXML
    private AnchorPane pane;

    private BooleanProperty editingStarted;

    private Model model;
    /** A reference to the main TabPane of the GUI. We will add new tabs to this that will show viewpoints in the
     * UCSC browser.
     */
    private TabPane tabpane;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setInitialWebView();
        /* The following line is needed to avoid a SSL handshake alert
         * when opening the UCSC Browser.
         */
        System.setProperty("jsse.enableSNIExtension", "false");
        initTable();
    }

    public void setInitialWebView() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>View Point Viewer</h3>");
        sb.append("<p>Please set up and initialize analysis using the Set Up Tab.</p>");
        sb.append("</body></html>");
        setData(sb.toString());
    }

    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }

    public void setModel(Model m) { this.model=m; }

    public void setTabPaneRef(TabPane tabp) {
        this.tabpane=tabp;
    }

    public AnchorPane getPane() { return this.pane; }


    public void showVPTable() {
        System.out.println("ShowVPTable");
        if (! this.model.viewpointsInitialized()) {
            System.out.println("[View Points not initialized");
            return;
        }
        updateWebview();
        ObservableList<VPRow> viewpointlist = FXCollections.observableArrayList();
        if (model==null) {
            System.err.println("[ERROR] VPAnalysisPresenter -- model null, should never happen" );
            return;
        }
        List<ViewPoint> vpl = this.model.getViewPointList();
        for (ViewPoint v : vpl) {
            viewpointlist.add(new VPRow(v,this.model));
        }
        tview.setItems(viewpointlist);
        tview.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        //this.tview.getChildren().clear();
        //this.tview.getChildren().add(tview);
    }

    private void updateWebview() {
        List<ViewPoint> vplist=this.model.getViewPointList();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>View Point Viewer</h3>");
        sb.append(String.format("<p>Number of viewpoints: %d.</p>",vplist.size()));
        sb.append("</body></html>");
        setData(sb.toString());
    }

    /**
     * Set up the table that will show the ViewPoints. Note that tview is constructed by fxml, do not call new.
     */
    private void initTable() {

        ObservableList columns = tview.getColumns();
        tview.setEditable(false);
        TableColumn<VPRow,Button> actionCol = new TableColumn<>("View");
        actionCol.setSortable(false);
        actionCol.setCellValueFactory(new PropertyValueFactory<>("DUMMY"));
        // this CellValueFactory generates a Button in column of the TableView
        actionCol.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<VPRow, Button>, ObservableValue<Button>>() {
                    @Override
                    public ObservableValue<Button> call(TableColumn.CellDataFeatures<VPRow, Button> features) {

                        // access the properties of the row where the Button is placed
                        VPRow row = features.getValue();

                        // access the properties of the column where the Button is placed
                        TableColumn<VPRow, Button> column = features.getTableColumn();

                        // Turn Button (or any other object) into ObservableValue that must be returned by this Callback
                        ReadOnlyObjectProperty<Button> btnWrapper = new ReadOnlyObjectWrapper<>(new Button("Hey ya!"));

                        // set action, e.g. creating a new tab
                        btnWrapper.get().setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                System.out.println(String.format("Hi there from the row with Target: %s, Chromosome: %s, Genomic pos: %d ",
                                        row.getTargetName(), row.getRefseqID(), row.getGenomicPos()));
                                addTabPane(row);
                            }
                        });
                        return btnWrapper;
                    }
                }
        );

        // create a cell value factory with an add button for each row in the table.



        columns.add(actionCol);
        final TableColumn<VPRow,String> targetnamecol = createTextColumn("targetName", "Target");
        targetnamecol.setMinWidth(60);
        targetnamecol.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent<VPRow, String>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<VPRow, String> event) {
                        ((VPRow) event.getTableView().getItems().get(event.getTablePosition().getRow())).setTargetName(event.getNewValue());
                    }
                }
        );
        columns.add(targetnamecol);
        final TableColumn<VPRow,String> refseqcol = createTextColumn("refseqID", "Chromosome");
        refseqcol.setMinWidth(60);
        refseqcol.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent<VPRow, String>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<VPRow, String> event) {
                        ((VPRow) event.getTableView().getItems().get(event.getTablePosition().getRow())).setRefseqID(event.getNewValue());
                    }
                }
        );
        columns.add(refseqcol);
        final TableColumn<VPRow,Integer> genomicposcol = new TableColumn<>("Genomic Position");
        genomicposcol.setMinWidth(60);
        genomicposcol.setCellValueFactory(new PropertyValueFactory<VPRow,Integer>("genomicPos"));
        //?? genomicposcol.setCellFactory(TextFieldTableCell.forTableColumn());
        genomicposcol.setOnEditCommit(
                new EventHandler<TableColumn.CellEditEvent<VPRow, Integer>>() {
                    @Override
                    public void handle(TableColumn.CellEditEvent<VPRow, Integer> event) {
                        Integer val = event.getNewValue();
                        ((VPRow) event.getTableView().getItems().get(event.getTablePosition().getRow())).setGenomicPos(val);
                    }
                }
        );
        columns.add(genomicposcol);

    }

    private void addTabPane(VPRow row) {
        final Tab tab = new Tab("Tab " + row.getTargetName());
        tab.setClosable(true);
        tab.setOnClosed(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if (tabpane.getTabs()
                        .size() == 2) {
                    event.consume();
                }
            }
        });

        ViewPointView vpv = new ViewPointView();
        ViewPointPresenter vpp= (ViewPointPresenter) vpv.getPresenter();
        vpp.setModel(this.model);
        //vpp.setTabPaneRef(this.tabpane);
        vpp.setVPRow(row);
        vpp.sendToUCSC();
        tab.setContent(vpp.getPane());


        this.tabpane.getTabs().add(tab);
        this.tabpane.getSelectionModel().select(tab);
    }



        private void showAddPersonDialog(Stage parent, final TableView<VPRow> table, double y) {
            System.err.println("TEST SHOW ADD PERSON");
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
