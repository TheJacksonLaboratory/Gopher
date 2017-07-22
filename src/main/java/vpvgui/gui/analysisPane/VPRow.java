package vpvgui.gui.analysisPane;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import vpvgui.model.Model;
import vpvgui.model.project.Segment;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.util.List;

/**
 * Created by peterrobinson on 7/11/17.
 * This class represents a ViewPoint and acts as a wrapper to put ViewPoint objects into the Table.
 */
public class VPRow {

    private ViewPoint viewpoint;
    /** The target is the target of a viewpoint, often a gene. Note that multiple viewpoints
     * can have the same target, e.g., if the same gene has multiple transcription start sites.*/
    private StringProperty targetName = new SimpleStringProperty("");
    private StringProperty refseqID = new SimpleStringProperty("");
    private IntegerProperty genomicPos = new SimpleIntegerProperty();
    private Model model;
    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private int offset=200;

    final private static String colors[]={"F08080","ABEBC6","FFA07A","C39BD3","F7DC6F"};
    private int coloridx=0;

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

    /** @return something like this highlight=<DB>.<CHROM>:<START>-<END>#<COLOR> for the active fragments. */
    private String getHighlightRegions(String db,String chrom) {
        StringBuilder sb = new StringBuilder();
        List<Segment> seglst = this.viewpoint.getActiveSegments();
        sb.append("highlight=");
        int i=0;
       // highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>
        for (Segment s:seglst) {
            Integer start=s.getStartPos();
            Integer end=s.getEndPos();
            String color = getNextColor();
            String part=String.format("%s.%s%%3A%d-%d%s",db,chrom,start,end,color);
            if (i>0) {
                sb.append("%7C");
            } else {
                i=1;
            }
            sb.append(part);
        }

        return sb.toString();
    }
    /** @return a rotating list of colors for the fragment highlights */
    private String getNextColor() {
       String color=this.colors[this.coloridx];
       this.coloridx=(this.coloridx+1)%(this.colors.length);
       return String.format("%%23%s",color);
    }

    /**
     * TODO needs more customization!
     * @return
     */
    public String getURL() {
        String genome=this.model.getGenomeBuild();
        if (genome.startsWith("UCSC-"))
            genome=genome.substring(5);
        int posFrom,posTo;
        posFrom = this.viewpoint.getStartPos()-offset;
        posTo=this.viewpoint.getEndPos()+offset;
        String chrom=viewpoint.getReferenceID();
        if (! chrom.startsWith("chr"))
            chrom="chr"+chrom; /* TODO MAKE THIS ROBUST! */
        String targetItem=viewpoint.getTargetName();
        String highlights=getHighlightRegions(genome,chrom);
        String url = String.format("http://genome.ucsc.edu/cgi-bin/hgRenderTracks?db=%s&position=%s%%3A%d-%d&hgFind.matches=%s&%s&pix=1400",genome,chrom,posFrom,posTo,targetItem,highlights);
        System.out.println(url);
        return url;
    }
}
