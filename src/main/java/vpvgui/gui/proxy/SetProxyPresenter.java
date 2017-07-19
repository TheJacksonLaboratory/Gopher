package vpvgui.gui.proxy;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.util.converter.NumberStringConverter;
import vpvgui.framework.Signal;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SetProxyPresenter implements Initializable {

    @FXML
    private Button acceptButton;
    @FXML
    private TextField proxyTextField;
    @FXML private TextField portTextField;
    private Consumer<Signal> signal;

    private IntegerProperty portProperty= new SimpleIntegerProperty(-1);
    public IntegerProperty portProperty() { return portProperty;  }
    public int getPort() {return portProperty.get();}
    public void setPort(Integer i) { this.portProperty.setValue(i);}
    private StringProperty proxyProperty = new SimpleStringProperty(this,"proxyProperty");
    public StringProperty proxyProperty() {  return proxyProperty;  }
    public String getProxy() { return proxyProperty.getValue();}
    public void setProxyProperty(String p) { this.proxyProperty.setValue(p);}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.portTextField.textProperty().bindBidirectional(portProperty,new NumberStringConverter());
        this.proxyTextField.textProperty().bindBidirectional(proxyProperty);
    }





    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    @FXML public void acceptProxy(ActionEvent e) {

    }
}
