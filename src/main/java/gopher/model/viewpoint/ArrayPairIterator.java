package gopher.model.viewpoint;

import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class ArrayPairIterator implements Iterator<ArrayPair> {
    private static Logger logger = Logger.getLogger(ArrayPairIterator.class.getName());

    /** The reader that is being read. */
    private final BufferedReader bufferedReader;
    /** The current line. */
    private String cachedLine;
    /** A flag indicating if the iterator has been fully read. */
    private boolean finished = false;
    /** This variable helps to keep track of when are finished with a chromosome and need to go to the next one.*/
    private  String prevChr="chr0";
    /**
     * The first and last positions of the chromosomes often consists of N's. For regions consisting of N's
     * there are no alignability scores. The parser for the bedgraph file will fill those gaps with
     * regions that have a score of -1. For the last positions of a chromosome the size of the chromosome is needed,
     * which are stored in this map.
     *
     */
    private ImmutableMap<String,Integer> chromSizesMap;

    private ArrayPair currentArrayPair=null;

    private boolean ready;

    /** Coordinates for the chromosome we are currently parsing. */
    private List<Integer> coordinateList;
    /** Scores for the chromosome we are currently parsing. */
    private List<Integer> scoreList;


    /**
     *
     * @param alignabilityMapPath Path of alignability map IncludingFileName
     * @param chromInfoPath Path of chromInfo Including FileName
     * @throws IOException
     */
    public ArrayPairIterator(String alignabilityMapPath,String chromInfoPath)  throws IOException {
        parseChromInfoFile(chromInfoPath);
        logger.debug("About to parse bedgraph file " + alignabilityMapPath + "...");
        InputStream fileStream = new FileInputStream(alignabilityMapPath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        this.bufferedReader = new BufferedReader(decoder);
        ready=true;
    }

    /**
     * Parses the content of a 'chromInfo.txt.gz' file and stores the chromosome sizes in the hash map 'chromSizesMap'.
     *
     * @param chromInfoPathIncludingFileName
     * @throws IOException
     */
    public void parseChromInfoFile(String chromInfoPathIncludingFileName)  {
        ImmutableMap.Builder<String, Integer> builder = new ImmutableMap.Builder<>();
        try (
            InputStream fileStream = new FileInputStream(chromInfoPathIncludingFileName);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream);
            BufferedReader br = new BufferedReader(decoder)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                String A[] = line.split("\t");
                String chromosome = A[0];
                Integer length = Integer.parseInt(A[1]);
                builder.put(chromosome, length);
            }
            // note: all four resources closed automatically
        } catch (IOException e){
            e.printStackTrace();
        }
        this.chromSizesMap = builder.build();
    }


    /** This checks if {@link #bufferedReader} is null -- if so, we are finished and there are no more ArrayPair objects. */
    @Override
    public boolean hasNext() {
        return this.ready;
    }


    @Override
    public ArrayPair next() {
        String line;
        Integer prevEnd = 0;
        String A[];
        String chromosome;
        Integer sta;
        Integer end;
        Integer alignabilityScore;
        int number_integer_score_pairs = 0;
        try {
            while ((line = this.bufferedReader.readLine()) != null) {
                A = line.split("\t");
                chromosome = A[0];
                sta = Integer.parseInt(A[1]) + 1; // start coordinates of the bedGraph format are 0-based
                end = Integer.parseInt(A[2]);     // end coordinates of the bedGraph format are 1-based
                alignabilityScore = (int) Math.round(1.0 / Double.parseDouble(A[3]));
                int dist = sta - prevEnd;
                if (prevChr.equals("chr0")) {
                    // then this is the first line and we need to initialize stuff
                    this.coordinateList = new ArrayList<>();
                    this.scoreList = new ArrayList<>();
                    if (sta != 1) {
                        // there is a gap before the first region of the chromosome
                        coordinateList.add(1);
                        scoreList.add(-1);
                    }
                    coordinateList.add(sta);
                    scoreList.add(alignabilityScore);
                    logger.trace("Making chromosome alignability map for " + chromosome);
                } else if (!chromosome.equals(prevChr)) {
                    // this is the first line for a new chromosome
                    // we are finished with the previous chromosome and can
                    // make a new ArrayPair object
                    // first we need to store the values on the current line,
                    // which are the first values for the "new" chromosome
                    logger.trace("Making chromosome alignability map for " + chromosome + " prev=" + prevChr);
                    currentArrayPair = new ArrayPair(prevChr, coordinateList, scoreList);
                    coordinateList.clear();
                    scoreList.clear();
                    if (sta != 1) {
                        // there is a gap before the first region of the chromosome
                        coordinateList.add(1);
                        scoreList.add(-1);
                    }
                    coordinateList.add(sta);
                    scoreList.add(alignabilityScore);
                    prevChr=chromosome;
                    return currentArrayPair; // THIS IS WHERE THE ITERATOR RETURNS THE NEXT OBJECT
                } else {
                    // this is NOT the first line for a new chromosome
                    if (1 < dist) {
                        // there is a gap before the current region
                        //alignabilityMap.get(chromosome).addCoordScorePair(prevEnd + 1, -1);
                        coordinateList.add(prevEnd + 1);
                        scoreList.add(-1);
                    }
                    coordinateList.add(sta);
                    scoreList.add(alignabilityScore);

                    if (chromSizesMap.containsKey(prevChr) && (prevEnd < chromSizesMap.get(prevChr))) {
                        // there were no alignability scores for the last postions of the last chromosome
                        coordinateList.add(prevEnd + 1);
                        scoreList.add(-1);
                    }
                }
                prevChr = chromosome;
                prevEnd = end;
            } // end while
            // When we get here, we want to return the very last ArrayPair object.
            logger.trace("Making last alignability map for " + prevChr);
            currentArrayPair = new ArrayPair(prevChr, coordinateList, scoreList);
            ready=false;
            return currentArrayPair;

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }

        return null;
    }

}
