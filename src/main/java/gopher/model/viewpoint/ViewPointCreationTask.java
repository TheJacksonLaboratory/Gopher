package gopher.model.viewpoint;

import gopher.model.GopherGene;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import gopher.model.Model;
import gopher.model.RestrictionEnzyme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for the tasks that create viewpoints.
 * @author Peter Robinson
 * @version 0.0.3 (2017-09-27)
 */
public abstract class ViewPointCreationTask extends Task<Void> {
    private static final Logger logger = Logger.getLogger(ViewPointCreationTask.class.getName());
    /**  Key: Name of chromosome; value: Chromosome with {@link GopherGene} objects located on the chromosome. */
    protected Map<String, ChromosomeGroup> chromosomes = null;
    /** The total number of genes for which we are making viewpoints. This number is only used for the progress
     * bar (Some genes have multiple transcription start sites and so one gene may have multiple ViewPoints)..*/
    private int n_totalGenes;

    private int n_total_promoters;
    /** Referece to the model with all project data. */
    protected Model model;
    /** This is used to show the name of the current Gene on the viewpoint creation dialog. */
    StringProperty currentVP = null;

    /**
     * List of {@link ViewPoint} objects that we will return to the Model when this Task is done.
     */
    List<ViewPoint> viewpointlist;

    protected abstract Void call() throws Exception;


    ViewPointCreationTask(Model model, StringProperty currentVPproperty) {
        this.model = model;
        this.viewpointlist = new ArrayList<>();
        assignVPVGenesToChromosomes(model.getVPVGeneList());
        logger.trace(String.format("ViewPointCreationTask -- we got %d total genes",n_totalGenes));
        this.currentVP = currentVPproperty;
        ViewPoint.setChosenEnzymes(model.getChosenEnzymelist());
        SegmentFactory.restrictionEnzymeMap = new HashMap<>();
        List<RestrictionEnzyme> chosen = model.getChosenEnzymelist();
        if (chosen == null) {
            logger.error("Unable to retrieve list of chosen restriction enzymes");
            return;
        } else {
            logger.trace(String.format("Setting up viewpoint creation for %d enzymes", chosen.size()));
        }
        for (RestrictionEnzyme re : chosen) {
            String site = re.getPlainSite();
            SegmentFactory.restrictionEnzymeMap.put(site, re);
        }
    }


    /**
     * Here, we assign the {@link GopherGene} objects to the corresponding chromosomes. This allows us to
     * create a FastReader only once for each chromosome (and thereby be much more efficient than going through
     * the  {@link GopherGene} obejcts in no particular order).
     * @param vgenes
     */
    private void assignVPVGenesToChromosomes(List<GopherGene> vgenes) {
        this.chromosomes = new HashMap<>();
        n_totalGenes =0;
        this.n_total_promoters=0;
        for (GopherGene g : vgenes) {
            String referenceseq = g.getContigID();
            ChromosomeGroup group;
            if (chromosomes.containsKey(referenceseq)) {
                group = chromosomes.get(referenceseq);
            } else {
                group = new ChromosomeGroup(referenceseq);
                chromosomes.put(referenceseq, group);
            }
            group.addVPVGene(g);
            n_totalGenes++;
            n_total_promoters += g.n_viewpointstarts();
        }
    }


    /**
     * Get the total number of genes for which we will create viewpoints (used for the progress indicator while
     * we are creating viewpoints)
     * @return total number of {@link GopherGene} objects.
     */
    int getTotalGeneCount() {
        return n_totalGenes;
    }

    int getTotalPromoterCount() {return n_total_promoters; }
}
