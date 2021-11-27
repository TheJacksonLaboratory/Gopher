package gopher.service.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger logger = LoggerFactory.getLogger(Bait.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    // coordinates of the bait
    private String refID;
    private int startPos;
    private int endPos;

    // average kmer alignabilty of the bait
    private double averageKmeralignabilty;

    // GC content of the bait
    private double GCcontent;

    // repeat content of the bait
    private double repeatContent;

//    // melting temperature of the bait
//    private Double meltingTemperature = null;

    /*
    * Todo --remove this and just use other constructor
     */
//    @Deprecated
//    public Bait(String refID, Integer startPos, Integer endPos, IndexedFastaSequenceFile fastaReader, AlignabilityMap alignabilityMap) {
//        this.refID = refID;
//        this.startPos = startPos;
//        this.endPos = endPos;
//        this.setGCContent(fastaReader);
//        this.setAlignabilityScore(alignabilityMap);
//        this.setRepeatContent(fastaReader);
//    }

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

    public boolean isUsable(Double minGCcontent, Double maxGCcontent, Double maxAlignabilityScore) {
        return  (minGCcontent <= this.getGCContent() &&
                this.getGCContent() <= maxGCcontent &&
                this.getAlignabilityScore() <= maxAlignabilityScore);
    }


    /**
     * Calculate GC content of the bait.
     * @param fastaReader FASTA Reader from HTSJDK
     */
    private void setGCContent(IndexedFastaSequenceFile fastaReader) {
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


    private void setAlignabilityScore(AlignabilityMap alignabilityMap) {

        Integer kmerSize = alignabilityMap.getKmersize();
        Double score = 0.0;
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

}