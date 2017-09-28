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

/**
 * This is the Task that coordinates creation of ViewPoints from the data entered by the user.
 * The class implements the extended probe design strategy with multiple fragments per ViewPoint.
 * @author Peter Robinson
 * @version 0.0.3 (2017-09-27)
 */
public class ExtendedViewPointCreationTask extends ViewPointCreationTask {
    private static final Logger logger = Logger.getLogger(ExtendedViewPointCreationTask.class.getName());
    /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     * @param model
     * @param currentVPproperty
     */
    public ExtendedViewPointCreationTask(Model model, StringProperty currentVPproperty){
        super(model,currentVPproperty);
    }


    /** This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     * @return
     * @throws Exception
     */
    protected Void call() throws Exception {
        if (ViewPoint.chosenEnzymes==null) {
            logger.error("Attempt to start ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        String cuttingMotif = ViewPoint.chosenEnzymes.get(0).getPlainSite(); // TODO Need to extend this to more than one enzyme
        logger.trace("Creating viewpoints for cuting pattern: "+cuttingMotif);

        int total= getTotalGeneCount();
        int i=0;

        for (ChromosomeGroup group : chromosomes.values()) {
            String referenceSequenceID = group.getReferenceSequenceID();/* Usually a chromosome */
            String path = this.model.getIndexFastaFilePath(referenceSequenceID);
            if (path == null) {
                logger.error("Could not retrieve faidx file for " + referenceSequenceID);
                continue;
            }
            logger.trace("Got RefID=" + referenceSequenceID);
            for (VPVGene vpvgene : group.getGenes()) {
                try {
                    IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(path));
                    int chromosomeLength=fastaReader.getSequence(referenceSequenceID).length();
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
                                chromosomeLength(chromosomeLength).
                                build();
                        updateProgress(i++, total); /* this will update the progress bar */
                        updateLabelText(this.currentVP, vpvgene.toString());
                        vp.generateViewpointExtendedApproach(model.getFragNumUp(), model.fragNumDown(), model.getMaxSizeUp(), model.getMaxSizeDown());
                        viewpointlist.add(vp);
                    }
                } catch (FileNotFoundException e) {
                    logger.error("[ERROR] could not open/find faidx file for " + referenceSequenceID);
                    logger.error(e, e);
                }
            }
        }
        logger.trace(String.format("Created %d extended viewpoints", viewpointlist.size()));
        this.model.setViewPoints(viewpointlist);
        return null;
    }

    /**
     * This function updates the label on the viewpoint creation dialog with the name of the current viewpoint.
     * @param sb A StringPoperty that is bound to a label on the viewpoint creation dialog
     * @param msg Name of the current viewpoint
     */
    private void updateLabelText(StringProperty sb,String msg) {
        Platform.runLater(new Runnable(){
            @Override
            public void run() {
                sb.setValue(String.format("Creating view point for %s",msg));
            }
        });
    }



}
