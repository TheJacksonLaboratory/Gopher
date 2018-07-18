package gopher.model.viewpoint;

import gopher.model.GopherGene;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import gopher.model.Model;
import gopher.model.RestrictionEnzyme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            logger.trace("Chrom group="+group.getReferenceSequenceID());
            n_totalGenes++;
            n_total_promoters += g.n_viewpointstarts();
        }
    }

    /**
     * Estimate the average size of restriction fragments for the chosen restriction enyzymes
     * by looking at at least 100,000 fragments
     * @param fastaReader
     * @return
     */
    double getEstimatedMeanRestrictionFragmentLength(IndexedFastaSequenceFile fastaReader) {
        logger.trace("Estimating the average length of restriction fragments from at least 100,000 fragments...");
        int THRESHOLD_NUMBER_OF_FRAGMENTS=100_000;
        // Combine all patterns into one regular expression.
        String regExCombinedCutPat = model.getChosenEnzymelist().
                stream().
                map(RestrictionEnzyme::getPlainSite).
                collect(Collectors.joining("|"));
        // count all occurrences of the cutting motifs and divide by sequence length
        int totalNumOfCuts = 0;
        long totalLength = 0;
        ReferenceSequence rf = fastaReader.nextSequence();
        while(rf != null) {
            if(rf.getName().contains("_")) {rf = fastaReader.nextSequence(); continue;} // skip random chromosomes
            if(rf.getName().contains("chrM")) {rf = fastaReader.nextSequence(); continue;} // skip random chromosome M
            logger.trace("Cutting: " + rf.getName());
            String sequence = fastaReader.getSequence(rf.getName()).getBaseString();
            logger.trace("\tPattern: " + regExCombinedCutPat);
            Pattern pattern = Pattern.compile(regExCombinedCutPat,Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sequence);
            while (matcher.find()) {
                totalNumOfCuts++;
            }

            totalLength = totalLength + sequence.length();
            logger.trace("\tCurrent number of cuts: " + totalNumOfCuts);
            logger.trace("\tCurrent length: " + totalLength);
            logger.trace("\tEstimated average length : " + (1.0*totalLength/totalNumOfCuts));
            rf = fastaReader.nextSequence();
            if(THRESHOLD_NUMBER_OF_FRAGMENTS<totalNumOfCuts) {break;}
        }

        double estAvgRestFragLen = (double)totalLength/totalNumOfCuts;
        model.setEstAvgRestFragLen(estAvgRestFragLen);
        logger.trace("Total number of cuts: " + totalNumOfCuts +"; Total length: " + totalLength);
        logger.trace("Estimated average length : " + estAvgRestFragLen);
        return estAvgRestFragLen;
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
