package gopher.gui.help;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import gopher.framework.Signal;


import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by peterrobinson on 7/3/17.
 */
public class HelpPresenter implements Initializable {

    @FXML
    WebView wview;

    private WebEngine webEngine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = wview.getEngine();
        webEngine.setUserDataDirectory(new File(gopher.io.Platform.getWebEngineUserDataDirectory(), getClass().getCanonicalName()));
    }

    public void setData(String html) {

        webEngine.loadContent(html);
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue<? extends String> ov, final String oldLoc, final String loc) {
                if (!loc.contains("google.com")) {
                    Platform.runLater(() -> webEngine.load(oldLoc)); // new Runnable
                }
            }
        });
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
