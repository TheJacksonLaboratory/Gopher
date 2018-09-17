package gopher.model;

import gopher.model.viewpoint.Bait;
import org.apache.log4j.Logger;
import gopher.model.viewpoint.Segment;
import gopher.model.viewpoint.ViewPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that is intended to be used by the ViewPointAnalysis presenter to calculate the statistics for the
 * intended probe design across all viewpoints.
 *
 * @author Peter Robinson
 * @version 0.0.2 (2017-10-17)
 */
public class Design {
    static Logger logger = Logger.getLogger(Design.class.getName());

    private int n_unique_fragments;
    /** total length in nt of all unique digest margins  */
    private int n_nucleotides_in_unique_fragment_margins;

    private int n_genes;

    private int n_viewpoints;

    private double avgFragmentsPerVP;

    private double avgVPscore;

    private double avgVPsize;
    /**
     * Number of successful (resolved) viewpoints, defined as having at least one active digest.
     */
    private int n_resolvedViewpoints;
    /**
     * Number of genes with at least one resolved viewpoint (see {@link #n_resolvedViewpoints}.
     */
    private int n_resolvedGenes;

    private int n_estimatedProbeCount;
    /** NUmber of viewpoints with more than one digest (only applies to simple approach).*/
    private int n_patched_viewpoints;

    private Model model;

    private Model.Approach approach;

    public int getN_unique_fragments() {
        return n_unique_fragments;
    }

    public int getN_nucleotides_in_unique_fragment_margins() {
        return n_nucleotides_in_unique_fragment_margins;
    }

    public int getN_genes() {
        return n_genes;
    }

    public int getN_viewpoints() {
        return n_viewpoints;
    }

    public double getAvgFragmentsPerVP() {
        return avgFragmentsPerVP;
    }

    public double getAvgVPscore() {
        return avgVPscore;
    }

    public double getAvgVPsize() {
        return avgVPsize;
    }

    public int getN_resolvedViewpoints() {
        return n_resolvedViewpoints;
    }

    public int getN_resolvedGenes() {
        return n_resolvedGenes;
    }

    public int getEstimatedNumberOfProbes() {
        return n_estimatedProbeCount;
    }


    public Design(Model mod) {

        this.model = mod;
        this.approach=model.getApproach();
    }



    private void calculateEstimatedProbeNumber() {
        int nProbes = 0;
        n_nucleotides_in_unique_fragment_margins =0;// total length in nt of all unique probes
        int probelen = model.getProbeLength();
        double RC=0d;
        int N=0;
        Set<String> uniqueFragmentMargins = new HashSet<>();
        for (ViewPoint vp : model.getViewPointList()) {
            if (vp.getNumOfSelectedFrags() == 0) {
                continue;
            }
            int k = 0; // index of selected digest
            for (Segment segment : vp.getActiveSegments()) {
                k++;
//                Integer rsStaPos = segment.getStartPos();
//                Integer rsEndPos = segment.getEndPos();
                // get unique margins of selected fragments
                for (int l = 0; l < segment.getSegmentMargins().size(); l++) {
                    Integer fmStaPos = segment.getSegmentMargins().get(l).getStartPos();
                    Integer fmEndPos = segment.getSegmentMargins().get(l).getEndPos();
                    RC += 0.5 * (segment.getRepeatContentMarginDown() + segment.getRepeatContentMarginUp());
                    double RC2 = 0.5 * (segment.getRepeatContentMarginDown() + segment.getRepeatContentMarginUp());
                    String target = String.format("%s-%d-%d-%s_margin_%d", vp.getReferenceID(), (fmStaPos - 1), fmEndPos, vp.getTargetName(), l);
                    if (uniqueFragmentMargins.contains(target))
                        continue;
                    else {
                        uniqueFragmentMargins.add(target);
                        nProbes +=  (1-RC2) * (fmEndPos - fmStaPos) / probelen;
                        n_nucleotides_in_unique_fragment_margins += fmEndPos - fmStaPos + 1;
                        N++;
                    }
                }
            }
        }
        n_estimatedProbeCount = nProbes;
        RC /=(double)N;
        n_estimatedProbeCount = (int)(n_nucleotides_in_unique_fragment_margins * (1-RC) ) / model.getProbeLength();
    }


    /**
     * Model has the list of ViewPoints and also the parameters for tiling, probe length etc.
     * So we do not need to pass anything to this functions.
     */
    public void calculateDesignParameters() {
        Set<String> genesWithValidViewPoint = new HashSet<>(); // set of genes with at least one valid viewpoint
        // valid is defined as with at least one active segment
        n_resolvedViewpoints = 0;
        List<ViewPoint> viewPointList = model.getViewPointList();
        //System.out.println(viewPointList.size());
        int probeLength = model.getProbeLength();

        Set<Segment> uniqueRestrictionFragments = new HashSet<>();
        Set<String> uniqueGeneSymbols = new HashSet<>();
        avgVPscore = 0.0;
        avgVPsize = 0.0;
        n_patched_viewpoints=0;
        // If the user calls this function on a new project before
        // creating viewpoints, then viewPointList is null, and we should just return.
        if (viewPointList == null) {
            return;
        }
        viewPointList.stream().forEach(vp -> {
            uniqueRestrictionFragments.addAll(vp.getActiveSegments());
            uniqueGeneSymbols.add(vp.getTargetName());
            avgVPscore += vp.getScore();
            avgVPsize += vp.getTotalLengthOfViewpoint();
            //avgVPsize += vp.getEndPos()-vp.getStartPos()+1;
            if (vp.hasValidDigest()){
                uniqueGeneSymbols.add(vp.getTargetName());
            }
            if (vp.hasValidDigest()) {
                n_resolvedViewpoints++;
                genesWithValidViewPoint.add(vp.getTargetName());
            }
            if (this.approach.equals(Model.Approach.SIMPLE) && vp.getActiveSegments().size()>1) {
                n_patched_viewpoints++;
            }
        });

        this.n_genes = uniqueGeneSymbols.size();
        this.n_viewpoints = viewPointList.size();
        this.n_resolvedGenes = genesWithValidViewPoint.size();

        this.n_unique_fragments = uniqueRestrictionFragments.size();

        this.n_nucleotides_in_unique_fragment_margins = 0;

        uniqueRestrictionFragments.stream().forEach(segment ->
            n_nucleotides_in_unique_fragment_margins += Math.min(2 * probeLength, segment.length())
        );
        if (n_viewpoints > 0) {
            this.avgFragmentsPerVP = (double) n_unique_fragments / (double) n_viewpoints;
            this.avgVPsize /= (double) n_viewpoints;
            this.avgVPscore /= (double) n_viewpoints;
        } else {
            // something didn't work. Set everything to zeero.
            this.avgFragmentsPerVP = 0;
            this.avgVPsize = 0;
            this.avgVPscore = 0;

        }
        calculateEstimatedProbeNumber();
//        logger.trace(String.format("Calculate params, n genes=%d [%s]",getN_genes(),uniqueGeneSymbols.stream().collect(Collectors.joining("; "))));
    }

    public Integer getTotalNumOfUniqueBaits() {
        // get rid of redundant segments emerging from overlapping viewpoints
        Set<Segment> uniqueDigests = new HashSet<>();
        List<ViewPoint> viewPointList = model.getViewPointList();
        for(ViewPoint vp : viewPointList) {
            uniqueDigests.addAll(vp.getActiveSegments());
        }
        // count number of baits
        Integer n_baits = 0;
        for(Segment seg : uniqueDigests) {
            n_baits += seg.getBaitNumTotal();
        }
        return n_baits;
    }

    public Integer getCaptureSize() {
        // get rid of redundant segments arising from overlapping viewpoints
        Set<Segment> uniqueDigests = new HashSet<>();
        List<ViewPoint> viewPointList = model.getViewPointList();
        for(ViewPoint vp : viewPointList) {
            uniqueDigests.addAll(vp.getActiveSegments());
        }
        // count number of covered positions
        Integer cSize = 0;
        for(Segment seg : uniqueDigests) {
            HashSet<Integer> cSizeSet = new HashSet<>();
            for(Bait b : seg.getBaitsForUpstreamMargin()) {
                for(Integer i = b.getStartPos(); i<=b.getEndPos(); i++) {
                    cSizeSet.add(i);
                }
            }
            for(Bait b : seg.getBaitsForDownstreamMargin()) {
                for(Integer i = b.getStartPos(); i<=b.getEndPos(); i++) {
                    cSizeSet.add(i);
                }
            }
            cSize = cSize + cSizeSet.size();
            cSizeSet.clear();
        }
        return cSize;
    }


    public Integer getTotalNumBalancedDigests() {
        // get rid of redundant segments emerging from overlapping viewpoints
        Set<Segment> uniqueDigests = new HashSet<>();
        List<ViewPoint> viewPointList = model.getViewPointList();
        for (ViewPoint vp : viewPointList) {
            uniqueDigests.addAll(vp.getActiveSegments());
        }
        // count number balanced
        Integer n_balanced_digests = 0;
        for (Segment seg : uniqueDigests) {
            if(seg.isBalanced()) {
                n_balanced_digests++;
            }
        }
        return n_balanced_digests;
    }

    public int getTotalNumUnbalancedDigests() {
        // get rid of redundant segments emerging from overlapping viewpoints
        Set<Segment> uniqueDigests = new HashSet<>();
        List<ViewPoint> viewPointList = model.getViewPointList();
        for (ViewPoint vp : viewPointList) {
            uniqueDigests.addAll(vp.getActiveSegments());
        }
        // count number balanced
        int n_unbalanced_digests = 0;
        for (Segment seg : uniqueDigests) {
            if(seg.isUnbalanced()) {
                n_unbalanced_digests++;
            }
        }
        return n_unbalanced_digests;
    }

    public int getN_patched_viewpoints(){ return n_patched_viewpoints;}



}
