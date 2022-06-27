package gopher.service.model;

/**
 * Created by peter on 06.05.17.
 * This class represents a data source we want to download, being
 * a combination of Genome, TranscriptList, and Repeats.
 *
 * @param genomeName Name of the genome chosen by the user, e.g., "UCSC-hg19"
 */
public record DataSource(String genomeName, String genomeURL) {
    /**
     * Basename of the file that we will store to disk: chromFa.tar.gz (same for all genomes).
     */
    private static final String basename = "chromFa.tar.gz";

    public String basename() {
        return basename;
    }

    private static final String UCSChg19url = "http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz";

    private static final String UCSChg38url = "http://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/hg38.chromFa.tar.gz";

    private static final String UCSCmm9url = "http://hgdownload.soe.ucsc.edu/goldenPath/mm9/bigZips/chromFa.tar.gz";

    private static final String UCSCmm10url = "http://hgdownload.soe.ucsc.edu/goldenPath/mm10/bigZips/chromFa.tar.gz";

    private static final String UCSCxenTro9url = "http://hgdownload.soe.ucsc.edu/goldenPath/xenTro9/bigZips/xenTro9.fa.gz";

    private static final String UCSCdanRer10url = "http://hgdownload.soe.ucsc.edu/goldenPath/danRer10/bigZips/danRer10.fa.gz";


    public static DataSource createUCSChg19() {
        return new DataSource("UCSC-hg19", UCSChg19url);
    }

    public static DataSource createUCSChg38() {
        return new DataSource("UCSC-hg38", UCSChg38url);
    }

    public static DataSource createUCSCmm9() {
        return new DataSource("UCSC-mm9", UCSCmm9url);
    }

    public static DataSource createUCSCmm10() {
        return new DataSource("UCSC-mm10", UCSCmm10url);
    }

    public static DataSource createUCSCxenTro9() {
        return new DataSource("UCSC-xenTro9", UCSCxenTro9url);
    }

    public static DataSource createUCSCdanRer10() {
        return new DataSource("UCSC-danRer10", UCSCdanRer10url);
    }
}
