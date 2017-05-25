package vpvgui.io;

import java.io.File;

/**
 * Created by robinp on 5/3/17.
 */
public class UCSChg37Operation implements Operation {

    private String directoryPath=null;
    /** Location of the UCSC hg37 genome sequence, which is a tar archive with one file per chromosome. */
    private static final String urlstring = "http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz";
    /** Basename of the file that we will store to disk */
    private static final String localFilename = "chromFa.tar.gz";

    /**
     * Download Entrez genes file to given dataDir
     * @param path path to directory where we want to download.
     */
    public UCSChg37Operation(String path) {
        this.directoryPath=path;
    }

    private String getLocalFilePath(){ return this.directoryPath; }


     /** This method checks whether the chromFa.tar.gz file or the unpacked
     * files already exist in the directory indicated by the user (This is the path argument to the constructors,
     * which is stored in localFilePath in the superclass Downloader).
     * We first check whether the tar archive is present. If it is not present, we check whether chr1.fa is present.
     * Note that this can break if the user deletes chr2.fa and then uses the directory, but this will cause an exception
     * and an error message further downstream in this application.
     * We
     * @return false if we find the UCSC genome file already downloaded.
     */
    @Override
    public boolean execute() {
        File f = new File(getLocalFilePath() + File.separator + "chromFa.tar.gz");
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        f = new File(getLocalFilePath() + File.separator + "chr1.fa");
        if(f.exists() && !f.isDirectory()) {
             return false;
        }
        return true;
    }

}
