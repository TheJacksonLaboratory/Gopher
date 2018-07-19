package gopher.model.viewpoint;

import gopher.exception.GopherException;
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
    /** Total number of viewpoints */
    private int total;
    /** Index of current viewpoint */
    private int i;


  /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     *  @param model
   */
  public SimpleViewPointCreationTask(Model model) {
      super(model);

  }

    /** This will be replace by the method below.*/
  //  @Deprecated
//    private void calculateViewPoints(GopherGene vpvgene, String referenceSequenceID, IndexedFastaSequenceFile fastaReader) {
//        int chromosomeLength = fastaReader.getSequence(referenceSequenceID).length();
//        logger.trace(String.format("Length of %s is %d", referenceSequenceID, chromosomeLength));
//        logger.error(String.format("Getting TSS for vpv %s", vpvgene.getGeneSymbol()));
//        List<Integer> gPosList = vpvgene.getTSSlist();
//        int n=0; // we will order the promoters from first (most upstream) to last
//        // Note we do this differently according to strand.
//        //Instrumentation inst=null;
//        for (Integer gPos : gPosList) {
//            ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos).
//                    targetName(vpvgene.getGeneSymbol()).
//                    upstreamLength(model.getSizeUp()).
//                    downstreamLength(model.getSizeDown()).
//                    maximumGcContent(model.getMaxGCcontent()).
//                    minimumGcContent(model.getMinGCcontent()).
//                    fastaReader(fastaReader).
//                    minimumFragmentSize(model.getMinFragSize()).
//                    maximumRepeatContent(model.getMaxRepeatContent()).
//                    marginSize(model.getMarginSize()).
//                    isForwardStrand(vpvgene.isForward()).
//                    accessionNr(vpvgene.getRefSeqID()).
//                    alignabilityMap(this.alignabilityMap).
//                    model(this.model).
//                    build();
//
//            vp.setPromoterNumber(++n,gPosList.size());
//            updateProgress(i++, total); /* this will update the progress bar */
//            updateLabelText(this.currentVP, vpvgene.toString());
//            vp.generateViewpointSimple(model);
//            if (vp.getResolved()) {
//                viewpointlist.add(vp);
//                logger.trace(String.format("Adding viewpoint %s to list (size: %d)", vp.getTargetName(), viewpointlist.size()));
//            } else {
//                logger.trace(String.format("Skipping viewpoint %s (size: %d) because it was not resolved", vp.getTargetName(), viewpointlist.size()));
//            }
//        }
//    }

    /** This method will replace calculateViewPoints -- still needs to be tested */
    private void calculateViewPoints(GopherGene vpvgene, String referenceSequenceID, IndexedFastaSequenceFile fastaReader, Chromosome2AlignabilityMap chr2alignMap) {
        int chromosomeLength = fastaReader.getSequence(referenceSequenceID).length();
        logger.trace(String.format("NEW Length of %s is %d", referenceSequenceID, chromosomeLength));
        logger.error(String.format("NEW Getting TSS for vpv %s", vpvgene.getGeneSymbol()));
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
                    c2alignabilityMap(chr2alignMap).
                    model(this.model).
                    build();
            vp.setPromoterNumber(++n,gPosList.size());
            updateProgress(i++, total); /* this will update the progress bar */
            updateMessage(String.format("[%d/%d] Creating view point for %s", i, total, vpvgene.toString()));
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
        updateTitle("Creating viewpoints using 'simple' approach");
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
        //TODO -- PLEASE CHECK
        double meanLen = getEstimatedMeanRestrictionFragmentLength(fastaReader);
        model.setEstAvgRestFragLen(meanLen);
        String chromInfoPath=model.getChromInfoPathIncludingFileNameGz();
        String alignabilitMapPath=model.getAlignabilityMapPathIncludingFileNameGz();
        int kmerSize=50; // TODO WHERE DOES THIS COME FROM?
        try {
            AlignabilityMapIterator apiterator = new AlignabilityMapIterator(alignabilitMapPath,chromInfoPath, kmerSize);
            logger.trace("About to start iteration in new function");

            while (apiterator.hasNext()) {
                Chromosome2AlignabilityMap apair = apiterator.next();
                String referenceSequenceID = apair.getChromName();
                logger.trace("NEW--Creating viewpoints for RefID=" + referenceSequenceID);
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
        long end = milli - System.currentTimeMillis();
        logger.trace(String.format("Generation of viewpoints (simple approach) took %.1f sec", end / 1000.0));
        this.model.setViewPoints(viewpointlist);
        return null;
    }

}
