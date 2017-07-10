package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class Segment {

    /* fields */

    private String referenceSequenceID;
    private Integer startPos;
    private Integer endPos;
    private boolean selected;
    private double repetitiveContent;
    private  Integer genomicPos;


    /* constructor */

    public Segment(String referenceSequenceID, Integer startPos, Integer endPos, Integer genomicPos, boolean selected) {

        this.referenceSequenceID = referenceSequenceID;
        this.startPos = startPos;
        this.endPos = endPos;
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

        String s = fastaReader.getSubsequenceAt(referenceSequenceID, startPos+genomicPos+1, endPos+genomicPos+1).getBaseString();


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
}
