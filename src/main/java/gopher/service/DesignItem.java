package gopher.service;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DesignItem {
    private final StringProperty key;
    private final StringProperty value;

    public DesignItem() {
        key = new SimpleStringProperty(this, "key");
        value = new SimpleStringProperty(this, "value");
    }

    public DesignItem(String k, String v) {
        this();
        setKey(k);
        setValue(v);
    }

    private void setKey(String k) {
        key.set(k);
    }

    private void setValue(String v) {
        value.set(v);
    }

    public StringProperty keyProperty() {
        return key;
    }

    public String getKey() {
        return key.get();
    }

    public StringProperty valueProperty() {
        return value;
    }

    public String getValue() {
        return value.get();
    }


}
