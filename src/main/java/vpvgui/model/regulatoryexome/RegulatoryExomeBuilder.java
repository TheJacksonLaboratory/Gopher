package vpvgui.model.regulatoryexome;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.log4j.Logger;
import vpvgui.io.GeneRegGTFParser;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class uses data from the Ensembl regulatory build as well as the UCSC refGene.txt.gz files to create
 * a regulatory exome build. We will create probes for all of the exons (including 5' and 3' UTRs) as well
 * as the regulatory elements in Ensembl that are sufficiently close to the transcription start site.
 * @author Peter Robinson
 * @version 0.1.1 (2017-11-11)
 */
public class RegulatoryExomeBuilder extends Task<Void> {
    static Logger logger = Logger.getLogger(RegulatoryExomeBuilder.class.getName());
    /** Path to regulatory build file, e.g., homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff.gz */
    private String pathToEnsemblRegulatoryBuild=null;
    /** Path to transcript definition file, refGene.txt.gz */
    private String pathToRefGeneFile=null;

    private Model model=null;

    private List<RegulatoryElement> targetRegulomeElementList=null;

    private int threshold=50_000;

    ProgressIndicator pi=null;


    public RegulatoryExomeBuilder(Model model) {
        this.pathToEnsemblRegulatoryBuild=model.getRegulatoryBuildPath();
        this.pathToRefGeneFile=model.getRefGenePath();
        this.model=model;
        targetRegulomeElementList=new ArrayList<>();
        logger.trace(String.format("Get regulatory build %s and refgene %s",pathToEnsemblRegulatoryBuild,pathToRefGeneFile));
    }

    public void setProgressIndicator(ProgressIndicator prog) {
        this.pi=prog;
    }


    private Map<String,List<Integer>> getChrom2PosListMap(Model model) {
        // get all of the viewpoints with at least one selected fragment.
        List<ViewPoint> activeVP = model.getActiveViewPointList();
        // key- a chromosome; value--list of genomicPos for all active viewpoints on the chromosome
        Map<String,List<Integer>> chrom2posListMap=new HashMap<>();
        activeVP.stream().forEach(viewPoint -> {
            String chrom=viewPoint.getReferenceID();
            chrom=chrom.replaceAll("chr",""); // remove the chr from chr1 etc.
            int pos = viewPoint.getGenomicPos();
            List<Integer> poslist=null;
            if (chrom2posListMap.containsKey(chrom)) {
                poslist=chrom2posListMap.get(chrom);
            } else {
                poslist=new ArrayList<>();
                chrom2posListMap.put(chrom,poslist);
            }
            poslist.add(pos);
        });
        return chrom2posListMap;
    }

    public void outputRegulatoryExomeBedFile(String path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            for (RegulatoryElement regelem : this.targetRegulomeElementList) {
                String name = String.format("%s[%s]",regelem.getId(),regelem.getCategory());
                String outline = String.format("%s\t%d\t%d\t%s", regelem.getChrom(), regelem.getFrom(), regelem.getTo(), name);
                writer.write(outline + "\n");
            }
            writer.close();

    }


    /** extractRegulomeForTargetGenes*/
    @Override
    protected Void call() {
        this.model=model;
        Map<String,List<Integer>> chrom2posListMap=getChrom2PosListMap(model);
        //read in the regulatory build and save the intervals that are in the right place.
        GeneRegGTFParser parser = new GeneRegGTFParser(model.getRegulatoryBuildPath());
        try {
            parser.initGzipReader();
            while (parser.hasNext()) {
                RegulatoryElement elem = parser.next();
                String chrom=elem.getChrom();
                if (! chrom2posListMap.containsKey(chrom)) {
                    logger.error(String.format("Could not find chromosome \"%s\" in reg map",chrom));
                    continue;
                }
                List<Integer> starts = chrom2posListMap.get(chrom);
                // if the regulatory element is within threshold of any target gene, then keep the regulatory element
                Integer keepers = starts.stream().filter(i -> elem.isLocatedWithinThreshold(i,threshold)).findAny().orElse(0);
                if (keepers>0) {
                    targetRegulomeElementList.add(elem);
                }
            }
            parser.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String c:chrom2posListMap.keySet()){
            logger.trace(String.format("chrom map has \"%s\"",c));
        }
        logger.trace(String.format("We got %d regulatory elemenets",targetRegulomeElementList.size()));
        return null;
    }



    /** Update the progress bar of the GUI in a separate thread.
     * @param pr Current progress.
     */
    private void updateProgress(double pr) {
        javafx.application.Platform.runLater(new Runnable() {
            @Override public void run() {
                if (pi==null) {
                    // do nothing
                    return;
                }
                pi.setProgress(pr);
            }
        });
    }




}
