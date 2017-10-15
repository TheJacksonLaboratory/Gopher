package vpvgui.gui.deletepane.delete;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import vpvgui.framework.Signal;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.analysisPane.VPAnalysisPresenter;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class DeletePresenter implements Initializable {
    @FXML
    Label deleteLabel;
    @FXML Label undoneLabel;

    @FXML
    private TableView<ProjectFile> tableView;

    @FXML
    private TableColumn<ProjectFile, Button> deleteButtonColumn;

    @FXML
    private TableColumn<ProjectFile, String> projectFileColumn;

    @FXML
    private Button closeButton;

    @FXML
    private AnchorPane apane;



    private static int count;

    private static List<File> chosen=null;

    private static Map<String,File> filemap;

    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //
        initTable();
    }




    private void initTable() {
        // The first column with buttons that open new tab for the ViewPoint
        deleteButtonColumn.setSortable(false);
        deleteButtonColumn.setCellValueFactory((cdf -> {
            ProjectFile pf = cdf.getValue();
            // create Button here & set the action
            Button btn = new Button("Delete");
            btn.setOnAction(e -> {
                pf.deleteFile();
                tableView.getItems().removeAll(pf);
            });
            // wrap it so it can be displayed in the TableView
            return new ReadOnlyObjectWrapper<>(btn);
        }));


        // the second column
        projectFileColumn.setSortable(false);
        projectFileColumn.setEditable(false);

        projectFileColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getProjectName()));
    }

    public void updateTable(List<ProjectFile> projects) {
        if (projects==null || projects.isEmpty()) {
            apane.getChildren().remove(tableView);
            apane.getChildren().remove(deleteLabel);
            apane.getChildren().remove(undoneLabel);
            Label cannot = new Label("Cannot delete active project. No other project files found.");
            apane.getChildren().add(cannot );
            cannot.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 10 20 10 20;");
            return;
        }
        tableView.getItems().clear();
        tableView.getItems().addAll(projects);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }




    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    @FXML public void closeButtonClicked(javafx.event.ActionEvent e){
        e.consume();
        signal.accept(Signal.DONE);
    }




}
