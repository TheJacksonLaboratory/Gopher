package vpvgui.gui.proxy;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.converter.NumberStringConverter;
import vpvgui.framework.Signal;


import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SetProxyPresenter implements Initializable {

    @FXML
    private Button acceptButton;
    @FXML
    private TextField proxyTextField;
    @FXML private TextField portTextField;
    private Consumer<Signal> signal;
    @FXML private Label proxyLabel;

    private Tooltip ttip;

    private IntegerProperty portProperty= new SimpleIntegerProperty();
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
        this.portTextField.textProperty().addListener(new ChangeListener<String>(){
            @Override public void changed(ObservableValue<? extends String> observable,
                String oldValue, String newValue) {
            validateInteger(portTextField);
        }
        });

        this.proxyTextField.textProperty().bindBidirectional(proxyProperty);
        this.proxyTextField.textProperty().addListener(new ChangeListener<String>(){
            @Override public void changed(ObservableValue<? extends String> observable,
                                          String oldValue, String newValue) {
                validateHttpUrl(proxyTextField);
            }

        });
        this.ttip= new Tooltip();
        ttip.setText("Do not enter \"http://\" or \"https://\" here");
        this.proxyLabel.setTooltip(ttip);
        this.proxyTextField.setTooltip(ttip);

    }

    /** This function adds a red border to the Port text field if the user enters something
     * that does not parse as an Integer.
     * @param tf
     */
    private void validateInteger(TextField tf) {
        ObservableList<String> styleClass = tf.getStyleClass();
        boolean invalidInteger=false;
        try {
            String num=tf.getText();
            Integer i = Integer.parseInt(num);
        } catch (NumberFormatException E){
            invalidInteger=true;
        }
        if (invalidInteger) {
            if (! styleClass.contains("error")) {
                styleClass.add("error");
            }
        } else {
            // remove all occurrences:
            styleClass.removeAll(Collections.singleton("error"));
        }
    }
    /** This function adds a red border to the Proxy text field if the user enters something
     * that does not look like a URL. ToDo -- maybe we should turn red only if the user leaves the text field.
     * @param tf
     */
    private void validateHttpUrl(TextField tf) {
        ObservableList<String> styleClass = tf.getStyleClass();
        boolean invalidUrl=false;
        String url=tf.getText().trim();
        int i=url.indexOf("http://");
        if (i>=0)
            invalidUrl=true;
        /* now look for at least one "." in the URL */
        i=url.indexOf(".",i+1);
        if (i<0)
            invalidUrl=true;

        if (invalidUrl) {
            if (! styleClass.contains("error")) {
                styleClass.add("error");
            }
        } else {
            // remove all occurrences:
            styleClass.removeAll(Collections.singleton("error"));
        }

    }



    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    @FXML public void acceptProxy(ActionEvent e) {
        System.out.println("proxy" + getProxy()+ ", port="+getPort());
        signal.accept(Signal.DONE);
    }
}
