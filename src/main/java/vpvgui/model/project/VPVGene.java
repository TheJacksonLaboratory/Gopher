package vpvgui.model.project;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class will represent Genes according to the way we are analyzing them in VPV.
 * One of the target genes for which we want to design capture C probes
 */
public class VPVGene {

    private Integer entrezGeneID=null;

    private String geneSymbol=null;

    private String referenceSequenceID=null;

    private boolean forward;
    /** this will keep count of the transcript start site position we have already seen
     * in order to avoid entering duplicate ViewPoint objects.
     * The set then has all TSS (unique)
     */
    private Set<Integer> seenPositions;

    private List<ViewPoint> viewPointList;

    private List<Integer> tssList;

    public VPVGene(String geneid, String symbol) {
        try {
            this.entrezGeneID=Integer.parseInt(geneid);
        } catch (NumberFormatException e) {
            this.entrezGeneID=0;
        }
        this.geneSymbol=symbol;
        this.viewPointList=new ArrayList<>();
        this.tssList=new ArrayList<>();
        this.seenPositions=new HashSet<>();
    }

    public List<Integer> getTSSlist() {
        List<Integer> lst = new ArrayList<>();
        lst.addAll(this.seenPositions);
        return lst;
    }

    public void setChromosome(String c) {
        this.referenceSequenceID=c;
    }
    public String getChromosome() { return this.referenceSequenceID; }

    public String getGeneSymbol() { return this.geneSymbol;}
    public Integer getGeneID() { return this.entrezGeneID;}

    /** Transform a Jannovar TranscriptModel to a VPVGene.
     * TODO -- referenceSequenceIDn stimmen fuer Maus nicht*/
    public static VPVGene geneFactory(TranscriptModel tmod) {
        VPVGene vpvg = new VPVGene(tmod.getGeneID(),tmod.getGeneSymbol());
        int c =tmod.getChr();
        if (c>0 && c<23) {
            vpvg.setChromosome(String.format("chr%d",c));
        } else if (c==23) {
            vpvg.setChromosome("chrX");
        } else if (c==24) {
            vpvg.setChromosome("chrY");
        } else if (c==25) {
            vpvg.setChromosome("chrM");
        }
        if (tmod.getStrand().isForward()) {
            vpvg.setForwardStrand();
        } else {
            vpvg.setReverseStrand();
        }
        return null;
    }

    public  void setForwardStrand() {
        this.forward=true;
    }

    public void setReverseStrand() {
        this.forward=false;
    }

    public void addViewPoint(ViewPoint vp) {
        if (this.seenPositions.contains(vp.getGenomicPos())) {
            return;
        }
        this.seenPositions.add(vp.getGenomicPos());
        this.viewPointList.add(vp);
    }

    /** Dumps the information about the VPVGene and itsviewpoints for debugging. */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        String strand="-";
        if (forward) {
            strand="+";
        }
        sb.append(String.format("%s [%s,%s]",geneSymbol,referenceSequenceID,strand));
        if (this.viewPointList==null || this.viewPointList.size()==0) {
            sb.append("\nError: No View points");
        } else {
            for (ViewPoint vp : this.viewPointList) {
                sb.append("\n\tViewPoint: "+vp);
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public List<ViewPoint> getviewPointList(){
        return viewPointList;
    }
    
    public String getReferenceSequenceID() {
        return  referenceSequenceID;
    }

}
