package gopher.model.viewpoint;

import gopher.exception.GopherException;
import gopher.model.Default;
import gopher.model.GopherGene;
import gopher.model.Model;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;


/**
 * This class coordinates the construction of simple (one probe per viewpoint) ViewPoints,
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2018-02-15)
 */
public class SimpleViewPointCreationTask extends ViewPointCreationTask {
    private static final Logger logger = Logger.getLogger(SimpleViewPointCreationTask.class.getName());

  /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     *  @param model Model of the panel design project
   */
  public SimpleViewPointCreationTask(Model model) {
      super(model);

  }

    /**
     * Calculate the data for the current ViewPoint object
     * @param vpvgene A representation of the input gene and target (usually the TSS)
     * @param referenceSequenceID Chromosome/scaffold on which the gene is located
     * @param fastaReader HTSJDK object to read FASTA file
     * @param chr2alignMap alignability map for the current chromosome
     */
    private void calculateViewPoints(GopherGene vpvgene,
                                     String referenceSequenceID,
                                     IndexedFastaSequenceFile fastaReader,
                                     AlignabilityMap chr2alignMap,
                                     int chromLen) {
        List<Integer> gPosList = vpvgene.getTSSlist();
        int n=0; // we will order the promoters from first (most upstream) to last
        // Note we do this differently according to strand.
        logger.trace("Creating simple viewpoint for " + vpvgene.getGeneSymbol());
        for (Integer gPos : gPosList) {
            ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos,chromLen).
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
                    c2alignabilityMap(chr2alignMap).
                    model(this.model).
                    build();
            vp.setPromoterNumber(++n,gPosList.size());
            updateProgress(i++, total); /* this will update the progress bar */
            updateMessage(String.format("[%d/%d] Creating view point for %s", i, total, vpvgene.toString()));
            vp.generateViewpointSimple(model);
            viewpointlist.add(vp);
        }
    }


    /**
     * This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     * @throws GopherException if we cannot create the viewpoints
     */
    protected Void call() throws GopherException {
        updateTitle("Creating viewpoints using 'simple' approach");
        if (ViewPoint.chosenEnzymes == null) {
            logger.error("Attempt to start Simple ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        this.total = getTotalPromoterCount();
        this.i = 0;
        logger.trace(String.format("extracting GopherGenes & have %d chromosome groups ", chromosomes.size()));
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
        double meanLen = getEstimatedMeanRestrictionFragmentLength(fastaReader);
        model.setEstAvgRestFragLen(meanLen);
        String chromInfoPath=model.getChromInfoPathIncludingFileNameGz();
        String alignabilitMapPath=model.getAlignabilityMapPathIncludingFileNameGz();
        int kmerSize=Default.KMER_SIZE;
        try {
            AlignabilityMapIterator apiterator = new AlignabilityMapIterator(alignabilitMapPath,chromInfoPath, kmerSize);
            while (apiterator.hasNext()) {
                AlignabilityMap apair = apiterator.next();
                String referenceSequenceID = apair.getChromName();
                logger.trace("Creating viewpoints for RefID=" + referenceSequenceID);
                if (!chromosomes.containsKey(referenceSequenceID)) {
                    continue; // skip if we have no gene on this chromosome
                }
                ChromosomeGroup group = chromosomes.get(referenceSequenceID);
                if (group == null) {
                    logger.error("group is null while searching for \"" + referenceSequenceID + "\"");
                    for (ChromosomeGroup g : chromosomes.values()) {
                        logger.error(g.getReferenceSequenceID());
                    }
                } else {
                    logger.trace("group=" + group.getReferenceSequenceID());
                }
                int chromosomeLen = fastaReader.getSequence(referenceSequenceID).length();
                //for (GopherGene vpvGene : gopherGene.getGenes()) {
                group.getGenes().parallelStream().forEach(gopherGene -> calculateViewPoints(gopherGene, referenceSequenceID, fastaReader, apair,chromosomeLen));
            }

        } catch (IOException e){
            e.printStackTrace();
        }
        long end = milli - System.currentTimeMillis();
        logger.trace(String.format("Generation of viewpoints (simple approach) took %.1f sec", end / 1000.0));
        this.model.setViewPoints(viewpointlist);
        return null;
    }

}
