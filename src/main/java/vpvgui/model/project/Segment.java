package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class Segment {

    /* fields */

    private String referenceSequenceID;
    private Integer startPos;
    private Integer endPos;
    private boolean selected;
    private float repetitiveContent;


    /* constructor */

    public Segment(String referenceSequenceID, Integer startPos, Integer endPos, boolean selected) {

        this.referenceSequenceID = referenceSequenceID;
        this.startPos = startPos;
        this.endPos = endPos;
        setSelected(selected);
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

        repetitiveContent = (float) lowerCase / (lowerCase + upperCase);
    }


    public float getRepetitiveContent() {
        return repetitiveContent;
    }
}
