package vpvgui.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by robinp on 5/11/17.
 * This is a window into which the user can paste a list of Gene symbols or Entrez ID that are
 * separated by comma, semicolon or are on separate lines.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.1 (May 11, 2017)
 */
public class CopyPasteGenesWindow {

    private static TextArea area;

    private static String [] targetgenes=null;

    public static String [] display() {

        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Target Genes");
        window.setMinWidth(250);
        Label label = new Label();
        label.setText("Target gene symbols");

        area = new TextArea();
        area.setPrefRowCount(40);
        area.setPrefColumnCount(100);
        area.setWrapText(true);
        area.setPrefWidth(150);
        area.setStyle(".scroll-pane{" +
                "-fx-vbar-policy:always; " +
                "-fx-hbar-policy:always; " +
                "}");

        Button button = new Button("Accept");
        button.setOnAction(e -> processText(area.getText()));


        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(10, 20, 10, 20));

        VBox layout = new VBox(10);

        layout.getChildren().addAll(label, area, button);
        layout.setAlignment(Pos.CENTER);
        bp.setCenter(layout);
        Scene scene = new Scene(bp);
        window.setScene(scene);
        window.showAndWait();

        return targetgenes;
    }


    private static void processText(String text) {
        targetgenes = text.split("\\s+");
    }


    public static void paste() {
        Clipboard systemClipboard = Clipboard.getSystemClipboard();
        if( !systemClipboard.hasContent(DataFormat.PLAIN_TEXT) ) {
            return;
        }

        String clipboardText = systemClipboard.getString();
        area.setText(clipboardText);
        area.positionCaret( 0 );
    }


}
