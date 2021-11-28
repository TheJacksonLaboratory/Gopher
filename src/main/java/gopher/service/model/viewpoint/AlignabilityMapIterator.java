package gopher.service.model.viewpoint;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Input data for the alignability map, one chromosome at a time.
 */
public class AlignabilityMapIterator implements Iterator<AlignabilityMap> {
    private static Logger logger = LoggerFactory.getLogger(AlignabilityMapIterator.class.getName());

    /**
     * The reader that is being read.
     */
    private final BufferedReader bufferedReader;
    /**
     * This variable helps to keep track of when are finished with a chromosome and need to go to the next one.
     */
    private String prevChr = "chr0";

    private Integer prevEnd = 0;
    /**
     * The first and last positions of the chromosomes often consists of N's. For regions consisting of N's
     * there are no alignability scores. The parser for the bedgraph file will fill those gaps with
     * regions that have a score of -1. For the last positions of a chromosome the size of the chromosome is needed,
     * which are stored in this map.
     */
    private ImmutableMap<String, Integer> chromSizesMap;

    private AlignabilityMap currentArrayPair = null;

    private final int kmerSize;

    private boolean ready;

    /**
     * Coordinates for the chromosome we are currently parsing.
     */
    private List<Integer> coordinateList;
    /**
     * Scores for the chromosome we are currently parsing.
     */
    private List<Integer> scoreList;


    private final static int NO_ALIGNABILITY_SCORE_AVAILABLE = -1;


    public int getKmerSize() { return kmerSize; }

    /**
     * @param alignabilityMapPath Path of alignability map IncludingFileName
     * @param chromInfoPath       Path of chromInfo Including FileName
     * @throws IOException throws exception if file cannot be properly opened
     */
    public AlignabilityMapIterator(String alignabilityMapPath, String chromInfoPath, int kmerSize) throws IOException {
        parseChromInfoFile(chromInfoPath);
        logger.debug("About to parse bedgraph file " + alignabilityMapPath + "...");
        InputStream fileStream = new FileInputStream(alignabilityMapPath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        this.bufferedReader = new BufferedReader(decoder);
        ready = true;
        this.scoreList = new ArrayList<>();
        this.coordinateList = new ArrayList<>();
        this.kmerSize=kmerSize;

    }

    /**
     * Parses the content of a 'chromInfo.txt.gz' file and stores the chromosome sizes in the hash map 'chromSizesMap'.
     *
     * @param chromInfoPathIncludingFileName Path to the chromosome info file.
     */
    private void parseChromInfoFile(String chromInfoPathIncludingFileName) {
        ImmutableMap.Builder<String, Integer> builder = new ImmutableMap.Builder<>();
        try (InputStream fileStream = new FileInputStream(chromInfoPathIncludingFileName);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream);
                BufferedReader br = new BufferedReader(decoder) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] A = line.split("\t");
                String chromosome = A[0];
                Integer length = Integer.parseInt(A[1]);
                builder.put(chromosome, length);
            }
            // note: all four resources closed automatically
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.chromSizesMap = builder.build();
    }


    /**
     * This checks if {@link #bufferedReader} is null -- if so, we are finished and there are no more Chromosome2AlignabilityMap objects.
     */
    @Override
    public boolean hasNext() {
        if (ready) return true;
        try {
            return this.bufferedReader.ready();
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public AlignabilityMap next() {
        String line;
        String[] A;
        String chromosome;
        Integer startPos;
        int endPos;
        Integer alignabilityScore;
        try {
            // This will be the first line for each chromosome, but may
            while ((line = this.bufferedReader.readLine()) != null) {
                A = line.split("\t");
                chromosome = A[0];
                startPos = Integer.parseInt(A[1]) + 1; // start coordinates of the bedGraph format are 0-based
                endPos = Integer.parseInt(A[2]);     // end coordinates of the bedGraph format are 1-based
                alignabilityScore = (int) Math.round(1.0 / Double.parseDouble(A[3]));
                int dist = startPos - prevEnd;
                if (prevChr.equals("chr0")) { // only the case for the version first line of the file.
                    if (startPos != 1) {
                        // there is a gap before the first region of the chromosome
                        // therefore, the first block starts after "1"
                        //the following fills in a block with -1 (a flag)
                        coordinateList.add(1);
                        scoreList.add(NO_ALIGNABILITY_SCORE_AVAILABLE);
                    }
                    coordinateList.add(startPos);
                    scoreList.add(alignabilityScore);

                } else if (!chromosome.equals(prevChr)) {
                    // this is the first line for a new chromosome
                    // we are finished with the previous chromosome and can
                    // make a new Chromosome2AlignabilityMap object
                    // first we need to store the values on the current line,
                    // which are the first values for the "new" chromosome

                    if (chromSizesMap.containsKey(prevChr) && (prevEnd < chromSizesMap.get(prevChr))) {
                        // there were no alignability scores for the last postions of the last chromosome
                        coordinateList.add(prevEnd + 1);
                        scoreList.add(-1);
                    }

                    currentArrayPair = new AlignabilityMap(prevChr, coordinateList, scoreList, this.kmerSize);
                    coordinateList.clear();
                    scoreList.clear();

                    prevChr = chromosome;
                    // when we get here, we are done making the Chromosome2AlignabilityMap object for the "previous" chromosome
                    logger.trace("Done making chromosome alignability map for " + chromosome + " prev=" + prevChr + " size=" + chromSizesMap.get(chromosome));
                    // now use the data from the "new" line for the first block of the "new chromosome"

                    if (startPos != 1) {
                        // there is a gap before the first region of the chromosome
                        // therefore, the first block starts after "1"
                        //the following fills in a block with -1 (a flag)
                        coordinateList.add(1);
                        scoreList.add(NO_ALIGNABILITY_SCORE_AVAILABLE);
                    }
                    coordinateList.add(startPos);
                    scoreList.add(alignabilityScore);
                    prevEnd = endPos;
                    logger.trace("returning array pair for " + currentArrayPair.getChromName());
                    return currentArrayPair; // THIS IS WHERE THE ITERATOR RETURNS THE NEXT OBJECT
                } else {
                    // this is NOT the first line for a new chromosome
                    if (1 < dist) {
                        // there is a gap before the current region
                        //alignabilityMap.get(chromosome).addCoordScorePair(prevEnd + 1, -1);
                        coordinateList.add(prevEnd + 1);
                        scoreList.add(-1);
                    }
                    coordinateList.add(startPos);
                    scoreList.add(alignabilityScore);
                }
                prevChr = chromosome;
                prevEnd = endPos;
            } // end while
            // When we get here, we want to return the very last Chromosome2AlignabilityMap object.
            logger.trace("Making last alignability map for " + prevChr);
            if (chromSizesMap.containsKey(prevChr) && (prevEnd < chromSizesMap.get(prevChr))) {
                // there were no alignability scores for the last postions of the last chromosome
                coordinateList.add(prevEnd + 1);
                scoreList.add(-1);
            }
            currentArrayPair = new AlignabilityMap(prevChr, coordinateList, scoreList, this.kmerSize);
            ready = false;
            return currentArrayPair;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
