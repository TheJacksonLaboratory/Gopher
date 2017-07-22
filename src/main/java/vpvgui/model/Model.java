package vpvgui.model;

import com.sun.org.apache.regexp.internal.RE;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.beans.property.*;
import javafx.util.converter.NumberStringConverter;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 05.05.17.
 * Modified by blauh on 06.12.17 to add Settings instance variable to Model constructor.
 */
public class Model {

    private DataSource datasource = null;
    /** This is a list of all possible enzymes from which the user can choose one on more. */
    private List<RestrictionEnzyme> enzymelist=null;
    /** The enzymes chosen by the user for ViewPoint production. */
    private List<RestrictionEnzyme> chosenEnzymelist=null;
    private List<ViewPoint> viewpointList=null;
    private List<VPVGene> geneList=null;
    /** Settings for the current project. */
    private Settings settings=null;
    /** Directory to which the Genome was downloaded */
    private String genomeDirectoryPath=null;
    /** Proxy (null if not needed/not set) */
    private String httpProxy=null;
    /** Proxy port (null if not set) */
    private Integer httpPort=null;


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
     * This suffix is appended to the project name to get the name of the file for storing the
     * project settings.
     */
    public static final String PROJECT_FILENAME_SUFFIX = "-settings.txt";

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

    private String refGenePath=null;

    private Map<String, String> indexedFaFiles=null;

    public List<VPVGene> getVPVGeneList() { return this.geneList; }

    /**
     * This is coupled to genomeTranscriptomeList in the Controller
     * ("UCSC-hg19","UCSC-hg38", "UCSC-mm10");
     * Consider better design
     * <p>
     * TODO: eliminate redundant code after Settings class is integrated into gui HB
     */

    public String genomeURL = null;

    public String getGenomeURL() {
        return this.genomeURL;
    }

    public String genomeBasename = null;

    public String getGenomeBasename() {
        System.out.println("Get genome banesfrom from Model=" + settings.getGenomeFileBasename());
        return this.settings.getGenomeFileBasename();
    }

    public String transcriptsURL = null;

    public String getTranscriptsURL() {
        return transcriptsURL;
    }

    public String transcriptsBasename = null;

    public String getTranscriptsBasename() {
        return transcriptsBasename;
    }

    //public String repeatsURL = null;

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings s) {
        settings = s;
    }

    public Model() {
        initializeEnzymesFromFile();
        settings = Settings.factory();    // creates empty Settings object
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
        this.settings.setGenomeFileURL(this.genomeURL);
        this.settings.setGenomeFileBasename(this.genomeBasename);
        //this.transcriptsURL = datasource.getTranscriptsURL();
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

}
