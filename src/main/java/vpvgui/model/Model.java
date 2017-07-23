package vpvgui.model;


import javafx.beans.property.*;
import org.apache.log4j.Logger;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import static vpvgui.io.Platform.getDefaultProjectName;
import static vpvgui.io.Platform.getVPVDir;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by peter on 05.05.17.
 * Modified by blauh on 06.12.17 to add Settings instance variable to Model constructor.
 */
public class Model {

    static Logger logger = Logger.getLogger(Model.class.getName());

    private DataSource datasource = null;
    /** This is a list of all possible enzymes from which the user can choose one on more. */
    private List<RestrictionEnzyme> enzymelist=null;
    /** The enzymes chosen by the user for ViewPoint production. */
    private List<RestrictionEnzyme> chosenEnzymelist=null;
    private List<ViewPoint> viewpointList=null;
    private List<VPVGene> geneList=null;
    /** Directory to which the Genome was downloaded */
    private String genomeDirectoryPath=null;
    /** Proxy (null if not needed/not set) */
    private String httpProxy=null;
    /** Proxy port (null if not set) */
    private Integer httpPort=null;
    /** the name of the project that will be used to write the settings file (default: vpvgui). */
    private String projectName="vpvgui";
    /** This suffix is appended to the project name to get the name of the file for storing project settings. */
    public static final String PROJECT_FILENAME_SUFFIX = "-vpvsettings.txt";
    /** HAs the UCSC Genome build been unpacked yet? :*/
    private boolean genomeUnpacked=false;
    /** Has the downloaded genome been FASTA indexed yet? */
    private boolean genomeIndexed=false;
    /** Path to the file with the uploaded target genes. */
    private String targetGenesPath=null;

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



    /**
     * The genome build chosen by theuser, e.g., hg19, GRCh38, mm10
     */
    private StringProperty genomeBuild = new SimpleStringProperty(this, "genomeBuild");
    public String getGenomeBuild() {
        return genomeBuild.getValue();
    }
    public void setGenomeBuild(String newDatabase) {
        genomeBuild.set(newDatabase);
    }
    public StringProperty genomeBuildProperty() {
        return genomeBuild;
    }
    /** Minimum size upstream of the view point. */
    final private IntegerProperty minSizeUpProperty = new SimpleIntegerProperty();
    public IntegerProperty minSizeUpProperty() { return minSizeUpProperty;  }
    public int minSizeUp() {return minSizeUpProperty.getValue();}
    public void setMinSizeUpProperty(Integer i) { this.minSizeUpProperty.setValue(i);}

    final private IntegerProperty minSizeDownProperty = new SimpleIntegerProperty();
    public IntegerProperty minSizeDownProperty() { return minSizeDownProperty;  }
    public int minSizeDown() {return minSizeDownProperty.getValue();}
    public void setMinSizeDownProperty(Integer i) { this.minSizeDownProperty.setValue(i);}

    final private IntegerProperty maxSizeUpProperty = new SimpleIntegerProperty();
    public IntegerProperty maxSizeUpProperty() { return maxSizeUpProperty;  }
    public int maxSizeUp() {return maxSizeUpProperty.getValue();}
    public void setMaxSizeUpProperty(Integer i) { this.maxSizeUpProperty.setValue(i);}

    final private IntegerProperty maxSizeDownProperty = new SimpleIntegerProperty();
    public IntegerProperty maxSizeDownProperty() { return maxSizeDownProperty;  }
    public int maxSizeDown() {return maxSizeDownProperty.getValue();}
    public void setMaxSizeDownProperty(Integer i) { this.maxSizeDownProperty.setValue(i);}

    final private IntegerProperty minFragSizeProperty = new SimpleIntegerProperty();
    public IntegerProperty minFragSizeProperty() { return minFragSizeProperty;  }
    public int minFragSize() { return minFragSizeProperty.getValue(); }
    public void setMinFragSizeProperty(Integer i) { this.minFragSizeProperty.setValue(i);}

    final private IntegerProperty fragNumUpProperty = new SimpleIntegerProperty();
    public IntegerProperty fragNumUpProperty() { return fragNumUpProperty;  }
    public int fragNumUp() { return fragNumUpProperty.getValue(); }
    public void setFragNumUpProperty(Integer i) { this.fragNumUpProperty.setValue(i);}

    final private IntegerProperty fragNumDownProperty = new SimpleIntegerProperty();
    public IntegerProperty fragNumDownProperty() { return fragNumDownProperty;  }
    public int fragNumDown() { return fragNumDownProperty.getValue(); }
    public void setFragNumDownProperty(Integer i) { this.fragNumDownProperty.setValue(i);}

    final private DoubleProperty maxRepeatContentProperty = new SimpleDoubleProperty();
    public DoubleProperty maxRepeatContentProperty() {return maxRepeatContentProperty; }
    public double maxRepeatContent() {return maxRepeatContentProperty.getValue();}
    public void setMaxRepeatContentProperty(double r) { this.maxRepeatContentProperty.setValue(r);}
    /** The complete path to the refGene.txt.gz transcript file on the user's computer. */
    private String refGenePath=null;

    private Map<String, String> indexedFaFiles=null;

    public List<VPVGene> getVPVGeneList() { return this.geneList; }

    public boolean isGenomeUnpacked() { return genomeUnpacked; }
    public boolean isGenomeIndexed() { return genomeIndexed; }
    public void setGenomeUnpacked() { this.genomeUnpacked=true;}
    public void setGenomeIndexed() { this.genomeIndexed=true;}


    public String genomeURL = null;

    public String getGenomeURL() {
        return this.genomeURL;
    }

    public String genomeBasename = null;
    /** The complete URLof the chosen transcript definition from UCSC. */
    public String transcriptsURL = null;


    public String getGenomeBasename() {
        return this.genomeBasename;
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
        initializeEnzymesFromFile();
        defaultInit();
    }

    private void defaultInit() {
        setProjectName(getDefaultProjectName());
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

    public void setGenomeDirectoryPath(String p) { this.genomeDirectoryPath=p;}
    public void setGenomeDirectoryPath(File f) { this.genomeDirectoryPath=f.getAbsolutePath();}
    public String getGenomeDirectoryPath() { return this.genomeDirectoryPath;}



    /**
     * This method expects there to be a file called enzymelist.tab in
     * src/main/resources. This file has a header line (with #) and
     * then a list of restriction enzymes structured as name\tsite
     */
    private void initializeEnzymesFromFile() {
        enzymelist = new ArrayList<>();
        String fileName = "enzymelist.tab";
        File file = new File(getClass().getClassLoader().getResource("enzymelist.tab").getFile());
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/enzymelist.tab")));
            String line;
            while ((line = br.readLine()) != null) {
                //System.err.println(line);
                if (line.startsWith("#"))
                    continue; /* skip header*/
                String a[] = line.split("\\s");
                RestrictionEnzyme re = new RestrictionEnzyme(a[0], a[1]);
                enzymelist.add(re);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method uses {@link DataSource} as a source for collections
     * of paths and names that represent the sets of data we will need
     * to analyze any one genome.
     */
    public void adjustGenomeDownloadPaths() throws DownloadFileNotFoundException {
        String gb = getGenomeBuild();
        if (gb.equals("hg19")) {
            this.datasource = DataSource.createUCSChg19();
        } else if (gb.equals("hg38")) {
            this.datasource = DataSource.createUCSChg38();
        } else if (gb.equals("mm9")) {
            this.datasource = DataSource.createUCSCmm9();
        } else if (gb.equals("mm10")) {
            this.datasource = DataSource.createUCSCmm10();
        } else {
            throw new DownloadFileNotFoundException(String.format("Need to implement code for genome build %s.", gb));
        }
        this.genomeURL = datasource.getGenomeURL();
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
        if (! this.indexedFaFiles.containsKey(contigname)) {
            System.err.println("[ERROR]--cound not find contig");
            //TODO Set up exception for this
            return null;
        } else {
            return this.indexedFaFiles.get(contigname);
        }
    }

    public void setViewPoints(List<ViewPoint> viewpointlist) {
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

    public void setHttpProxyPort(int port) {this.httpPort=port; }
    public Integer getHttpProxyPort() { return this.httpPort; }
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
        properties.setProperty("fragNumUp",String.format("%d",model.fragNumUp()));
        properties.setProperty("fragNumDown",String.format("%d",model.fragNumDown()));
        properties.setProperty("minSizeUp",String.format("%d",model.minSizeUp()));
        properties.setProperty("minSizeDown",String.format("%d",model.minSizeDown()));
        properties.setProperty("maxSizeUp",String.format("%d",model.maxSizeUp()));
        properties.setProperty("maxSizeDown",String.format("%d",model.maxSizeDown()));
        properties.setProperty("minFragSize",String.format("%d",model.minFragSize()));
        properties.setProperty("maxRepeatContent",String.format("%f",model.maxRepeatContent()));
        return properties;
    }


    /** Write the settings included in the model to a file.
     * The file will be in the .vpvgui directory in the user's
     * home directory, and will be named according to the project
     * name (default: vpvgui.vpv_settings).
     * @param model
     */
    public static void writeSettingsToFile(Model model) {
        File dir=getVPVDir();
        File settingsFile=new File(dir+File.separator+model.getProjectName());
        logger.info("Writting settings to file at "+settingsFile.getAbsolutePath());
        Properties properties = getProperties(model);
        try {
            OutputStream output = new FileOutputStream(settingsFile.getAbsolutePath());
            properties.store(output, null);
        } catch (IOException e) {
            ErrorWindow.display("Could not write settings to file",e.getMessage());
        }
    }

    /**
     * Initialize the Model object based on the arguments contained in the settings file
     * @param path Absolute complete path to the settings file.
     * @return
     */
    public static Model initializeModelFromSettingsFile(String path) {
        logger.debug("Initializing Model from settings file. path="+path);
        Model model = new Model();
        File settingsFile=new File(path);
        if (!settingsFile.exists()) {
            logger.error("Could not find settings file at "+settingsFile.getAbsolutePath());
            ErrorWindow.display("Could not read settings file",
                    String.format("Settings file %s did not exist",settingsFile.getAbsolutePath()));
            return model; /* empty model. */
        }
        Properties properties=null;
        try {
            InputStream input = new FileInputStream(path);
            properties = new Properties();
            properties.load(input);
        } catch (IOException e) {
            logger.error("could not read VPVgui settings file");
            logger.error(e,e);
            ErrorWindow.display("Could not read settings file",
                    e.getMessage());
            return model; /* empty model. */
        }
        String projectName = properties.getProperty("project_name");
        if (projectName!=null)
            model.setProjectName(projectName);
        String genomeBuild = properties.getProperty("genome_build");
        if (genomeBuild!=null){
            model.setGenomeBuild(genomeBuild);
        }
        String path_to_downloaded_genome_directory = properties.getProperty("path_to_downloaded_genome_directory");
        if (path_to_downloaded_genome_directory!=null) {
            model.setGenomeDirectoryPath(path_to_downloaded_genome_directory);
        }
        String unpacked=properties.getProperty("genome_unpacked");
        if (unpacked!=null && unpacked.equals("true")){
            model.setGenomeUnpacked();
        }
        String indexed=properties.getProperty("genome_indexed");
        if (indexed!=null && indexed.equals("true")){
            model.setGenomeIndexed();
        }
        String transcriptURL=properties.getProperty("transcript_url");
        if (transcriptURL!=null) {
            model.setTranscriptsURL(transcriptURL);
        }
        String refgene_path=properties.getProperty("refgene_path");
        if (refgene_path!=null) {
            model.setRefGenePath(refgene_path);
        }
        String restriction_enzymes = properties.getProperty("restriction_enzymes");
        System.err.println("[TODO] -import enzymes from settings: "+restriction_enzymes);
        String target_genes_path = properties.getProperty("target_genes_path");
        if (target_genes_path!=null) {
            model.setTargetGenesPath(target_genes_path);
        }
        String fragNumUp = properties.getProperty("fragNumUp");
        if (fragNumUp!=null) {
            Integer i = Integer.parseInt(fragNumUp);
            model.setFragNumUpProperty(i);
        }
        String fragNumDown = properties.getProperty("fragNumDown");
        if (fragNumDown!=null) {
            Integer i = Integer.parseInt(fragNumDown);
            model.setFragNumDownProperty(i);
        }
        String minSizeUp = properties.getProperty("minSizeUp");
        if (minSizeUp!=null) {
            Integer i = Integer.parseInt(minSizeUp);
            model.setMinSizeUpProperty(i);
        }
        String minSizeDown = properties.getProperty("minSizeDown");
        if (minSizeDown!=null) {
            Integer i = Integer.parseInt(minSizeDown);
            model.setMinSizeDownProperty(i);
        }
        String maxSizeUp = properties.getProperty("maxSizeUp");
        if (maxSizeUp!=null) {
            Integer i = Integer.parseInt(maxSizeUp);
            model.setMaxSizeUpProperty(i);
        }
        String maxSizeDown = properties.getProperty("maxSizeDown");
        if (maxSizeDown!=null) {
            Integer i = Integer.parseInt(maxSizeDown);
            model.setMaxSizeDownProperty(i);
        }
        String minFragSize = properties.getProperty("minFragSize");
        if (minFragSize!=null) {
            Integer i = Integer.parseInt(minFragSize);
            model.setMinFragSizeProperty(i);
        }
        String maxRepeatContent = properties.getProperty("maxRepeatContent");
        if (maxRepeatContent!=null) {
            Double d = Double.parseDouble(maxRepeatContent);
            model.setMaxRepeatContentProperty(d);
        }
        logger.info("Set model from settings file at "+settingsFile.getAbsolutePath());
        return model;
    }



}
