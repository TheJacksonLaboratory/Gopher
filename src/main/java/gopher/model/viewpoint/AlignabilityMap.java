package gopher.model.viewpoint;

import gopher.model.genome.Genome;
import org.apache.commons.jexl2.UnifiedJEXL;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Created by hansep on 6/12/18.
 *
 * Input bedGraph files are assumed to be sorted like this:
 *
 * sort -k1,1 -k2,2n hg19.100mer.alignabilityMap.bedgraph
 *
 * First column (chromosome) lexicographically, second column numerically start position.
 * The downloadable files are sorted like this.
 *
 *
 */
public class AlignabilityMap {
    private static Logger logger = Logger.getLogger(AlignabilityMap.class.getName());

    /**
     *
     * Path including file name to gzipped bedGraph file.
     *
     */
    private String alignabilityMapPathIncludingFileName = null;

    /**
     *
     * Core structure of this class.
     *
     * Keys: chromosome names
     *
     * Values: pairs of arrays, one Integer array for the starting coordinates of intervals and a Double array for alignabilty scores.
     */
    private HashMap<String,ArrayPair> alignabilityMap = null; // key: chromosome name, value: starting position of an interval


    AlignabilityMap(String alignabilityMapPathIncludingFileName) throws IOException {

        this.alignabilityMapPathIncludingFileName = alignabilityMapPathIncludingFileName;


    }

    public boolean parseBedGraphFile() throws IOException {

        InputStream fileStream = new FileInputStream(this.alignabilityMapPathIncludingFileName);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        BufferedReader br = new BufferedReader(decoder);

        try {
            String line;
            String prevChr="NA";
            Integer prevEnd = -1;
            String prevLine = "NA";
            while ((line=br.readLine())!=null) {
                String A[]=line.split("\t");
                String chromosome = A[0];
                Integer sta = Integer.parseInt(A[1]) + 1;             // start coordinates of the bedGraph format are 0-based
                Integer end = Integer.parseInt(A[2]);                   // end coordinates of the bedGraph format are 1-based
                Double alignabilityScore= Double.parseDouble(A[3]);

                if(!chromosome.equals("NA") && prevEnd != -1) { // first line of the file

                    ArrayPair arrayPair = new ArrayPair(); // create new pair of arrays
                    alignabilityMap = new HashMap<String,ArrayPair>();
                    alignabilityMap.put(chromosome, arrayPair);

                    if(sta!=1) {
                        alignabilityMap.get(chromosome).addCoordScorePair(1,-1.0); // no scores available for the first bases of the chromosome
                    }
                    alignabilityMap.get(chromosome).addCoordScorePair(sta,alignabilityScore);
                }



                    if(chromosome.equals(prevChr)) {
                        Integer d = sta-prevEnd;
                        if(d>1) { // there was a gap before the current region

                            // add missing interval chr prevEnd + 1 with value -1


                            logger.trace(chromosome + " " + d + " " + prevLine + " " + line);
                        }

                    }






                prevChr = chromosome;
                prevEnd = end;
                prevLine = line;
            }
        } finally {
            br.close();
        }

        return false;
    }

    private class IntPair {

        Integer sta;
        Integer end;
        IntPair(Integer sta, Integer end) {
             this.sta = sta;
             this.end = end;
             if(end<=sta) {
                 //throw Exception;
             }
        }
    }

    private class ArrayPair {

        private ArrayList<Integer> coordArray = null;
        private ArrayList<Double> scoreArray = null;

        public void addCoordScorePair(Integer sta, Double score) {
            if(coordArray == null) {
                coordArray = new ArrayList<Integer>();
                scoreArray = new ArrayList<Double>();
            }
            coordArray.add(sta);
            scoreArray.add(score);
        }
    }
}
