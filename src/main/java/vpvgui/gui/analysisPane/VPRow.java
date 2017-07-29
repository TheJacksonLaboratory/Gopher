package vpvgui.gui.analysisPane;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.project.Segment;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.util.List;

/**
 * Created by peterrobinson on 7/11/17.
 * This class represents a ViewPoint and acts as a wrapper to put ViewPoint objects into the Table.
 */
@Deprecated
public class VPRow {
    static Logger logger = Logger.getLogger(VPRow.class.getName());
    private ViewPoint viewpoint;
    /** The target is the target of a viewpoint, often a gene. Note that multiple viewpoints
     * can have the same target, e.g., if the same gene has multiple transcription start sites.*/
    private StringProperty targetName = new SimpleStringProperty("");
    private StringProperty refseqID = new SimpleStringProperty("");
    private IntegerProperty genomicPos = new SimpleIntegerProperty();
    private IntegerProperty nSelected=new SimpleIntegerProperty();
    private Model model;



    private VPRow(){
        /* do not allow default CTOR*/
    }

    public VPRow(ViewPoint vp, Model model) {
        viewpoint=vp;
        this.model=model;
        this.refseqID.set(vp.getReferenceID());
        this.genomicPos.set(vp.getGenomicPos());
        this.targetName.set(vp.getTargetName());
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

    public String getTargetName() {
        return targetName.get();
    }
    public void setTargetName(String name) {
        this.targetName.set(name);
        this.viewpoint.setTargetName(name);
    }

    public Integer getNSelected() {
        return nSelected.get();
    }
    public void setnSelected(Integer n) {
        this.nSelected.set(n);
        //TODO this.viewpoint.setGenomicPos(n);
    }

}
