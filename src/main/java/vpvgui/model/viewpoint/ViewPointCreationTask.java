package vpvgui.model.viewpoint;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.RestrictionEnzyme;
import vpvgui.model.VPVGene;

import javax.swing.text.View;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This is the Task that coordinates creation of ViewPoints from the data entered by the user.
 * @author Peter Robinson
 * @version 0.0.2 (2017-09-29)
 */
public class ViewPointCreationTask extends Task {
    private static final Logger logger = Logger.getLogger(ViewPointCreationTask.class.getName());
    Model model=null;

    /** List of {@link VPVGene} objects representing User's gene list. */
    List<VPVGene> vpvGeneList;
    /** List of {@link ViewPoint} objects that we will return to the Model when this Task is done. */
    List<ViewPoint> viewpointlist=null;
    /** Restriction enzyme cuttings patterns (must have at least one) */
    //private  String[] cuttingPatterns;
    /** Maximum distance from central position (e.g., transcription start site) of the upstream boundary of the viewpoint.*/
    private  int maxDistanceUp;
    /** Maximum distance from central position (e.g., transcription start site) of the downstream boundary of the viewpoint.*/
    private  int maxDistanceDown;

    private int minDistToGenomicPosDown;


    /* declare viewpoint parameters as requested by Darío */

    private  Integer fragNumUp;
    private  Integer fragNumDown;
    //private  String cuttingMotif;
    private  Integer minSizeUp;

    private StringProperty currentVP=null;
    /** List of one or more restriction enzymes choseon by the user. */
    private List<RestrictionEnzyme> chosenEnzymes=null;


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
    public ViewPointCreationTask(Model model, StringProperty currentVPproperty){
        this.model=model;
        this.viewpointlist=new ArrayList<>();
        this.currentVP=currentVPproperty;
        ViewPoint.setChosenEnzymes(model.getChosenEnzymelist());
        CuttingPositionMap.restrictionEnzymeMap = new HashMap<>();
        List<RestrictionEnzyme> chosen = model.getChosenEnzymelist();
        if (chosen==null) {
            logger.error("Unable to retrieve list of chosen restriction enzymes");
            return;
        } else {
            logger.trace(String.format("Setting up viewpoint creation for %d enzymes", chosen.size() ));
        }
        for (RestrictionEnzyme re : chosen) {
            String site = re.getPlainSite();
            CuttingPositionMap.restrictionEnzymeMap.put(site,re);

        }
        init_parameters();
    }



    private void init_parameters() {
        this.vpvGeneList=model.getVPVGeneList();
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
    protected Object call() throws Exception {
        if (ViewPoint.chosenEnzymes==null) {
            logger.error("Attempt to start ViewPoint creation thread with null chosenEnzymes");
            return null;
        }
        String cuttingMotif = ViewPoint.chosenEnzymes.get(0).getPlainSite(); // TODO Need to extend this to more than one enzyme
        logger.trace("Creating viewpoints for cuting pattern: "+cuttingMotif);

        int total=getTotalViewpoints();
        int i=0;

        int maxSizeUp=1500;
        int maxSizeDown=1500;

        for (VPVGene vpvgene:this.vpvGeneList) {
            String referenceSequenceID = vpvgene.getContigID();/* Usually a chromosome */
            //logger.trace("Retrieving indexed fasta file for contig: "+referenceSequenceID);
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
                            maxDistToGenomicPosUp(maxDistanceUp).
                            maxDistToGenomicPosDown(maxDistanceDown).
                            //cuttingPatterns(this.cuttingPatterns).
                            fastaReader(fastaReader).
                            minimumSizeUp(minSizeUp).
                            maximumSizeUp(maxDistanceUp).
                            minimumSizeDown(minDistToGenomicPosDown).
                            maximumSizeDown(maxDistanceDown).
                            minimumFragmentSize(minFragSize).
                            maximumRepeatContent(maxRepContent).
                            marginSize(marginSize).
                            build();
                    updateProgress(i++,total); /* this will update the progress bar */
                    updateLabelText(this.currentVP,vpvgene.toString());
                    vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif,maxSizeUp,maxSizeDown);
                    viewpointlist.add(vp);
                    logger.trace(String.format("Adding viewpoint %s to list (size: %d)",vp.getTargetName(),viewpointlist.size()));
                }
            } catch (FileNotFoundException e) {
                logger.error("[ERROR] could not open/find faidx file for "+referenceSequenceID);
                logger.error(e,e);
                // just skip this TODO -- better error handling
            }
        }
        this.model.setViewPoints(viewpointlist);
        return true;
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
