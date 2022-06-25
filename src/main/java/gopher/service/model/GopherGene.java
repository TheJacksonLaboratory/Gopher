package gopher.service.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class GopherGene implements Comparable<GopherGene>, Serializable {
    static Logger logger = LoggerFactory.getLogger(GopherGene.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 3L;
    /** An NCBI RefSeq id such as NM_001353311. */
    private final String refSeqID;
    /** A gene symbol such as IGSF11 */
    private final String geneSymbol;
    /** The contig where the RefSeq lives, e.g., chr8.*/
    private final String contigID;
    /** Is this a plus strand gene? */
    private final boolean forward;
    /** Is this a non-coding gene? */
    private final boolean noncoding;
    /** The transcript start site positions that correspond to this gene. We use a TreeSet to avoid duplicates and keep
     * the entries sorted in ascending order.   */
    private final TreeSet<Integer> positions;

    /**
     * @param geneid The RefSeq id (e.g., NM_12345)
     * @param symbol The official gene symbol
     */
    public GopherGene(String geneid, String symbol,boolean isNoncoding, String contig, String strand) {
        this.refSeqID =geneid;
        this.geneSymbol=symbol;
        this.noncoding=isNoncoding;
        this.contigID=contig;
        if (strand.equals("+")) forward=true;
        else if (strand.equals("-")) forward=false;
        else {
            logger.error("[ERROR] did not recognize strand \""+ strand + "\"");
            forward=false; // this never happens
        }
        this.positions =new TreeSet<>();
    }
    /** @return a list of TSS sorted in ascending order with one-based numbering. */
    public List<Integer> getTSSlist() {
        return new ArrayList<>(positions);
    }


    public String getChromosome() { return this.contigID; }
    public String getGeneSymbol() { return this.geneSymbol;}
    public String getRefSeqID() { return this.refSeqID;}
   public boolean isCoding() { return !noncoding;}
   public boolean isNonCoding() { return noncoding; }

    /** This function adds a position (such as a transcription start site) that is the
     * "central" or "important" position around which we want to construct a ViewPoint.
     * @param pos Position on the reference sequence (usually a chromosome) given in one-based numbering
     */
    public void addGenomicPosition(int pos) {
        this.positions.add(pos);
    }

    /** Dumps the information about the GopherGene and its viewpoints for debugging. */
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
            sb.append("-TSS pos: ").append(posstring);
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
    public int compareTo(GopherGene other) {
        return other.positions.first() - this.positions.first();
    }
}
