package vpvgui.model;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.beans.property.*;
import javafx.util.converter.NumberStringConverter;
import vpvgui.exception.DownloadFileNotFoundException;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 05.05.17.
 * Modified by blauh on 06.12.17 to add Settings instance variable to Model constructor.
 */
public class Model {

    private DataSource datasource = null;

    private List<RestrictionEnzyme> enzymelist;

    private List<VPVGene> geneList;
    /** Settings for the current project. */
    private Settings settings;
    /** Directory to which the Genome was downloaded */
    private String genomeDirectoryPath=null;

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
        return genomeBuild.get();
    }
    public void setGenomeBuild(String newDatabase) {
        genomeBuild.set(newDatabase);
    }
    public StringProperty genomeBuildProperty() {
        return genomeBuild;
    }

    final private IntegerProperty minSizeUpProperty = new SimpleIntegerProperty(-1);
    public IntegerProperty minSizeUpProperty() { return minSizeUpProperty;  }
    public int minSizeUp() {return minSizeUpProperty.get();}
    public void setMinSizeUpProperty(Integer i) { this.minSizeUpProperty.setValue(i);}

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
        if (gb.equals("UCSC-hg19")) {
            this.datasource = DataSource.createUCSChg19();
        } else if (gb.equals("UCSC-hg38")) {
            this.datasource = DataSource.createUCSChg38();
        } else if (gb.equals("UCSC-mm9")) {
            this.datasource = DataSource.createUCSCmm9();
        } else if (gb.equals("UCSC-mm10")) {
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
    /** Takes the list of VPVGenes and creates a list of ViewPoints.
     * TODO -- add correct parameters.*/
    public List<ViewPoint> getViewPointList() {
        List<ViewPoint> vplist = new ArrayList<>();
        Integer maxDistToGenomicPosUp=200;
        Integer maxDistToGenomicPosDown=200;
        String[] cutPat=new String[1];
        cutPat[0] = "ACT";
        IndexedFastaSequenceFile dummyFile=null;//TODO
        for (VPVGene vpvg:this.geneList) {
            List<Integer> tsslist = vpvg.getTSSlist();
            String refSeqID = vpvg.getChromosome();
            for (Integer i : tsslist) {
                ViewPoint vp = new ViewPoint(refSeqID,i,maxDistToGenomicPosUp,maxDistToGenomicPosDown);
                vp.setTargetName(vpvg.getGeneSymbol());
                vplist.add(vp);
            }
        }
            return vplist;
    }

    /** @return true if we have at least one VPVGene (which contain ViewPoints). */
    public boolean viewpointsInitialized() {
        return (this.geneList!=null && this.geneList.size()>0);
    }


}
