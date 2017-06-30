package vpvgui.model.project;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import javafx.beans.property.StringProperty;

import java.util.List;

/**
 * This class will represent Genes according to the way we are analyzing them in VPV.
 * One of the target genes for which we want to design capture C probes
 */
public class VPVGene {

    private Integer entrezGeneID=null;

    private String geneSymbol=null;

    private String chromosome=null;

    private boolean forward;

    private List<ViewPoint> viewPointList;

    public VPVGene(String geneid, String symbol) {
        this.entrezGeneID=Integer.parseInt(geneid);
        this.geneSymbol=symbol;
    }

    public void setChromosome(String c) {
        this.chromosome=c;
    }

    /** Transform a Jannovar TranscriptModel to a VPVGene. */
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

    private void setForwardStrand() {
        this.forward=true;
    }

    private void setReverseStrand() {
        this.forward=false;
    }
}
