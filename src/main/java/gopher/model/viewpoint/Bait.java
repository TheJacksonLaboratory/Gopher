package gopher.model.viewpoint;

import gopher.exception.GopherException;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class represents a bait or probe sequence that used for enrichment. The efficiency of a bait depends on
 * GC content, alignability and melting temperature. Upon initialization of an Bait object corresponding values are
 * calculated and set. The individual properties of the bait can accessed via getter functions.
 * There is also a function 'isUsable' that can be used to determine if a bait is usable given thresholds
 * for the individual properties such as GC content.
 */
public class Bait implements Serializable {
    private static Logger logger = Logger.getLogger(Bait.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;

    // coordinates of the bait
    private String refID;
    private Integer startPos;
    private Integer endPos;

    // average kmer alignabilty of the bait
    private Double averageKmeralignabilty = null;

    // GC content of the bait
    private Double GCcontent = null;

    // repeat content of the bait
    private Double repeatContent = null;

    // melting temperature of the bait
    private Double meltingTemperature = null;

    /*
    Interface of class Bait
     */

    public Bait(String refID, Integer startPos, Integer endPos, IndexedFastaSequenceFile fastaReader, AlignabilityMap alignabilityMap) {
        this.refID = refID;
        this.startPos = startPos;
        this.endPos = endPos;
        this.setGCContent(fastaReader);
        this.setAlignabilityScore(alignabilityMap);
        this.setRepeatContent(fastaReader);
    }

    public String getRefId() {
        return refID;
    }
    public Integer getStartPos() {
        return startPos;
    }
    public Integer getEndPos() {
        return endPos;
    }
    public Double getGCContent()  {
        return this.GCcontent;
    }
    public Double getAlignabilityScore() {
        return this.averageKmeralignabilty;
    }
    public Double getRepeatContent() { return this.repeatContent; }
    public Double getMeltingTemperature() { return this.meltingTemperature; }

    public boolean isUsable(Double minGCcontent, Double maxGCcontent, Double maxAlignabilityScore) {
        if(minGCcontent <= this.getGCContent() && this.getGCContent() <= maxGCcontent &&  this.getAlignabilityScore() <= maxAlignabilityScore) {
            return true;
        } else {
            return false;
        }
    }


    /*
    Private functions
     */

    /**
     * Calculate GC content of the bait.
     *
     * @param fastaReader
     * @return
     */
    private Double setGCContent(IndexedFastaSequenceFile fastaReader) {

        if(this.GCcontent == null) {
            // GC content is not yet calculated
            String subsequence = fastaReader.getSubsequenceAt(this.refID, this.startPos, this.endPos).getBaseString();

            // count Gs and Cs
            int GC = 0;
            for (int i = 0; i < subsequence.length(); i++) {
                switch (subsequence.charAt(i)) {
                    case 'G':
                    case 'g':
                    case 'C':
                    case 'c':
                        GC++;
                }
            }
            this.GCcontent = (double) GC / (double) subsequence.length();
        }
        return this.GCcontent;
    }

    /**
     * Calculate the average kmer alignability for the probe, which is defined to be the sum of the alignability
     * scores of all kmers divided by the number of all kmers.
     *
     * @param alignabilityMap
     */
    private void setAlignabilityScore(AlignabilityMap alignabilityMap) {

        Integer kmerSize = alignabilityMap.getKmerSize();
        Double score = 0.0;
        ArrayList<Integer> alignabilityScoreList = alignabilityMap.getScoreFromTo(refID, startPos, endPos - kmerSize + 1);

        for (Integer d : alignabilityScoreList) {
            if(d==(-1.0)) {score=-1.0; break;} // probe contains Ns, which have an alignability of -1
            score = score + d;

        }
        this.averageKmeralignabilty = score/alignabilityScoreList.size();
    }


    /**
     * Calculate repeat content based on lower (repeat) and uppercase letters.
     *
     * @param fastaReader
     */
    private void setRepeatContent(IndexedFastaSequenceFile fastaReader) {

        String subsequence = fastaReader.getSubsequenceAt(this.refID, this.startPos, this.endPos).getBaseString();

        // count upper and lower case
        int lowerCase = 0, upperCase = 0;
        for (int i = 0; i < subsequence.length(); i++) {
            if (Character.isLowerCase(subsequence.charAt(i))) lowerCase++;
            if (Character.isUpperCase(subsequence.charAt(i))) upperCase++;
        }
        this.repeatContent = ((double) lowerCase / (lowerCase + (double) upperCase));
    }

    /**
     * Calculate melting temperature of the bait.
     */
    private void setMeltingTemperature() {
        // TODO: https://www.sigmaaldrich.com/technical-documents/articles/biology/oligos-melting-temp.html
    }
}