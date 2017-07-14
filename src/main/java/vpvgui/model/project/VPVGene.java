package vpvgui.model.project;

import java.util.*;

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
    private Set<Integer> positions;

    private List<ViewPoint> viewPointList;



    public VPVGene(String geneid, String symbol) {
        try {
            this.entrezGeneID=Integer.parseInt(geneid);
        } catch (NumberFormatException e) {
            this.entrezGeneID=0;
        }
        this.geneSymbol=symbol;
        this.viewPointList=new ArrayList<>();
        this.positions =new HashSet<>();
    }
    /** @return a sorted list of TSS. */
    public List<Integer> getTSSlist() {
        List<Integer> lst = new ArrayList<>();
        lst.addAll(this.positions);
        Collections.sort(lst);
        return lst;
    }

    public void setChromosome(String c) {
        this.referenceSequenceID=c;
    }
    public String getChromosome() { return this.referenceSequenceID; }

    public String getGeneSymbol() { return this.geneSymbol;}
    public Integer getGeneID() { return this.entrezGeneID;}
    /** Set this VPVGene to be on the forward strand */
    public  void setForwardStrand() {
        this.forward=true;
    }
    /** Set this VPVGene to be on the reverse strand */
    public void setReverseStrand() {
        this.forward=false;
    }
    /** Ich würde gern diese Funktion rausnehmen und die ViewPoints anders erzeugen! */
    @Deprecated
    public void addViewPoint(ViewPoint vp) {
        if (this.positions.contains(vp.getGenomicPos())) {
            return;
        }
        this.positions.add(vp.getGenomicPos());
        this.viewPointList.add(vp);
    }

    /** This function adds a position (such as a transcription start site) that is the
     * "central" or "important" position around which we want to construct a ViewPoint.
     * @param pos
     */
    public void addGenomicPosition(int pos) {
        this.positions.add(pos);
    }

    /** Dumps the information about the VPVGene and its viewpoints for debugging. */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        String strand="-";
        if (forward) {
            strand="+";
        }
        sb.append(String.format("%s [%s,%s]",geneSymbol,referenceSequenceID,strand));
        if (this.viewPointList==null || this.viewPointList.size()==0) {
           // no-op
        } else {
            for (ViewPoint vp : this.viewPointList) {
                sb.append("\n\tViewPoint: "+vp);
            }
        }
        for (Integer ii : positions) {
            sb.append("\n\tTSS pos: "+ ii);
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
