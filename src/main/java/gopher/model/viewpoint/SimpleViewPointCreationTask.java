package gopher.model.viewpoint;

import gopher.model.GopherGene;
import gopher.model.RestrictionEnzyme;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import org.apache.log4j.Logger;
import gopher.exception.GopherException;
import gopher.model.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class coordinates the construction of simple (one probe per viewpoint) ViewPoints,
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2018-02-15)
 */
public class SimpleViewPointCreationTask extends ViewPointCreationTask {
    private static final Logger logger = Logger.getLogger(SimpleViewPointCreationTask.class.getName());
    /** Total number of viewpoints */
    private int total;
    /** Index of current viewpoint */
    private int i;

    private AlignabilityMap alignabilityMap = null;


  /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     *  @param model
     * @param currentVPproperty
   * @param alignabilityMap
   */
    public SimpleViewPointCreationTask(Model model, StringProperty currentVPproperty, AlignabilityMap alignabilityMap) {
        super(model,currentVPproperty);
        this.alignabilityMap=alignabilityMap;
    }


    private void calculateViewPoints(GopherGene vpvgene, String referenceSequenceID, IndexedFastaSequenceFile fastaReader) { //throws GopherException{
        int chromosomeLength = fastaReader.getSequence(referenceSequenceID).length();
        logger.trace(String.format("Length of %s is %d", referenceSequenceID, chromosomeLength));
        logger.error(String.format("Getting TSS for vpv %s", vpvgene.getGeneSymbol()));
        List<Integer> gPosList = vpvgene.getTSSlist();
        int n=0; // we will order the promoters from first (most upstream) to last
        // Note we do this differently according to strand.
        //Instrumentation inst=null;
        for (Integer gPos : gPosList) {
            ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos).
                    targetName(vpvgene.getGeneSymbol()).
                    upstreamLength(model.getSizeUp()).
                    downstreamLength(model.getSizeDown()).
                    maximumGcContent(model.getMaxGCcontent()).
                    minimumGcContent(model.getMinGCcontent()).
                    fastaReader(fastaReader).
                    minimumFragmentSize(model.getMinFragSize()).
                    maximumRepeatContent(model.getMaxRepeatContent()).
                    marginSize(model.getMarginSize()).
                    isForwardStrand(vpvgene.isForward()).
                    accessionNr(vpvgene.getRefSeqID()).
                    alignabilityMap(this.alignabilityMap).
                    model(this.model).
                    build();

            vp.setPromoterNumber(++n,gPosList.size());
            updateProgress(i++, total); /* this will update the progress bar */
            updateLabelText(this.currentVP, vpvgene.toString());
            vp.generateViewpointSimple(model);
            if (vp.getResolved()) {
                viewpointlist.add(vp);
                logger.trace(String.format("Adding viewpoint %s to list (size: %d)", vp.getTargetName(), viewpointlist.size()));
            } else {
                logger.trace(String.format("Skipping viewpoint %s (size: %d) because it was not resolved", vp.getTargetName(), viewpointlist.size()));
            }
        }
    }


    /**
     * This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     *
     * @return
     * @throws GopherException if we cannot create the viewpoints
     */
    protected Void call() throws GopherException {
        if (ViewPoint.chosenEnzymes == null) {
            logger.error("Attempt to start Simple ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        this.total = getTotalPromoterCount();
        this.i = 0;
        logger.trace(String.format("extracting VPVGenes & have %d chromosome groups ", chromosomes.size()));
        long milli = System.currentTimeMillis();

        String faipath = this.model.getIndexedGenomeFastaIndexFile();
        String fastapath = this.model.getGenomeFastaFile();
        if (faipath == null) {
            logger.error("Could not retrieve faidx file for " + fastapath);
            throw new GopherException("Could not retrieve faidx file for " + fastapath);
        }
        IndexedFastaSequenceFile fastaReader;
        try {
            fastaReader =new IndexedFastaSequenceFile(new File(fastapath));
        } catch (FileNotFoundException fnfe) {
            throw new GopherException(String.format("Could not find genome fasta file [%s]",fnfe.getMessage()));
        }

        // estimate average size of restriction fragments
        // TODO: Move this code to a function getEstAvgRestFragLen
        logger.trace("Estimating the average length of restriction fragments from at least 100,000 fragments...");
        // Combine all patterns in one regular expression.
        String regExCombinedCutPat ="";
        for(int i=0; i < model.getChosenEnzymelist().size(); i++) {
            regExCombinedCutPat +=  model.getChosenEnzymelist().get(i).getPlainSite();
            if(i<model.getChosenEnzymelist().size()-1) {
                regExCombinedCutPat += "|";
            }
        }
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
            if(100000<totalNumOfCuts) {break;}
        }

        Double estAvgRestFragLen = 1.0*totalLength/totalNumOfCuts;
        model.setEstAvgRestFragLen(estAvgRestFragLen);
        logger.trace("Total number of cuts: " + totalNumOfCuts);
        logger.trace("Total length: " + totalLength);
        logger.trace("Estimated average length : " + (1.0*totalLength/totalNumOfCuts));
        logger.trace("Average length: " + estAvgRestFragLen);
        logger.trace("...done.");


        for (ChromosomeGroup group : chromosomes.values()) {
            String referenceSequenceID = group.getReferenceSequenceID();/* Usually a chromosome */
            logger.trace("Creating viewpoints for RefID=" + referenceSequenceID);
            for (GopherGene gene : group.getGenes()) {
           // group.getGenes().parallelStream().forEach(vpvGene -> {
                calculateViewPoints(gene, referenceSequenceID, fastaReader);
            }
        }
        long end = milli - System.currentTimeMillis();
        logger.trace(String.format("Generation of viewpoints (simple approach) took %.1f sec", end / 1000.0));
        this.model.setViewPoints(viewpointlist);
        return null;
    }

    /** This updates the message on the GUI on a JavaFX thread to show the user which view points are being generated. */
    private void updateLabelText(StringProperty sb, String msg) {
        Platform.runLater( () -> sb.setValue(String.format("[%d/%d] Creating view point for %s",i,total, msg)) );
    }
}
