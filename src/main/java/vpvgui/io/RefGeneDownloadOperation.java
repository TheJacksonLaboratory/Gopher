package vpvgui.io;

import java.io.File;

/**
 * This class is used to check whether the UCSC transcript file, {@code refGene.txt.gz}, has already been downloaded
 * to the directory specified by the user.
 */
public class RefGeneDownloadOperation implements Operation {
    /** Path to the directory where the refGene.txt.gz file from UCSC will be downloaded. */
    private String directoryPath =null;
    /** Basename of the UCSC transcript file that we will store to disk */
    private static final String localFilename = "refGene.txt.gz";

    /**
     * Download UCSC RefSeq  file to given dataDir
     * @param path path to directory where we want to download.
     */
    public RefGeneDownloadOperation(String path) {
        this.directoryPath =path;
    }
    /** @return absolute path of directory where {@code refGene.txt.gz} will be stored */
    private String getLocalDirectoryPath(){ return this.directoryPath; }


    /** This method checks whether the UCSC refGene.txt.gz file or the corresponding unpacked
     * file already exist in the directory indicated by the user (This is the path argument to the constructors,
     * which is stored in localFilePath in the superclass Downloader).
     * We first check whether the gzip'd file is present. If it is not present, we check whether the corresponding
     * unpacked file is present.
     * We
     * @return false if we find the refGene.txt.gz already downloaded.
     */
    @Override
    public boolean execute() {
        File f = new File(getLocalDirectoryPath() + File.separator + "refGene.txt.gz");
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        f = new File(getLocalDirectoryPath() + File.separator + "refGene.txt");
        if(f.exists() && !f.isDirectory()) {
            return false;
        }
        return true;
    }

}
