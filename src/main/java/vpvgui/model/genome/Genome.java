package vpvgui.model.genome;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.Serializable;

/**
 * This is a superclass of a hierarchy of genome classes that know what chromosomes we want to parse and
 * keep track of the chromosome lengths and whether the genome has been downloaded, unpacked, and indesed.
 * @author Peter Robinson
 * @version 0.0.1
 */
public abstract class Genome implements Serializable {
    static Logger logger = Logger.getLogger(Genome.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    /** Absolute path to the directory where ther genome file was downloaded from UCSC. */
    protected String pathToGenomeDirectory=null;
    /** Basename of the compressed genome file that we will store to disk */
    protected static final String localFilename = "chromFa.tar.gz";


    /** Does the directory indicated by {@link #pathToGenomeDirectory} contain either the chromFa.tar.gz file or
     * the corresponding unpacked chromosome files (note: we require only the canonical files; other files can be deleted
     * by the user without affecting this program.
     */
    protected boolean downloadComplete=false;

    protected boolean unpackingComplete=false;

    protected boolean indexingComplete=false;

    public String getPathToGenomeDirectory() {
        return pathToGenomeDirectory;
    }
    public void setPathToGenomeDirectory(String absolutePath) { this.pathToGenomeDirectory=absolutePath;}

    public boolean isDownloadComplete() {
        return pathToGenomeDirectory!= null && downloadComplete;
    }

    public boolean isUnpackingComplete() {
        return pathToGenomeDirectory!= null && unpackingComplete;
    }

    public boolean isIndexingComplete() {
        return pathToGenomeDirectory!= null && indexingComplete;
    }

    /**
     * The default constructor is called when the {@link vpvgui.model.Model} object is initialized.
     * It is expected that the {@link #pathToGenomeDirectory} will be set for the first time
     * when the genome is downloaded (or a path to a pre-existing file is provided).
     */
    public Genome() {
    }

    /**
     *
     * @param pathToGenome Path to the directory where the genome file will be (or was)  downloaded.
     */
    public Genome(String pathToGenome) {
        this.pathToGenomeDirectory=pathToGenome;
    }

    /** @return true if the chromFar.tar.gz file is found in the indicated directory. */
    protected boolean gZippedGenomeFileDownloaded() {
        File f = new File(this.pathToGenomeDirectory + File.separator + "chromFa.tar.gz");
        logger.trace(String.format("Checking existing of file %s",f.getAbsolutePath()));
        return (f.exists() && !f.isDirectory());
    }

    public abstract String getGenomeBuild();

    /** This should be the first function that is called to create and collect the data about the
     * downloaded genome files.
     * @param path Path to the directory where the chromFa.tar.gz file will be downloaed.
     * @return true if the genome file(s) have been completely downloaded.
     */
    public abstract boolean checkDownloadComplete(String path);

    /**
     * We use this method to check if we need to g-unzip the genome files. (We only check for the
     * presence of chr1.fa--this will break if species without chr1 are analyzed).
     * @return true if the chromFGa.tar.gz file has been previously extracted
     */
    private  boolean alreadyExtracted(String path) {
        File f = new File(path + File.separator + "chr1.fa");
        return f.exists();
    }

    public abstract boolean isCanonical(String file);

}
