package vpvgui.io;

import vpvgui.exception.DownloadFileNotFoundException;

/**
 * This class will coordinate downloading the RefSeq.txt.gz file for the
 * indicated species.
 */
public class RefGeneDownloader {
    final private static String hg19="http://hgdownload.soe.ucsc.edu/goldenPath/hg19/database/refGene.txt.gz";
    final private static String hg38="http://hgdownload.soe.ucsc.edu/goldenPath/hg38/database/refGene.txt.gz";
    final private static String mm9="http://hgdownload.soe.ucsc.edu/goldenPath/mm9/database/refGene.txt.gz";
    final private static String mm10="http://hgdownload.soe.ucsc.edu/goldenPath/mm10/database/refGene.txt.gz";

    private String genome=null;


    public RefGeneDownloader(String genome) {
        this.genome=genome;
    }

    public String getURL()throws DownloadFileNotFoundException  {
        String url=null;
        if (this.genome.equals("hg19"))
            url=hg19;
        else if (this.genome.equals("hg38"))
            url=hg38;
        else if (this.genome.equals("mm9"))
            url=mm9;
        else if (this.genome.equals("mm10"))
            url=mm10;
        else
            throw new DownloadFileNotFoundException("Could not identify download URL for genome: "+genome);
        return url;
    }

    public String getTranscriptName() { return String.format("%s (%s)",getBaseName(),genome); }

    public String getBaseName() { return "refGene.txt.gz";}


    public void downloadTo(String directoryPath) throws DownloadFileNotFoundException {
        String url=null;
        if (this.genome.equals("hg19"))
            url=hg19;
        else if (this.genome.equals("hg38"))
            url=hg38;
        else if (this.genome.equals("mm9"))
            url=mm9;
        else if (this.genome.equals("mm10"))
            url=mm10;
        else
            throw new DownloadFileNotFoundException("Could not identify download URL for genome: "+genome);
    }

}
