package gopher.model.viewpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Consists of an Integer array containing to positions at which the alignability score changes in sorted order
 * and an array with associated scores.
 * TODO -- I WOULD PROBABLY NAME THIS CLASS ALignablityMap once we have gotten rid of the other one????
 */
public class Chromosome2AlignabilityMap {
    /** Array of coordinates with boundaries of the region that has a certain score. TODO what? */
    final int coordArray[];
     /** Array of alignability score entries */
    final int scoreArray[];

    final String chromName;

    final private int kmersize;


    public String getChromName() {
        return chromName;
    }

    public int getKmersize() { return kmersize; }

    Chromosome2AlignabilityMap(String chrom, List<Integer> coordinateList, List<Integer> scoreList, int kmer) {
        this.coordArray = coordinateList.stream().mapToInt(Integer::intValue).toArray();
        this.scoreArray = scoreList.stream().mapToInt(Integer::intValue).toArray();
        this.chromName=chrom;
        this.kmersize=kmer;


    }

    /** @return number of elements in the array of alignability scores. */
    public int getSize() {
        return coordArray.length;
    }

    /** @return */
    public ArrayList<Integer> getScoreFromTo(int fromPos, int toPos) {
        ArrayList scoreArrayForRegion = new ArrayList<Double>();
        // get start index from binary search
        int index = Arrays.binarySearch(coordArray, fromPos);
        if(index < 0) {
            // pos does not correspond to a position at which the score changes (this should happen more often)
            index = (index+2)*(-1); // take the next previous index
        }

        for(int pos = fromPos; pos <= toPos; pos++) {
            if( (index < getSize()-1) && (pos == coordArray[index + 1])) {
                index++;
            }
            scoreArrayForRegion.add(scoreArray[index]);
            //logger.trace("index: " + index + "\t" + "pos: " + pos + "\t" + "score: " + this.alignabilityMap.get(chromosome).scoreArray.get(index));
        }
        return scoreArrayForRegion;
    }


}
