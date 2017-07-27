package vpvgui.model.project;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.apache.log4j.Logger;
import vpvgui.model.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peterrobinson on 7/22/17.
 */
public class ViewPointFactory {
    static Logger logger = Logger.getLogger(ViewPointFactory.class.getName());
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


    private  Integer minFragSize;
    private  double maxRepContent;

    private  Integer marginSize=200; /* ToDo -- allow this to be set via the menu */

    public ViewPointFactory(Model model){
        this.model=model;
        this.viewpointlist=new ArrayList<>();
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





    public  List<VPVGene> getViewPoints(){ return vpvGeneList;}


    public void createViewPoints() {
        String cuttingMotif=this.cuttingPatterns[0];/* TODO -- Why do we need this instead of taking cutting patterns? */
        logger.trace("Creating viewpoints for cuting pattern: "+cuttingMotif);
        int max=10;
        int i=0;
        for (VPVGene vpvgene:this.vpvGeneList) {
            String referenceSequenceID = vpvgene.getContigID();/* Usually a chromosome */
            logger.trace("Retrieving indexed fasta file for contig: "+referenceSequenceID);
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
    }



}
