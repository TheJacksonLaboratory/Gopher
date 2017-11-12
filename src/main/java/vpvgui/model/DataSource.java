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
    /** Name of the genome chosen by the user, e.g., "UCSC-hg19" */
    private String genomeName;

    public String getGenomeURL() {
        return genomeURL;
    }

    private String genomeURL;

    public String getGenomeBasename() {
        return genomeBasename;
    }

    /** chromFa.tar.gz (same for all genomes). */
    private String genomeBasename;


    private static final String UCSChg19url = "http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz";
    /** Basename of the file that we will store to disk */
    private static final String basename = "chromFa.tar.gz";

    private static final String UCSChg38url = "http://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/hg38.chromFa.tar.gz";


    private static final String UCSCmm9url = "http://hgdownload.soe.ucsc.edu/goldenPath/mm9/bigZips/chromFa.tar.gz";


    private static final String UCSCmm10url = "http://hgdownload.soe.ucsc.edu/goldenPath/mm10/bigZips/chromFa.tar.gz";


    private DataSource(){
        this.genomeBasename=basename;
    }


    public static DataSource createUCSChg19() {
        DataSource ds = new DataSource();
        ds.genomeName = "UCSC-hg19";
        ds.genomeURL=UCSChg19url;
        return ds;
    }

    public static DataSource createUCSChg38() {
        DataSource ds = new DataSource();
        ds.genomeName="UCSC-hg38";
        ds.genomeURL=UCSChg38url;
        return ds;
    }

    public static DataSource createUCSCmm9() {
        DataSource ds = new DataSource();
        ds.genomeName="UCSC-mm9";
        ds.genomeURL=UCSCmm9url;
        return ds;
    }

    public static DataSource createUCSCmm10() {
        DataSource ds = new DataSource();
        ds.genomeName="UCSC-mm10";
        ds.genomeURL=UCSCmm10url;
        return ds;
    }


}
