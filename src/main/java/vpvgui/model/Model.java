package vpvgui.model;


import org.apache.log4j.Logger;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.genome.Genome;
import vpvgui.model.genome.HumanHg19;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class stores all of the data related to the project,including the list of  the viewpoint objects.
 * @author Peter Robinson
 * @author Hannah Blau
 * @version 0.0.3 (2017-09-04)
 */
public class Model implements Serializable {
    private static final Logger logger = Logger.getLogger(Model.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 4L;

    private static final String VERSION="0.1.14";
    private static final String LAST_CHANGE_DATE="10/04/2017, 4:22 PM";



    /** This is the name of the file we download from UCSC for any of the genomes. */
    private static final String DEFAULT_GENOME_BASENAME = "chromFa.tar.gz";
    /** This is a list of all possible enzymes from which the user can choose one on more. */
    private List<RestrictionEnzyme> enzymelist=null;
    /** The enzymes chosen by the user for ViewPoint production. */
    private List<RestrictionEnzyme> chosenEnzymelist=null;
    /** List of {@link ViewPoint} objects created from the gene list chosen by the user. */
    private List<ViewPoint> viewpointList=null;
    /** List of all target genes chosen by the user. Note: One gene can have one or more ViewPoints (one for each transcription start site) .*/
    private List<VPVGene> geneList=null;
    /** Key:Name of a chromosome (or in general, of a contig). Value: length in nucleotides */
    private Map<String,Integer> contigLengths;
    /** Proxy (null if not needed/not set) */
    private String httpProxy=null;
    /** Proxy port (null if not set). Note we store this as a String,but it has been validated as an Integer. */
    private String httpPort=null;
    /** the name of the viewpoint that will be used to write the settings file (default: vpvgui). */
    private String projectName=null;
    /** Has the UCSC Genome build been unpacked yet? :*/
    //private boolean genomeUnpacked=false;
    /** Has the downloaded genome been FASTA indexed yet? */
    //private boolean genomeIndexed=false;
    /** Path to the file with the uploaded target genes. */
    private String targetGenesPath=null;
    /** An object to coordinate the genome build as well as the status of download, unpacking, and indexing. */
    private Genome genome;
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
        } else {
            ErrorWindow.display("setGenomeBuild error",String.format("genome build %s not implemented",newDatabase));
            return;
        }
    }

    public Genome getGenome() { return this.genome; }
    /** Minimum size of the view point upstream of the anchor (transcription start site, usually). */
    private Integer minSizeUp = null;
    public int getMinSizeUp() {return minSizeUp;}
    public void setMinSizeUp(Integer i) { this.minSizeUp=i;}
    /** Minimum size of the view point downstream of the anchor (transcription start site, usually). */
    private Integer minSizeDown=null;
    public int getMinSizeDown() {return minSizeDown;}
    public void setMinSizeDown(Integer i) { this.minSizeDown=i;}
    /** Maximum size of the view point upstream of the anchor (transcription start site, usually). */
    private Integer maxSizeUp=null;
    public int getMaxSizeUp() {return maxSizeUp;}
    public void setMaxSizeUp(Integer i) { this.maxSizeUp=i;}
    /** Maximum size of the view point downstream of the anchor (transcription start site, usually). */
    private Integer maxSizeDown = null;
    public int getMaxSizeDown() {return maxSizeDown;}
    public void setMaxSizeDown(Integer i) { this.maxSizeDown=i;}
    /** Minimum allowable size of a restriction fragment within a ViewPoint chosen for capture Hi C enrichment. */
    private Integer minFragSize = null;
    public int getMinFragSize() { return minFragSize; }
    public void setMinFragSize(Integer i) { this.minFragSize=i;}
    /** TODO exact definition. */
    private Integer fragNumUp = null;
    public int getFragNumUp() { return fragNumUp; }
    public void setFragNumUp(Integer i) { this.fragNumUp=i;}
    /** TODO exact definition. */
    private Integer fragNumDown = null;
    public int fragNumDown() { return fragNumDown; }
    public void setFragNumDown(Integer i) { this.fragNumDown=i;}
    /** Maximum allowable repeat content in the margin of a selected fragment. */
    private Double maxRepeatContent = null;
    public double getMaxRepeatContent() {return maxRepeatContent;}
    public void setMaxRepeatContent(double r) { this.maxRepeatContent=r;}
    /** The complete path to the refGene.txt.gz transcript file on the user's computer. */
    private String refGenePath=null;
    /** The length of a probe that will be used to enrich a restriction fragment within a viewpoint. */
    private Integer probeLength =null;
    public int getProbeLength() { return probeLength; }
    public void setProbeLength(Integer probeLength) {this.probeLength=probeLength; }

    private Integer tilingFactor =null;
    public int getTilingFactor(){return tilingFactor; }
    public void setTilingFactor(Integer tilingFactor) {
        logger.trace(String.format("Setting tiling factor to %d",tilingFactor));
        this.tilingFactor=tilingFactor;
    }
    public Integer maximumAllowedRepeatOverlap =null;
    public int getMaximumAllowedRepeatOverlap(){return maximumAllowedRepeatOverlap;}
    public void setMaximumAllowedRepeatOverlap(Integer maximumAllowedRepeatOverlap) {this.maximumAllowedRepeatOverlap=maximumAllowedRepeatOverlap; }

    public Integer marginSize =null;
    public int getMarginSize(){return marginSize;}
    public void setMarginSize(Integer s) {this.marginSize=s; }

    private Map<String, String> indexedFaFiles=null;

    public List<VPVGene> getVPVGeneList() { return this.geneList; }

    public boolean isGenomeUnpacked() { return this.genome.isUnpackingComplete(); }
    public boolean isGenomeIndexed() { return this.genome.isIndexingComplete(); }
    public void setGenomeUnpacked() { this.genome.setGenomeUnpacked(true);}
    public void setGenomeIndexed() { this.genome.setGenomeIndexed(true);}

    public void setContigLengths(Map<String,Integer> contigLens) { this.contigLengths=contigLens;}




//    public String getGenomeURL() { return this.genome.getGenomeURL(); }

    public String genomeBasename = DEFAULT_GENOME_BASENAME;
    /** The complete URL of the chosen transcript definition from UCSC. */
    public String transcriptsURL = null;


    public String getGenomeBasename() {
        return this.genomeBasename;
    }
    public void setTargetGenesPath(String path){this.targetGenesPath=path; }
    public String getTargetGenesPath() { return this.targetGenesPath; }

    /** @return array of enzyme cutting sites. */
    public String[] getCuttingPatterns() {
        int n = this.enzymelist.size();
        String patterns[]=new String[n];
        for (int i=0;i<n;i++) {
            RestrictionEnzyme re = enzymelist.get(i);
            patterns[i]=re.getSite();
        }
        return patterns;
    }


    public String getTranscriptsURL() {
        return transcriptsURL;
    }
    public void setTranscriptsURL(String url) {this.transcriptsURL=url; }

    public String transcriptsBasename = null;
    public String getTranscriptsBasename() {
        return transcriptsBasename;
    }



    public Model() {
        this.genome=new HumanHg19(); /* the default genome */
        initializeEnzymesFromFile();
    }



    /** This method should be called when we create a new Model and want to use default settings.
     */
    public void setDefaultValues() {
        setFragNumUp(Default.NUMBER_OF_FRAGMENTS_UPSTREAM);
        setFragNumDown(Default.NUMBER_OF_FRAGMENTS_DOWNSTREAM);
        setMinSizeUp(Default.MINIMUM_SIZE_UPSTREAM);
        setMinSizeDown(Default.MINIMUM_SIZE_DOWNSTREAM);
        setMaxSizeUp(Default.MAXIMUM_SIZE_UPSTREAM);
        setMaxSizeDown(Default.MAXIMUM_SIZE_DOWNSTREAM);
        setMinFragSize(Default.MINIMUM_FRAGMENT_SIZE);
        setMaxRepeatContent(Default.MAXIMUM_REPEAT_CONTENT);
        setProbeLength(Default.PROBE_LENGTH);
        setTilingFactor(Default.TILING_FACTOR);
        setMaximumAllowedRepeatOverlap(Default.MAXIMUM_ALLOWED_REPEAT_OVERLAP);
        setMarginSize(Default.MARGIN_SIZE);
        setGenomeBuild(Default.GENOME_BUILD);
    }








    /**
     * @return List of enzymes for the user to choose from.
     */
    public List<RestrictionEnzyme> getRestrictionEnymes() {
        return enzymelist;
    }

    public void setRestrictionEnzymes(List<RestrictionEnzyme> lst){
        this.enzymelist=lst;
    }

    public void setGenomeDirectoryPath(String p) { this.genome.setPathToGenomeDirectory(p); }
    public void setGenomeDirectoryPath(File f) { this.genome.setPathToGenomeDirectory(f.getAbsolutePath());}
    public String getGenomeDirectoryPath() {
        return this.genome.getPathToGenomeDirectory();
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
                //System.err.println(line);
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


    public void setIndexedFastaFiles(Map<String, String> indexedFa) {
        this.indexedFaFiles=indexedFa;
    }

    public String getIndexFastaFilePath(String contigname) {
        logger.debug("Will attempt to retrieve indexed Fasta file for contig: \""+contigname+"\"");
        if (this.indexedFaFiles==null){
            logger.error("indexFaFiles is NULL");
            return null;
        }
        if (! this.indexedFaFiles.containsKey(contigname)) {
            logger.error("Coud not find indexed fasta file for contig: "+contigname);
            logger.error("Size of indexFaFiles: "+indexedFaFiles.size());
            for (String s:indexedFaFiles.keySet()) {
                logger.debug("indexedFa files: "+s+">"+indexedFaFiles.get(s));
            }
            return null;
        } else {
            logger.info("Found indexed fasta file for contig: "+contigname);
            return this.indexedFaFiles.get(contigname);
        }
    }

    public void setViewPoints(List<ViewPoint> viewpointlist) {
        logger.trace("setViewPoints: viewpointlist with size="+viewpointlist.size());
        this.viewpointList=viewpointlist;
    }

    public String getRestrictionEnzymeString() {
        if (chosenEnzymelist==null) return "null";
        StringBuilder sb = new StringBuilder();
        boolean morethanone=false;
        for (RestrictionEnzyme re:chosenEnzymelist) {
            if (morethanone) sb.append("; ");
            sb.append(re.getName());
            morethanone=true;
        }
        return sb.toString();
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
     * @param model
     * @return
     */
    public static Properties getProperties(Model model) {
        Properties properties = new Properties();
        properties.setProperty("project_name", model.getProjectName());
        properties.setProperty("genome_build",model.getGenomeBuild());
        String genomePath=model.getGenomeDirectoryPath()!=null?model.getGenomeDirectoryPath():"null";
        properties.setProperty("path_to_downloaded_genome_directory",genomePath);
        String unpacked=model.isGenomeUnpacked()?"true":"false";
        properties.setProperty("genome_unpacked",unpacked);
        String indexed=model.isGenomeIndexed()?"true":"false";
        properties.setProperty("genome_indexed",indexed);
        String transcURL=model.getTranscriptsURL()!=null?model.getTranscriptsURL():"null";
        properties.setProperty("transcript_url",transcURL);
        String refgenep=model.getRefGenePath()!=null?model.getRefGenePath():"null";
        properties.setProperty("refgene_path",refgenep);
        properties.setProperty("restriction_enzymes",model.getRestrictionEnzymeString());
        String tgpath=model.getTargetGenesPath()!=null?model.getTargetGenesPath():"null";
        properties.setProperty("target_genes_path",tgpath);
        properties.setProperty("getFragNumUp",String.format("%d",model.getFragNumUp()));
        properties.setProperty("fragNumDown",String.format("%d",model.fragNumDown()));
        properties.setProperty("getMinSizeUp",String.format("%d",model.getMinSizeUp()));
        properties.setProperty("getMinSizeDown",String.format("%d",model.getMinSizeDown()));
        properties.setProperty("getMaxSizeUp",String.format("%d",model.getMaxSizeUp()));
        properties.setProperty("getMaxSizeDown",String.format("%d",model.getMaxSizeDown()));
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
}
