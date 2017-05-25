package vpvgui.model;

/**
 * Created by peter on 06.05.17.
 * This class represents a data source we want to download, being
 * a combination of Genome, TranscriptList, and Repeats.
 */
public class DataSource {
    public String getGenomeName() {
        return genomeName;
    }

    public void setGenomeName(String genomeName) {
        this.genomeName = genomeName;
    }

    private String genomeName;

    public String getGenomeURL() {
        return genomeURL;
    }

    public void setGenomeURL(String genomeURL) {
        this.genomeURL = genomeURL;
    }

    private String genomeURL;

    public String getGenomeBasename() {
        return genomeBasename;
    }

    public void setGenomeBasename(String genomeBasename) {
        this.genomeBasename = genomeBasename;
    }

    public String getTranscriptsName() {
        return transcriptsName;
    }

    public void setTranscriptsName(String transcriptsName) {
        this.transcriptsName = transcriptsName;
    }

    public String getTranscriptsURL() {
        return transcriptsURL;
    }

    public void setTranscriptsURL(String transcriptsURL) {
        this.transcriptsURL = transcriptsURL;
    }

    private String genomeBasename;

    private String transcriptsName;

    public static String getUCSChg19basename() {
        return UCSChg19basename;
    }

    private String transcriptsURL;

    public String getTranscriptsBasename() {
        return transcriptsBasename;
    }

    public void setTranscriptsBasename(String transcriptsBasename) {
        this.transcriptsBasename = transcriptsBasename;
    }

    private String transcriptsBasename;


    private static final String UCSChg19url = "http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz";
    /** Basename of the file that we will store to disk */
    private static final String UCSChg19basename = "chromFa.tar.gz";

    private static final String UCSCrefGeneHg19url="http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/refGene.txt.gz";
    private static final String UCSCrefGeneBasename="refGene.txt.gz";

    private DataSource(){
    }


    public static DataSource createUCSChg19() {
        DataSource ds = new DataSource();
        ds.setGenomeName("UCSC-hg19");
        ds.setGenomeBasename(UCSChg19basename);
        ds.setGenomeURL(UCSChg19url);
        ds.setTranscriptsName("RefGene");
        ds.setTranscriptsURL(UCSCrefGeneHg19url);
        ds.setTranscriptsBasename(UCSCrefGeneBasename);
        return ds;
    }

}
