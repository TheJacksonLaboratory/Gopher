package vpvgui.model;


import org.apache.log4j.Logger;
import vpvgui.gui.popupdialog.PopupFactory;
import vpvgui.model.genome.Genome;
import vpvgui.model.genome.HumanHg19;
import vpvgui.model.genome.HumanHg38;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.*;
import java.util.*;

/**
 * This class stores all of the data related to the project,including the list of  the viewpoint objects.
 * @author Peter Robinson
 * @author Hannah Blau
 * @version 0.2.14 (2017-11-10)
 */
public class Model implements Serializable {
    private static final Logger logger = Logger.getLogger(Model.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 5L;
    private static final String VERSION="0.2.11";
    private static final String LAST_CHANGE_DATE="10/24/2017, 4:22 PM";
    /** This is a list of all possible enzymes from which the user can choose one on more. */
    private List<RestrictionEnzyme> enzymelist=null;
    /** The enzymes chosen by the user for ViewPoint production. */
    private List<RestrictionEnzyme> chosenEnzymelist=null;
    /** List of {@link ViewPoint} objects created from the gene list chosen by the user. */
    private List<ViewPoint> viewpointList=null;
    /** List of all target genes chosen by the user. Note: One gene can have one or more ViewPoints (one for each transcription start site) .*/
    private List<VPVGene> geneList=null;
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
        } else {
            PopupFactory.displayError("setGenomeBuild error",String.format("genome build %s not implemented",newDatabase));
            return;
        }
    }

    public Genome getGenome() { return this.genome; }

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
    /** Total horizontal dimension  of the user's screen. */
    private int xdim;

    public int getXdim() { return xdim; }
    public void setXdim(int xdim) {this.xdim = xdim; }
    public int getYdim() { return ydim; }
    public void setYdim(int ydim) { this.ydim = ydim; }

    /** Total vertical dimension the user's screen. */
    private int ydim;


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

    public Integer marginSize =Default.MARGIN_SIZE;
    public int getMarginSize(){return marginSize;}
    public void setMarginSize(Integer s) {this.marginSize=s; }

    private Map<String, String> indexedFaFiles=null;

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
        String refgenep=model.getRefGenePath()!=null?model.getRefGenePath():"null";
        properties.setProperty("refgene_path",refgenep);
        properties.setProperty("restriction_enzymes",model.getRestrictionEnzymeString());
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

}
