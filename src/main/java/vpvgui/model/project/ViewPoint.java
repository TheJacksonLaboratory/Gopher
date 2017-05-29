package vpvgui.model.project;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

/**
 * A region, usually at the transcription start site of a gene,
 * that will be enriched in a capture C experiment. It is flanked by
 * restriction sites.
 */
public class ViewPoint {
    /** Usually a chromosome */
    private StringProperty referenceSequenceID;

    private IntegerProperty startPos, endPos;

    public ViewPoint() {

    }
}
