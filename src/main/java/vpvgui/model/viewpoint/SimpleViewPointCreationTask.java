package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.VPVGene;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleViewPointCreationTask  extends ViewPointCreationTask {
    private static final Logger logger = Logger.getLogger(SimpleViewPointCreationTask.class.getName());
    Model model=null;
    /** List of {@link VPVGene} objects representing User's gene list. */
    List<VPVGene> vpvGeneList;
    /** List of {@link ViewPoint} objects that we will return to the Model when this Task is done. */
    List<ViewPoint> viewpointlist=null;

    private StringProperty currentVP=null;
    /** List of one or more restriction enzymes choseon by the user. */
    private List<RestrictionEnzyme> chosenEnzymes=null;




//    private  Integer marginSize=200; /* ToDo -- allow this to be set via the menu */

    /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     * @param model
     * @param currentVPproperty
     */
    public SimpleViewPointCreationTask(Model model, StringProperty currentVPproperty){
        this.model=model;
        this.vpvGeneList=model.getVPVGeneList();
        this.viewpointlist=new ArrayList<>();
        this.currentVP=currentVPproperty;
        ViewPoint.setChosenEnzymes(model.getChosenEnzymelist());
        SegmentFactory.restrictionEnzymeMap = new HashMap<>();
        List<RestrictionEnzyme> chosen = model.getChosenEnzymelist();
        if (chosen==null) {
            logger.error("Unable to retrieve list of chosen restriction enzymes");
            return;
        } else {
            logger.trace(String.format("Setting up viewpoint creation for %d enzymes", chosen.size() ));
        }
        for (RestrictionEnzyme re : chosen) {
            String site = re.getPlainSite();
            SegmentFactory.restrictionEnzymeMap.put(site,re);
        }
    }


    /** Get the total number of viewpoints we will create.This is needed in order
     * to get the progress indicator to be accurate.
     * @return
     */
    private int getTotalViewpoints() {
        int n=0;
        for (VPVGene vpvgene:this.vpvGeneList) {
            n += vpvgene.n_viewpointstarts();
        }
        return n;
    }

    /** This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     * @return
     * @throws Exception
     */
    protected Void call() throws Exception {
        if (ViewPoint.chosenEnzymes==null) {
            logger.error("Attempt to start Simple ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        int total=getTotalViewpoints();
        int i=0;

        for (VPVGene vpvgene:this.vpvGeneList) {
            String referenceSequenceID = vpvgene.getContigID();/* Usually a chromosome */
            String path=this.model.getIndexFastaFilePath(referenceSequenceID);
            if (path==null) {
                logger.error("Could not retrieve faidx file for "+referenceSequenceID);
                continue;
            }
            try {
                IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(path));
                List<Integer> gPosList = vpvgene.getTSSlist();
                for (Integer gPos : gPosList) {
                    ViewPoint vp = new ViewPoint.Builder(referenceSequenceID,gPos).
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
                    updateProgress(i++,total); /* this will update the progress bar */
                    updateLabelText(this.currentVP,vpvgene.toString());
                    vp.generateViewpointSimple();
                    if (vp.getResolved()) {
                        viewpointlist.add(vp);
                        logger.trace(String.format("Adding viewpoint %s to list (size: %d)",vp.getTargetName(),viewpointlist.size()));
                    } else {
                        logger.trace(String.format("Skipping viewpoint %s (size: %d) because it was not resolved",vp.getTargetName(),viewpointlist.size()));
                    }

                }
            } catch (FileNotFoundException e) {
                logger.error("[ERROR] could not open/find faidx file for "+referenceSequenceID);
                logger.error(e,e);
            }
        }
        this.model.setViewPoints(viewpointlist);
        return null;
    }


    private void updateLabelText(StringProperty sb,String msg) {
        Platform.runLater(new Runnable(){
            @Override
            public void run() {
                sb.setValue(String.format("Creating view point for %s",msg));
            }
        });
    }








}
