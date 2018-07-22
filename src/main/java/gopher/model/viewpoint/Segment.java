package gopher.model.viewpoint;

import gopher.model.Model;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import gopher.gui.popupdialog.PopupFactory;
import gopher.model.IntPair;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a restriction digest that is a member of a viewpoint.
 * Note that {@link #startPos} and {@link #endPos} use one-based inclusive numbering.
 * @author Peter Hansen
 * @version 0.3.6 (2018-07-17)
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
    private boolean unselectable;
    private boolean targetable;
    private boolean rescuable;
    /** This is true if the fragment was selected when the ViewPoint was originally created. If
     * originallySelected != selected, then this Fragment was manually changed by the user. */
    private boolean originallySelected;

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

    private transient IndexedFastaSequenceFile fastaReader;

    private final List<Bait> baitListUpStreamMargin;
    private final List<Bait> baitListDownStreamMargin;


    /** Size of the margins in up and downstream direction. */
    private Integer marginSize;
    /** Is this digest overlapping the TSS, i.e., it is the center digest? */
    private boolean overlapsTSS=false;
    /** Used to return {@link #startPos} and {@link #endPos} in String format. */
    private static DecimalFormat formatter = new DecimalFormat("#,###");

    private Segment(Builder builder) {
        this.referenceSequenceID=builder.referenceSequenceID;
        this.startPos=builder.startPos;
        this.endPos=builder.endPos;
        this.marginSize=builder.marginSize;
        this.selected=false; /* default */
        this.fastaReader = builder.fastaReader;
        this.baitListDownStreamMargin=new ArrayList<>();
        this.baitListUpStreamMargin=new ArrayList<>();
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

    /**
     * @param selected true if the segment is to be included in a viewpoint.
     * @param updateOrginallySelected if true, then we are creating the viewpoint and want to record the original state of this fragment
     * */
    public void setSelected(boolean selected, boolean updateOrginallySelected) {
        this.selected = selected;
        if (updateOrginallySelected) {
            originallySelected=selected;
        }
    }

    public boolean wasOriginallySelected() {
        return this.originallySelected;
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

        List<IntPair> margins = getSegmentMargins();

        if (margins.size()==1) { // not enough space to have two margins, because the digest is too small
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
    public List<IntPair> getSegmentMargins() {

        List<IntPair> marginList = new ArrayList<>();

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
        if (! startPos.equals(other.startPos))
            return false;
        return endPos.equals(other.endPos);
    }

    /**
     * this is used for the binary tree.
     */
    public boolean preceeds(Segment other) {
        if (other.getReferenceSequenceID().compareTo(getReferenceSequenceID()) == 0) {
            // we are on same chromosome -- check the position
            return getStartPos() < other.getStartPos();
        } else {
            // return true iff this chromosome lexigraphicall preceeds the other one
            return (getReferenceSequenceID().compareTo(other.getReferenceSequenceID()) < 0);
        }
    }


    public boolean preceeds(String chrom, int pos) {
        if (getReferenceSequenceID().compareTo(chrom)==0) {
            // we are on same chromosome -- check the position
            return getStartPos() < pos;
        } else {
            // return true iff this chromosome lexigraphicall preceeds the other one
            return (getReferenceSequenceID().compareTo(chrom) < 0);
        }
    }


    /**
     *
     * @param model
     * @param chromosome2AlignabilityMap
     * @param maxAlignabilityScore
     */
    public void setUsableBaits(Model model, Chromosome2AlignabilityMap chromosome2AlignabilityMap, double maxAlignabilityScore) {
        Integer bmin =model.getMinBaitCount();
        Integer bmax = model.getMaxBaitCount();
        Integer baitSize = model.getProbeLength();
        Double minGCcontent = model.getMinGCcontent();
        Double maxGCcontent = model.getMaxGCcontent();
        if (this.length() < baitSize) {
            // do not place baits in segments shorter than the bait size
            this.unselectable = true;
            this.targetable = false;
            this.rescuable = false;
            return;
        }

        // try to set bmax usable baits independently for up and downstream margin
        this.setUsableBaitsForUpstreamMargin(bmin, baitSize, chromosome2AlignabilityMap, minGCcontent, maxGCcontent, maxAlignabilityScore);
        this.setUsableBaitsForDownstreamMargin(bmin, baitSize, chromosome2AlignabilityMap, minGCcontent, maxGCcontent, maxAlignabilityScore);
        this.removeRedundantBaits(); // remove redundant baits that may occur for segments shorter than 2 times the margin size

        if (bmin <= this.getBaitNumUp() && bmin <= this.getBaitNumDown()) {
            // both margin have at least bmin non redundant baits -> this segment is targetable
            this.unselectable = false;
            this.targetable = true;
            this.rescuable = false;
        } else {
            // try to rescue the segment by allowing unbalanced probes
            if (this.getBaitNumUp() < bmin && this.getBaitNumDown() < bmin) {
                // both margins have less than bmin baits this segment, i.e. bait cannot be rescued
                this.unselectable = true;
                this.targetable = false;
                this.rescuable = false;
                return;
            }
            if (this.getBaitNumUp() < bmin) {
                // the upstream margin has less than bmin baits; try to set missing baits in downstream margin
                Integer numOfMissingBaits = 2 * bmin - getBaitNumUp(); // determine number of missing baits
                this.setUsableBaitsForDownstreamMargin(numOfMissingBaits, baitSize, chromosome2AlignabilityMap, minGCcontent, maxGCcontent, maxAlignabilityScore); // try to set this number in downstream margin
                this.removeRedundantBaits();

                if (this.getBaitNumTotal() == 2 * bmin) {
                    // segment can be rescued
                    this.unselectable = false;
                    this.targetable = false;
                    this.rescuable = true;
                } else {
                    this.unselectable = true;
                    this.targetable = false;
                    this.rescuable = false;
                }
            } else {
                // the downstream margin has less than bmin baits; try to set missing baits in upstream margin
                Integer numOfMissingBaits = 2 * bmin - getBaitNumDown(); // determine number of missing baits
                this.setUsableBaitsForUpstreamMargin(numOfMissingBaits, baitSize, chromosome2AlignabilityMap, minGCcontent, maxGCcontent, maxAlignabilityScore); // try to set this number in upstream margin
                this.removeRedundantBaits();
                if (this.getBaitNumTotal() == 2 * bmin) {
                    // segment can be rescued
                    this.unselectable = false;
                    this.targetable = false;
                    this.rescuable = true;
                } else {
                    this.unselectable = true;
                    this.targetable = false;
                    this.rescuable = false;
                }
            }
        }

    }



    public Integer getBaitNumTotal() { return this.baitListUpStreamMargin.size() + this.baitListDownStreamMargin.size(); }

    public Integer getBaitNumUp() { return this.baitListUpStreamMargin.size(); }

    public Integer getBaitNumDown() {return this.baitListDownStreamMargin.size(); }


    /** NEW VERSION */
    public List<Bait> setUsableBaitsForUpstreamMargin(Integer bmax, Integer baitSize, Chromosome2AlignabilityMap alignabilityMap,  Double minGCcontent, Double maxGCcontent, Double maxAlignabilityScore) {

        Integer sta = this.getStartPos();
        Integer end = this.getStartPos() + marginSize - 1;

        //ArrayList<Bait> baitListUpStreamMargin = new ArrayList<>();
        for(int i = sta; i <= end - baitSize + 1; i++ ) { // from left to right because this is the upstream margin

            // init bait
            Bait b = new Bait(this.referenceSequenceID, i, i + baitSize - 1, fastaReader, alignabilityMap);

            // check for constraints and add if appropriate
            if( b.isUsable(minGCcontent, maxGCcontent, maxAlignabilityScore) ) {
                baitListUpStreamMargin.add(b);
            }

            // abort if bmax is reached
            if(baitListUpStreamMargin.size()==bmax) { break; }

            // abort if end of bait reaches end of segment
            if(i + baitSize - 1 == this.getEndPos()) { break; }
        }
        return this.baitListUpStreamMargin;
    }





    public List<Bait> getBaitsForUpstreamMargin() {
            return this.baitListUpStreamMargin;
    }



    /** NEW VERSION */
    private List<Bait> setUsableBaitsForDownstreamMargin(Integer bmax, Integer baitSize, Chromosome2AlignabilityMap alignabilityMap,  Double minGCcontent, Double maxGCcontent, Double maxAlignabilityScore) {

        Integer sta = this.getEndPos() - marginSize + 1;
        Integer end = this.getEndPos();

        this.baitListDownStreamMargin.clear();
        for (int i = end - baitSize + 1; sta < i; i--) { // from right to left because this is the upstream margin

            // init bait
            Bait b = new Bait(this.referenceSequenceID, i, i + baitSize - 1, fastaReader, alignabilityMap);

            // check for constraints and add if appropriate
            if( b.isUsable(minGCcontent, maxGCcontent, maxAlignabilityScore) ) {
                baitListDownStreamMargin.add(b);
            }

            // abort if bmax is reached
            if(baitListDownStreamMargin.size()==bmax) { break; }

            // abort if end of bait reaches start of segment
            if(i == this.getStartPos()) { break; }

        }
        return this.baitListDownStreamMargin;
    }


    public List<Bait> getBaitsForDownstreamMargin()  { return this.baitListDownStreamMargin; }


    /**
     *  If the segment is shorter than 2 times the margin size and the baits are derived independently for the
     * up and down stream margin, then there are potentially redundant baits with in the overlapping part
     * of the two margins. This fuction removes redundant baits from the list od baits of the downstream margin.
     *
     * @return Number of redundant baits removed from downstream margin
     */
    public Integer removeRedundantBaits() {

        Integer numOfRedundantBaitsRemoved = 0;

        // put the the coordinates of all upstream baits in a string Set
        Set<String> upstreamCoordSet = new HashSet<>();
        for(Bait b : baitListUpStreamMargin) {
                upstreamCoordSet.add(b.getRefId() + b.getStartPos());
        }

        // iterate over downstream baits and skip those also contained in set for upstream baits
        List<Bait> newBaitListDownStreamMargin = new ArrayList<>();
        for(Bait b : baitListDownStreamMargin) {
            if( !upstreamCoordSet.contains(b.getRefId() + b.getStartPos()) ) {
                newBaitListDownStreamMargin.add(b);
            } else {
                baitListDownStreamMargin.remove(b);
                numOfRedundantBaitsRemoved++;
            }

        }
        //this.baitListDownStreamMargin = newBaitListDownStreamMargin;
        return numOfRedundantBaitsRemoved;
    }

    public boolean isTargetable() {
        return this.targetable;
    }

    public boolean isRescuable() {
        return this.rescuable;
    }

    public boolean isUnselectable() {
        return this.unselectable;
    }

    public Double getMeanGCcontentOfBaits() {
        Double meanGCcontentOfBaits = 0.0;
        for(Bait b : baitListUpStreamMargin) {
            meanGCcontentOfBaits = meanGCcontentOfBaits + b.getGCContent();
        }
        for(Bait b : baitListDownStreamMargin) {
            meanGCcontentOfBaits = meanGCcontentOfBaits + b.getGCContent();
        }
        return meanGCcontentOfBaits/this.getBaitNumTotal();
    }

    public Double getMeanAlignabilityOfBaits() {
        Double meanAlignabilityOfBaits = 0.0;
        for(Bait b : baitListUpStreamMargin) {
            meanAlignabilityOfBaits = meanAlignabilityOfBaits + b.getAlignabilityScore();
        }
        for(Bait b : baitListDownStreamMargin) {
            meanAlignabilityOfBaits = meanAlignabilityOfBaits + b.getAlignabilityScore();
        }
        return meanAlignabilityOfBaits/this.getBaitNumTotal();
    }

    public Double getMeanRepeatContentOfBaits() {
        Double meanRepeatContentOfBaits = 0.0;
        for(Bait b : baitListUpStreamMargin) {
            meanRepeatContentOfBaits = meanRepeatContentOfBaits + b.getRepeatContent();
        }
        for(Bait b : baitListDownStreamMargin) {
            meanRepeatContentOfBaits = meanRepeatContentOfBaits + b.getRepeatContent();
        }
        return meanRepeatContentOfBaits/this.getBaitNumTotal();
    }


    @Override
    public String toString() {
        return String.format("%d-%d %s",getStartPos(),getEndPos(),isSelected()?"selected":"not selected");
    }


}

