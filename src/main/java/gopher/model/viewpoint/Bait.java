package gopher.model.viewpoint;

import org.apache.log4j.Logger;

import java.util.ArrayList;

public class Bait {
    private static Logger logger = Logger.getLogger(Bait.class.getName());

    private String refID = null;

    private Integer startPos = null;
    private Integer endPos = null;

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

    public Double getAlignabilityScore(AlignabilityMap alignabilityMap) {

        Integer kmerSize = 100;
        Double score = 0.0;
        ArrayList<Double> alignabilityScoreList = alignabilityMap.getScoreFromTo(refID, startPos, endPos - kmerSize + 1);

        for (Double d : alignabilityScoreList) {
            if(d==(-1.0)) {score=-1.0; break;}
            score = score + 1/d;

        }
        return score/alignabilityScoreList.size();
    }

}
