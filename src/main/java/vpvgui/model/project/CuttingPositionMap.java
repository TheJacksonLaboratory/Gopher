package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a utility class for the class 'ViewPoint' that holds the cutting positions for the provided restriction enzyme motifs.
 * The cutting positions are calculated when an object of the class is created.
 * <p>
 * The most central data structure in this class is a <i>HashMap</i> with 'Strings' as keys and <i>ArrayLists</i> of <i>Integers</i> as values.
 * The <i>Strings</i> are cutting motifs and the related <i>ArrayList</i> contains all positions of occurrences within a given range.
 * <p>
 * In addition to the usual getter and setter function, the class provides a functions that allow to query the <i>HashMap</i>.
 * @author Peter Hansen
 */
public class CuttingPositionMap {

    /* fields */

    private Integer genomicPos;
    private Integer maxDistToGenomicPosUp;
    private Integer maxDistToGenomicPosDown;
    private static HashMap<String,ArrayList<Integer>> cuttingPositionMap;
    private static HashMap<String,Integer> cuttingPositionMapOffsets;


    /* constructor */

    /**
     * The constructor will set all fields of this class and create the actual <i>HashMap</i> for cutting positions,
     * which will be derived only for the interval [<i>genomicPos-maxDistToGenomicPosUp,genomicPos+maxDistToGenomicPosDown</i>].
     * <p>
     * The keys for the <i>HashMap</i> will be the same as passed by the argument <i>cuttingPatterns</i>,
     * but with '^' characters removed.
     * The information about the cutting position within in a particular motif will be recorded in  <i>cuttingPositionMapOffsets</i>, an additional  <i>HashMap</i>.
     * In addition to the passed motifs, there is one special key <i>ALL</i> which contains the cutting positions for the union of all motifs.
     * @param referenceSequenceID name of the genomic sequence, e.g. <i>chr1</i>.
     * @param genomicPos central position of the region for which the CuttingPositionMap is created.
     * @param maxDistToGenomicPosUp maximal distance to 'genomicPos' in upstream direction.
     * @param maxDistToGenomicPosDown maximal distance to 'genomicPos' in downstream direction.
     * @param fastaReader indexed FASTA file that contains the sequence information required for the calculation of cutting positions.
     * @param cuttingPatterns array of cutting motifs, e.g. <i>A^AGCTT</i> for the resrtiction enzyme <i>HindIII</i>. The '^' indicates the cutting position within the motif.
     */
    public CuttingPositionMap(String referenceSequenceID, Integer genomicPos, IndexedFastaSequenceFile fastaReader, Integer maxDistToGenomicPosUp, Integer maxDistToGenomicPosDown, String[] cuttingPatterns) {

        /* set fields */

        setGenomicPos(genomicPos);
        setMaxDistToGenomicPosUp(maxDistToGenomicPosUp);
        setMaxDistToGenomicPosDown(maxDistToGenomicPosDown);


        /* create maps */

        cuttingPositionMap = new HashMap<String,ArrayList<Integer>>();

        // determine offsets
        cuttingPositionMapOffsets = new HashMap<String,Integer>();
        for(int i=0;i<cuttingPatterns.length;i++) {
            Integer offSet=cuttingPatterns[i].indexOf('^');
            cuttingPatterns[i] = cuttingPatterns[i].replace("^",""); // remove '^' characters
            cuttingPositionMapOffsets.put(cuttingPatterns[i],offSet);
        }

        ArrayList<Integer> cuttingPositionListUnion = new ArrayList<Integer>();
        for(int i=0;i<cuttingPatterns.length;i++) {

            // get sequence around genomic position and convert everything to uppercase
            String genomicPosRegionString = fastaReader.getSubsequenceAt(referenceSequenceID,genomicPos - maxDistToGenomicPosUp,genomicPos + maxDistToGenomicPosDown).getBaseString().toUpperCase();

            Pattern pattern = Pattern.compile(cuttingPatterns[i]);
            Matcher matcher = pattern.matcher(genomicPosRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<Integer>();

            while(matcher.find()) {

                if (matcher.start()<=genomicPos) {
                    cuttingPositionList.add(matcher.start() - maxDistToGenomicPosUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]) - 1);
                    cuttingPositionListUnion.add(matcher.start()-maxDistToGenomicPosUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]) - 1);
                }
                else if (genomicPos<matcher.start()) {
                    cuttingPositionList.add(matcher.start() - maxDistToGenomicPosUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]) - 1);
                    cuttingPositionListUnion.add(matcher.start() - maxDistToGenomicPosUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]) - 1);
                }

            }
            cuttingPositionMap.put(cuttingPatterns[i],cuttingPositionList); // push array list to map
        }

        // add an array for the union of all cutting positions with key 'ALL'
        Set<Integer> uniqueSet = new HashSet<>(); // remove duplicates
        uniqueSet.addAll(cuttingPositionListUnion);
        cuttingPositionListUnion.clear();
        cuttingPositionListUnion.addAll(uniqueSet);
        Collections.sort(cuttingPositionListUnion); // sort
        cuttingPositionMap.put("ALL",cuttingPositionListUnion); // push array list to map
    }


    /* getter and setter functions */

    public final void setGenomicPos(Integer genomicPos) { this.genomicPos=genomicPos; }

    public final Integer getGenomicPos() {
        return genomicPos;
    }


    public final void setMaxDistToGenomicPosUp(Integer maxDistToGenomicPosUp) { this.maxDistToGenomicPosUp=maxDistToGenomicPosUp; }

    public final Integer getMaxDistToGenomicPosUp() {
        return maxDistToGenomicPosUp;
    }


    public final void setMaxDistToGenomicPosDown(Integer maxDistToGenomicPosDown) { this.maxDistToGenomicPosDown=maxDistToGenomicPosDown; }

    public final Integer getMaxDistToGenomicPosDown() {
        return maxDistToGenomicPosDown;
    }


    /**
     *
     * @return only the <i>HashMap</i> object for the cutting positions.
     */
    public final HashMap<String,ArrayList<Integer>> getHashMapOnly() {
        return cuttingPositionMap;
    }

    /**
     *
     * @return only the <i>ArrayList</i> of <i>Integers</i> for a given motif.
     */
    public final ArrayList<Integer> getArrayListForGivenMotif(String motif) {

        return cuttingPositionMap.get(motif);
    }

    /**
     *
     * @return Only the <i>HashMap</i> object for the cutting offsets.
     */
    public final HashMap<String,Integer> getCuttingPositionMapOffsets() {
        return cuttingPositionMapOffsets;
    }


    /* utility functions */

    /**
     * Given a position within the interval [-maxDistToGenomicPosUp,maxDistToGenomicPosDown],
     * this function returns the next cutting position in up or downstream direction.
     *
     * @param pos       Position relative to 'genomicPos'.
     * @param direction Direction in which the next cutting site will be searched.
     * @return Position of the next cutting position relative to 'genomicPos'.
     * @throws IllegalArgumentException if a value different than 'up' or 'down' is passed as 'direction' parameter.
     * @throws IntegerOutOfRangeException if 'pos' is not within the interval [-maxDistToGenomicPosUp,maxDistToGenomicPosDown].
     * @throws NoCuttingSiteFoundUpOrDownstreamException if there is no cutting position up or downstream of 'pos'. Exception is handled by returning the position of outermost cutting site up or downstream.
     */
    public Integer getNextCutPos(Integer pos, String direction) throws IllegalArgumentException, IntegerOutOfRangeException, NoCuttingSiteFoundUpOrDownstreamException {

        // throw exception
        if (!(Objects.equals(direction, "up") || Objects.equals(direction, "down"))) {
            throw new IllegalArgumentException("Please pass either 'up' or 'down' for 'direction'.");
        }

        // throw exception

        if (pos < -maxDistToGenomicPosUp || pos > maxDistToGenomicPosDown) {
            throw new IntegerOutOfRangeException("Please pass a value within the interval [-maxDistToGenomicPosUp=" + -maxDistToGenomicPosUp + ",maxDistToGenomicPosDown=" + maxDistToGenomicPosDown + "].");
        }

        // get array with cutting positions
        ArrayList<Integer> cutPosArray = new ArrayList<Integer>();
        cutPosArray = cuttingPositionMap.get("ALL");
        Integer returnCutPos = cutPosArray.get(cutPosArray.size() - 1);

        // reverse array, if the functions is called with 'up'
        if (direction == "up") {
            Collections.reverse(cutPosArray);
        }

        // find the next cutting site in up or downstream direction
        try {
            Iterator<Integer> cutPosArrayIt = cutPosArray.iterator();
            boolean found = false;
            while (cutPosArrayIt.hasNext()) {
                Integer nextCutPos = cutPosArrayIt.next();
                if (direction == "down" && (pos <= nextCutPos)) {
                    found = true;
                    returnCutPos = nextCutPos;
                    break;
                }
                if (direction == "up" && (pos >= nextCutPos)) {
                    found = true;
                    returnCutPos = nextCutPos;
                    break;
                }

            }
            if (!found) {
                returnCutPos = cutPosArray.get(cutPosArray.size() - 1);
                throw new NoCuttingSiteFoundUpOrDownstreamException("EXCEPTION in function 'getNextCutPos': No cutting site " + direction + "stream of position " + pos + ". Will return the " +
                        "outermost cutting site in " + direction + "stream direction.");
            }
        } catch (NoCuttingSiteFoundUpOrDownstreamException e) {
            System.out.println(e.getMessage());
        } finally {

        }

        // reverse the array for future calls
        if (direction == "up") {
            Collections.reverse(cutPosArray);
        }

        return returnCutPos;
    }


}
