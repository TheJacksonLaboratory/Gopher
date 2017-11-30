package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import org.apache.log4j.Logger;
import vpvgui.exception.VPVException;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.VPVGene;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This is the Task that coordinates creation of ViewPoints from the data entered by the user.
 * The class implements the extended probe design strategy with multiple fragments per ViewPoint.
 *
 * @author Peter Robinson
 * @version 0.2.1 (2017-09-27)
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
     *
     * @param model
     * @param currentVPproperty
     */
    public ExtendedViewPointCreationTask(Model model, StringProperty currentVPproperty) {
        super(model, currentVPproperty);
    }

    private void calculateViewPoints(VPVGene vpvgene, String referenceSequenceID, IndexedFastaSequenceFile fastaReader) {
        int chromosomeLength = fastaReader.getSequence(referenceSequenceID).length();
        List<Integer> gPosList = vpvgene.getTSSlist();
        if (! vpvgene.isForward()) {
            Collections.reverse(gPosList);
        }

        int n=0; // we will order the promoters from first (most upstream) to last
        // Note we do this differently according to strand.
        for (Integer gPos : gPosList) {
            ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos).
                    targetName(vpvgene.getGeneSymbol()).
                    upstreamLength(model.getSizeUp()).
                    downstreamLength(model.getSizeDown()).
                    maximumGcContent(model.getMaxGCcontent()).
                    minimumGcContent(model.getMinGCcontent()).
                    fastaReader(fastaReader).
                    isForwardStrand(vpvgene.isForward()).
                    minimumFragmentSize(model.getMinFragSize()).
                    maximumRepeatContent(model.getMaxRepeatContent()).
                    marginSize(model.getMarginSize()).
                    isForwardStrand(vpvgene.isForward()).
                    accessionNr(vpvgene.getRefSeqID()).
                    build();
            vp.setPromoterNumber(++n,gPosList.size());
            updateProgress(i++, total); /* this will update the progress bar */
            updateLabelText(this.currentVP, vpvgene.toString());
            vp.generateViewpointExtendedApproach(model.getSizeUp(), model.getSizeDown(),model.getAllowSingleMargin());
            viewpointlist.add(vp);
        }
    }


    /**
     * This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     *
     * @return
     * @throws Exception
     */
    protected Void call() throws Exception {
        if (ViewPoint.chosenEnzymes == null) {
            logger.error("Attempt to start ViewPoint creation with chosenEnzymes=null");
            throw new VPVException("Attempt to start ViewPoint creation thread with null chosenEnzymes");
        }
        this.total = getTotalGeneCount();
        this.i = 0;

        for (ChromosomeGroup group : chromosomes.values()) {
            String referenceSequenceID = group.getReferenceSequenceID();/* Usually a chromosome */
            String path = this.model.getIndexFastaFilePath(referenceSequenceID);
            if (path == null) {
                logger.error("Could not retrieve faidx file for " + referenceSequenceID);
                continue;
            }
            logger.trace("Creating viewpoints for RefID=" + referenceSequenceID);
            IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(path));
            group.getGenes().parallelStream().forEach(vpvGene -> {
                calculateViewPoints(vpvGene, referenceSequenceID, fastaReader);
            });
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
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sb.setValue(String.format("Creating view point for %s", msg));
            }
        });
    }


}
