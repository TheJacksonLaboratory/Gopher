package gopher.controllers;

import gopher.service.model.viewpoint.Segment;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.CheckBox;

/**
 * Container for binding Segment
 */
class ColoredSegment {
    /** Color for highlighting an active segment. */
    private final String color;

    private final Segment segment;

    private final CheckBox checkBox;

    private ChangeListener<Boolean> changeListener;

    ColoredSegment(Segment segment, String color) {
        this.segment = segment;
        this.color = color;
        this.checkBox = new CheckBox();
        this.changeListener = null;
    }

    CheckBox getCheckBox() {
        return checkBox;
    }

    String getColor() {
        if (this.segment.isSelected())
            return color;
        else
            return null;

    }

    Segment getSegment() {
        return segment;
    }

    boolean isSelected() {
        return segment.isSelected();
    }

    @Override
    public String toString() {
        return "ColoredSegment{color='" +
                color +
                "', segment=" +
                segment +
                "}";
    }

    public ChangeListener<Boolean> getChangeListener() { return changeListener; }

    public void setChangeListener(ChangeListener<Boolean> changeListener) { this.changeListener = changeListener; }
}



