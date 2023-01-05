package gopher.service.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class represents a bait (i.e., probe sequence) used for capture Hi-C enrichment.
 * The efficiency of a bait depends on GC content, alignability and melting temperature.
 * There is also a function 'isUsable' that can be used to determine if a bait is usable given thresholds
 * for the individual properties such as GC content.
 */
public class Bait implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bait.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    /**  coordinates of the bait. */
    private final String refID;
    private final int startPos;
    private final int endPos;

    /**  average kmer alignabilty of the bait. */
    private double averageKmeralignabilty;

    /** GC content of the bait */
    private double GCcontent;

    /** repeat content of the bait . */
    private double repeatContent;

    public Bait(String refID, int startPos, int endPos, IndexedFastaSequenceFile fastaReader, AlignabilityMap alignabilityMap) {
        this.refID = refID;
        this.startPos = startPos;
        this.endPos = endPos;
        String subsequence = fastaReader.getSubsequenceAt(this.refID, this.startPos, this.endPos).getBaseString();
        this.setGCContent(subsequence);
        this.setRepeatContent(subsequence);
        this.setAlignabilityScore(alignabilityMap);
    }

    public String getRefId() {
        return refID;
    }
    public int getStartPos() {
        return startPos;
    }
    public int getEndPos() {
        return endPos;
    }
    public double getGCContent()  {
        return this.GCcontent;
    }
    public double getAlignabilityScore() {
        return this.averageKmeralignabilty;
    }
    public double getRepeatContent() { return this.repeatContent; }
    public String getContigStartPosKey() {
        return getRefId() + ":" + getStartPos();
    }

    /**
     *
     * @param location "up" or "down" for baits at upstream or downstream part of restriction fragment
     * @return TSV string intended for BED File Export
     */
    private String getTsvLine(String location) {
        return getRefId() + "\t" + getStartPos() + "\t" + getEndPos() + "\t" + location + "|GC:" +
                  String.format("%.2f", getGCContent()) + "|Ali:" + String.format("%.2f", getAlignabilityScore()) +
                "|Rep:" + String.format("%.2f", getRepeatContent()) + "\t" + (int) Math.round(1000/getAlignabilityScore());
    }

    public String getTsvLineDownstream() {
        return getTsvLine("down");
    }
    public String getTsvLineUpstream() {
        return getTsvLine("up");
    }

    public boolean isUsable(double minGCcontent, double maxGCcontent, double maxAlignabilityScore) {
        return  (minGCcontent <= this.getGCContent() &&
                this.getGCContent() <= maxGCcontent &&
                this.getAlignabilityScore() <= maxAlignabilityScore);
    }


    /**
     * Calculate GC content of the bait.
     * @param subsequence DNA sequence of the bait
     */
    private void setGCContent(String subsequence) {
        int GC = 0;
        for (int i = 0; i < subsequence.length(); i++) {
            switch (subsequence.charAt(i)) {
                case 'G', 'g', 'C', 'c' -> GC++;
            }
        }
        this.GCcontent = (double) GC / (double) subsequence.length();
    }


    private void setAlignabilityScore(AlignabilityMap alignabilityMap) {
        int kmerSize = alignabilityMap.getKmersize();
        double score = 0.0;
        ArrayList<Integer> alignabilityScoreList = alignabilityMap.getScoreFromTo(startPos, endPos - kmerSize + 1);
        for (Integer d : alignabilityScoreList) {
            if(d==(-1.0)) {score=-1.0; break;} // probe contains Ns, which have an alignability of -1
            score = score + d;
        }
        this.averageKmeralignabilty = score/alignabilityScoreList.size();
    }



    /**
     * Calculate repeat content based on lower (repeat) and uppercase letters.
     *
     * @param subsequence DNA sequence of the bait
     */
    private void setRepeatContent(String subsequence) {
        // count upper and lower case
        int lowerCase = 0, upperCase = 0;
        for (int i = 0; i < subsequence.length(); i++) {
            if (Character.isLowerCase(subsequence.charAt(i))) lowerCase++;
            if (Character.isUpperCase(subsequence.charAt(i))) upperCase++;
        }
        this.repeatContent = ((double) lowerCase / (lowerCase + (double) upperCase));
    }

}