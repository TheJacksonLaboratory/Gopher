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
    Model model=null;
    /** List of {@link ViewPoint} objects that we will return to the Model when this Task is done. */
    List<ViewPoint> viewpointlist=null;

    /** Maximum distance from central position (e.g., transcription start site) of the upstream boundary of the viewpoint.*/
    private  int maxDistanceUp;
    /** Maximum distance from central position (e.g., transcription start site) of the downstream boundary of the viewpoint.*/
    private  int maxDistanceDown;

    private int minDistToGenomicPosDown;

    /** The total number of viewpoints we are making (equal to the number of unique transcription
     * start sites on all of the {@link #chromosomes}.*/
    private int n_totalViewpoints;
    /* declare viewpoint parameters as requested by Darío */

    private  Integer fragNumUp;
    private  Integer fragNumDown;

    private  Integer minSizeUp;

    private StringProperty currentVP=null;
    /** List of one or more restriction enzymes choseon by the user. */
    //private List<RestrictionEnzyme> chosenEnzymes=null;


    private  Integer minFragSize;
    private  double maxRepContent;

    private  Integer marginSize=200; /* ToDo -- allow this to be set via the menu */

    /**
     * The constructor sets up the Task of creating ViewPoints. It sets the chosen enzymes from the Model
     * Since we use the same enzymes for all ViewPoints; therefore, ViewPoint .chosenEnzymes and
     * CuttingPositionMap.restrictionEnzymeMap are static class-wide variables that get set with the corresponding
     * values for the enzymes.
     * @param model
     * @param currentVPproperty
     */
    public ExtendedViewPointCreationTask(Model model, StringProperty currentVPproperty){
        this.model=model;
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
        init_parameters();
    }

    private void assignVPVGenesToChromosomes(List<VPVGene> vgenes) {
        this.chromosomes = new HashMap<>();
        n_totalViewpoints=0;
        for (VPVGene g : vgenes) {
            String referenceseq = g.getContigID();
            ChromosomeGroup group = null;
            if (chromosomes.containsKey(referenceseq)) {
                group = chromosomes.get(referenceseq);
            } else {
                group = new ChromosomeGroup(referenceseq);
                chromosomes.put(referenceseq, group);
            }
            group.addVPVGene(g);
            n_totalViewpoints++;
        }
    }



    private void init_parameters() {
        assignVPVGenesToChromosomes(model.getVPVGeneList());
        this.fragNumUp=model.getFragNumUp();
        this.fragNumDown=model.fragNumDown();
        this.minSizeUp=model.getMinSizeUp();
        this.minDistToGenomicPosDown=model.getMinSizeDown();
        this.maxDistanceUp =model.getMaxSizeUp();
        this.maxDistanceDown =model.getMaxSizeDown();
        this.minFragSize=model.getMinFragSize();
        this.maxRepContent=model.getMaxRepeatContent();
        logger.trace("Finished initializing parameters to create ViewPoints.");
    }


    /** Get the total number of viewpoints we will create.This is needed in order
     * to get the progress indicator to be accurate.
     * @return
     */
    private int getTotalGeneCount() {
        return n_totalViewpoints;
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
                        vp.generateViewpointExtendedApproach(fragNumUp, fragNumDown, model.getMaxSizeUp(), model.getMaxSizeDown());
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
