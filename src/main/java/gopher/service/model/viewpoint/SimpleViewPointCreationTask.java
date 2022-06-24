package gopher.service.model.viewpoint;

import gopher.exception.GopherException;
import gopher.service.GopherService;
import gopher.service.model.Default;
import gopher.service.model.GopherGene;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleViewPointCreationTask.class.getName());

  /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     *  @param service Model of the panel design project
   */
  public SimpleViewPointCreationTask(GopherService service) {
      super(service);
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
        LOGGER.info("Creating simple viewpoint for " + vpvgene.getGeneSymbol());
        for (Integer gPos : gPosList) {
            ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos,chromLen).
                    targetName(vpvgene.getGeneSymbol()).
                    upstreamLength(gopherService.getSizeUp()).
                    downstreamLength(gopherService.getSizeDown()).
                    maximumGcContent(gopherService.getMaxGCcontent()).
                    minimumGcContent(gopherService.getMinGCcontent()).
                    fastaReader(fastaReader).
                    minimumFragmentSize(gopherService.getMinFragSize()).
                    maximumRepeatContent(gopherService.getMaxRepeatContent()).
                    marginSize(gopherService.getMarginSize()).
                    isForwardStrand(vpvgene.isForward()).
                    accessionNr(vpvgene.getRefSeqID()).
                    c2alignabilityMap(chr2alignMap).
                    model(this.gopherService).
                    build();
            vp.setPromoterNumber(++n,gPosList.size());
            updateProgress(i++, total); /* this will update the progress bar */
            updateMessage(String.format("[%d/%d] Creating view point for %s", i, total, vpvgene));
            vp.generateViewpointSimple(gopherService);
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
        LOGGER.info("SimpleViewPointCreationTask, starting");
        if (ViewPoint.chosenEnzymes == null) {
            LOGGER.error("Attempt to start Simple ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        this.total = getTotalPromoterCount();
        LOGGER.info("Got {} total promoters", this.total);
        updateTitle(String.format("Creating viewpoints using 'simple' approach with %d promoters", this.total));
        updateProgress(0,1000);
        this.i = 0;
        LOGGER.info(String.format("extracting GopherGenes & have %d chromosome groups ", chromosomes.size()));
        long milli = System.currentTimeMillis();

        String faipath = this.gopherService.getIndexedGenomeFastaIndexFile();
        updateProgress(2,1000);
        String fastapath = this.gopherService.getGenomeFastaFile();
        updateProgress(20,1000);
        if (faipath == null) {
            LOGGER.error("Could not retrieve faidx file for " + fastapath);
            throw new GopherException("Could not retrieve faidx file for " + fastapath);
        }
        IndexedFastaSequenceFile fastaReader;
        LOGGER.info("Reading genome FAST from {}", fastapath);
        try {
            fastaReader = new IndexedFastaSequenceFile(new File(fastapath));
        } catch (FileNotFoundException fnfe) {
            throw new GopherException(String.format("Could not find genome fasta file [%s]",fnfe.getMessage()));
        }
        updateProgress(40,1000);
        double meanLen = getEstimatedMeanRestrictionFragmentLength(fastaReader);
        LOGGER.info("MeanRestrictionFragmentLength = {}", meanLen);
        gopherService.setEstAvgRestFragLen(meanLen);
        gopherService.setNormalDistributionSimple(meanLen);
        String chromInfoPath= gopherService.getChromInfoPathIncludingFileNameGz();
        String alignabilitMapPath= gopherService.getAlignabilityMapPathIncludingFileNameGz();
        int kmerSize=Default.KMER_SIZE;
        try {
            AlignabilityMapIterator apiterator = new AlignabilityMapIterator(alignabilitMapPath, chromInfoPath, kmerSize);
            while (apiterator.hasNext()) {
                AlignabilityMap apair = apiterator.next();
                String referenceSequenceID = apair.getChromName();
                LOGGER.trace("Creating viewpoints for RefID=" + referenceSequenceID);
                if (!chromosomes.containsKey(referenceSequenceID)) {
                    continue; // skip if we have no gene on this chromosome
                }
                ChromosomeGroup chromosome = chromosomes.get(referenceSequenceID);
                if (chromosome == null) {
                    LOGGER.error("chromosome is null while searching for \"" + referenceSequenceID + "\"");
                    for (ChromosomeGroup g : chromosomes.values()) {
                        LOGGER.error(g.getReferenceSequenceID());
                    }
                    continue;
                } else {
                    LOGGER.trace("chromosome=" + chromosome.getReferenceSequenceID());
                }
                int chromosomeLen = fastaReader.getSequence(referenceSequenceID).length();
                chromosome.getGenes().stream().forEach(gopherGene ->
                        calculateViewPoints(gopherGene, referenceSequenceID, fastaReader, apair,chromosomeLen));
            }

        } catch (IOException e){
            e.printStackTrace();
        }
        long end = milli - System.currentTimeMillis();
        LOGGER.trace(String.format("Generation of viewpoints (simple approach) took %.1f sec", end / 1000.0));
        this.gopherService.setViewPoints(viewpointlist);
        return null;
    }

}
