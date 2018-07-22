package gopher.model.viewpoint;

import gopher.exception.GopherException;
import gopher.model.Default;
import gopher.model.GopherGene;
import gopher.model.Model;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This is the Task that coordinates creation of ViewPoints from the data entered by the user.
 * The class implements the extended probe design strategy with multiple fragments per ViewPoint.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2018-02-15)
 */
public class ExtendedViewPointCreationTask extends ViewPointCreationTask {
    private static final Logger logger = Logger.getLogger(ExtendedViewPointCreationTask.class.getName());
    /** Total number of created viewpoints (this variable is used for the progress indicator) . */
    private int total;
    /** Current number of created viewpoints (this variable is used for the progress indicator) . */
    private int i;


    /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     *  @param model Model of the panel design project
     */
    public ExtendedViewPointCreationTask(Model model) {
        super(model);
    }

    private void calculateViewPoints(GopherGene gopherGene, String referenceSequenceID, IndexedFastaSequenceFile fastaReader,Chromosome2AlignabilityMap c2aMap) {
        int chromosomeLength = fastaReader.getSequence(referenceSequenceID).length();
        updateMessage("calculating viewpoints for " + gopherGene.getGeneSymbol() + ", chromosome length="+chromosomeLength);
        logger.trace("calculating viewpoints for " + gopherGene.getGeneSymbol() + ", chromosome length="+chromosomeLength);
        List<Integer> gPosList = gopherGene.getTSSlist();
        if (! gopherGene.isForward()) {
            Collections.reverse(gPosList);
        }

        int n=0; // we will order the promoters from first (most upstream) to last
        // Note we do this differently according to strand.
        for (Integer gPos : gPosList) {
            if (isCancelled()) // true if user has cancelled the task
                return;
            logger.trace("Working on viewpoint for gPos=" + gPos);
            ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos).
                    targetName(gopherGene.getGeneSymbol()).
                    upstreamLength(model.getSizeUp()).
                    downstreamLength(model.getSizeDown()).
                    maximumGcContent(model.getMaxGCcontent()).
                    minimumGcContent(model.getMinGCcontent()).
                    fastaReader(fastaReader).
                    isForwardStrand(gopherGene.isForward()).
                    minimumFragmentSize(model.getMinFragSize()).
                    maximumRepeatContent(model.getMaxRepeatContent()).
                    marginSize(model.getMarginSize()).
                    isForwardStrand(gopherGene.isForward()).
                    accessionNr(gopherGene.getRefSeqID()).
                    c2alignabilityMap(c2aMap).
                    model(this.model).
                    build();
            vp.setPromoterNumber(++n,gPosList.size());
            updateProgress(i++, total); /* this will update the progress bar */
            updateMessage(String.format("Creating view point for %s", gopherGene.toString()));
            vp.generateViewpointExtendedApproach(model.getSizeUp(), model.getSizeDown(),model);
            viewpointlist.add(vp);
        }
    }


    /**
     * This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     * @throws GopherException if there is an I/O issue with reading the genome sequences.
     */
    protected Void call() throws GopherException {
        updateTitle("Creating viewpoints using 'extended' approach");
        if (ViewPoint.chosenEnzymes == null) {
            logger.error("Attempt to start ViewPoint creation with chosenEnzymes=null");
            throw new GopherException("Attempt to start ViewPoint creation thread with null chosenEnzymes");
        }
        this.total = getTotalGeneCount();
        this.i = 0;
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

        double meanLen = getEstimatedMeanRestrictionFragmentLength(fastaReader);
        model.setEstAvgRestFragLen(meanLen);
        String chromInfoPath=model.getChromInfoPathIncludingFileNameGz();
        String alignabilitMapPath=model.getAlignabilityMapPathIncludingFileNameGz();
        int kmerSize=Default.KMER_SIZE;

        try {
        AlignabilityMapIterator apiterator = new AlignabilityMapIterator(alignabilitMapPath,chromInfoPath, kmerSize);
        logger.trace("About to start iteration in new function");

        while (apiterator.hasNext()) {
            if (isCancelled()) // true if user has cancelled the task
                return null;
            Chromosome2AlignabilityMap apair = apiterator.next();
            String referenceSequenceID = apair.getChromName();
            logger.trace("NEW--Creating viewpoints for RefID Extended=" + referenceSequenceID);
            if (! chromosomes.containsKey(referenceSequenceID)) {
                continue; // skip if we have no gene on this chromosome
            }
            ChromosomeGroup group = chromosomes.get(referenceSequenceID);
            if (group==null) {
                logger.error("group is null while searching for \"" + referenceSequenceID +"\"");
                for (ChromosomeGroup g : chromosomes.values()) {
                    logger.error(g.getReferenceSequenceID());
                }
            } else {
                logger.trace("group="+group.getReferenceSequenceID());
            }
            for (GopherGene gene : group.getGenes()) {
                logger.trace("About to calculate gene "+gene.getGeneSymbol());
                // group.getGenes().parallelStream().forEach(vpvGene -> {
                calculateViewPoints(gene, referenceSequenceID, fastaReader,apair);
            }
        }


    } catch (IOException e){
        e.printStackTrace();
    }
        logger.trace(String.format("Created %d extended viewpoints", viewpointlist.size()));
        this.model.setViewPoints(viewpointlist);
        return null;
    }

    /**
     * This function updates the label on the viewpoint creation dialog with the name of the current viewpoint.
     *
     * @param sb  A StringPoperty that is bound to a label on the viewpoint creation dialog
     * @param msg Name of the current viewpoint
     */
    private void updateLabelText(StringProperty sb, String msg) {
        Platform.runLater( () -> sb.setValue(String.format("Creating view point for %s", msg)));
    }


}
