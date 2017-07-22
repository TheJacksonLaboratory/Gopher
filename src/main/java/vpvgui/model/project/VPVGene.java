package vpvgui.model.project;

import java.util.*;

/**
 * This class will represent Genes according to the way we are analyzing them in VPV.
 * One of the target genes for which we want to design capture C probes
 */
public class VPVGene {
    /** An NCBI RefSeq id such as NM_001353311. */
    private String refSeqID =null;
    /** A gene symbolsuch as IGSF11 */
    private String geneSymbol=null;
    /** The contig where the RefSeq lives, e.g., chr8.*/
    private String contigID =null;
    /** Is this a plus strand gene? */
    private boolean forward;
    /** this will keep count of the transcript start site position we have already seen
     * in order to avoid entering duplicate ViewPoint objects.
     * The set then has all TSS (unique)
     */
    private Set<Integer> positions;
    /** Remove this, we do not need it followiing refactor. */
   @Deprecated  private List<ViewPoint> viewPointList;



    public VPVGene(String geneid, String symbol) {
        this.refSeqID =geneid;
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
        this.contigID =c;
    }
    public String getChromosome() { return this.contigID; }

    public String getGeneSymbol() { return this.geneSymbol;}
    public String getRefSeqID() { return this.refSeqID;}
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
        sb.append(String.format("%s [%s,%s]",geneSymbol, contigID,strand));
        if (this.positions==null || this.positions.size()==0) {
           // no-op
        } else {
            for (Integer ii : positions) {
                sb.append("\n\tTSS pos: " + ii);
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public List<ViewPoint> getviewPointList(){
        return viewPointList;
    }
    
    public String getContigID() {
        return contigID;
    }

    public boolean isForward() {
        return forward;
    }

    public int n_viewpointstarts() {
        if (this.positions==null) return 0;
        else return this.positions.size();
    }
}
