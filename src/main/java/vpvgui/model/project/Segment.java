package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.IntPair;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * This class is a temporary substitute fot the class <i>Fragment</i>, because there was consensus about the concept of the class fragment.
 * The class represents a restriction fragment that is a member of a viewpoint.
 */
public class Segment implements Serializable {
    private static final Logger logger = Logger.getLogger(Segment.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    /** The id of the larger sequence where this Segment is located (usually a chromosome).*/
    private String referenceSequenceID;
    /** The most 5' position of this Segment on the {@link #referenceSequenceID}. */
    private Integer startPos;
    /** The most 3' position of this Segment on the {@link #referenceSequenceID}. */
    private Integer endPos;
    /** The value of this variable is true if this Segment is selected (and will thus be used for Capture HiC probe production). */
    private boolean selected;
    /** The repetitive content of an object of class Segment. */
    private double repeatContent;
    /** The repetitive content of the margin in upstream direction of this object of class Segment. */
    private double repeatContentUp;
    /** The repetitive content of the margin in downstream direction of this object of class Segment. */
    private double repeatContentDown;
    /** Object for reading a given FASTA file (HTSJDK). */
    //private IndexedFastaSequenceFile fastaReader;
    /** Size of the margins in up and downstream direction. */
    private Integer marginSize;
    /** Used to return {@link #startPos} and {@link #endPos} in String format. */
    private static DecimalFormat formatter = new DecimalFormat("#,###");


    /* constructor -- we will move to the builder. */
    @Deprecated
    public Segment(String referenceSequenceID, Integer startPos, Integer endPos, boolean selected, IndexedFastaSequenceFile fastaReader) {

        this.referenceSequenceID = referenceSequenceID;
        this.startPos = startPos; // absolute coordinate
        this.endPos = endPos;     // absolute coordinate
        //this.fastaReader = fastaReader;
        setSelected(selected);
        calculateRepeatContent(fastaReader);
        this.marginSize=150; /* todo--deprecated */
        calculateRepeatContentMargins(fastaReader);
    }


    private Segment(Builder builder) {
        this.referenceSequenceID=builder.referenceSequenceID;
        this.startPos=builder.startPos;
        this.endPos=builder.endPos;
        //this.fastaReader=builder.fastaReader;
        this.marginSize=builder.marginSize;
        this.selected=false; /* default */
        calculateRepeatContent(builder.fastaReader);
        calculateRepeatContentMargins(builder.fastaReader);
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

    /**
     * Set the absolute coordinate of the starting position of the Segment.
     *
     * @param startPos absolute coordinate of the starting position of the Segment.
     */
    public void setStartPos(Integer startPos) { this.startPos=startPos; }

    /**
     * Returns starting position of the Segment.
     * @return Starting position of the Segment.
     */
    public Integer getStartPos() {
        return startPos;
    }

    /**
     * Set the absolute coordinate of the end position of the Segment.
     *
     * @param endPos absolute coordinate of the end position of the Segment.
     */
    public void setEndPos(Integer endPos) {
        this.startPos=endPos;
    }

    /**
     * Returns end position of the Segment.
     * @return End position of the Segment.
     */
    public Integer getEndPos() {
        return endPos;
    }

    /**
     *
     * @param selected true if the segment is to be included in a viewpoint.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Returns true, if the Segment is selected, otherwise false.
     * @return true, if the Segment is selected, otherwise false.
     */
    public boolean isSelected() {
        return selected;
    }


    /**
     * Returns length of the segment:
     *
     * @return Length of the segment.
     */
    public Integer length() {
        return endPos - startPos + 1;
    }


    /**
     * This function calculates the {@link #repeatContent} of this segment by counting lower and uppercase.
     */
    private void calculateRepeatContent(IndexedFastaSequenceFile fastaReader) {

        /* get dna string */
        String s = fastaReader.getSubsequenceAt(referenceSequenceID, startPos, endPos).getBaseString();

        /* determine repeat content */

        Integer lowerCase = 0, upperCase = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) lowerCase++;
            if (Character.isUpperCase(s.charAt(i))) upperCase++;
        }

        this.repeatContent = ((double) lowerCase / (lowerCase + (double) upperCase));
    }

    /**
     * Calculates the repetitive content on the margins of the segment (if the segment is too small, we take the
     * repeat content of the entire segment to be the margin repeat content).
     */
    private void calculateRepeatContentMargins(IndexedFastaSequenceFile fastaReader) {

        /* generate Segment objects for margins */

        ArrayList<IntPair> margins = getSegmentMargins();

        if (margins.size()==1) { // not enough space to have two margins, because the fragment is too small
            this.repeatContentDown=this.repeatContent; // we therefore just use the overall repeat content for the up/downstream values
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
            String s = fastaReader.getSubsequenceAt(this.referenceSequenceID,start,end).getBaseString();

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
    public String getRepeatContentMarginUpAsPercent() { return String.format("%.2f%%",100*repeatContentUp);}

    public double getRepeatContentMarginDown() {
        return repeatContentDown;
    }
    public String getRepeatContentMarginDownAsPercent() { return String.format("%.2f%%",100*repeatContentDown);}

    public double getMeanMarginRepeatContent() { return 0.5*(repeatContentUp+repeatContentDown);}

    public String getChromosomalPositionString() {
        String s=formatter.format(startPos);
        String e=formatter.format(endPos);
        return String.format("%s:%s-%s",referenceSequenceID,s,e);
    }


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

    /**
     * If the segment is large enough,it will contain two separate margins each of which is of size {@link #marginSize}.
     * For smaller segments, the entire segment is taken to be the "margin",and we return the size of the entire segment.
     * @return The total size of the margin(s) of this {@link Segment}.
     */
    public int getMarginSize() {
        if (2 * marginSize < length()) { // return a pair of Segment objects
            return 2*this.marginSize;
        } else { // return a single Segment object with identical coordinates as the original Segment object
            return (endPos-startPos+1);
        }
    }


    /** Hash code is based on the end and start positions as well as on the chromosome */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + startPos;
        result = prime * result + endPos;
        result = prime * result + referenceSequenceID.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Segment other = (Segment) obj;
        if (!referenceSequenceID.equals(other.referenceSequenceID))
            return false;
        if (startPos != other.startPos)
            return false;
        if (endPos != other.endPos)
            return false;
        return true;
    }

}
