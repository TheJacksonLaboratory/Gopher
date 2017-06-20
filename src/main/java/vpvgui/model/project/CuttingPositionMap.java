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
import vpvgui.exception.IntegerOutOfRangeException;
import vpvgui.exception.NoCuttingSiteUpStreamException;

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
            String tssRegionString = fastaReader.getSubsequenceAt(referenceSequenceID,genomicPos - maxDistToTssUp,genomicPos+maxDistToTssDown).getBaseString().toUpperCase();

            Pattern pattern = Pattern.compile(cuttingPatterns[i]);
            Matcher matcher = pattern.matcher(tssRegionString);
            ArrayList<Integer> cuttingPositionList = new ArrayList<Integer>();

            while(matcher.find()) {

                if (matcher.start()<=genomicPos) {
                    cuttingPositionList.add(matcher.start() - maxDistToTssUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                    cuttingPositionListUnion.add(matcher.start()-maxDistToTssUp + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
                }
                else if (genomicPos<matcher.start()) {
                    cuttingPositionList.add(matcher.start() - genomicPos + cuttingPositionMapOffsets.get(cuttingPatterns[i]));
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

    public final HashMap<String,Integer> getCuttingPositionMapOffsets() {
        return cuttingPositionMapOffsets;
    }

    /* utilities */


    public Integer getNextCutPos(Integer pos, String direction) {

         /*
         * Given a position relative to 'genomicPos' that is within the interval [maxDistToTssUp,maxDistToTssDown],
         * return the next cutting position upstream.
         *
         * The given and returned positions are relative to the TSS.
         *
         * TODO: Handle exceptions using try catch contruct.
         * TODO: Test fucnction.
         *
         */

        if(pos<-maxDistToTssUp || pos>maxDistToTssDown) {
            System.out.println("The specified position is out of range. Will change nothing.");
            return pos;
        }

        ArrayList<Integer> cutPosArray = new ArrayList<Integer>();
        cutPosArray = cuttingPositionMap.get("ALL");
        Integer returnCutPos=cutPosArray.get(cutPosArray.size()-1);

        if( direction=="up")
        {
            Collections.reverse(cutPosArray);
            returnCutPos=cutPosArray.get(cutPosArray.size()-1);
        }

        Iterator<Integer> cutPosArrayIt = cutPosArray.iterator();
        while(cutPosArrayIt.hasNext()) {
            Integer nextCutPos=cutPosArrayIt.next();
            if(direction=="down" && (pos<=nextCutPos)) {
                returnCutPos=nextCutPos;break;
            }
            if(direction=="up" && (pos>=nextCutPos)) {
                System.out.println(returnCutPos);
                returnCutPos=nextCutPos;break;
            }
        }
        if(!cutPosArrayIt.hasNext()) {
            System.out.println("No cutting site " + direction + "stream of position " + pos + ". Will return the " +
                    "outermost cutting site in " + direction + "stream direction.");
        }
        return returnCutPos;
    }
}
