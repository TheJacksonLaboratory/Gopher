package gopher.service.model;


import com.google.common.collect.ImmutableList;
import gopher.gui.factories.PopupFactory;
import gopher.io.RestrictionEnzymeParser;
import gopher.service.model.genome.*;
import gopher.service.model.viewpoint.ViewPoint;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * This class stores all of the data related to the project,including the list of the viewpoint objects.
 * @author Peter Robinson
 * @author Hannah Blau
 * @version 0.2.16 (2018-02-18)
 */
@Component
public class GopherModel implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(GopherModel.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 6L;
    private static final String VERSION="0.7.1";
    private static final String LAST_CHANGE_DATE="2021-11-28";

    /** This is a list of all possible enzymes from which the user can choose one on more. */
    private List<RestrictionEnzyme> enzymelist=null;
    /** The enzymes chosen by the user for ViewPoint production. */
    private List<RestrictionEnzyme> chosenEnzymelist;
    /** List of {@link ViewPoint} objects created from the gene list chosen by the user. */
    private List<ViewPoint> viewpointList=null;
    /** List of all target genes chosen by the user. Note: One gene can have one or more ViewPoints (one for each transcription start site) .*/
    private List<GopherGene> geneList=null;
    /** This variable is set to false if something was changed in the GUI that the user might want to save.
     * We initialize it to true so that we have a fresh slate at the start of each session. */
    private transient boolean clean=true;



    NormalDistribution simpleNd;
    NormalDistribution extendedNdUp;
    NormalDistribution extendedNdDown;

    /** TARGET_GENES: a gene panel; ALL_GENES: promoterome; BED_TARGETS: custom. NONE: uninitialized. */
    public enum TargetType {
        NONE,TARGET_GENES,ALL_GENES,BED_TARGETS
    }
    /** This variable records the kind of gene/target set we are analyzing. */
    private TargetType targetType;


    private Approach approach=Approach.UNINITIALIZED;



    /** The number of unique gene symbols representing the genes in {@link #geneList} (note that {@link #geneList} has
     * one entry for each transcription start site and chromosomal location of the target genes entered by the user).
     */
    private int n_validGeneSymbols;
    /** Proxy (null if not needed/not set) */
    private String httpProxy=null;
    /** Proxy port (null if not set). Note we store this as a String,but it has been validated as an Integer. */
    private String httpPort=null;
    /** the name of the viewpoint that will be used to write the settings file (default: gopher). */
    private String projectName=null;
    /** Path to the file with the uploaded target genes. */
    private String targetGenesPath=null;
    /** An object to coordinate the genome build as well as the status of download, unpacking, and indexing. */
    private Genome genome;
    /** Path on local file system of the downloaded regulatory build file from Ensembl. */
    private String regulatoryBuildPath=null;

    public Properties getRegulatoryExomeProperties() {
        return regulatoryExomeProperties;
    }

    public void setRegulatoryExomeProperties(Properties regulatoryExomeProperties) {
        this.regulatoryExomeProperties = regulatoryExomeProperties;
        clean=false;
    }

    /** A list of the analysis results for the regulatory exome. */

    private Properties regulatoryExomeProperties=null;

    public String getRegulatoryBuildPath() { return regulatoryBuildPath; }
    public void setRegulatoryBuildPath(String regulatoryBuildPath) {
        this.regulatoryBuildPath = regulatoryBuildPath;
        clean=false;
    }
    public boolean regulatoryBuildPathInitialized(){ return regulatoryBuildPath!=null;}

    /** @return true if the genome files have been previously downloaded to the indicated path. */
    public boolean checkDownloadComplete(String path) {
        return this.genome.checkDownloadComplete(path);
    }
    /** @return The genome build chosen by the user, e.g., hg19, GRCh38, mm10  */
    public String getGenomeBuild() { return genome.getGenomeBuild(); }
    /** @param newDatabase The genome build chosen by the user, e.g., hg19, GRCh38, mm10  */
    public void setGenomeBuild(String newDatabase) {
        switch (newDatabase) {
            case "hg19" -> this.genome = new HumanHg19();
            case "hg38" -> this.genome = new HumanHg38();
            case "mm9" -> this.genome = new MouseMm9();
            case "mm10" -> this.genome = new MouseMm10();
            case "xenTro9" -> this.genome = new FrogXenTro9();
            case "danRer10" -> this.genome = new FishDanRer10();
            default -> PopupFactory.displayError("setGenomeBuild error", String.format("genome build %s not implemented", newDatabase));
        }
        clean=false;
    }

    public Genome getGenome() { return this.genome; }

    public void setApproach (String s) {
        if (s.equalsIgnoreCase("simple")) this.approach = Approach.SIMPLE;
        else if (s.equalsIgnoreCase("extended")) this.approach = Approach.EXTENDED;
        else {
            logger.error(String.format("Malformed approach string %s",s));
        }
        clean=false;
    }

    /** Set the variable clean (which we use to keep track of changes to the model that the user might want to save)*/
    public void setClean(boolean b){
        this.clean=b;
    }
    /** Is the model clean, i.e., it does not have unsaved changes? */
    public boolean isClean(){
        return clean;
    }

    public boolean useSimpleApproach() {
        return approach==Approach.SIMPLE;
    }

    public boolean useExtendedApproach() {
        return approach==Approach.EXTENDED;
    }

    public Approach getApproach() {
        return approach;
    }

    public void setNormalDistributionSimple(double meanDigestSize) {
        double mean = 0; // corresponds to TSS position
        double sd = meanDigestSize/6; // chosen by eye
        this.simpleNd = new NormalDistribution(mean,sd);
    }
    public NormalDistribution getNormalDistributionSimple() {
        return simpleNd;
    }

    public void setNormalDistributionsExtended() {
        double mean = 0; // shifts the normal distribution, so that almost the entire area under the curve is to the left of the y-axis
        // three standard deviations cover 99.7% of the data
        double sd = (double)this.sizeUp/6; // the factor 1/6 was chosen by eye
        this.extendedNdUp = new NormalDistribution(mean,sd);
        sd = (double)this.sizeDown/6;
        this.extendedNdDown = new NormalDistribution(mean,sd);
    }
    public NormalDistribution getNormalDistributionExtendedUp() {
        return extendedNdUp;
    }
    public NormalDistribution getNormalDistributionExtendedDown() {
        return extendedNdDown;
    }


    /** This integer property is declared transient because properties cannot be serialized. We keep it in synch with
     * a corresponding normal integer variable that can be
     */
    private int sizeUp;
    public final int getSizeUp() { return sizeUp;}
    public final void setSizeUp(int su) {  sizeUp=su; clean=false;}

    private int sizeDown;
    public final int getSizeDown() { return sizeDown;}
    public final void setSizeDown(int sd) { sizeDown=sd; clean=false;}

    /** Minimum allowable size of a restriction digest within a ViewPoint chosen for capture Hi C enrichment. */
    private int minFragSize;
    public int getMinFragSize() { return minFragSize; }
    public void setMinFragSize(int i) { this.minFragSize=i; clean=false;}

    /** Maximum allowable repeat content in the margin of a selected digest. */
    // TODO -- this is being replaced by the alignability score
    private double maxRepeatContent;
    public  double getMaxRepeatContent() {return maxRepeatContent;}
    public  void setMaxRepeatContent(double r) { this.maxRepeatContent=r; clean=false;}
    public double getMaxRepeatContentPercent(){return 100*maxRepeatContent; }

    /** Maximum allowable mean kmer alignability score of a margin. */
    private int maxMeanKmerAlignability;
    public int getMaxMeanKmerAlignability() {return this.maxMeanKmerAlignability;}
    public void setMaxMeanKmerAlignability(int mmka) { this.maxMeanKmerAlignability=mmka; clean=false;}

    /** Minimum allowable GC content in a selected digest. */
    private double minGCcontent;
    public  double getMinGCcontent() { return minGCcontent;}
    public  void setMinGCcontent(double mgc) { minGCcontent=mgc; clean=false;}
    public double getMinGCContentPercent() { return 100*minGCcontent; }
    /** Maximum allowable GC content in a selected digest. */
    private double maxGCcontent;
    public  double getMaxGCcontent() { return maxGCcontent;}
    public  void setMaxGCcontent(double mgc) { maxGCcontent=mgc; clean=false;}
    public double getMaxGCContentPercent() { return 100*maxGCcontent; }
    /** Should we allow Fragments to be chosen if only one of the two margins satisfies GC and repeat criteria? */

    private boolean allowUnbalancedMargins = Default.ALLOW_UNBALANCED_MARGINs; // true
    public boolean getAllowUnbalancedMargins() { return allowUnbalancedMargins; }
    public void setAllowUnbalancedMargins(boolean b) { allowUnbalancedMargins =b; clean=false;}

    private boolean allowPatching = Default.ALLOW_PATCHING; // false
    public boolean getAllowPatching() { return this.allowPatching; }
    public void setAllowPatching(boolean b) { this.allowPatching=b; clean=false;}

    /** Minimum number of baits (probes) per valid margin */
    private int minBaitCount;
    public int getMinBaitCount(){return minBaitCount;}
    public void setMinBaitCount(int bc) { this.minBaitCount=bc; clean=false;}
    /** Maximum number of baits (probes) per valid margin */
    private int maxBaitCount;
    public int getMaxBaitCount(){return maxBaitCount;}
    public void setMaxBaitCount(int bc) { this.maxBaitCount=bc; clean=false;}

    /** Estimated average length of restriction fragments */
    private Double estAvgRestFragLen = null;
    public void setEstAvgRestFragLen(Double estAvgRestFragLen) {
        this.estAvgRestFragLen = estAvgRestFragLen;
        clean=false;
    }
    public Double getEstAvgRestFragLen() {
        return this.estAvgRestFragLen;
    }

    /** Total horizontal dimension  of the user's screen. */
    private int xdim;
    /** Return the current X dimension of the user's screen, but a minimum of 1000 -- this will be the width of the UCSC image shown. */
    public int getXdim() { return Math.max(1000,xdim); }
    public void setXdim(int xdim) {this.xdim = xdim; }
    public int getYdim() { return ydim; }
    public void setYdim(int ydim) { this.ydim = ydim; }

    /** Total vertical dimension the user's screen. */
    private int ydim;

    public int getUniqueTSScount() { return uniqueTSScount; }

    public void setUniqueTSScount(int n) { this.uniqueTSScount = n; clean=false; }

    /** Total number of Transcription start sites associated with the genes chosen by the user. Viewpoints will be chosen
     * from these start sites.
     */
    private int uniqueTSScount;

    public int getUniqueChosenTSScount() {
        return uniqueChosenTSScount;
    }

    public void setUniqueChosenTSScount(int uniqueChosenTSScount) {
        logger.trace(String.format("Setting chosen TSS count to %d",uniqueChosenTSScount));
        this.uniqueChosenTSScount = uniqueChosenTSScount;
        clean=false;
    }

    private int uniqueChosenTSScount;

    public int getChosenGeneCount() {
        return chosenGeneCount;
    }

    public void setChosenGeneCount(int chosenGeneCount) {
        logger.trace(String.format("Setting chosen gene count to %d",chosenGeneCount));
        this.chosenGeneCount = chosenGeneCount;
        clean=false;
    }

    /** Number of genes initialially chosen by user. Can be different from final number of genes if no valid viewpoints
     * are found for a gene.

     */
    private int chosenGeneCount;

    public int getTotalRefGeneCount() {
        return totalRefGeneCount;
    }

    public void setTotalRefGeneCount(int totalRefGeneCount) {
        this.totalRefGeneCount = totalRefGeneCount;
        clean=false;
    }

    /** Total number of RefGenes in the UCSC file. */
    private int totalRefGeneCount;


    /** The complete path to the refGene.txt.gz transcript file on the user's computer. */
    private String refGenePath=null;
    private String alignabilityMapPathIncludingFileNameGz = null;
    private String chromInfoPathIncludingFileNameGz = null;

    /** The length of a probe that will be used to enrich a restriction digest within a viewpoint. */
    private int probeLength=Default.BAIT_LENGTH;
    public int getProbeLength() { return probeLength; }
    public void setProbeLength(Integer probeLength) {this.probeLength=probeLength; clean=false;}


    private Integer marginSize =Default.MARGIN_SIZE;
    public int getMarginSize(){return marginSize;}
    public void setMarginSize(Integer s) {this.marginSize=s; clean=false;}

    //private Map<String, String> indexedFaFiles=null;
    /** Path to the genome fai file, e.g., hg19.fa.fai. */
    private String indexedGenomeFastaIndexFile=null;
    public void setIndexedGenomeFastaIndexFile(String path) { indexedGenomeFastaIndexFile=path; clean=false;}
    public String getIndexedGenomeFastaIndexFile() { return indexedGenomeFastaIndexFile; }

    public List<GopherGene> getGopherGeneList() { return this.geneList; }

    public boolean isGenomeUnpacked() { return this.genome.isUnpackingComplete(); }
    public boolean isGenomeIndexed() { return this.genome.isIndexingComplete(); }

    public void setGenomeUnpacked() { this.genome.setGenomeUnpacked(true); clean=false;}
    public void setGenomeIndexed() { this.genome.setGenomeIndexed(true); clean=false;}

    public String getGenomeBasename() { return this.genome.getGenomeBasename(); }
    public void setTargetGenesPath(String path){this.targetGenesPath=path; clean=false;}
    public String getTargetGenesPath() { return this.targetGenesPath; }

    private String transcriptsBasename = null;
    public String getTranscriptsBasename() {
        return transcriptsBasename;
    }
    public void setTranscriptsBasename(String bname) {
        this.transcriptsBasename=bname;clean=false;
    }

    public int getN_validGeneSymbols() { return n_validGeneSymbols; }
    public void setN_validGeneSymbols(int n_validGeneSymbols) { this.n_validGeneSymbols = n_validGeneSymbols; clean=false;}

    public GopherModel() {
        this.genome=new HumanHg19(); /* the default genome */
        this.chosenEnzymelist=new ArrayList<>(); /* empty list for enzymes that have been chosen by user */
        try {
            this.enzymelist = RestrictionEnzymeParser.getEnzymes(GopherModel.class.getResourceAsStream("/data/enzymelist.tab"));
        } catch (IOException e) {
            logger.warn("Unable to load restriction enzymes from bundled '/enzymelist.tab' file");
        }
        this.allowUnbalancedMargins =false;
        this.allowPatching=false;
        this.targetType=TargetType.NONE;
    }

    public void setTargetType(TargetType ttype) { this.targetType=ttype; clean=false;}
    public TargetType getTargetType(){ return this.targetType; }


    /** @return List of enzymes for the user to choose from. */
    public List<RestrictionEnzyme> getRestrictionEnymes() {
        return enzymelist;
    }

    public void setGenomeDirectoryPath(String p) { this.genome.setPathToGenomeDirectory(p); clean=false;}
    public void setGenomeDirectoryPath(File f) { setGenomeDirectoryPath(f.getAbsolutePath()); }
    public String getGenomeDirectoryPath() {
        return this.genome.getPathToGenomeDirectory();
    }

    public String getGenomeFastaFile() {
        String dir = getGenomeDirectoryPath();
        String genomeFa =this.genome.getGenomeFastaName();
        return  dir + File.separator + genomeFa;
    }


    public void setGopherGenes(List<GopherGene> gopherGenelist) {
        this.geneList = gopherGenelist;
        this.clean=false;
    }

    public void debugPrintGopherGenes() {
        if (this.geneList==null || this.geneList.size()==0) {
            System.err.println("No GOPHER Genes in Model!");
            return;
        }
        System.err.println("GOPHER Genes in Model:");
        for (GopherGene vg : geneList) {
            System.err.println(vg);
        }
    }
    /** @return list of all {@link ViewPoint} objects to be displayed. */
    public List<ViewPoint> getViewPointList() {
        if (viewpointList==null) return ImmutableList.of();
        return this.viewpointList;
    }

    /** @return true if viewpoints have been set (not null). Note that we allow empty lists (which can occur with weird targets) */
    public boolean viewpointsInitialized() {
        return (this.viewpointList!=null);
    }


    public void setViewPoints(List<ViewPoint> viewpointlist) {
        logger.trace("setViewPoints: viewpointlist with size="+viewpointlist.size());
        this.viewpointList=viewpointlist;
        clean=false;
    }
    /** @return the plain cutting site (no caret symbol) of the first enyzme chosen. */
    public String getFirstRestrictionEnzymeString() {
        if (chosenEnzymelist==null || chosenEnzymelist.size()<1) return "none";
       else return chosenEnzymelist.get(0).getPlainSite();
    }
    /** @return all selected enzymes as semicolon-separated string. */
    public String getAllSelectedEnzymeString() {
        if (chosenEnzymelist==null || chosenEnzymelist.size()<1) return "none";
        return chosenEnzymelist.stream().map(RestrictionEnzyme::getPlainSite).collect(Collectors.joining(";"));
    }

    public void setChosenRestrictionEnzymes(List<RestrictionEnzyme> chosenEnzymes) {
        this.chosenEnzymelist = chosenEnzymes; clean=false;
    }

    public List<RestrictionEnzyme> getChosenEnzymelist(){return this.chosenEnzymelist; }

    public void setHttpProxyPort(String port) {this.httpPort=port; clean=false;}
    public String getHttpProxyPort() { return this.httpPort; }
    public String getHttpProxy() { return this.httpProxy; }
    public void setHttpProxy(String proxy) {this.httpProxy=proxy; clean=false; }
    public boolean needsProxy() { return (httpProxy!=null && httpPort!=null); }
    /** @return number of successfully parsed genes. */
    public int n_valid_genes() {
        if (this.geneList==null) return 0;
        else return this.geneList.size();
    }




    /** @return number of viewppoint starts (e.g., TSS) of the valid genes. */
    public int n_viewpointStarts() {
        if (this.geneList==null) return 0;
        int n=0;
        for (GopherGene g:geneList) {
            n+=g.n_viewpointstarts();
        }
        return n;
    }


    public void setRefGenePath(String p) { refGenePath=p;clean=false; }
    public String getRefGenePath() { return this.refGenePath; }

    public void setAlignabilityMapPathIncludingFileNameGz(String p) { alignabilityMapPathIncludingFileNameGz = p;clean=false; }
    public String getAlignabilityMapPathIncludingFileNameGz() { return this.alignabilityMapPathIncludingFileNameGz; }
    public boolean alignabilityMapPathIncludingFileNameGzExists() {
        return ( alignabilityMapPathIncludingFileNameGz != null &&
                (new File(alignabilityMapPathIncludingFileNameGz)).exists()
        );
    }

    public void setChromInfoPathIncludingFileNameGz(String p) { chromInfoPathIncludingFileNameGz = p; clean=false;}
    public String getChromInfoPathIncludingFileNameGz() { return this.chromInfoPathIncludingFileNameGz; }
    public boolean chromInfoPathIncludingFileNameGzExists() {
        return ( chromInfoPathIncludingFileNameGz != null &&
                (new File(chromInfoPathIncludingFileNameGz)).exists());
    }


    public void setProjectName(String name) { this.projectName=name; clean=false;}
    public String getProjectName() { return this.projectName; }


    /**
     * See the static version of this function.
     * @return Properties object for this Model.
     */
    public Properties getProperties() {
        return getProperties(this);
    }

    /** Collect all of the important attributes of the {@link GopherModel} object and
     * place them into a Properties object (intended to write or show the settings).
     * @param model The model with all of the properties of the analysis
     * @return Properties of the model (intended for display)
     */
    private static Properties getProperties(GopherModel model) {
        Properties properties = new Properties();
        properties.setProperty("project_name", model.getProjectName());
        properties.setProperty("genome_build",model.getGenomeBuild());
        String genomePath=model.getGenomeDirectoryPath()!=null?model.getGenomeDirectoryPath():"null";
        properties.setProperty("path_to_downloaded_genome_directory",genomePath);
        String unpacked=model.isGenomeUnpacked()?"true":"false";
        properties.setProperty("genome_unpacked",unpacked);
        String indexed=model.isGenomeIndexed()?"true":"false";
        properties.setProperty("genome_indexed",indexed);
        String refgenep=model.getRefGenePath()!=null?model.getRefGenePath():"null";
        properties.setProperty("refgene_path",refgenep);
        properties.setProperty("restriction_enzymes",model.getFirstRestrictionEnzymeString());
        String tgpath=model.getTargetGenesPath()!=null?model.getTargetGenesPath():"null";
        properties.setProperty("target_genes_path",tgpath);
        properties.setProperty("min GC content",String.format("%.2f",model.getMinGCcontent()));
        properties.setProperty("max GC content",String.format("%.2f",model.getMaxGCcontent()));
        properties.setProperty("SizeUp",String.format("%d",model.getSizeUp()));
        properties.setProperty("SizeDown",String.format("%d",model.getSizeDown()));
        properties.setProperty("getMinFragSize",String.format("%d",model.getMinFragSize()));
        properties.setProperty("maxRepeatContent",String.format("%f",model.getMaxRepeatContent()));
        return properties;
    }


    public String getLastChangeDate() {
        return LAST_CHANGE_DATE;
    }

    /** Remove a ViewPoint from the list {@link #viewpointList}. */
    public void deleteViewpoint(ViewPoint vp) {
        Iterator<ViewPoint> it = viewpointList.listIterator();
        while (it.hasNext()) {
            ViewPoint vpit = it.next();
            if (vpit.equals(vp)) {
                it.remove();
                clean=false;
                break;
            }
        }
    }

    /**
     * Note that this function will return an empty list if the viewpoint list is null, which can occur if the
     * user has not created view points yet. This will be picked up as an error and reported to the user later on.
     * @return A list of Viewpoints that contain at least one selected digest.
     * */
    public List<ViewPoint> getActiveViewPointList() {
        if (viewpointList==null ) return new ArrayList<>();
       return this.viewpointList.stream().filter(ViewPoint::hasValidDigest).collect(Collectors.toList());
    }

    /** @return the version number. */
    public static String getVersion() {
        return VERSION;
    }

}
