package vpvgui.model;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class will represent Genes according to the way we are analyzing them in VPV.
 * The main purpose of the class is to keep track of the gene symbols and all of the
 * independent transcription start sites that are derived from the UCSC RefGenes file.
 * <p> Note that the numbering system used internally by this class is one-based, fully closed. The
 * transcription start sites recorded in the set {@link #positions} is thus one-based and corresponds to
 * the positions seen in the UCSC browser.
 * @author Peter Robinson
 * @version 0.1.3 (2017-10-02)
 */
public class VPVGene implements Comparable<VPVGene>, Serializable {
    /** serialization version ID */
    static final long serialVersionUID = 2L;
    /** An NCBI RefSeq id such as NM_001353311. */
    private String refSeqID =null;
    /** A gene symbolsuch as IGSF11 */
    private String geneSymbol=null;
    /** The contig where the RefSeq lives, e.g., chr8.*/
    private String contigID =null;
    /** Is this a plus strand gene? */
    private boolean forward;
    /** The transcript start site positions that correspond to this gene. We use a TreeSet to avoid duplicates and keep
     * the entries sorted in ascending order.   */
    private TreeSet<Integer> positions;

    /**
     * @param geneid The RefSeq id (e.g., NM_12345)
     * @param symbol The official gene symbol
     */
    public VPVGene(String geneid, String symbol) {
        this.refSeqID =geneid;
        this.geneSymbol=symbol;
        this.positions =new TreeSet<>();
    }
    /** @return a list of TSS sorted in ascending order with one-based numbering. */
    public List<Integer> getTSSlist() {
        return new ArrayList<>(positions);
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

    /** This function adds a position (such as a transcription start site) that is the
     * "central" or "important" position around which we want to construct a ViewPoint.
     * @param pos Position on the reference sequence (usually a chromosome) given in one-based numbering
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
        if (this.positions ==null || this.positions.size()==0) {
           // no-op
        } else {
            String posstring = positions.stream().map(String::valueOf).collect(Collectors.joining(";"));
            sb.append("-TSS pos: " + posstring);
        }
        return sb.toString();
    }
    
    public String getContigID() {
        return contigID;
    }

    public boolean isForward() {
        return forward;
    }

    public int n_viewpointstarts() {
        if (this.positions ==null) return 0;
        else return positions.size();
    }

    @Override
    public int compareTo(VPVGene other) {
        return other.positions.first() - this.positions.first();
    }
}
