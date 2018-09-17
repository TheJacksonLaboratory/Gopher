package gopher.model.viewpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by hansep on 6/12/18.
 *
 * This class reads an array hash of pairs of arrays. The keys are chromosome names. The array pairs are combined
 * in a simple private sub class (AlignabilityMap) of this class and consists of an Integer and a Double array. The first
 * array has all positions at which the alignability score changes in sorted order. The second array has the
 * associated scores.
 *
 * This data structure can be efficiently queried for alignabilty scores at given positions. The search function
 * performs a binary search on the array for position within the array pair object of the given chromosome. The
 * determined index corresponds to the first position before the given postion at which the alignabilty score changes
 * and can be used to fetch the alignabilty score from the second array.
 *
 * Input bedGraph files are assumed to be sorted like this:
 *
 * sort -k1,1 -k2,2n hg19.100mer.alignabilityMap.bedgraph
 *
 * First column (chromosome) lexicographically, second column numerically start position.
 * The downloadable files are sorted like this.
 */
public class AlignabilityMap {
    /** Array of coordinates with boundaries of the region that has a certain score. */
    private final int coordArray[];
     /** Array of alignability score entries */
    private final int scoreArray[];

    private final String chromName;

    private final int kmersize;


    String getChromName() {
        return chromName;
    }

    int getKmersize() { return kmersize; }

    AlignabilityMap(String chrom, List<Integer> coordinateList, List<Integer> scoreList, int kmer) {
        this.coordArray = coordinateList.stream().mapToInt(Integer::intValue).toArray();
        this.scoreArray = scoreList.stream().mapToInt(Integer::intValue).toArray();
        this.chromName=chrom;
        this.kmersize=kmer;


    }

    /** @return number of elements in the array of alignability scores. */
    public int getSize() {
        return coordArray.length;
    }

    /** @return list of alignability scores for the interval between fromPos and toPos*/
    ArrayList<Integer> getScoreFromTo(int fromPos, int toPos) {
        ArrayList<Integer> scoreArrayForRegion = new ArrayList<>();
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
