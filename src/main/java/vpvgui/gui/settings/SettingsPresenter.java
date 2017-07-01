package vpvgui.gui.settings;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import vpvgui.framework.Signal;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by peter on 01.07.17.
 */
public class SettingsPresenter  implements Initializable {

    private static final String RED = "-fx-fill: red; -fx-font-weight: bold";
    private static final String BLACK = "-fx-fill: black";
    /**
     * WebView will show the annotated text with HPO terms in color
     */
    @FXML
    private WebView wview;

    @FXML
    private Button okButon;

    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // no-op, we need to receive data via setData
    }

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

    public void setData(String html) {
        if (wview==null) {
            System.err.println("wview is null");
            System.exit(1);
        }
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }

    public void okButtonClicked(ActionEvent e){
        e.consume();
        signal.accept(Signal.DONE);
    }
}
