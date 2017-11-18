package vpvgui.model;

import org.apache.log4j.Logger;
import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that is intended to be used by the ViewPointAnalysis presenter to calculate the statistics for the
 * intended probe design across all viewpoints.
 * @author Peter Robinson
 * @version 0.0.2 (2017-10-17)
 */
public class Design {
    static Logger logger = Logger.getLogger(Design.class.getName());

    private int n_unique_fragments;

    private int n_nucleotides_in_probes;

    private int n_genes;

    private int n_viewpoints;

    private double avgFragmentsPerVP;

    private double avgVPscore;

    private double avgVPsize;
    /** Number of successful (resolved) viewpoints, defined as having at least one active fragment. */
    private int n_resolvedViewpoints;
    /** Number of genes with at least one resolved viewpoint (see {@link #n_resolvedViewpoints}. */
    private int n_resolvedGenes;

    private Model model=null;

    public int getN_unique_fragments() {
        return n_unique_fragments;
    }

    public int getN_nucleotides_in_probes() {
        return n_nucleotides_in_probes;
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

    public Design(Model mod) {

        this.model=mod;
    }

    public int totalEffectiveSize() {
        return model.getTilingFactor() * n_nucleotides_in_probes;
    }




    /**
     * Model has the list of ViewPoints and also the parameters for tiling, probe length etc.
     * So we do not need to pass anything to this functions.
     */
    public void calculateDesignParameters() {
        Set<String> genesWithValidViewPoint = new HashSet<>(); // set of genes with at least one valid viewpoint
        // valid is defined as with at least one active segment
        n_resolvedViewpoints=0;
        List<ViewPoint> viewPointList = model.getViewPointList();
        //System.out.println(viewPointList.size());
        int probeLength = model.getProbeLength();

        Set<Segment> uniqueRestrictionFragments=new HashSet<>();
        Set<String> uniqueGeneSymbols=new HashSet<>();
        avgVPscore=0.0;
        avgVPsize=0.0;
        // If the user calls this function on a new project before
        // creating viewpoints, then viewPointList is null, and we should just return.
        if (viewPointList==null) {return;}
        viewPointList.stream().forEach(vp -> {
            uniqueRestrictionFragments.addAll(vp.getActiveSegments());
            uniqueGeneSymbols.add(vp.getTargetName());
            avgVPscore += vp.getScore();
            avgVPsize += vp.getTotalLengthOfViewpoint();
            if (vp.getResolved()) {
                uniqueGeneSymbols.add(vp.getTargetName());
            }
            if (vp.hasValidProbe()) {
                n_resolvedViewpoints++;
                genesWithValidViewPoint.add(vp.getTargetName());
            }
        });

        this.n_genes=uniqueGeneSymbols.size();
        this.n_viewpoints=viewPointList.size();
        this.n_resolvedGenes=genesWithValidViewPoint.size();

        this.n_unique_fragments=uniqueRestrictionFragments.size();

        this.n_nucleotides_in_probes=0;

        uniqueRestrictionFragments.stream().forEach(segment -> {
                    n_nucleotides_in_probes += Math.min(2*probeLength,segment.length());
                });
        if (n_viewpoints>0) {
            this.avgFragmentsPerVP = (double) n_unique_fragments / (double) n_viewpoints;
            this.avgVPsize /= (double)n_viewpoints;
            this.avgVPscore /= (double)n_viewpoints;
        } else {
            // something didn't work. Set everything to zeero.
            this.avgFragmentsPerVP = 0;
            this.avgVPsize = 0;
            this.avgVPscore = 0;

        }

//        logger.trace(String.format("Calculate params, n genes=%d [%s]",getN_genes(),uniqueGeneSymbols.stream().collect(Collectors.joining("; "))));
    }

}
