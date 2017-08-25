package vpvgui.gui.help;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import vpvgui.framework.Signal;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by peterrobinson on 7/3/17.
 */
public class HelpPresenter implements Initializable {

    @FXML
    WebView wview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // no op
    }

    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }

    private Consumer<Signal> signal;

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    @FXML
    public void closeWindow(ActionEvent e) {
        e.consume();
        //signal.accept(Signal.DONE);
    }
}
