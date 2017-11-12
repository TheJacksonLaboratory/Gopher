package vpvgui.model.regulatoryexome;

import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class uses data from the Ensembl regulatory build as well as the UCSC refGene.txt.gz files to create
 * a regulatory exome build. We will create probes for all of the exons (including 5' and 3' UTRs) as well
 * as the regulatory elements in Ensembl that are sufficiently close to the transcription start site.
 * @author Peter Robinson
 * @version 0.1.1 (2017-11-11)
 */
public class RegulatoryExomeBuilder {
    static Logger logger = Logger.getLogger(RegulatoryExomeBuilder.class.getName());
    /** Path to regulatory build file, e.g., homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff.gz */
    private String pathToEnsemblRegulatoryBuild=null;
    /** Path to transcript definition file, refGene.txt.gz */
    private String pathToRefGeneFile=null;

    private Model model=null;

    private int threshold=50_000;


    public RegulatoryExomeBuilder(String pathToRegulatoryBuild, String pathToRefGene) {
        this.pathToEnsemblRegulatoryBuild=pathToRegulatoryBuild;
        this.pathToRefGeneFile=pathToRefGene;
        logger.trace(String.format("Get regulatory build %s and refgene %s",pathToEnsemblRegulatoryBuild,pathToRefGeneFile));
    }


    public void extractRegulomeForTargetGenes(Model model) {
        this.model=model;
        // get all of the viewpoints with at least one selected fragment.
        List<ViewPoint> activeVP = model.getActiveViewPointList();
        // key- a chromosome; value--list of genomicPos for all active viewpoints on the chromosome
        Map<String,List<Integer>> chrom2posListMap=new HashMap<>();
        activeVP.stream().forEach(viewPoint -> {
            String chrom=viewPoint.getReferenceID();
            int pos = viewPoint.getGenomicPos();
            List<Integer> poslist=null;
            if (chrom2posListMap.containsKey(chrom)) {
                poslist=chrom2posListMap.get(chrom);
            } else {
                poslist=new ArrayList<>();
            }
            poslist.add(pos);
        });
        //TODO continue -- read in the regulatory build and save the intervals that are in the right place.
    }






}
