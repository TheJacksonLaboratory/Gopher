package gopher.model.viewpoint;

import java.util.List;

/**
 * Consists of an Integer array containing to positions at which the alignability score changes in sorted order
 * and a Double array with associtated scores.
 */
public class ArrayPair {
    /** Array of coordinates with boundaries of the region that has a certain score. TODO what? */
    final int coordArray[];
     /** Array of alignability score entries */
    final int scoreArray[];

    final String chromName;


    public String getChromName() {
        return chromName;
    }

    ArrayPair(String chrom, List<Integer> coordinateList, List<Integer> scoreList) {
        this.coordArray = coordinateList.stream().mapToInt(Integer::intValue).toArray();
        this.scoreArray = scoreList.stream().mapToInt(Integer::intValue).toArray();
        this.chromName=chrom;

    }



    public int getSize() {
        return coordArray.length;
    }


}
