package vpvgui.model;

import vpvgui.model.project.ViewPoint;

import java.util.List;

public class Design {


    private Model model=null;

    public Design(Model mod) {
        this.model=mod;
    }


    /**
     * Model has the list of ViewPoints and also the parameters for tiling, probe length etc.
     * So we do not need to pass anything to this function
     * @return
     */
    public int getTotalNumberOfProbeNucleotides() {

        List<ViewPoint> viewpointlist=model.getViewPointList();
        int probelength=model.getProbeLength();
        int maximumAllowedOverlap=model.getMaximumAllowedRepeatOverlap();
        double tilingFactor=model.getTilingFactor();

        /* TODO calculate total number of nucleotides in model */

        for (ViewPoint vp : viewpointlist) {

        }

        int n=42;
        model.setTotalNumberOfProbeNucleotides(n);
        return n;

    }

}
