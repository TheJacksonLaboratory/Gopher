package vpvgui.model.project;

/*
 * This a utility class for the class 'ViewPoint'.
 * The constructor is called with an 'IndexedFastaSequenceFile' (htsjdk), a reference sequence ID, a genomic position,
 * a list of cutting motifs, and maximum distances to the genomic position in up and downstream direction.
 *
 * The reference sequence ID and the genomic position need to exist in the must in the 'IndexedFastaSequenceFile',
 * otherwise an error will be thrown and no object will be created.
 *
 * The most central data structure in the class a 'HashMap' with 'Strings' as keys and 'ArrayLists' of 'Integers'
 * as values. The keys are cutting site motifs and the 'ArrayList' contains all positions of occurrences
 * relative to the transcription start site. There is one special key which contains the positions for the
 * union of all positions.
 *
 * In addition the usual getter and setter functions,
 * the class provides functions to navigate through the cutting sites of the viewpoints,
 * e.g. given a position within the interval [maxDistToTssUp,maxDistToTssDown] a function returns the
 * the postion of the next cuttig site up or downstream.
 *
 * Furthermore, there might be functions that return the lengths of all restriction fragments
 * within the interval [maxDistToTssUp,maxDistToTssDown].
 *
 */

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CuttingPositionMap {

    /* fields */

    String referenceSequenceID;
    Integer genomicPos;
    Integer maxDistToTssUp;
    Integer maxDistToTssDown;
    private static HashMap<String,ArrayList<Integer>> cuttingPositionMap;
    private static HashMap<String,Integer> cuttingPositionMapOffsets;


    /* constructor */

    public CuttingPositionMap(IndexedFastaSequenceFile fastaReader, String referenceSequenceID, Integer genomicPos, String[] cuttingPatterns, Integer maxDistToTssUp, Integer maxDistToTssDown) {


        /* set fields */

        setReferenceID(referenceSequenceID);
        setGenomicPos(genomicPos);
        setMaxDistToTssUp(maxDistToTssUp);
        setMaxDistToTssDown(maxDistToTssDown);


        /* create maps */

        // determine offsets
        cuttingPositionMapOffsets = new HashMap<String,Integer>();
        for(int i=0;i<cuttingPatterns.length;i++) {
            Integer offSet=cuttingPatterns[i].indexOf('^');
            cuttingPatterns[i] = cuttingPatterns[i].replace("^",""); // remove '^' characters
            cuttingPositionMapOffsets.put(cuttingPatterns[i],offSet);
            System.out.println(cuttingPositionMapOffsets.get(cuttingPatterns[i]));
        }

        cuttingPositionMap = new HashMap<String,ArrayList<Integer>>();

        // TODO: So far, the starting position of the map is taken as cutting position,
        // TODO: But it would be more correct to use an offset parameter and take the real cutting within the motif,
        // TODO: which can be indivcated by a '^' character, e.g. for A^GATCT, would be 1.
        ArrayList<Integer> cuttingPositionListUnion = new ArrayList<Integer>();
        for(int i=0;i<cuttingPatterns.length;i++) {

            // get sequence around genomic position and convert everything to uppercase
            String tssRegionString = fastaReader.getSubsequenceAt(referenceSequenceID,genomicPos-maxDistToTssUp,genomicPos+maxDistToTssDown).getBaseString().toUpperCase();

            Pattern pattern = Pattern.compile(cuttingPatterns[i]);
            Matcher matcher = pattern.matcher(tssRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<Integer>();

            while(matcher.find()) {

                if (matcher.start()<=genomicPos) {
                    cuttingPositionList.add(matcher.start() - maxDistToTssUp);
                    cuttingPositionListUnion.add(matcher.start()-maxDistToTssUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                }
                else if (genomicPos<matcher.start()) {
                    cuttingPositionList.add(matcher.start() - genomicPos);
                    cuttingPositionListUnion.add(matcher.start() - genomicPos + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
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


        Iterator it = cuttingPositionMap.entrySet().iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
/*
System.out.println(cuttingPositionMap.size());
        for(int i=0;i<cuttingPatterns.length;i++) {
            System.out.println(cuttingPatterns[i]);
            System.out.println(cuttingPositionMap.get(cuttingPatterns[i]));
        }
*/

    }


    /* getter and setter functions */

    public final String getReferenceID() {
        return referenceSequenceID;
    }

    public final void setReferenceID(String setReferenceID) {
        this.referenceSequenceID=setReferenceID;
    }


    public final void setGenomicPos(Integer genomicPos) { this.genomicPos=genomicPos; }

    public final Integer getGenomicPos() {
        return genomicPos;
    }


    public final void setMaxDistToTssUp(Integer maxDistToTssUp) { this.maxDistToTssUp=maxDistToTssUp; }

    public final Integer getMaxDistToTssUp() {
        return maxDistToTssUp;
    }


    public final void setMaxDistToTssDown(Integer maxDistToTssDown) { this.maxDistToTssDown=maxDistToTssDown; }

    public final Integer getMaxDistToTssDown() {
        return maxDistToTssDown;
    }


    public final HashMap<String,ArrayList<Integer>> getCuttingPositionMap() {
        return cuttingPositionMap;
    }



    /* utilities */

    public Integer getNextCutUp(){
        return 0;
    }

    public Integer getNextCutDown(){
        return 0;
    }

}
