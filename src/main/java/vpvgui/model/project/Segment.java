package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.util.ArrayList;

public class Segment {

    /* fields */

    private String referenceSequenceID;
    private Integer startPos;
    private Integer endPos;
    private boolean selected;
    private double repetitiveContent;
    private  Integer genomicPos;


    /* constructor */

    public Segment(String referenceSequenceID, Integer startPos, Integer endPos, boolean selected) {

        this.referenceSequenceID = referenceSequenceID;
        this.startPos = startPos; // absolute coordinate
        this.endPos = endPos;     // absolute coordinate
        setSelected(selected);
        this.genomicPos=genomicPos;
    }


    /* getter and setter methods */

    public Integer getStartPos() {
        return startPos;
    }

    public Integer getEndPos() {
        return endPos;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected() {
        return selected;
    }

    public Integer length() {
        return endPos - startPos + 1;
    }


    public void setRepetitiveContent(IndexedFastaSequenceFile fastaReader) {

        /* check if 'referenceSequenceID' is contained in 'fastaReader' */

        // TODO


        /* get dna string */

        String s = fastaReader.getSubsequenceAt(referenceSequenceID, startPos, endPos).getBaseString();


        /* determine repetitive content */

        Integer lowerCase = 0, upperCase = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) lowerCase++;
            if (Character.isUpperCase(s.charAt(i))) upperCase++;
        }

        this.repetitiveContent = ((double) lowerCase / (lowerCase + (double) upperCase));
    }


    public double getRepetitiveContent() {
        return repetitiveContent;
    }

    public ArrayList<Segment> getSegmentMargins(Integer marginSize) {

        ArrayList<Segment> marginList = new ArrayList<>();

        Segment upStreamFrag;
        Segment downStreamFrag;

        if (2 * marginSize < length()) { // return a pair of Segment objects

            upStreamFrag = new Segment(referenceSequenceID, startPos, startPos + marginSize-1, true);
            marginList.add(upStreamFrag);
            downStreamFrag = new Segment(referenceSequenceID, endPos - marginSize + 1, endPos, true);
            marginList.add(downStreamFrag);

        } else { // return a single Segment object with odentical coordinates as the original Segment object

            upStreamFrag = new Segment(referenceSequenceID, startPos, endPos, true);
            marginList.add(upStreamFrag);
        }
        return marginList;
    }
}


