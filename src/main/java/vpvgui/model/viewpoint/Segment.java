package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import vpvgui.gui.popupdialog.PopupFactory;
import vpvgui.model.IntPair;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * This class represents a restriction fragment that is a member of a viewpoint.
 * Note that {@link #startPos} and {@link #endPos} use one-based inclusive numbering.
 * @author Peter Hansen
 * @version 0.3.5 (2017-09-19)
 */
public class Segment implements Serializable {
    private static final Logger logger = Logger.getLogger(Segment.class.getName());

    public boolean isOverlapsTSS() {
        return overlapsTSS;
    }

    public void setOverlapsTSS(boolean overlapsTSS) {
        this.overlapsTSS = overlapsTSS;
    }

    /** serialization version ID */
    static final long serialVersionUID = 3L;
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
    /** Proportion of Gs anc Cs in sequence */
    private double GCcontent;

    private double GCcontentDown;

    private double GCcontentUp;



    /** Size of the margins in up and downstream direction. */
    private Integer marginSize;
    /** Is this fragment overlapping the TSS, i.e., it is the center fragment? */
    private boolean overlapsTSS=false;
    /** Used to return {@link #startPos} and {@link #endPos} in String format. */
    private static DecimalFormat formatter = new DecimalFormat("#,###");

    private Segment(Builder builder) {
        this.referenceSequenceID=builder.referenceSequenceID;
        this.startPos=builder.startPos;
        this.endPos=builder.endPos;
        this.marginSize=builder.marginSize;
        this.selected=false; /* default */
        calculateGCandRepeatContent(builder.fastaReader);
        calculateRepeatAndGcContentMargins(builder.fastaReader);
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

    /**
     * @param startPos absolute coordinate of the starting position of the Segment.
     */
    public void setStartPos(Integer startPos) { this.startPos=startPos; }

    /** @return Starting position of the Segment. */
    public Integer getStartPos() {
        return startPos;
    }

    /** @param endPos absolute coordinate of the end position of the Segment. */
    public void setEndPos(Integer endPos) {
        this.startPos=endPos;
    }

    /** @return End position of the Segment. */
    public Integer getEndPos() {
        return endPos;
    }

    /** @param selected true if the segment is to be included in a viewpoint. */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** @return true, if the Segment is selected, otherwise false. */
    public boolean isSelected() {
        return selected;
    }


    /** @return Length of the segment. */
    public Integer length() {
        return endPos - startPos + 1;
    }


    /**
     * This function calculates the {@link #repeatContent} of this segment by counting lower and uppercase.
     */
    private void calculateGCandRepeatContent(IndexedFastaSequenceFile fastaReader) {
        String subsequence = fastaReader.getSubsequenceAt(referenceSequenceID, startPos, endPos).getBaseString();
        /* determine repeat content */
        int lowerCase = 0, upperCase = 0; int GC=0;
        if (subsequence.length()==0) return;
        for (int i = 0; i < subsequence.length(); i++) {
            if (Character.isLowerCase(subsequence.charAt(i))) lowerCase++;
            if (Character.isUpperCase(subsequence.charAt(i))) upperCase++;
            switch (subsequence.charAt(i)){
                case 'G':
                case 'g':
                case 'C':
                case 'c':
                    GC++;
            }
        }
        this.repeatContent = ((double) lowerCase / (lowerCase + (double) upperCase));
        this.GCcontent=(double)GC/(double)subsequence.length();
    }

    /**
     * Calculates the repetitive and GC content on the margins of the segment (if the segment is too small, we take the
     * repeat content of the entire segment to be the margin repeat content).
     */
    private void calculateRepeatAndGcContentMargins(IndexedFastaSequenceFile fastaReader) {

        /* generate Segment objects for margins */

        ArrayList<IntPair> margins = getSegmentMargins();

        if (margins.size()==1) { // not enough space to have two margins, because the fragment is too small
            this.repeatContentDown=this.repeatContent; // we therefore just use the overall repeat content for the up/downstream values
            this.repeatContentUp=this.repeatContent;
            this.GCcontentDown=GCcontent;
            this.GCcontentUp=GCcontent;
            return;
        } else if (margins.size()!=2) {
            PopupFactory.displayError("Error in Segment Class","Number of margin segments was neither 1 nor 2 (report to developers)");
            return;
        }

        /* If we get here, then we have two IntPair segments, for Up and Downstream */

         for(int i=0; i<margins.size(); ++i) {
            IntPair seg = margins.get(i);
            int start = seg.getStartPos();
            int end = seg.getEndPos();
            String s = fastaReader.getSubsequenceAt(this.referenceSequenceID,start,end).getBaseString();

            /* determine repeat content */
            int lowerCase = 0, upperCase = 0;
            int gc = 0; int at = 0;
            for (int j = 0; j < s.length(); j++) {
                if (Character.isLowerCase(s.charAt(j))) lowerCase++;
                if (Character.isUpperCase(s.charAt(j))) upperCase++;
                if (s.charAt(j)=='A' || s.charAt(j)=='T' || s.charAt(j)=='a' || s.charAt(j)=='t') at++;
                if (s.charAt(j)=='G' || s.charAt(j)=='C' || s.charAt(j)=='g' || s.charAt(j)=='c') gc++;
            }
            double repcon = ((double) lowerCase / (lowerCase + (double) upperCase));
            double gccon = ((double) gc / (lowerCase + (double) (at+gc)));
            if (i==0) {
                this.repeatContentUp = repcon;
                this.GCcontentUp = gccon;
            } else if (i==1) {
                this.repeatContentDown = repcon;
                this.GCcontentDown = gccon;
            }
        }
    }

    public String getReferenceSequenceID() { return referenceSequenceID; }
    /**
     * This function returns the repeat content calculated by the function {@link #calculateGCandRepeatContent}.
     * @return repeat content of this segment.
     */
    public double getRepeatContent() {
        return repeatContent;
    }
    /** @return A formatted string, e.g., "7.23%", representing the repeat content */
    public String getRepeatContentAsPercent() { return String.format("%.2f%%",100*repeatContent);}

    public double getRepeatContentMarginUp() {
        return repeatContentUp;
    }
    /** @return A formatted string, e.g., "7.23%", representing the repeat content of the margin on the up side */
    public String getRepeatContentMarginUpAsPercent() { return String.format("%.2f%%",100*repeatContentUp);}

    public double getRepeatContentMarginDown() {
        return repeatContentDown;
    }
    public double getGcContentMarginDown() {
        return GCcontentDown;
    }
    public double getGcContentMarginUp() {
        return GCcontentUp;
    }
    /** @return A formatted string, e.g., "7.23%", representing the repeat content of the margin on the down side */
    public String getRepeatContentMarginDownAsPercent() { return String.format("%.2f%%",100*repeatContentDown);}

    public double getMeanMarginRepeatContent() { return 0.5*(repeatContentUp+repeatContentDown);}

    public String getGCcontentAsPercent() {
        return String.format("%.2f%%",100*GCcontent);
    }

    public String getGCcontentUpAsPercent() {
        return String.format("%.2f%%",100*GCcontentUp);
    }

    public String getGCcontentDownAsPercent() {
        return String.format("%.2f%%",100*GCcontentDown);
    }

    public double getGCcontent() {
        return GCcontent;
    }
    /** @return a String such as {@code chr3:425930-736434}. */
    public String getChromosomalPositionString() {
        String s=formatter.format(startPos);
        String e=formatter.format(endPos);
        String location=String.format("%s:%s-%s",referenceSequenceID,s,e);
        if (overlapsTSS)
            return String.format("%s (*)",location);
        else
            return location;
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
        return Math.min(2*marginSize,length());
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
