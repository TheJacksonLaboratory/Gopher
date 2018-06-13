package gopher.gui.taskprogressbar;

import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.apache.log4j.Logger;
import gopher.framework.Signal;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Presenter for the Viewpoint creation progress bar.
 */
public class TaskProgressBarPresenter implements Initializable {
    static Logger logger = Logger.getLogger(TaskProgressBarPresenter.class.getName());
    @FXML private Label titleLabel;
    @FXML private Label currentVPlabel;
    @FXML private ProgressBar vpProgressBar;

    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void initBindings(Task task, StringProperty sp) {
        this.vpProgressBar.progressProperty().unbind();
        this.vpProgressBar.progressProperty().bind(task.progressProperty());
        currentVPlabel.textProperty().bind(sp);
    }

    public void setTitle(String t) { this.titleLabel.setText(t);}

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

    public void closeWindow(){
        signal.accept(Signal.DONE);
    }

}
