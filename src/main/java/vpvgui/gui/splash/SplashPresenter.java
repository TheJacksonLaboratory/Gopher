package vpvgui.gui.splash;

import javafx.fxml.Initializable;
import vpvgui.framework.Signal;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;


/**
 * This will show the startup screen that will include a button for creating a new project as well as
 * a list of buttons for opening previous projects.
 */
public class SplashPresenter implements Initializable {


    private Consumer<Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // no-op, we need to receive data via setData
    }

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

}
