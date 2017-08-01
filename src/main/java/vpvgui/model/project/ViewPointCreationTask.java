package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import vpvgui.model.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peterrobinson on 7/22/17.
 */
public class ViewPointCreationTask extends Task {
    static Logger logger = Logger.getLogger(ViewPointCreationTask.class.getName());
    Model model=null;

    /* List of VPVGenes representing User's gene list. */
    List<VPVGene> vpvGeneList;

    List<ViewPoint> viewpointlist=null;

    /** Restriction enzyme cuttings patterns (must have at least one) */
    private  String[] cuttingPatterns;
    /** Maximum distance from central position (e.g., transcription start site) of the upstream boundary of the viewpoint.*/
    private  int maxDistToGenomicPosUp;
    /** Maximum distance from central position (e.g., transcription start site) of the downstream boundary of the viewpoint.*/
    private  int maxDistToGenomicPosDown;

    private int minDistToGenomicPosDown;

    private  IndexedFastaSequenceFile fastaReader;

    /* declare viewpoint parameters as requested by Darío */

    private  Integer fragNumUp;
    private  Integer fragNumDown;
    //private  String cuttingMotif;
    private  Integer minSizeUp;

    private StringProperty currentVP=null;


    private  Integer minFragSize;
    private  double maxRepContent;

    private  Integer marginSize=200; /* ToDo -- allow this to be set via the menu */

    public ViewPointCreationTask(Model model, StringProperty currentVPproperty){
        this.model=model;
        this.viewpointlist=new ArrayList<>();
        this.currentVP=currentVPproperty;
        init_parameters();
    }





    private void init_parameters() {
        this.vpvGeneList=model.getVPVGeneList();
        this.fragNumUp=model.fragNumUp();
        this.fragNumDown=model.fragNumDown();
        this.minSizeUp=model.minSizeUp();
        this.minDistToGenomicPosDown=model.minSizeDown();
        this.maxDistToGenomicPosUp=model.maxSizeUp();
        this.maxDistToGenomicPosDown=model.maxSizeDown();
        this.minFragSize=model.minFragSize();
        this.maxRepContent=model.maxRepeatContent();
        //this.cuttingPatterns=model.getCuttingPatterns();
        //TODO Get the cuttings patterns from GUI!
        this.cuttingPatterns=  new String[]{"GATC"};
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

    public  List<VPVGene> getViewPoints(){ return vpvGeneList;}

    /** This is the method that will create the viewpoints.
     * We have placed it in a task because it takes a while.
     * @return
     * @throws Exception
     */
    protected Object call() throws Exception {
        String cuttingMotif=this.cuttingPatterns[0];/* TODO -- Why do we need this instead of taking cutting patterns? */
        logger.trace("Creating viewpoints for cuting pattern: "+cuttingMotif);

        int total=getTotalViewpoints();
        int i=0;

        for (VPVGene vpvgene:this.vpvGeneList) {
            String referenceSequenceID = vpvgene.getContigID();/* Usually a chromosome */
            //logger.trace("Retrieving indexed fasta file for contig: "+referenceSequenceID);
            String path=this.model.getIndexFastaFilePath(referenceSequenceID);
            if (path==null) {
                logger.error("Could not retrieve faidx file for "+referenceSequenceID);
                continue;
            }
            try {
                this.fastaReader = new IndexedFastaSequenceFile(new File(path));
                List<Integer> gPosList = vpvgene.getTSSlist();
                for (Integer gPos : gPosList) {
                    ViewPoint vp = new ViewPoint(referenceSequenceID, gPos, maxDistToGenomicPosUp, maxDistToGenomicPosDown,
                            cuttingPatterns, fastaReader);
                    vp.setTargetName(vpvgene.getGeneSymbol());
                    updateProgress(i++,total); /* this will update the progress bar */
                    updateLabelText(this.currentVP,vpvgene.getGeneSymbol());
                    vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif, minSizeUp, maxDistToGenomicPosUp, minDistToGenomicPosDown, maxDistToGenomicPosDown,
                            minFragSize, maxRepContent, marginSize);
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
