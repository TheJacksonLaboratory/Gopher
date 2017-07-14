package vpvgui.gui.analysisPane;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private BooleanProperty editingStarted;

    private Model model;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setInitialWebView();
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
            viewpointlist.add(new VPRow(v));
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
        TableColumn actionCol = new TableColumn("View");
        actionCol.setCellValueFactory(new PropertyValueFactory<>("DUMMY"));

        Callback<TableColumn<VPRow, String>, TableCell<VPRow, String>> cellFactory
                = //
                new Callback<TableColumn<VPRow, String>, TableCell<VPRow, String>>() {
                    @Override
                    public TableCell call(final TableColumn<VPRow, String> param) {
                        final TableCell<VPRow, String> cell = new TableCell<VPRow, String>() {
                            final Button btn = new Button("View in UCSC");
                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                    setText(null);
                                } else {
                                    btn.setOnAction(event -> {
                                        VPRow vpr = getTableView().getItems().get(getIndex());
                                        System.out.println(vpr.getGenomicPos()
                                                + "   " + vpr.getRefseqID());
                                    });
                                    setGraphic(btn);
                                    setText(null);
                                }
                            }
                        };
                        return cell;
                    }
                };
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
