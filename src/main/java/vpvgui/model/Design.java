package vpvgui.model;

import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.util.ArrayList;
import java.util.List;

public class Design {


    private Model model=null;

    public Design(Model mod) {
        this.model=mod;
    }

    public Design(List<ViewPoint> L,int probeLength,int maximumAllowedOverlap,double tilingFactor) {

    }


    /**
     * Model has the list of ViewPoints and also the parameters for tiling, probe length etc.
     * So we do not need to pass anything to this function
     * @return
     */
    public Integer getTotalNumberOfProbeNucleotides() {

        List<ViewPoint> viewPointList = model.getViewPointList();
        //System.out.println(viewPointList.size());
        Integer probeLength = model.getProbeLength();
        Integer maximumAllowedOverlap = model.getMaximumAllowedRepeatOverlap();
        Double tilingFactor = model.getTilingFactor();

        /* TODO calculate total number of nucleotides in model */

        for (ViewPoint vp : viewPointList) {
            /* get selected fragments of viewpoint */
            ArrayList<Segment> selectedSegments = vp.getSelectedRestSegList("GATC"); // TODO: Make this more general with 'ALL', the combination of all cutting motifs
            for (Segment ss : selectedSegments) {
            }
        }



        int n = 42;
        //model.setTotalNumberOfProbeNucleotides(n);
        return n;

    }

}
