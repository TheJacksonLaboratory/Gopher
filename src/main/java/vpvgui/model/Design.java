package vpvgui.model;

import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Design {

    private int n_unique_fragments;

    private int n_nucleotides_in_probes;

    private int n_genes;

    private int n_viewpoints;

    private double avgFragmentsPerVP;

    private double avgVPscore;

    private double avgVPsize;



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

    public Design(Model mod) {

        this.model=mod;
    }

    public int totalEffectiveSize() {
        return model.getTilingFactor() * n_nucleotides_in_probes;
    }




    /**
     * Model has the list of ViewPoints and also the parameters for tiling, probe length etc.
     * So we do not need to pass anything to this function
     * @return
     */
    public void calculateDesignParameters() {

        List<ViewPoint> viewPointList = model.getViewPointList();
        //System.out.println(viewPointList.size());
        int probeLength = model.getProbeLength();
        int maximumAllowedOverlap = model.getMaximumAllowedRepeatOverlap();
        int tilingFactor = model.getTilingFactor();

        /* TODO calculate total number of nucleotides in model */

        Set<Segment> uniqueRestrictionFragments=new HashSet<>();
        Set<String> uniqueGeneSymbols=new HashSet<>();
        avgVPscore=0.0;
        avgVPsize=0.0;
        viewPointList.stream().forEach(vp -> {
            uniqueRestrictionFragments.addAll(vp.getActiveSegments());
            uniqueGeneSymbols.add(vp.getTargetName());
            avgVPscore += vp.getScore();
            avgVPsize += vp.getTotalLengthOfViewpoint();
        });

        this.n_genes=uniqueGeneSymbols.size();
        this.n_viewpoints=viewPointList.size();

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
    }

}
