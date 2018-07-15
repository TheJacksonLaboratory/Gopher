package gopher.model.viewpoint;

import org.apache.log4j.Logger;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Created by hansep on 6/12/18.
 *
 * This class reads an array hash of pairs of arrays. The keys are chromosome names. The array pairs are combined
 * in a simple private sub class (ArrayPair) of this class and consists of an Integer and a Double array. The first
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
    private static Logger logger = Logger.getLogger(AlignabilityMap.class.getName());

    private  final String chromInfoPath;

    private Integer kmerSize = null;
    public Integer getKmerSize() {
        return this.kmerSize;
    }


    /**
     * Core function of this class.
     *
     * @param pos
     * @return Score at pos
     */
    public Integer getScoreAtPos(String chromosome, Integer pos) {

        Integer score;

        // get index from binary search
        int index = Arrays.binarySearch(this.alignabilityMap.get(chromosome).coordArray, pos);

        if(0 <= index) {

            // pos cooresponds to a position at which the score changes
            score = this.alignabilityMap.get(chromosome).scoreArray[index];

        } else {

            // pos does not correspond to a position at which the score changes (this should happen more often)
            int preceding_index = (index+2)*(-1);
            score = this.alignabilityMap.get(chromosome).scoreArray[preceding_index];
        }
        return score;
    }

    public ArrayList<Integer> getScoreFromTo(String chromosome, Integer fromPos, Integer toPos) {

        ArrayList scoreArrayForRegion = new ArrayList<Double>();

        // get start index from binary search
        int index = Arrays.binarySearch(this.alignabilityMap.get(chromosome).coordArray, fromPos);


        if(index < 0) {

            // pos does not correspond to a position at which the score changes (this should happen more often)
            index = (index+2)*(-1); // take the next previous index
        }

        for(int pos = fromPos; pos <= toPos; pos++) {

            if( (index < this.alignabilityMap.get(chromosome).getSize()-1) &&
                    (pos == this.alignabilityMap.get(chromosome).coordArray[index + 1])) {
                index++;
            }
            scoreArrayForRegion.add(this.alignabilityMap.get(chromosome).scoreArray[index]);
            //logger.trace("index: " + index + "\t" + "pos: " + pos + "\t" + "score: " + this.alignabilityMap.get(chromosome).scoreArray.get(index));
        }


        return scoreArrayForRegion;
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
     * The first and last positions of the chromosomes often consists of N's. For regions consisting of N's
     * there are no alignability scores. The parser for the bedgraph file will fill those gaps with
     * regions that have a score of -1. For the last positions of a chromosome the size of the chromosome is needed,
     * which are stored in this map.
     *
     */
    private HashMap<String,Integer> chromSizesMap = null;


    /**
     * Constructor
     *
     * @param alignabilityMapPathIncludingFileName Path including file name to gzipped bedGraph file.
     * @throws IOException
     */
    public AlignabilityMap(String chromInfoPathIncludingFileName, String alignabilityMapPathIncludingFileName, Integer kmerSize) throws IOException {
        this.kmerSize = kmerSize;
        this.chromInfoPath=chromInfoPathIncludingFileName;
        this.parseChromInfoFile(chromInfoPathIncludingFileName);
        this.parseBedGraphFile(alignabilityMapPathIncludingFileName);

    }

    /**
     * Parses the content of a 'chromInfo.txt.gz' file and stores the chromosome sizes in the hash map 'chromSizesMap'.
     *
     * @param chromInfoPathIncludingFileName
     * @throws IOException
     */
    public void parseChromInfoFile(String chromInfoPathIncludingFileName) throws IOException {

        chromSizesMap = new HashMap<>();

        InputStream fileStream = new FileInputStream(chromInfoPathIncludingFileName);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        BufferedReader br = new BufferedReader(decoder);


        try {
            String line;
            while ((line = br.readLine()) != null) {

                // extract information from line
                String A[] = line.split("\t");
                String chromosome = A[0];
                Integer length = Integer.parseInt(A[1]);
                chromSizesMap.put(chromosome,length);
            }
        } finally {
            br.close();
            decoder.close();
            gzipStream.close();
            fileStream.close();
        }
    }


    /**
     * Reads a bedGraph file to a data structure (alignabilityMap) described above.
     *
     * @throws IOException
     */
    public void parseBedGraphFile(String alignabilityMapPathIncludingFileName) throws IOException {

        logger.debug("About to parse bedgraph file " + alignabilityMapPathIncludingFileName + "...");

        alignabilityMap = new HashMap<>(chromSizesMap.size());
        ArrayPairIterator iterator = new ArrayPairIterator(alignabilityMapPathIncludingFileName, this.chromInfoPath);
        while (iterator.hasNext()) {
            ArrayPair ap = iterator.next();
            alignabilityMap.put(ap.getChromName(),ap);
            logger.trace("Put: "+ap.getChromName());
        }
    }




    /**
     * Helper function. Prints the content of alignabilityMap to the screen.
     */
    public void printAlignabilityMap() {
        for (String key : alignabilityMap.keySet()) {
            for (int j = 0; j < alignabilityMap.get(key).getSize(); j++) {
                logger.trace(key + "\t" + alignabilityMap.get(key).coordArray[j] + "\t" + alignabilityMap.get(key).scoreArray[j]);

            }
        }
    }

}
