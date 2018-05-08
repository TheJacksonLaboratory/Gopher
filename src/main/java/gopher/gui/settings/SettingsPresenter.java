package gopher.gui.settings;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import gopher.framework.Signal;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by peter on 01.07.17.
 */
public class SettingsPresenter  implements Initializable {

    /**
     * WebView will show the settings text
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
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }

    public void okButtonClicked(ActionEvent e){
        e.consume();
        signal.accept(Signal.DONE);
    }
}
