package gopher.io;

import gopher.exception.DownloadFileNotFoundException;

import java.io.File;

public class AlignabilityDownloader {
    final private static String hg19="http://hgdownload.cse.ucsc.edu/goldenPath/hg19/encodeDCC/wgEncodeMapability/wgEncodeCrgMapabilityAlign100mer.bigWig";
    final private static String mm9="http://hgdownload.cse.ucsc.edu/goldenPath/mm9/encodeDCC/wgEncodeMapability/wgEncodeCrgMapabilityAlign100mer.bigWig";
    /* No mapability maps available for mm10 und hg38  */

    private String genome=null;

    /** @param genome The name of the genome assembly, e.g., hg19, hg38, mm9,mm10. */
    public AlignabilityDownloader(String genome) {
        this.genome=genome;
    }
    /** @return The UCSC URL from which the transcript file is to be downloaded. */
    public String getURL()throws DownloadFileNotFoundException {
        String url=null;
        if (this.genome.equals("hg19"))
            url=hg19;
        else if (this.genome.equals("mm9"))
            url=mm9;
        else
            throw new DownloadFileNotFoundException("Could not identify download URL for genome: " + genome);
        return url;
    }

    public String getAlignabilityMapName() { return String.format("%s (%s)",getBaseName(),genome); }
    /** @return "refGene.txt.gz", the basename of all of the downloaded UCSC files. */
    public String getBaseName() { return "wgEncodeCrgMapabilityAlign100mer.bigWig";}



    public boolean needToDownload(String localDirectory) {
        File f = new File(localDirectory + File.separator + "wgEncodeCrgMapabilityAlign100mer.bigWig");
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        f = new File(localDirectory + File.separator + "wgEncodeCrgMapabilityAlign100mer.bigWig");
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        return true;
    }


}