package gopher.io;

import gopher.exception.DownloadFileNotFoundException;

import java.io.File;

public class AlignabilityMapDownloader {
    final private static String hg19="https://www.dropbox.com/s/e0um2wfyq1ru80v/wgEncodeCrgMapabilityAlign100mer.bedgraph.gz?dl=1";
    final private static String mm9="https://www.dropbox.com/s/nqq1c8vzuh5o4ky/wgEncodeCrgMapabilityAlign100mer.bedgraph.gz?dl=1";
    /* No mapability maps available for mm10 und hg38  TODO: create using GEM tools */

    private String genome=null;

    /** @param genome The name of the genome assembly, e.g., hg19, hg38, mm9,mm10. */
    public AlignabilityMapDownloader(String genome) {
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
    public String getBaseName() {
        String basename = genome;
        basename += ".100mer.alignabilityMap.bedgraph.gz";
        return basename;
    }






}