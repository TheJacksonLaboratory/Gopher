package gopher.gui.qcCheckPane;

import gopher.io.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import gopher.framework.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;


public class QCCheckPresenter implements Initializable {
    static Logger logger = LoggerFactory.getLogger(QCCheckPresenter.class.getName());
    @FXML
    private WebView wview;

    private WebEngine webEngine;

    @FXML private Button cancelButon;
    @FXML private Button continueButton;
    @FXML private Label warning;

    private boolean wasCanceled=true;

    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = wview.getEngine();
        webEngine.setUserDataDirectory(new File(Platform.getWebEngineUserDataDirectory(), getClass().getCanonicalName()));
    }

    public QCCheckPresenter() {

    }
    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

    public void setData(String html) {
        webEngine.loadContent(html);
    }

    @FXML public void cancelButtonClicked(ActionEvent e) {
        e.consume();
        wasCanceled=true;
        signal.accept(Signal.DONE);
    }

    @FXML public void continueButtonClicked(ActionEvent e) {
        e.consume();
        wasCanceled=false;
        signal.accept(Signal.DONE);
    }


    public boolean wasCanceled() { return  this.wasCanceled;}

    public void setLabel(String text) {
        warning.setStyle("-fx-text-alignment: right; -fx-font-size: 14pt; -fx-text-fill: red; ");
        warning.setText(text);
    }


}
