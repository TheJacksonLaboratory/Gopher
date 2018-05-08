package gopher.io;

import gopher.exception.DownloadFileNotFoundException;

import java.io.File;

/**
 * This class stores the URLs for the RefSeq.txt.gz file for the Ensembl regulatory features GTF file for the
 * indicated species (mm9,mm10,hg19,hg38).
 * See <a href="http://www.ensembl.org/info/genome/funcgen/regulatory_build.html">http://www.ensembl.org/info/genome/funcgen/regulatory_build.html</a>
 * This class is intended to be used with {@link Downloader}
 *
 * @author Peter Robinson
 * @version 0.1.3 (2017-11-12)
 */
public class RegulatoryBuildDownloader {
    final private static String hg19="ftp://ftp.ensembl.org/pub/grch37/update/regulation/homo_sapiens/homo_sapiens.GRCh37.Regulatory_Build.regulatory_features.20161117.gff.gz";
    final private static String hg38 = "ftp://ftp.ensembl.org/pub/release-90/regulation/homo_sapiens/homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff.gz";


    private String genome = null;

    /**
     * @param genome The name of the genome assembly, e.g., hg19, hg38, mm9,mm10.
     */
    public RegulatoryBuildDownloader(String genome) {
        this.genome = genome;
    }

    /**
     * @return The UCSC URL from which the transcript file is to be downloaded.
     */
    public String getURL() throws DownloadFileNotFoundException {
        String url = null;
        if (this.genome.equals("hg19"))
            url=hg19;
        else if (this.genome.equals("hg38"))
            url = hg38;
//        else if (this.genome.equals("mm9"))
//            url=mm9;
//        else if (this.genome.equals("mm10"))
//            url=mm10;
        else
            throw new DownloadFileNotFoundException("This feature is not available for regulatory build: " + genome);
        return url;
    }


    /**
     * @return the basename  of the downloaded Ensembl regulatory build files.
     */
    public String getBaseName() {
        if (this.genome.equals("hg38")) {
            return "homo_sapiens.GRCh38.Regulatory_Build.regulatory_features.20161111.gff.gz";
        } else if (this.genome.equals("hg19") ) {
            return "homo_sapiens.GRCh37.Regulatory_Build.regulatory_features.20161117.gff.gz";
        } else {
            return "?";
        }
    }

    public boolean needToDownload(String localDirectory) {
        String basename=getBaseName();
        File f = new File(localDirectory + File.separator + basename);
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        return true;
    }

}
