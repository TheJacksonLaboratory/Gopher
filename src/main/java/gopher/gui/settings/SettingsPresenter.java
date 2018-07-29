package gopher.gui.settings;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import gopher.framework.Signal;


import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Created by peter on 01.07.17.
 */
public class SettingsPresenter  implements Initializable {


    @FXML private ListView<String> lviewKey;
    @FXML private ListView<String> lviewValue;

    @FXML
    private Button closeButton;

    private Consumer<Signal> signal;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // no-op, we need to receive data via setData
    }

    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }



    public void closeButtonClicked(ActionEvent e){
        e.consume();
        signal.accept(Signal.DONE);
    }


    void setSettingsMap(Map<String,String> settings) {
        ObservableList<String> keys = FXCollections.observableArrayList(settings.keySet());
        ObservableList<String> values = FXCollections.observableArrayList(settings.values());
        lviewKey.setItems(keys);
        lviewValue.setItems(values);
    }

}
