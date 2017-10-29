package vpvgui.gui.qcCheckPane;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.log4j.Logger;
import vpvgui.framework.Signal;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;


public class QCCheckPresenter implements Initializable {
    static Logger logger = Logger.getLogger(QCCheckPresenter.class.getName());
    @FXML
    private WebView wview;

    @FXML private Button cancelButon;
    @FXML private Button continueButton;

    private boolean wasCanceled=true;

    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public QCCheckPresenter() {

    }
    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
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


}
