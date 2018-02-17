package vpvgui.model;


import org.apache.log4j.Logger;
import vpvgui.gui.popupdialog.PopupFactory;
import vpvgui.model.genome.Genome;
import vpvgui.model.genome.HumanHg19;
import vpvgui.model.genome.HumanHg38;
import vpvgui.model.genome.MouseMm9;
import vpvgui.model.genome.MouseMm10;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class stores all of the data related to the project,including the list of  the viewpoint objects.
 * @author Peter Robinson
 * @author Hannah Blau
 * @version 0.2.16 (2018-02-18)
 */
public class Model implements Serializable {
    private static final Logger logger = Logger.getLogger(Model.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 5L;
    private static final String VERSION="0.3.2";
    private static final String LAST_CHANGE_DATE="2018-02-15";
    /** This is a list of all possible enzymes from which the user can choose one on more. */
    private List<RestrictionEnzyme> enzymelist=null;
    /** The enzymes chosen by the user for ViewPoint production. */
    private List<RestrictionEnzyme> chosenEnzymelist;
    /** List of {@link ViewPoint} objects created from the gene list chosen by the user. */
    private List<ViewPoint> viewpointList=null;
    /** List of all target genes chosen by the user. Note: One gene can have one or more ViewPoints (one for each transcription start site) .*/
    private List<VPVGene> geneList=null;

    public enum Approach {
        SIMPLE, EXTENDED, SIMPLE_WITH_MANUAL_REVISIONS, EXTENDED_WITH_MANUAL_REVISIONS,UNINITIALIZED;
        public String toString() {
            switch (this) {
                case SIMPLE:
                    return "simple";
                case SIMPLE_WITH_MANUAL_REVISIONS:
                    return "simple with manual revisions";
                case EXTENDED:
                    return "extended";
                case EXTENDED_WITH_MANUAL_REVISIONS:
                    return "extended with manual revisions";
                case UNINITIALIZED:
                default:
                    return "uninitialized";
            }
        }
    }

    private Approach approach=Approach.UNINITIALIZED;



    /** The number of unique gene symbols representing the genes in {@link #geneList} (note that {@link #geneList} has
     * one entry for each transcription start site and chromosomal location of the target genes entered by the user).
     */
    private int n_validGeneSymbols;
    /** Key:Name of a chromosome (or in general, of a contig); Value: length in nucleotides */
    private Map<String,Integer> contigLengths;
    /** Proxy (null if not needed/not set) */
    private String httpProxy=null;
    /** Proxy port (null if not set). Note we store this as a String,but it has been validated as an Integer. */
    private String httpPort=null;
    /** the name of the viewpoint that will be used to write the settings file (default: vpvgui). */
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
    }

    /** A list of the analysis results for the regulatory exome. */

    private Properties regulatoryExomeProperties=null;

    public String getRegulatoryBuildPath() { return regulatoryBuildPath; }
    public void setRegulatoryBuildPath(String regulatoryBuildPath) {
        logger.trace("Setting reg buiold path in model to " + regulatoryBuildPath);
        this.regulatoryBuildPath = regulatoryBuildPath; }

    /** @return true if the genome files have been previously downloaded to the indicated path. */
    public boolean checkDownloadComplete(String path) {
        return this.genome.checkDownloadComplete(path);
    }
    /** @return The genome build chosen by the user, e.g., hg19, GRCh38, mm10  */
    public String getGenomeBuild() { return genome.getGenomeBuild(); }
    /** @param newDatabase The genome build chosen by the user, e.g., hg19, GRCh38, mm10  */
    public void setGenomeBuild(String newDatabase) {
        if (newDatabase.equals("hg19")) {
            this.genome = new HumanHg19();
        } else if (newDatabase.equals("hg38")) {
            this.genome = new HumanHg38();
        } else if (newDatabase.equals("mm9")) {
            this.genome = new MouseMm9();
        } else if (newDatabase.equals("mm10")) {
            this.genome = new MouseMm10();
        } else {
            PopupFactory.displayError("setGenomeBuild error",String.format("genome build %s not implemented",newDatabase));
            return;
        }
    }

    public Genome getGenome() { return this.genome; }

    public void setApproach (String s) {
        if (s.equalsIgnoreCase("simple")) this.approach = Approach.SIMPLE;
        else if (s.equalsIgnoreCase("extended")) this.approach = Approach.EXTENDED;
        else {
            logger.error(String.format("Malformed approach string %s",s));
        }
    }

    public boolean useSimpleApproach() {
        return approach==Approach.SIMPLE || approach==Approach.SIMPLE_WITH_MANUAL_REVISIONS;
    }

    public boolean useExtendedApproach() {
        return approach==Approach.EXTENDED || approach==Approach.EXTENDED_WITH_MANUAL_REVISIONS;
    }

    public Approach getApproach() {
        return approach;
    }

    /** This integer property is declared transient because properties cannot be serialized. We keep it in synch with
     * a corresponding normal integer variable that can be
     */
    private int sizeUp;
    public final int getSizeUp() { return sizeUp;}
    public final void setSizeUp(int su) {  sizeUp=su;}

    private int sizeDown;
    public final int getSizeDown() { return sizeDown;}
    public final void setSizeDown(int sd) { sizeDown=sd;}

    /** Minimum allowable size of a restriction fragment within a ViewPoint chosen for capture Hi C enrichment. */
    private int minFragSize;
    public int getMinFragSize() { return minFragSize; }
    public void setMinFragSize(int i) { this.minFragSize=i;}

    /** Maximum allowable repeat content in the margin of a selected fragment. */
    private double maxRepeatContent;
    public  double getMaxRepeatContent() {return maxRepeatContent;}
    public  void setMaxRepeatContent(double r) { this.maxRepeatContent=r;}
    public double getMaxRepeatContentPercent(){return 100*maxRepeatContent; }
    /** Minimum allowable GC content in a selected fragment. */
    private double minGCcontent;
    public  double getMinGCcontent() { return minGCcontent;}
    public  void setMinGCcontent(double mgc) { minGCcontent=mgc;}
    public double getMinGCContentPercent() { return 100*minGCcontent; }
    /** Maximum allowable GC content in a selected fragment. */
    private double maxGCcontent;
    public  double getMaxGCcontent() { return maxGCcontent;}
    public  void setMaxGCcontent(double mgc) { maxGCcontent=mgc;}
    public double getMaxGCContentPercent() { return 100*maxGCcontent; }
    /** Should we allow Fragments to be chosen if only one of the two margins satisfies GC and repeat criteria? */
    private boolean allowSingleMargin=Default.ALLOW_SINGLE_MARGIN; // true
    public boolean getAllowSingleMargin() { return allowSingleMargin; }
    public void setAllowSingleMargin(boolean b) { allowSingleMargin=b; }


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

    public void setUniqueTSScount(int n) { this.uniqueTSScount = n; }

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
    }

    private int uniqueChosenTSScount;

    public int getChosenGeneCount() {
        return chosenGeneCount;
    }

    public void setChosenGeneCount(int chosenGeneCount) {
        logger.trace(String.format("Setting chosen gene count to %d",chosenGeneCount));
        this.chosenGeneCount = chosenGeneCount;
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
    }

    /** Total number of RefGenes in the UCSC file. */
    private int totalRefGeneCount;


    /** The complete path to the refGene.txt.gz transcript file on the user's computer. */
    private String refGenePath=null;
    /** The length of a probe that will be used to enrich a restriction fragment within a viewpoint. */
    private int probeLength=Default.PROBE_LENGTH;
    public int getProbeLength() { return probeLength; }
    public void setProbeLength(Integer probeLength) {this.probeLength=probeLength; }
    /** This is the number of times we will tile the target regions with probes. */
    private Integer tilingFactor =Default.TILING_FACTOR;
    public int getTilingFactor(){return tilingFactor; }
    public void setTilingFactor(Integer tilingFactor) {
        logger.trace(String.format("Setting tiling factor to %d",tilingFactor));
        this.tilingFactor=tilingFactor;
    }

    private Integer marginSize =Default.MARGIN_SIZE;
    public int getMarginSize(){return marginSize;}
    public void setMarginSize(Integer s) {this.marginSize=s; }

    //private Map<String, String> indexedFaFiles=null;
    /** Path to the genome fai file, e.g., hg19.fa.fai. */
    private String indexedGenomeFastaIndexFile=null;
    public void setIndexedGenomeFastaIndexFile(String path) { indexedGenomeFastaIndexFile=path;}
    public String getIndexedGenomeFastaIndexFile() { return indexedGenomeFastaIndexFile; }

    public List<VPVGene> getVPVGeneList() { return this.geneList; }

    public boolean isGenomeUnpacked() { return this.genome.isUnpackingComplete(); }
    public boolean isGenomeIndexed() { return this.genome.isIndexingComplete(); }
    public void setGenomeUnpacked() { this.genome.setGenomeUnpacked(true);}
    public void setGenomeIndexed() { this.genome.setGenomeIndexed(true);}

    public void setContigLengths(Map<String,Integer> contigLens) { this.contigLengths=contigLens;}


    public String getGenomeBasename() { return this.genome.getGenomeBasename(); }
    public void setTargetGenesPath(String path){this.targetGenesPath=path; }
    public String getTargetGenesPath() { return this.targetGenesPath; }

    private String transcriptsBasename = null;
    public String getTranscriptsBasename() {
        return transcriptsBasename;
    }

    public int getN_validGeneSymbols() { return n_validGeneSymbols; }
    public void setN_validGeneSymbols(int n_validGeneSymbols) { this.n_validGeneSymbols = n_validGeneSymbols; }

    public Model() {
        this.genome=new HumanHg19(); /* the default genome */
        this.chosenEnzymelist=new ArrayList<>(); /* empty list for enzymes that have been chosen by user */
        initializeEnzymesFromFile();
    }


    /** @return List of enzymes for the user to choose from. */
    public List<RestrictionEnzyme> getRestrictionEnymes() {
        return enzymelist;
    }

    public void setGenomeDirectoryPath(String p) { this.genome.setPathToGenomeDirectory(p); }
    public void setGenomeDirectoryPath(File f) { this.genome.setPathToGenomeDirectory(f.getAbsolutePath());}
    public String getGenomeDirectoryPath() {
        return this.genome.getPathToGenomeDirectory();
    }

    public String getGenomeFastaFile() {
        String dir = getGenomeDirectoryPath();
        String genomeFa =this.genome.getGenomeFastaName();
        return  dir + File.separator + genomeFa;
    }



    /**
     * This method expects there to be a file called enzymelist.tab in
     * src/main/resources. This file has a header line (with #) and
     * then a list of restriction enzymes structured as name\tsite
     */
    private void initializeEnzymesFromFile() {
        enzymelist = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/enzymelist.tab")));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; /* skip header*/
                String a[] = line.split("\\s+");
                RestrictionEnzyme re = new RestrictionEnzyme(a[0], a[1]);
                enzymelist.add(re);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setVPVGenes(List<VPVGene> vpvgenelist) {
        this.geneList = vpvgenelist;
    }

    public void debugPrintVPVGenes() {
        if (this.geneList==null || this.geneList.size()==0) {
            System.err.println("No VPV Genes in Model!");
            return;
        }
        System.err.println("VPV Genes in Model:");
        for (VPVGene vg : geneList) {
            System.err.println(vg);
        }
    }
    /** @return list of all {@link ViewPoint} objects to be displayed. */
    public List<ViewPoint> getViewPointList() {
        return this.viewpointList;
    }

    /** @return true if we have at least one VPVGene (which contain ViewPoints). */
    public boolean viewpointsInitialized() {
        return (this.viewpointList!=null && this.viewpointList.size()>0);
    }


    public void setViewPoints(List<ViewPoint> viewpointlist) {
        logger.trace("setViewPoints: viewpointlist with size="+viewpointlist.size());
        this.viewpointList=viewpointlist;
    }
    /** @return the plain cutting site (no caret symbol) of the first enyzme chosen. */
    public String getFirstRestrictionEnzymeString() {
        if (chosenEnzymelist==null || chosenEnzymelist.size()<1) return "none";
       else return chosenEnzymelist.get(0).getPlainSite();
    }
    /** @return all selected enzymes as semicolon-separated string. */
    public String getAllSelectedEnzymeString() {
        if (chosenEnzymelist==null || chosenEnzymelist.size()<1) return "none";
        return chosenEnzymelist.stream().map(re-> re.getPlainSite()).collect(Collectors.joining(";"));
    }

    public void setChosenRestrictionEnzymes(List<RestrictionEnzyme> chosenEnzymes) {
        this.chosenEnzymelist = chosenEnzymes;
    }

    public List<RestrictionEnzyme> getChosenEnzymelist(){return this.chosenEnzymelist; }

    public void setHttpProxyPort(String port) {this.httpPort=port; }
    public String getHttpProxyPort() { return this.httpPort; }
    public String getHttpProxy() { return this.httpProxy; }
    public void setHttpProxy(String proxy) {this.httpProxy=proxy; }
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
        for (VPVGene g:geneList) {
            n+=g.n_viewpointstarts();
        }
        return n;
    }


    public void setRefGenePath(String p) { refGenePath=p; }
    public String getRefGenePath() { return this.refGenePath; }

    public void setProjectName(String name) { this.projectName=name;}
    public String getProjectName() { return this.projectName; }


    /**
     * See the static version of this function.
     * @return Properties object for this Model.
     */
    public Properties getProperties() {
        return getProperties(this);
    }

    /** Collect all of the important attributes of the {@link Model} object and
     * place them into a Properties object (intended to write or show the settings).
     * @param model The model with all of the properties of the analysis
     * @return Properties of the model (intended for display)
     */
    private static Properties getProperties(Model model) {
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

    public String getVersion() {
        return VERSION;
    }

    /** Remove a ViewPoint from the list {@link #viewpointList}. */
    public void deleteViewpoint(ViewPoint vp) {
        Iterator<ViewPoint> it = viewpointList.listIterator();
        while (it.hasNext()) {
            ViewPoint vpit = it.next();
            if (vpit.equals(vp)) {
                it.remove();
                break;
            }
        }
    }

    /**
     * Note that this function will return an empty list if the viewpoint list is null, which can occur if the
     * user has not created view points yet. This will be picked up as an error and reported to the user later on.
     * @return A list of Viewpoints that contain at least one selected fragment.
     * */
    public List<ViewPoint> getActiveViewPointList() {
        if (viewpointList==null ) return new ArrayList<>();
       return this.viewpointList.stream().filter(viewPoint -> viewPoint.hasValidProbe()).collect(Collectors.toList());
    }

}
