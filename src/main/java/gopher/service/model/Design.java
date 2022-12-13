package gopher.service.model;

import gopher.service.GopherService;
import gopher.service.model.viewpoint.Bait;
import gopher.service.model.viewpoint.Segment;
import gopher.service.model.viewpoint.ViewPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A class that is intended to be used by the ViewPointAnalysis presenter to calculate the statistics for the
 * intended probe design across all viewpoints.
 *
 * @author Peter Robinson
 * @version 0.0.2 (2017-10-17)
 */
public class Design {
    static final Logger LOGGER = LoggerFactory.getLogger(Design.class.getName());

    private int n_unique_fragments;
    /** total length in nt of all unique digest margins  */
    private int n_nucleotides_in_unique_fragment_margins;

    private int n_genes;

    private int n_viewpoints;

    int n_segments_with_no_bait = 0;
    int n_segments_with_one_bait = 0;
    int n_segments_with_two_bait = 0;

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

    private final GopherService service;

    private final Approach approach;

    private int wellPlacedGoodQuality;
    /**
     * If true, one side has zero baits
     */
    private int unilateralBait;
    /** Number of restriction fragments that are not unilateral but are shifted. */
    private int shiftedBaits;
    /**
     *  that are not unilateral but are shifted but have a bait with high GC
     */
    private int highGc;

    private int totalBaitedRestrictionFragments;

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

    @Autowired
    public Design(GopherService service) {
        this.service = service;
        this.approach = service.getApproach();
    }



    private void calculateEstimatedProbeNumber() {
        int nProbes = 0;
        n_nucleotides_in_unique_fragment_margins =0;// total length in nt of all unique probes
        int probelen = service.getProbeLength();
        double RC=0d;
        int N=0;
        List<BaitedRestrictionFragmentEvaluation> evalList = new ArrayList<>();
        Set<String> uniqueFragmentMargins = new HashSet<>();
        Set<Segment> seenSegments = new HashSet<>(); // set to avoid duplicated
        for (ViewPoint vp : service.getViewPointList()) {
            if (vp.getNumOfSelectedFrags() == 0) {
                continue;
            }
            for (Segment segment : vp.getActiveSegments()) {
                if (!seenSegments.contains(segment)) {
                    evalList.add(new BaitedRestrictionFragmentEvaluation(segment));
                    seenSegments.add(segment);
                }
                // get unique margins of selected fragments
                for (int l = 0; l < segment.getSegmentMargins().size(); l++) {
                    Integer fmStaPos = segment.getSegmentMargins().get(l).startPos();
                    Integer fmEndPos = segment.getSegmentMargins().get(l).endPos();
                    RC += 0.5 * (segment.getRepeatContentMarginDown() + segment.getRepeatContentMarginUp());
                    double RC2 = 0.5 * (segment.getRepeatContentMarginDown() + segment.getRepeatContentMarginUp());
                    String target = String.format("%s-%d-%d-%s_margin_%d", vp.getReferenceID(), (fmStaPos - 1), fmEndPos, vp.getTargetName(), l);
                    if (! uniqueFragmentMargins.contains(target)) {
                        uniqueFragmentMargins.add(target);
                        nProbes +=  (1-RC2) * (fmEndPos - fmStaPos) / probelen;
                        n_nucleotides_in_unique_fragment_margins += fmEndPos - fmStaPos + 1;
                        N++;
                    }
                }
            }
        }
        this.wellPlacedGoodQuality = BaitedRestrictionFragmentEvaluation.getGoodQualityFragmentCount(evalList);
        this.unilateralBait = BaitedRestrictionFragmentEvaluation.getUnilateralBaitCount(evalList);
        this.shiftedBaits = BaitedRestrictionFragmentEvaluation.getShiftedBaitCount(evalList);
        this.highGc = BaitedRestrictionFragmentEvaluation.getHighGcBaitCount(evalList);
        this.totalBaitedRestrictionFragments = evalList.size();
        n_estimatedProbeCount = nProbes;
        RC /= N;
        n_estimatedProbeCount = (int)(n_nucleotides_in_unique_fragment_margins * (1-RC) ) / service.getProbeLength();
    }


    private void calculateBaitedRestrictionFragmentQualityParams() {

    }


    /**
     * Model has the list of ViewPoints and also the parameters for tiling, probe length etc.
     * So we do not need to pass anything to this functions.
     */
    public void calculateDesignParameters() {
        Set<String> genesWithValidViewPoint = new HashSet<>(); // set of genes with at least one valid viewpoint
        // valid is defined as with at least one active segment
        n_resolvedViewpoints = 0;
        List<ViewPoint> viewPointList = service.getViewPointList();
        LOGGER.trace("Calculating degin parameters for {} viewpoints", viewPointList.size());
        int probeLength = service.getProbeLength();

        Set<Segment> uniqueRestrictionFragments = new HashSet<>();
        Set<String> uniqueGeneSymbols = new HashSet<>();
        avgVPscore = 0.0;
        avgVPsize = 0.0;
        n_patched_viewpoints=0;


        viewPointList.forEach(vp -> {
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
            if (this.approach.equals(Approach.SIMPLE) && vp.getActiveSegments().size()>1) {
                n_patched_viewpoints++;
            }
        });
        LOGGER.trace("Obtained set of {} unique restriction fragments", uniqueRestrictionFragments.size());

        this.n_genes = uniqueGeneSymbols.size();
        this.n_viewpoints = viewPointList.size();
        this.n_resolvedGenes = genesWithValidViewPoint.size();

        this.n_unique_fragments = uniqueRestrictionFragments.size();

        this.n_nucleotides_in_unique_fragment_margins = 0;

        uniqueRestrictionFragments.forEach(segment ->
            n_nucleotides_in_unique_fragment_margins += Math.min(2 * probeLength, segment.length())
        );
        if (n_viewpoints > 0) {
            this.avgFragmentsPerVP = (double) n_unique_fragments / (double) n_viewpoints;
            this.avgVPsize /= n_viewpoints;
            this.avgVPscore /= n_viewpoints;
        } else {
            // something didn't work. Set everything to zeero.
            this.avgFragmentsPerVP = 0;
            this.avgVPsize = 0;
            this.avgVPscore = 0;

        }
        calculateEstimatedProbeNumber();
        LOGGER.trace("Calculate params, n genes={} [{}]",getN_genes(),uniqueGeneSymbols.stream().collect(Collectors.joining("; ")));
    }

    public Integer getTotalNumOfUniqueBaits() {
        // get rid of redundant segments emerging from overlapping viewpoints
        Set<Segment> uniqueDigests = new HashSet<>();
        List<ViewPoint> viewPointList = service.getViewPointList();
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
        List<ViewPoint> viewPointList = service.getViewPointList();
        for(ViewPoint vp : viewPointList) {
            uniqueDigests.addAll(vp.getActiveSegments());
        }
        // count number of covered positions
        int cSize = 0;
        for(Segment seg : uniqueDigests) {
            HashSet<Integer> cSizeSet = new HashSet<>();
            for(Bait b : seg.getBaitsForUpstreamMargin()) {
                for(int i = b.getStartPos(); i<=b.getEndPos(); i++) {
                    cSizeSet.add(i);
                }
            }
            for(Bait b : seg.getBaitsForDownstreamMargin()) {
                for(int i = b.getStartPos(); i<=b.getEndPos(); i++) {
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
        List<ViewPoint> viewPointList = service.getViewPointList();
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
        List<ViewPoint> viewPointList = service.getViewPointList();
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


    public Map<String, String> getDesignStatisticsList() {
        calculateDesignParameters();
        Map<String, String> listItems = new LinkedHashMap<>();
        int ngenes = getN_genes();
        int resolvedGenes = getN_resolvedGenes();
        String geneV = String.format("n=%d of which %d have \u2265 1 viewpoint with \u2265 1 selected digest", ngenes, resolvedGenes);
        listItems.put("Genes", geneV);
        int nviewpoints = getN_viewpoints();
        int resolvedVP = getN_resolvedViewpoints();
        double avVpSize = getAvgVPsize();
        double avgVpScore = getAvgVPscore();
        String vpointV = String.format("n=%d of which %d have \u2265 1 selected digest",
                nviewpoints, resolvedVP);
        if (service.getApproach().equals(Approach.SIMPLE)) {
            int n_patched = getN_patched_viewpoints();
            vpointV = String.format("%s %d viewpoints were patched", vpointV, n_patched);
        }
        String enzymes = service.getChosenEnzymelist().stream().map(RestrictionEnzyme::getName).collect(Collectors.joining(";"));
        listItems.put("Restriction enzyme(s)", enzymes);
        listItems.put("Viewpoints", vpointV);
        String vpointV2 = String.format("Mean size=%.1f bp; Mean score=%.1f%%",
                avVpSize, 100 * avgVpScore);
        listItems.put(" ", vpointV2);

        int nfrags = getN_unique_fragments();
        double avg_n_frag = getAvgFragmentsPerVP();
        String fragmentV = String.format("Total number of unique digests=%d; Mean number of digests per viewpoint: %.1f",
                nfrags, avg_n_frag);
        listItems.put("Digests", fragmentV);

        int n_balancedDigests = getTotalNumBalancedDigests();
        int n_unbalanced = getTotalNumUnbalancedDigests();
        listItems.put("", String.format("Balanced: %d; Unbalanced: %d", n_balancedDigests, n_unbalanced));
        int n_baits = getTotalNumOfUniqueBaits();
        Double captureSize = getCaptureSize() / 1000000.0;
        String baitV = String.format("n=%d; Capture size: %.3f Mbp", n_baits, captureSize);
        listItems.put("Probes", baitV);
        listItems.put("Active segments", String.format("%d: 0 baints, %d: 1 bait, %s: 2 baits",
                this.n_segments_with_no_bait, n_segments_with_one_bait, n_segments_with_two_bait));

        listItems.put("Total baited fragments", String.valueOf(this.totalBaitedRestrictionFragments));
        listItems.put("Total high quality fragments", String.valueOf(wellPlacedGoodQuality));
        listItems.put("Total unilaterally baited fragments", String.valueOf(this.unilateralBait));
        listItems.put("Total shifted fragments", String.valueOf(this.shiftedBaits));
       // listItems.put("Fragments with high GC", String.valueOf(this.highGc));
        return listItems;
    }






}
