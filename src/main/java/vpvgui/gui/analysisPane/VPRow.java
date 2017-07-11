package vpvgui.gui.analysisPane;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

/**
 * Created by peterrobinson on 7/11/17.
 * This class represents a ViewPoint and acts as a wrapper to put ViewPoint objects into the Table.
 */
public class VPRow {

    private ViewPoint viewpoint;

    private StringProperty refseqID = new SimpleStringProperty("");
    private IntegerProperty genomicPos = new SimpleIntegerProperty();

    private VPRow(){
        /* do not allow default CTOR*/
    }

    public VPRow(ViewPoint vp) {
        viewpoint=vp;
        this.refseqID.set(vp.getReferenceID());
        this.genomicPos.set(vp.getGenomicPos());
    }

    public String getRefseqID() {
        return refseqID.get();
    }
    public void setRefseqID(String id) {
        this.refseqID.set(id);
        this.viewpoint.setReferenceID(id);
    }

    public Integer getGenomicPos() {
        return genomicPos.get();
    }
    public void setGenomicPos(Integer pos) {
        this.genomicPos.set(pos);
        this.viewpoint.setGenomicPos(pos);
    }
}
