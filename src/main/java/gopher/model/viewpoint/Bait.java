package gopher.model.viewpoint;

import gopher.exception.GopherException;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class Bait {
    private static Logger logger = Logger.getLogger(Bait.class.getName());

    // coordinates of the bait
    private String refID = null;
    private Integer startPos = null;
    private Integer endPos = null;

    // average kmer alignabilty of the bait
    private Double averageKmeralignabilty = null;

    // GC content of the bait
    private Double GCcontent = null;

    // repeat content of the bait
    private Double repeatContent = null;

    // melting temperature ?

    public Bait(String refID, Integer startPos, Integer endPos) {
        this.refID = refID;
        this.startPos = startPos;
        this.endPos = endPos;
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

    public Double setAlignabilityScore(AlignabilityMap alignabilityMap) {

        Integer kmerSize = alignabilityMap.getKmerSize();
        Double score = 0.0;
        ArrayList<Integer> alignabilityScoreList = alignabilityMap.getScoreFromTo(refID, startPos, endPos - kmerSize + 1);

        for (Integer d : alignabilityScoreList) {
            if(d==(-1.0)) {score=-1.0; break;}
            score = score + d;

        }
        this.averageKmeralignabilty = score/alignabilityScoreList.size();
        return this.averageKmeralignabilty ;
    }
    public Double getAlignabilityScore() {
            return this.averageKmeralignabilty;
    }


    public Double setGCContent(IndexedFastaSequenceFile fastaReader) {

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
    public Double getGCContent()  {
            return this.GCcontent;
    }

    public Double setRepeatContent(IndexedFastaSequenceFile fastaReader) {

        if(this.repeatContent == null) {
            // repeat content is not yet calculated
            String subsequence = fastaReader.getSubsequenceAt(this.refID, this.startPos, this.endPos).getBaseString();

            // count upper and lower case
            int lowerCase = 0, upperCase = 0;
            for (int i = 0; i < subsequence.length(); i++) {
                if (Character.isLowerCase(subsequence.charAt(i))) lowerCase++;
                if (Character.isUpperCase(subsequence.charAt(i))) upperCase++;
            }
            this.repeatContent = ((double) lowerCase / (lowerCase + (double) upperCase));
        }
        return this.repeatContent;
    }
    public Double getRepeatContent() throws GopherException {
        if(this.repeatContent != null) {
            return this.repeatContent;
        } else {
            throw new GopherException("Repeat content is not yet calculated!");
        }
    }

    public Double getMeltingTemperature() {
        // TODO: https://www.sigmaaldrich.com/technical-documents/articles/biology/oligos-melting-temp.html
        return 0.0;
    }
}