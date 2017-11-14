package vpvgui.model.regulatoryexome;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.log4j.Logger;
import vpvgui.exception.VPVException;
import vpvgui.io.GeneRegGTFParser;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

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
    /** Each item that we want to enrich on our regulatory gene set will become an entry in this list, including both
     * regulatory elements and exons of our target genes. */
    private Set<RegulatoryBEDFileEntry> regulatoryElementSet=null;
    /** Maximum distance 3' (downstream) to TSS (genomicPos) to be included as a regulatory element.*/
    private int downstreamThreshold =10_000;
    /** Maximum distance 5' (upstream) to TSS (genomicPos) to be included as a regulatory element.*/
    private int upstreamThreshold=50_000;
    /** Reference to the progress indicator that will be shown while we are creating the elements and the BED file. */
    ProgressIndicator progressInd =null;


    public RegulatoryExomeBuilder(Model model,ProgressIndicator pi) {
        this.pathToEnsemblRegulatoryBuild=model.getRegulatoryBuildPath();
        this.pathToRefGeneFile=model.getRefGenePath();
        this.model=model;
        this.regulatoryElementSet=new HashSet<>();
        this.progressInd=pi;
        logger.trace(String.format("We will export regulatory build %s and refgene %s",pathToEnsemblRegulatoryBuild,pathToRefGeneFile));
    }

    /** @return map of active viewpoints arranged according to chromosome so that we can quickly find them when we
     * are screening regulatory elements (which are arranged according to chromosome.
     * @param model
     * @return Map with key: a chromosome, and value: list of all {@link ViewPoint} objects on that chromosome.
     */
    private Map<String,List<ViewPoint>> getChrom2PosListMap(Model model) {
        // get all of the viewpoints with at least one selected fragment.
        List<ViewPoint> activeVP = model.getActiveViewPointList();
        // key- a chromosome; value--list of genomicPos for all active viewpoints on the chromosome
        Map<String,List<ViewPoint>> chrom2posListMap=new HashMap<>();
        activeVP.stream().forEach(viewPoint -> {
            String chrom=viewPoint.getReferenceID();
            chrom=chrom.replaceAll("chr",""); // remove the chr from chr1 etc.
            List<ViewPoint> vplist=null;
            if (chrom2posListMap.containsKey(chrom)) {
                vplist=chrom2posListMap.get(chrom);
            } else {
                vplist=new ArrayList<>();
                chrom2posListMap.put(chrom,vplist);
            }
            vplist.add(viewPoint);
        });
        return chrom2posListMap;
    }

    public void outputRegulatoryExomeBedFile(String directoryPath) throws IOException {
        String name = this.model.getProjectName();
        String fullpath = String.format("%s%s%s-regulatoryExomePanel.bed", directoryPath, File.separator, name);
        // sort the elements
        List<RegulatoryBEDFileEntry> lst = new ArrayList<>();
        lst.addAll(regulatoryElementSet);
        Collections.sort(lst);


        BufferedWriter writer = new BufferedWriter(new FileWriter(fullpath));
        for (RegulatoryBEDFileEntry rentry : lst) {
            writer.write(rentry.toString() + "\n");
        }
        writer.close();
    }

    /**
     * Parse the {@code refGene.txt.gz} file. Note that we parse zero-based numbers here.
     */
    private void collectExonsFromTargetGenes() throws Exception {
        Map<String, ViewPoint> vpmap = new HashMap<>();
        updateProgress(0.50);
        int j=0;
        int totalgenes=vpmap.size();
        for (ViewPoint vp : this.model.getActiveViewPointList()) {
            vpmap.put(vp.getAccession(), vp);
        }


        InputStream fileStream = new FileInputStream(this.pathToRefGeneFile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        BufferedReader br = new BufferedReader(decoder);
        String line;
        while ((line = br.readLine()) != null) {
            String A[] = line.split("\t");
            String accession = A[1];
            if (!vpmap.containsKey(accession)) {
                continue; // this transcript is not contained in our active targets, so we can skip it.
            }
            String chrom = A[2];
            if (chrom.contains("_")) {
                continue;
            }
            if (chrom.contains("random")) {
                continue;
            } /* do not take gene models on random contigs. */
            String strand = A[3];
            String[] beginnings = A[9].split(",");
            String[] endings = A[10].split(","); // exon begins and ends.
            String name2=A[12];
            if (beginnings.length != endings.length) { // should never happen!
                throw new VPVException(String.format("Malformed line for gene %s (%s): number of exon starts/ends should be equal, but we found %d/%d",
                        name2,accession,beginnings.length,endings.length));
            }
            for (int i=0;i<beginnings.length;++i) {
                int b=Integer.parseInt(beginnings[i]);
                int e=Integer.parseInt(endings[i]);
                String name=String.format("%s-exon%d-%d",name2,b,e); // this name will take care of duplicates from different transcripts.
                //String chrom, int from, int to, String name
                RegulatoryBEDFileEntry regentry = new RegulatoryBEDFileEntry(chrom,b,e,name);
                this.regulatoryElementSet.add(regentry);
            }
            updateProgress((0.5 + (double)++j/(double)totalgenes));

        }
        br.close();

    }





    /** extractRegulomeForTargetGenes. We will guestimate the progress based on the number of viewpoints*10*/
    @Override
    protected Void call() {
        Map<String,List<ViewPoint>> chrom2vpListMap=getChrom2PosListMap(model);
        int n_genesTimesTen=model.getVPVGeneList().size()*10;
        int j=0;
        //read in the regulatory build and save the intervals that are in the right place.
        GeneRegGTFParser parser = new GeneRegGTFParser(model.getRegulatoryBuildPath());
        try {
            parser.initGzipReader();
            while (parser.hasNext()) {
                RegulatoryElement elem = parser.next();
                String chrom=elem.getChrom();
                if (! chrom2vpListMap.containsKey(chrom)) {
                    //Very probably not an error but just a chromosome or scaffold that has not target gene for this panel
                    continue;
                }
                List<ViewPoint> vpList = chrom2vpListMap.get(chrom);
                // if the regulatory element is within downstreamThreshold of any target gene, then keep the regulatory element
                ViewPoint keepers = vpList.stream().filter(vp -> elem.isLocatedWithinThreshold(vp, upstreamThreshold,downstreamThreshold)).findAny().orElse(null);
                if (keepers!=null) {
                    RegulatoryBEDFileEntry rentry = new RegulatoryBEDFileEntry(elem);
                    this.regulatoryElementSet.add(rentry);
                    if (++j%10==0) {
                        updateProgress((double)j/(double)n_genesTimesTen);
                    }
                }
            }
            parser.close();
            collectExonsFromTargetGenes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.trace(String.format("We got %d regulatory elemenets",regulatoryElementSet.size()));
        return null;
    }



    /** Update the progress bar of the GUI in a separate thread.
     * @param pr Current progress.
     */
    private void updateProgress(double pr) {
        javafx.application.Platform.runLater(new Runnable() {
            @Override public void run() {
                if (progressInd ==null) {
                    // do nothing
                    return;
                }
                progressInd.setProgress(pr);
            }
        });
    }




}
