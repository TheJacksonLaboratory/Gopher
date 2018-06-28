package gopher.model.genome;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a superclass of a hierarchy of genome classes that know what chromosomes we want to parse and
 * keep track of the chromosome lengths and whether the genome has been downloaded, unpacked, and indesed.
 * @author Peter Robinson
 * @version 0.0.2 (2017-10-24)
 */
public abstract class Genome implements Serializable {
    private static Logger logger = Logger.getLogger(Genome.class.getName());
    /** serialization version ID */
    static final long serialVersionUID = 2L;
    /** Absolute path to the directory where ther genome file was downloaded from UCSC. */
    private String pathToGenomeDirectory=null;
    /** Basename of the compressed genome file that we will store to disk */
    protected static final String localFilename = "chromFa.tar.gz";
    /** The valid chromosomes for the current genome build. (will be initialized from the subclasses).*/
    protected Set<String> valid=null;
    /** Set of base chromosome file names for canonical chromosomes. For instnace, "chr1.fa", but not "chr1random12432.fa" .*/
    protected Set<String> chromosomeFileNames;
    /** This is the name of the file we download from UCSC for any of the genomes. */
    private static final String DEFAULT_GENOME_BASENAME = "chromFa.tar.gz";
    private String genomeBasename = DEFAULT_GENOME_BASENAME;


    private boolean unpackingComplete=false;
    /** A flag to indicate that the production of a FAI file for the genome is completed. */
    private boolean indexingComplete=false;

    private String genomeURL = null;

    public String getPathToGenomeDirectory() {
        return pathToGenomeDirectory;
    }
    public void setPathToGenomeDirectory(String absolutePath) { this.pathToGenomeDirectory=absolutePath;}



    public boolean isUnpackingComplete() {
        return unpackingComplete;
    }

    public boolean isIndexingComplete() { return indexingComplete; }

    public String getGenomeURL() { return this.genomeURL;}

    public abstract String getGenomeBasename();

    /**
     * The default constructor is called when the {@link gopher.model.Model} object is initialized.
     * It is expected that the {@link #pathToGenomeDirectory} will be set for the first time
     * when the genome is downloaded (or a path to a pre-existing file is provided).
     */
    public Genome() {
        chromosomeFileNames=new HashSet<>();
        valid=new HashSet<>();
        init();
    }

    protected abstract void init();

    /**
     *
     * @param pathToGenome Path to the directory where the genome file will be (or was)  downloaded.
     */
    public Genome(String pathToGenome) {
        this.pathToGenomeDirectory=pathToGenome;
    }

    /** @return true if the chromFar.tar.gz file is found in the indicated directory. */
    private boolean gZippedGenomeFileDownloaded() {
        File f = new File(this.pathToGenomeDirectory + File.separator + getGenomeBasename());
        return (f.exists() && !f.isDirectory());
    }

    public abstract String getGenomeBuild();
    public abstract String getGenomeFastaName();
    public void setGenomeUnpacked(boolean b) { this.unpackingComplete=b;}
    public void setGenomeIndexed(boolean b) { this.indexingComplete=b;}





    /**
     * @return true if the genome file has been previously downloaded to the indicated path.
     */
    public boolean checkDownloadComplete(String path) {
        this.pathToGenomeDirectory = path;
        return gZippedGenomeFileDownloaded();
    }

    /**
     * We use this method to check if we need to g-unzip the genome files. (We only check for the
     * presence of chr1.fa--this will break if species without chr1 are analyzed).
     *
     * @return true if the chromFGa.tar.gz file has been previously extracted
     */
    public boolean alreadyExtracted() {
        List<String> missingChromosomes = new ArrayList<>();
        for (String chr : valid) {
            File f = new File(this.pathToGenomeDirectory + File.separator + chr);
            if (!f.exists()) {
                missingChromosomes.add(f.getAbsolutePath());
            }
        }
        return (missingChromosomes.size() == 0); // true if we found all expected chromosomes
    }


    /**
     * This funtion is called by the FASTAIndexManager, and it expects to receive the results
     * of file.getName(), i.e., the basename of a chromosome file, which is something like {@code chr1.fa}.
     * The function should return true for the canonical chromosomes such as {@code chr1.fa}
     * and should return false for non-canonidal chromosomes such as {@code chr18_gl000207_random.fa}.
     * @param chromosomeFileBasename A string such as "chr12"
     * @return true for canonical chromosome files.
     */
    public boolean isCanonical(String chromosomeFileBasename) {
        chromosomeFileBasename=chromosomeFileBasename.replaceAll(".fa", "");
        boolean ok = this.valid.contains(chromosomeFileBasename);
        logger.trace(String.format("%s was %s",chromosomeFileBasename,ok));
        return ok;
    }

    public abstract int getNumberOfCanonicalChromosomes();

    public boolean isCanonicalChromosome(String filename) {
        int i = filename.lastIndexOf("/");
        if (i>0 && filename.length()>i) { filename = filename.substring(i+1); }
        if (filename.startsWith("chroms/")) {
            filename=filename.substring(7);
        }
        return chromosomeFileNames.contains(filename);
    }

}
