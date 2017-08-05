package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.IntPair;

import java.util.ArrayList;

/**
 * This class is a temporary substitute fot the class <i>Fragment</i>, because there was consensus about the concept of the class fragment.
 * The class represents a restriction fragment that is a member of a viewpoint.
 */
public class Segment {
    private static final Logger logger = Logger.getLogger(Segment.class.getName());
    /** The id of the larger sequence where this Segment is located (usually a chromosome).*/
    private String referenceSequenceID;
    /** The most 5' position of this Segment on the {@link #referenceSequenceID}*/
    private Integer startPos;
    /** The most 3' position of this Segment on the {@link #referenceSequenceID}*/
    private Integer endPos;
    /** The value of this variable is true if this Segment is selected (and will thus be used for Capture HiC probe production).*/
    private boolean selected;
    private double repeatContent;
    private double repeatContentUp;
    private double repeatContentDown;
    //private Integer genomicPos;
    private IndexedFastaSequenceFile fastaReader;
    private Integer marginSize;


    /* constructor -- we will move to the builder. */
    @Deprecated
    public Segment(String referenceSequenceID, Integer startPos, Integer endPos, boolean selected, IndexedFastaSequenceFile fastaReader) {

        this.referenceSequenceID = referenceSequenceID;
        this.startPos = startPos; // absolute coordinate
        this.endPos = endPos;     // absolute coordinate
       // this.genomicPos = genomicPos;
        this.fastaReader = fastaReader;
        setSelected(selected);
        calculateRepeatContent();
        this.marginSize=150; /* todo--depreacted */
        calculateRepeatContentMargins();
    }


    private Segment(Builder builder) {
        this.referenceSequenceID=builder.referenceSequenceID;
        this.startPos=builder.startPos;
        this.endPos=builder.endPos;
        //this.genomicPos=builder.genomicPos;
        this.fastaReader=builder.fastaReader;
        this.marginSize=builder.marginSize;
        this.selected=false; /* default */
        calculateRepeatContent();
        calculateRepeatContentMargins();
    }




    public static class Builder {
        private String referenceSequenceID;
        private Integer startPos;
        private Integer endPos;
        private Integer genomicPos;
        private IndexedFastaSequenceFile fastaReader;
        private Integer marginSize;

        public Builder(String refSequenceID, Integer start, Integer end) {
            this.referenceSequenceID=refSequenceID;
            this.startPos=start;
            this.endPos=end;
        }
        public Builder fastaReader(IndexedFastaSequenceFile val) {
            this.fastaReader=val; return this;
        }
        public Builder genomicPos(Integer val) {
            this.genomicPos=val; return this;
        }
        public Builder marginSize(Integer val) {
            this.marginSize=val; return this;
        }
        public Segment build() {
            return new Segment(this);
        }

    }





    /* getter and setter methods */

    public void setStartPos(Integer startPos) { this.startPos=startPos; }

    public Integer getStartPos() {
        return startPos;
    }

    public void setEndPos(Integer endPos) {
        this.startPos=endPos;
    }

    public Integer getEndPos() {
        return endPos;
    }
    /** @param selected true if the segment is to be included in the sequences for the probes for this viewpoint.*/
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }
    /** Length of the entire segment in nucleotides. */
    public Integer length() {
        return endPos - startPos + 1;
    }


    /**
     * This function calculates the {@link #repeatContent} of this segment by counting lower and uppercase.
     */
    private void calculateRepeatContent() {
        /* get dna string */
        String s = this.fastaReader.getSubsequenceAt(referenceSequenceID, startPos, endPos).getBaseString();

        /* determine repeat content */
        Integer lowerCase = 0, upperCase = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) lowerCase++;
            if (Character.isUpperCase(s.charAt(i))) upperCase++;
        }

        this.repeatContent = ((double) lowerCase / (lowerCase + (double) upperCase));
    }

    /**
     * Calculates the repat content on the margins of the segment (if the segment is too small, we take the
     * repeat content of the entire segment to be the margin repeat content).
     */
    private void calculateRepeatContentMargins() {
        /* generate Segment objects for margins */
        ArrayList<IntPair> margins = getSegmentMargins();
        if (margins.size()==1) {
            /* this means there is not enough space to have two margins, because the fragment is too small.
            * We therefore just use the overall repeat content for the up/downstream values.
             */
            this.repeatContentDown=this.repeatContent;
            this.repeatContentUp=this.repeatContent;
            return;
        } else if (margins.size()>2 || margins.size()==0) {
            ErrorWindow.display("Error in Segment Class","Number of margin segments was neither 1 nor 2 (report to developers)");
            return;
        }
        /* If we get here, then we have two IntPair segments, for Up and Downstream */
         for(int i=0;i<margins.size();++i) {
            IntPair seg =margins.get(i);
            int start=seg.getStartPos();
            int end=seg.getEndPos();
            String s = this.fastaReader.getSubsequenceAt(this.referenceSequenceID,start,end).getBaseString();
            /* determine repeat content */
            Integer lowerCase = 0, upperCase = 0;
            for (int j = 0; j < s.length(); j++) {
                if (Character.isLowerCase(s.charAt(j))) lowerCase++;
                if (Character.isUpperCase(s.charAt(j))) upperCase++;
            }
            double repcon = ((double) lowerCase / (lowerCase + (double) upperCase));
            if (i==0) {
                this.repeatContentUp = repcon;
            } else if (i==1) {
                this.repeatContentDown = repcon;
            }
        }
    }


    /**
     * This function returns the repetitive content calculated by the function {@link #calculateRepeatContent}.
     *
     * @return repeat content of this segment.
     */
    public double getRepeatContent() {
        return repeatContent;
    }
    private void setRepeatContent(double val) {this.repeatContent=val;}
    public String getRepeatContentAsPercent() { return String.format("%.2f%%",100*repeatContent);}

    public double getRepeatContentMarginUp() {
        return repeatContentUp;
    }

    public double getRepeatContentMarginDown() {
        return repeatContentDown;
    }


    public double getMeanMarginRepeatContent() { return 0.5*(repeatContentUp+repeatContentDown);}



    /**
     * This function creates either a list with one new object of the class {@link IntPair},
     * if this {@link Segment} is shorter than {@code 2 * marginSize},
     * or a list with two new objects of class {@link IntPair} with a length of {@code marginSize} otherwise.
     * @return list of either one or two {@link IntPair} objects.
     */
    public ArrayList<IntPair> getSegmentMargins() {

        ArrayList<IntPair> marginList = new ArrayList<>();

        IntPair upStreamFrag;
        IntPair downStreamFrag;

        if (2 * marginSize < length()) { // return a pair of Segment objects

            upStreamFrag = new IntPair(startPos, startPos + this.marginSize - 1);
            marginList.add(upStreamFrag);
            downStreamFrag = new IntPair(endPos - this.marginSize + 1, endPos);
            marginList.add(downStreamFrag);

        } else { // return a single Segment object with identical coordinates as the original Segment object

            upStreamFrag = new IntPair(startPos, endPos);
            marginList.add(upStreamFrag);
        }
        return marginList;
    }
}
