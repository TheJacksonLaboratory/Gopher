package gopher.gui.factories;

import gopher.service.model.dialog.RestrictionEnzymeResult;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static javafx.application.Platform.runLater;

public class DeleteProjectFilesFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteProjectFilesFactory.class.getName());

    /**
     *
     * @param projects Project files that the user can delete
     */
    public static void dispay(List<ProjectFile> projects) {
        LOGGER.trace("Presenting project files for possible deletion");
        Dialog<RestrictionEnzymeResult> dialog = new Dialog<>();
        dialog.setTitle("Project Files");
        dialog.setHeaderText("Delete unwanted project files");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        VBox vbox = new VBox();
        vbox.setMinWidth(400);
        TableView<ProjectFile> tableView = new TableView<>();
        TableColumn<ProjectFile, Button> deleteButtonColumn = new TableColumn<>();
        TableColumn<ProjectFile, String> projectFileColumn  = new TableColumn<>();
        projectFileColumn.setSortable(false);
        projectFileColumn.setEditable(false);
        projectFileColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getProjectName()));
        deleteButtonColumn.setCellValueFactory((cdf -> {
            ProjectFile pf = cdf.getValue();
            Button btn = new Button("Delete");
            btn.setOnAction(e -> {
                pf.deleteFile();
                tableView.getItems().removeAll(pf);
            });
            // wrap it so it can be displayed in the TableView
            return new ReadOnlyObjectWrapper<>(btn);
        }));
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.getColumns().addAll(projectFileColumn, deleteButtonColumn);
        tableView.getItems().clear();
        tableView.getItems().addAll(projects);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        vbox.getChildren().add(tableView);
        dialogPane.setContent(vbox);
        runLater(tableView::requestFocus);
        dialog.showAndWait();
    }

}
