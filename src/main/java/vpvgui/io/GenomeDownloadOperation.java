package vpvgui.io;

import java.io.File;

/**
 * This class encapsulates the operation of checking whether we have already
 * downloaded a requested genome. We assume that there will be a file called
 * {@code chromFa.tar.gz} and/or individual iunpacked chromosome files ({@code chr1.fa} etc.)
 * in the directory of the user has previously downloaded the genome.  Note that the user is
 * responsible for choosing the right directory.
 * * @author Peter N Robinson
 * @version 0.0.2 (July 11, 2017)
 * Created by robinp on 5/3/17.
 */
public class GenomeDownloadOperation implements Operation {

    private String directoryPath=null;
    /** Basename of the file that we will store to disk */
    private static final String localFilename = "chromFa.tar.gz";

    /**
     * Download Entrez genes file to given dataDir
     * @param path path to directory where we want to download.
     */
    public GenomeDownloadOperation(String path) {
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
        return !f.exists() || f.isDirectory();
    }

}
