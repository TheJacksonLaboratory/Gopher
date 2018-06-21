package gopher.model.viewpoint;

import org.apache.log4j.Logger;

import java.util.ArrayList;

public class Bait {
    private static Logger logger = Logger.getLogger(Bait.class.getName());

    // coordinates of the bait
    private String refID = null;
    private Integer startPos = null;
    private Integer endPos = null;

    // depends on the alignabilty map used for calculating the average k-mer alignabilty of given baits
    private Integer kmerSize = null;

    // GC content of the bait
    private Double GCcontent = null;

    // melting temperature ?

    public Bait(String refID, Integer startPos, Integer endPos, Integer kmerSize) {
        this.refID = refID;
        this.startPos = startPos;
        this.endPos = endPos;
        this.kmerSize = kmerSize;
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

    public Double getAlignabilityScore(AlignabilityMap alignabilityMap) {

        Integer kmerSize = alignabilityMap.getKmerSize();
        Double score = 0.0;
        ArrayList<Integer> alignabilityScoreList = alignabilityMap.getScoreFromTo(refID, startPos, endPos - kmerSize + 1);

        for (Integer d : alignabilityScoreList) {
            if(d==(-1.0)) {score=-1.0; break;}
            score = score + d;

        }
        return score/alignabilityScoreList.size();
    }

}
