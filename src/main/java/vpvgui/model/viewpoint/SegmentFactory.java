package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteFoundUpOrDownstreamException;
import vpvgui.model.RestrictionEnzyme;

import java.io.Serializable;
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
 *
 * @author Peter Hansen
 * @version 0.1.1 (2017-11-01)
 */
public class SegmentFactory implements Serializable {
    private static final Logger logger = Logger.getLogger(SegmentFactory.class.getName());
    /**
     * serialization version ID
     */
    static final long serialVersionUID = 1L;
    /** The central, anchor position of the viewpoint (one-based numbering). */
    private Integer genomicPos;
    /** The distance from {@link #genomicPos} in 3' direction.*/
    private Integer maxDistToGenomicPosUp;
    /** The distance from {@link #genomicPos} in 5' direction.*/
    private Integer maxDistToGenomicPosDown;
    /** Map of the positions where the enzymes cut. Key: a site, such as GATC, value: list of cutting site positions
     * relative to {@link #genomicPos}. */
    private HashMap<String, ArrayList<Integer>> cuttingPositionMap;
    /** TODO what is key?. Value, the corresponding {@link RestrictionEnzyme} object. */
    static Map<String, RestrictionEnzyme> restrictionEnzymeMap;

    public static void setRestrictionEnzymeMap(Map<String, RestrictionEnzyme> m) {
        restrictionEnzymeMap = m;
    }



    /**
     * The constructor will set all fields of this class and create the actual <i>HashMap</i> for cutting positions,
     * which will be derived only for the interval [<i>genomicPos-maxDistToGenomicPosUp,genomicPos+maxDistToGenomicPosDown</i>].
     * <p>
     * The keys for the <i>HashMap</i> will be the cutting sites, e.g., GATC (without '^' characters).
     * In addition to the specific sites, there is one special key <i>ALL</i> which contains the cutting positions for the union of all motifs.
     *
     * @param referenceSequenceID     name of the genomic sequence, e.g. {@code chr1}.
     * @param genomicPos              central position of the region for which the CuttingPositionMap is created (one-based numbering).
     * @param maxDistToGenomicPosUp   maximal distance to 'genomicPos' in upstream direction.
     * @param maxDistToGenomicPosDown maximal distance to 'genomicPos' in downstream direction.
     * @param fastaReader             indexed FASTA file that contains the sequence information required for the calculation of cutting positions.
     * @param chosenEnzymeList        List of cutting {@link RestrictionEnzyme} objects.
     */
    public SegmentFactory(String referenceSequenceID,
                          Integer genomicPos,
                          IndexedFastaSequenceFile fastaReader,
                          Integer maxDistToGenomicPosUp,
                          Integer maxDistToGenomicPosDown,
                          List<RestrictionEnzyme> chosenEnzymeList) {
        setGenomicPos(genomicPos);
        setMaxDistToGenomicPosUp(maxDistToGenomicPosUp);
        setMaxDistToGenomicPosDown(maxDistToGenomicPosDown);
        cuttingPositionMap = new HashMap<String, ArrayList<Integer>>();
        Set<Integer> allPositionSet = new HashSet<>(); // remove duplicates
        ArrayList<Integer> cuttingPositionListUnion = new ArrayList<>();

        int chromosomeLength=fastaReader.getSequence(referenceSequenceID).length();

        for (RestrictionEnzyme enzyme : chosenEnzymeList) {
            String cutpat = enzyme.getPlainSite();
            int offset = enzyme.getOffset();
            // get sequence around genomic position and convert everything to uppercase
            if (chromosomeLength < genomicPos + maxDistToGenomicPosDown) {
                logger.warn("maxDistToGenomicPosDown = " + maxDistToGenomicPosDown + " plus genomicPos = " + genomicPos + " greater than than the length of " + referenceSequenceID + " (" + chromosomeLength + ").");
                maxDistToGenomicPosDown = chromosomeLength - genomicPos;
            }
            // note fastaReader refers to one-based numbering scheme.
            String genomicPosRegionString = fastaReader.getSubsequenceAt(referenceSequenceID, genomicPos - maxDistToGenomicPosUp, genomicPos + maxDistToGenomicPosDown).getBaseString().toUpperCase();
            Pattern pattern = Pattern.compile(cutpat);
            Matcher matcher = pattern.matcher(genomicPosRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<>();
            int fragmentStart=genomicPos - maxDistToGenomicPosUp; /* one-based position of first nucleotide in the entire subsequence returned by fasta reader */
            while (matcher.find()) {
                    // replaces matcher.start() - maxDistToGenomicPosUp + offset;
                    int pos = fragmentStart + matcher.start() + offset; /* one-based position of first nucleotide after the restriction enzyme cuts */
                    cuttingPositionList.add(pos);
                    allPositionSet.add(pos);
            }
            cuttingPositionMap.put(enzyme.getPlainSite(), cuttingPositionList); // push array list to map
        }

        cuttingPositionListUnion.addAll(allPositionSet);
        Collections.sort(cuttingPositionListUnion);
        cuttingPositionMap.put("ALL", cuttingPositionListUnion); // push array list to map
    }



    public final void setGenomicPos(Integer genomicPos) {
        this.genomicPos = genomicPos;
    }
    /** @return the genomic position of the center of the ViewPoint. */
    public final Integer getGenomicPos() {
        return genomicPos;
    }


    public final void setMaxDistToGenomicPosUp(Integer maxDistToGenomicPosUp) {
        this.maxDistToGenomicPosUp = maxDistToGenomicPosUp;
    }

    public final Integer getMaxDistToGenomicPosUp() {
        return maxDistToGenomicPosUp;
    }


    public final void setMaxDistToGenomicPosDown(Integer maxDistToGenomicPosDown) {
        this.maxDistToGenomicPosDown = maxDistToGenomicPosDown;
    }

    public final Integer getMaxDistToGenomicPosDown() {
        return maxDistToGenomicPosDown;
    }


    /** @return only the <i>HashMap</i> object for the cutting positions (which are relative to {@link #genomicPos}). */
    public final HashMap<String, ArrayList<Integer>> getCuttingPositionHashMap() {
        return cuttingPositionMap;
    }

    /** @return only the <i>ArrayList</i> of <i>Integers</i> (relative to {@link #genomicPos}) for a given motif. */
    public final ArrayList<Integer> getAllCutsForGivenMotif(String motif) {
        return cuttingPositionMap.get(motif);
    }
    /** @return List of cuttings positions (relative to {@link #genomicPos} for all Restriction enzymes. */
    public final List<Integer> getAllCuts() {
        return cuttingPositionMap.get("ALL");
    }


    public Integer getUpstreamCut(int j) {
        // replaces return relToAbsPos(getAllCuts().get(j));
        return getAllCuts().get(j);
    }


    public Integer getDownstreamCut(int j) {
        // replaces return relToAbsPos(getAllCuts().get(j+1));
        return getAllCuts().get(j+1);
    }


}
