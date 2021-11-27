package gopher.io;

import gopher.exception.DownloadFileNotFoundException;

import java.io.File;

/**
 * This class stores the URLs for the RefSeq.txt.gz file for the
 * indicated species (mm9,mm10,hg19,hg38.
 * @author Peter Robinson
 * @version 0.1.2
 */
public class RefGeneDownloader {
    final private static String hg19="http://hgdownload.soe.ucsc.edu/goldenPath/hg19/database/refGene.txt.gz";
    final private static String hg38="http://hgdownload.soe.ucsc.edu/goldenPath/hg38/database/refGene.txt.gz";
    final private static String mm9="http://hgdownload.soe.ucsc.edu/goldenPath/mm9/database/refGene.txt.gz";
    final private static String mm10="http://hgdownload.soe.ucsc.edu/goldenPath/mm10/database/refGene.txt.gz";
    final private static String xenTro9="http://hgdownload.soe.ucsc.edu/goldenPath/xenTro9/database/refGene.txt.gz";
    final private static String danRer10="http://hgdownload.soe.ucsc.edu/goldenPath/danRer10/database/refGene.txt.gz";

    private String genome=null;

    /** @param genome The name of the genome assembly, e.g., hg19, hg38, mm9,mm10. */
    public RefGeneDownloader(String genome) {
        this.genome=genome;
    }
    /** @return The UCSC URL from which the transcript file is to be downloaded. */
    public String getURL()throws DownloadFileNotFoundException  {
        String url = switch (this.genome) {
            case "hg19" -> hg19;
            case "hg38" -> hg38;
            case "mm9" -> mm9;
            case "mm10" -> mm10;
            case "xenTro9" -> xenTro9;
            case "danRer10" -> danRer10;
            default -> throw new DownloadFileNotFoundException("Could not identify download URL for genome: " + genome);
        };
        return url;
    }

    public String getTranscriptName() { return String.format("%s (%s)",getBaseName(),genome); }
    /** @return "refGene.txt.gz", the basename of all of the downloaded UCSC files. */
    public String getBaseName() { return "refGene.txt.gz";}



    public boolean needToDownload(String localDirectory) {
        File f = new File(localDirectory + File.separator + "refGene.txt.gz");
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        f = new File(localDirectory + File.separator + "refGene.txt");
        return !f.exists() || f.isDirectory();
    }


}
