package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.VPVGene;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class coordinates the construction of simple (one probe per viewpoint) ViewPoints,
 * @author Peter Robinson
 * @version 0.0.2
 */
public class SimpleViewPointCreationTask extends ViewPointCreationTask {
    private static final Logger logger = Logger.getLogger(SimpleViewPointCreationTask.class.getName());
  /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     *
     * @param model
     * @param currentVPproperty
     */
    public SimpleViewPointCreationTask(Model model, StringProperty currentVPproperty) {
        super(model,currentVPproperty);
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
            logger.error("Attempt to start Simple ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        int total = getTotalGeneCount();
        int i = 0;
        logger.trace(String.format("extracting VPVGenes & have %d chromosome groups ", chromosomes.size()));
        long milli=System.currentTimeMillis();
        for (ChromosomeGroup group : chromosomes.values()) {
            String referenceSequenceID = group.getReferenceSequenceID();/* Usually a chromosome */
            String path = this.model.getIndexFastaFilePath(referenceSequenceID);
            if (path == null) {
                logger.error("Could not retrieve faidx file for " + referenceSequenceID);
                continue;
            }
            logger.trace("Got RefID="+referenceSequenceID);
            for (VPVGene vpvgene : group.getGenes()) {
                try {
                    IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(path));
                    logger.error(String.format("Getting TSS for vpv %s", vpvgene.getGeneSymbol()));
                    List<Integer> gPosList = vpvgene.getTSSlist();
                    for (Integer gPos : gPosList) {
                        ViewPoint vp = new ViewPoint.Builder(referenceSequenceID, gPos).
                                targetName(vpvgene.getGeneSymbol()).
                                maxDistToGenomicPosUp(model.getMaxSizeUp()).
                                maxDistToGenomicPosDown(model.getMaxSizeDown()).
                                minimumSizeDown(model.getMinSizeDown()).
                                maximumSizeDown(model.getMaxSizeDown()).
                                fastaReader(fastaReader).
                                minimumSizeUp(model.getMinSizeUp()).
                                maximumSizeUp(model.getMaxSizeUp()).
                                minimumFragmentSize(model.getMinFragSize()).
                                maximumRepeatContent(model.getMaxRepeatContent()).
                                marginSize(model.getMarginSize()).
                                build();
                        updateProgress(i++, total); /* this will update the progress bar */
                        updateLabelText(this.currentVP, vpvgene.toString());
                        vp.generateViewpointSimple();
                        if (vp.getResolved()) {
                            viewpointlist.add(vp);
                            logger.trace(String.format("Adding viewpoint %s to list (size: %d)", vp.getTargetName(), viewpointlist.size()));
                        } else {
                            logger.trace(String.format("Skipping viewpoint %s (size: %d) because it was not resolved", vp.getTargetName(), viewpointlist.size()));
                        }

                    }
                } catch (FileNotFoundException e) {
                    logger.error("[ERROR] could not open/find faidx file for " + referenceSequenceID);
                    logger.error(e, e);
                }
            }
        }
        long end=milli-System.currentTimeMillis();
        logger.trace(String.format("It took %.1f sec",end/1000.0 ));
        this.model.setViewPoints(viewpointlist);
        return null;
    }


    private void updateLabelText(StringProperty sb, String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sb.setValue(String.format("Creating view point for %s", msg));
            }
        });
    }


}
