package gopher.model.viewpoint;

import org.apache.log4j.Logger;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Created by hansep on 6/12/18.
 *
 * This class reads an array hash of pairs of arrays. The keys are chromosome names. The array pairs are combined
 * in a simple private sub class (ArrayPair) of this class and consists of an Integer and a Double array. The first
 * array has all positions at which the alignability score changes. The second array has the associated scores.
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
    private static Logger logger = Logger.getLogger(AlignabilityMap.class.getName());

    /**
     * Core function of this class.
     *
     * @param pos
     * @return Score at pos
     */
    public Double getScoreAtPos(String chromosome, Integer pos) {
        return 1.0;
    }

    /**
     * Core structure of this class.
     *
     * Keys:
     * chromosome names
     *
     * Values:
     * pairs of arrays, one Integer array for the positions at which the alignability score changes
     * and a Double array for alignabilty scores.
     */
    private HashMap<String,ArrayPair> alignabilityMap = null;


    /**
     * Constructor
     *
     * @param alignabilityMapPathIncludingFileName Path including file name to gzipped bedGraph file.
     * @throws IOException
     */
    AlignabilityMap(String alignabilityMapPathIncludingFileName) throws IOException {
        this.parseBedGraphFile(alignabilityMapPathIncludingFileName);
    }


    /**
     * Reads a bedGraph file to a data structure (alignabilityMap) described above.
     *
     * @throws IOException
     */
    public void parseBedGraphFile(String alignabilityMapPathIncludingFileName) throws IOException {

        InputStream fileStream = new FileInputStream(alignabilityMapPathIncludingFileName);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        BufferedReader br = new BufferedReader(decoder);

        try {
            String line;
            String prevChr="chr0";
            Integer prevEnd = 0;
            while ((line = br.readLine()) != null) {

                // extract information from line
                String A[] = line.split("\t");
                String chromosome = A[0];
                Integer sta = Integer.parseInt(A[1]) + 1;                   // start coordinates of the bedGraph format are 0-based
                Integer end = Integer.parseInt(A[2]);                       // end coordinates of the bedGraph format are 1-based
                Double alignabilityScore= Double.parseDouble(A[3]);

                int dist = sta - prevEnd;
                if(!chromosome.equals(prevChr)) {

                    // this is the first line of the file for a new chromosome
                    alignabilityMap = new HashMap<String, ArrayPair>();     // create new hash map for chromosome
                    ArrayPair posVal = new ArrayPair();                     // create new pair of arrays
                    alignabilityMap.put(chromosome, posVal);                // and put ararray pair to hash map
                    if (1 != dist) {
                        // there is a gap before the first region of the chromosome
                        alignabilityMap.get(chromosome).addCoordScorePair(1, -1.0);
                        alignabilityMap.get(chromosome).addCoordScorePair(sta, alignabilityScore);
                    } else {
                        alignabilityMap.get(chromosome).addCoordScorePair(sta, alignabilityScore);
                    }

                } else {

                    // this is NOT the first line of the file for a new chromosome
                    if (1 < dist) {
                        // there is a gap before the current region
                        alignabilityMap.get(chromosome).addCoordScorePair(1, -1.0);
                        alignabilityMap.get(chromosome).addCoordScorePair(sta-1, alignabilityScore);
                    } else {
                        alignabilityMap.get(chromosome).addCoordScorePair(sta, alignabilityScore);
                    }
                }
                prevChr = chromosome;
                prevEnd = end;
            } // end while
        } finally {
            br.close();
        }
    }


    /**
     * Consists of an Integer array containing to positions at which the alignability score changes in sorted order
     * and a Double array with associtated scores.
     */
    private class ArrayPair {

        private ArrayList<Integer> coordArray = null;
        private ArrayList<Double> scoreArray = null;

        public void addCoordScorePair(Integer sta, Double score) {
            if(coordArray == null) {
                // these are the first values added to this object
                coordArray = new ArrayList<Integer>();
                scoreArray = new ArrayList<Double>();
            }
            coordArray.add(sta);
            scoreArray.add(score);
        }
    }
}
