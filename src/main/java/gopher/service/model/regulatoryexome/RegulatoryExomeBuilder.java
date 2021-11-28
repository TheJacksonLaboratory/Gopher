package gopher.service.model.regulatoryexome;

import gopher.service.GopherService;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import gopher.exception.GopherException;
import gopher.gui.factories.PopupFactory;
import gopher.io.GeneRegGTFParser;
import gopher.service.model.GopherModel;
import gopher.service.model.viewpoint.ViewPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * This class uses data from the Ensembl regulatory build as well as the UCSC refGene.txt.gz files to create
 * a regulatory exome build. We will create probes for all of the exons (including 5' and 3' UTRs) as well
 * as the regulatory elements in Ensembl that are sufficiently close to the transcription start site.
 * @author Peter Robinson
 * @version 0.1.3 (2018-02-16)
 */
public class RegulatoryExomeBuilder extends Task<Void> {
    private static Logger logger = LoggerFactory.getLogger(RegulatoryExomeBuilder.class.getName());
    /** Path to regulatory build file, e.g., homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff.gz */
    private final String pathToEnsemblRegulatoryBuild;
    /** Path to transcript definition file, refGene.txt.gz */
    private final String pathToRefGeneFile;

    private GopherService model;
    /** Each item that we want to enrich on our regulatory gene set will become an entry in this list, including both
     * regulatory elements and exons of our target genes. */
    private Set<RegulatoryBEDFileEntry> regulatoryElementSet;
    /** Maximum distance 3' (downstream) to TSS (genomicPos) to be included as a regulatory element.*/
    private int downstreamThreshold =10_000;
    /** Maximum distance 5' (upstream) to TSS (genomicPos) to be included as a regulatory element.*/
    private int upstreamThreshold=50_000;
    /** Reference to the progress indicator that will be shown while we are creating the elements and the BED file. */
    private final ProgressIndicator progressInd;

    private int totalRegulatoryElements;
    private int chosenRegulatoryElements;
    private int totalExons;
    private List<String> status=new ArrayList<>();
    private final List<RegulationCategory> chosenCategories;

    public RegulatoryExomeBuilder(GopherService model, List<RegulationCategory>  chosen, ProgressIndicator pi) {
        this.pathToEnsemblRegulatoryBuild=model.getRegulatoryBuildPath();
        this.pathToRefGeneFile=model.getRefGenePath();
        this.model=model;
        this.regulatoryElementSet=new HashSet<>();
        this.progressInd=pi;
        chosenCategories=chosen;
        String msg = String.format("We will create regulatory build from %s and %s",pathToEnsemblRegulatoryBuild,pathToRefGeneFile);
        logger.trace(msg);
        status.add(msg);
    }

    /**
     * @param model reference to the {@link GopherModel} object
     * @return Map with key: a chromosome, and value: list of all {@link ViewPoint} objects on that chromosome.
     */
    private Map<String,List<ViewPoint>> getChrom2PosListMap(GopherService model) {
        // get all of the viewpoints with at least one selected digest.
        List<ViewPoint> activeVP = model.getActiveViewPointList();
        // key- a chromosome; value--list of genomicPos for all active viewpoints on the chromosome
        Map<String,List<ViewPoint>> chrom2posListMap=new HashMap<>();
        activeVP.forEach(viewPoint -> {
            String chrom=viewPoint.getReferenceID();
            chrom=chrom.replaceAll("chr",""); // remove the chr from chr1 etc.
            List<ViewPoint> vplist;
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


    private void checkOverlap(List<RegulatoryBEDFileEntry> elementlist) {
        RegulatoryBEDFileEntry currententry=null;
        logger.info("Checking overlap of entries for regulatory exome");
        for (RegulatoryBEDFileEntry entry : elementlist) {
            if (entry.overlaps(currententry)) {
                logger.info(entry.toString() + " overlaps " + (currententry != null ? currententry.toString() : ""));
            }
            currententry=entry;
        }
        logger.info("Done overlap check");
    }





    public void outputRegulatoryExomeBedFile(String directoryPath) throws IOException {
        String name = this.model.getProjectName();
        String fullpath = String.format("%s%s%s-regulatoryExomePanel.bed", directoryPath, File.separator, name);
        status.add("Exporting to " + fullpath);
        // sort the elements
        List<RegulatoryBEDFileEntry> lst = new ArrayList<>(regulatoryElementSet);
        Collections.sort(lst);
        checkOverlap(lst);
        logger.trace(String.format("We will export reg build to %s",fullpath ));
        BufferedWriter writer = new BufferedWriter(new FileWriter(fullpath));
        for (RegulatoryBEDFileEntry rentry : lst) {
            writer.write(rentry.toString() + "\n");
        }
        writer.close();
    }

    /**
     * Parse the {@code refGene.txt.gz} file. Note that we parse zero-based numbers here.
     */
    private void collectExonsFromTargetGenes() throws GopherException,IOException {
        Map<String, ViewPoint> vpmap = new HashMap<>();
        updateProgress(0.05);
        int j=0;
        for (ViewPoint vp : this.model.getActiveViewPointList()) {
            vpmap.put(vp.getAccession(), vp);
        }
        int totalgenes=vpmap.size();
        status.add(String.format("%d genes for regulatory exome",totalgenes));

        InputStream fileStream = new FileInputStream(this.pathToRefGeneFile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);
        BufferedReader br = new BufferedReader(decoder);
        String line;
        while ((line = br.readLine()) != null) {
            String[] A = line.split("\t");
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
            //String strand = A[3];
            String[] beginnings = A[9].split(",");
            String[] endings = A[10].split(","); // exon begins and ends.
            String name2=A[12];
            if (beginnings.length != endings.length) { // should never happen!
                throw new GopherException(String.format("Malformed line for gene %s (%s): number of exon starts/ends should be equal, but we found %d/%d",
                        name2,accession,beginnings.length,endings.length));
            }
            for (int i=0;i<beginnings.length;++i) {
                int b=Integer.parseInt(beginnings[i]);
                int e=Integer.parseInt(endings[i]);
                totalExons += beginnings.length;
                String name=String.format("%s-exon%d-%d",name2,b,e); // this name will take care of duplicates from different transcripts.
                //String chrom, int from, int to, String name
                RegulatoryBEDFileEntry regentry = new RegulatoryBEDFileEntry(chrom,b,e,name);
                this.regulatoryElementSet.add(regentry);
            }
            updateProgress((0.5D + ++j/(double)totalgenes));
        }
        br.close();
    }

    /** Add all viewpoints to our regulatory panel. The intention of this is that we would like to
     * enrich the DNA of all segments we are investigating in the capture HiC, in addition to the elements from the
     * Ensembl regulatory build. This will allow us to sequence things like variants in promotoers and UTRs.
     */
    private void collectViewPointsFromHiCPanel() {
        for (ViewPoint vp : this.model.getViewPointList()) {
            String name=String.format("viewpoint%s_%s:%d-%d",vp.getReferenceID(),vp.getReferenceID(),vp.getStartPos(),vp.getEndPos());
            RegulatoryBEDFileEntry VPentry = new RegulatoryBEDFileEntry(vp.getReferenceID(),vp.getStartPos(),vp.getEndPos(),name);
            this.regulatoryElementSet.add(VPentry);
        }
    }




    public String getStatus() {
        return status.stream().collect(Collectors.joining("\n"));
    }


    /** extractRegulomeForTargetGenes. We will guestimate the progress based on the number of viewpoints*10*/
    @Override
    protected Void call() throws GopherException {
        Map<String,List<ViewPoint>> chrom2vpListMap = getChrom2PosListMap(model);
        if (chrom2vpListMap.size()==0) {
                PopupFactory.displayError("No Viewpoints chosen",
                        "Create view points before exporting regulatory bed file");

            return null;
        }
        int n_genesTimesTen=model.getGopherGeneList().size()*10;
        int j=0;
        //read in the regulatory build and save the intervals that are in the right place.
        GeneRegGTFParser parser = new GeneRegGTFParser(model.getRegulatoryBuildPath());
        totalRegulatoryElements=0;
        chosenRegulatoryElements=0;
        try {
            parser.initGzipReader();
            while (parser.hasNext()) {
                RegulatoryElement elem = parser.next();
                if (! chosenCategories.contains(elem.getCategory())) {
                    continue; // only inlcude the chosen regulatory categories
                }
                totalRegulatoryElements++;
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
                    chosenRegulatoryElements++;
                    if (++j%10==0) {
                        updateProgress((double)j/(double)n_genesTimesTen);
                    }
                }
            }
            parser.close();
            if (chosenCategories.contains(RegulationCategory.EXON)) {
                collectExonsFromTargetGenes();
            }
            if (chosenCategories.contains(RegulationCategory.VIEWPOINT)) {
                collectViewPointsFromHiCPanel();
            }
        } catch (IOException e) {
            String msg = String.format("Could not input regulatory elements: %s",e.getMessage());
            status.add(msg);
            throw new GopherException(msg);
        }
        logger.trace(String.format("We got %d regulatory elements",regulatoryElementSet.size()));
        return null;
    }



    /** Update the progress bar of the GUI in a separate thread.
     * @param pr Current progress.
     */
    private void updateProgress(double pr) {
        if (progressInd ==null) {
            return; // do nothing
        }
        javafx.application.Platform.runLater( ()-> progressInd.setProgress(pr) );
    }

    /** This information will be used in the "report" dialog that gives the User feedbqck about the data and the chosen
     * viewpoints
     * @return key-value property pairs with information about the regulatory panel.
     */
    public Properties getRegulatoryReport() {
        Properties props = new Properties();
        props.setProperty("Regulatory exome downstream distance threshold", String.valueOf(downstreamThreshold));
        props.setProperty("Regulatory exome upstream distance threshold", String.valueOf(upstreamThreshold));
        props.setProperty("path to Ensembl regulatory build",pathToEnsemblRegulatoryBuild);
        props.setProperty("total regulatory elements", String.valueOf(totalRegulatoryElements));
        props.setProperty("chosen regulatory elements",String.valueOf(chosenRegulatoryElements));
        props.setProperty("total exons", String.valueOf(totalExons));
        int totalUniqueExons = this.regulatoryElementSet.size()-chosenRegulatoryElements;
        props.setProperty("total chosen exons ", String.valueOf(totalUniqueExons));
        return props;
    }

}
