package gopher.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

public class Utils {


    /**
     *
     * @param proxy http/https proxy to be set
     * @param port corresponding port.
     */
    public static void setSystemProxyAndPort(String proxy,String port) {
        System.setProperty("http.proxyHost",proxy);
        System.setProperty("http.proxyPort",port);
        System.setProperty("https.proxyHost",proxy);
        System.setProperty("https.proxyPort",port);
    }

    /**
     * Allow column name to be wrapped into multiple lines. Based on
     * <a href="https://stackoverflow.com/questions/10952111/javafx-2-0-table-with-multiline-table-header">this
     * post</a>.
     *
     * @param col {@link TableColumn} with a name that will be wrapped
     */
    public static <T, U> void  makeHeaderWrappable(TableColumn<T, U> col) {
        Label label = new Label(col.getText());
        label.setStyle("-fx-padding: 8px;");
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);

        StackPane stack = new StackPane();
        stack.getChildren().add(label);
        stack.prefWidthProperty().bind(col.widthProperty().subtract(5));
        label.prefWidthProperty().bind(stack.prefWidthProperty());
        col.setGraphic(stack);
    }

}
