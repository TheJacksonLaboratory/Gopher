package gopher.gui.taskprogressbar;

import gopher.framework.Signal;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Presenter for the Viewpoint creation progress bar.
 */
public class TaskProgressBarPresenter implements Initializable {
    static Logger logger = LoggerFactory.getLogger(TaskProgressBarPresenter.class.getName());
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private ProgressBar progressBar;

    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public StringProperty titleProperty() {
        return titleLabel.textProperty();
    }

    public StringProperty messageProperty() {
        return messageLabel.textProperty();
    }

    public DoubleProperty progressProperty() {
        return progressBar.progressProperty();
    }

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

    public void cancelButtonAction() {
        signal.accept(Signal.CANCEL);
    }
}
