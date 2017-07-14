package vpvgui.gui.analysisPane;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
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
        TableColumn<VPRow,Boolean> actionCol = new TableColumn<>("View");
        actionCol.setSortable(false);
        actionCol.setCellValueFactory(new PropertyValueFactory<>("DUMMY"));
        // define a simple boolean cell value for the action column so that the column will only be shown for non-empty rows.
        actionCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<VPRow, Boolean>, ObservableValue<Boolean>>() {
            @Override public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<VPRow, Boolean> features) {
                return new SimpleBooleanProperty(features.getValue() != null);
            }
        });
        // create a cell value factory with an add button for each row in the table.
        actionCol.setCellFactory(new Callback<TableColumn<VPRow, Boolean>, TableCell<VPRow, Boolean>>() {
            @Override public TableCell<VPRow, Boolean> call(TableColumn<VPRow, Boolean> personBooleanTableColumn) {
                return new AddPersonCell(null, tview);
            }
        });



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


    /** A table cell containing a button for adding a new person. */
    private class AddPersonCell extends TableCell<VPRow, Boolean> {
        // a button for adding a new person.
        final Button addButton = new Button("Add");
        // pads and centers the add button in the cell.
        final StackPane paddedButton = new StackPane();
        // records the y pos of the last button press so that the add person dialog can be shown next to the cell.
        final DoubleProperty buttonY = new SimpleDoubleProperty();

        /**
         * AddPersonCell constructor
         *
         * @param stage the stage in which the table is placed.
         * @param table the table to which a new person can be added.
         */
        AddPersonCell(final Stage stage, final TableView table) {
            paddedButton.setPadding(new Insets(3));
            paddedButton.getChildren().add(addButton);
            addButton.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    buttonY.set(mouseEvent.getScreenY());
                }
            });
            addButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    showAddPersonDialog(stage, table, buttonY.get());
                    table.getSelectionModel().select(getTableRow().getIndex());
                }
            });
        }
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
