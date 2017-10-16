package vpvgui.io;

//import htsjdk.samtools.reference.FastaSequenceIndexCreator;
import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.log4j.Logger;
import vpvgui.model.genome.Genome;

import java.io.*;

/**
 * This class is responsible for g-unzipping and untarring a downloaded genome file.
 * @author Peter Robinson
 * @version 0.0.4 (5 October, 2017)
 */
public class GenomeGunZipper extends Task<Void>  {
    static Logger logger = Logger.getLogger(GenomeGunZipper.class.getName());
    /** Path to the directory where we will download and decompress the genome file. */
   // private String genomeDirectoryPath=null;
    private Genome genome=null;
    /** This is the basename of the compressed genome file that we download from UCSC. */
    private static final String genomeFileNameTarGZ = "chromFa.tar.gz";
    /** This is the basename of the decompressed but still tar'd genome file that we download from UCSC. */
    private static final String genomeFileNameTar = "chromFa.tar";
    /** Size of buffer for reading the g-zip'd files.*/
    private static final int BUFFER_SIZE=1024;

    private ProgressIndicator progress=null;

    private String status=null;

    private boolean OK = false;
    public boolean OK() {return OK;}

    /**
     * @param genom Reference to the model for downloaded/unpacked/index genome data.
     */
    public GenomeGunZipper(Genome genom, ProgressIndicator pi) {
        this.progress=pi;
        this.genome=genom;
    }

    public Genome getGenome() {
        return genome;
    }

    public String getStatus() { return status; }

    /**
     * We use this method to check if we need to g-unzip the genome files. (We only check for the
     * presence of chr1.fa--this will break if species without chr1 are analyzed).
     * @return true if the chromFGa.tar.gz file has been previously extracted
     */
    private boolean alreadyExtracted() {
        File f = new File(this.genome.getPathToGenomeDirectory() + File.separator + "chr1.fa");
        return f.exists();
    }





    /** This function uses the apache library to transform the chromFa.tar.gz file into the individual chromosome files.
     * It is packaged as a Task to allow concurrency. */
    @Override
    protected Void call() {
        logger.debug("entering extractTarGZ");
        if (alreadyExtracted()) {
            logger.debug("Found already extracted files, returning.");
            this.progress.setProgress(100.0);
            this.status="extraction previously completed.";
            OK=true;
            return null;
        }
        if (this.progress != null)
            this.progress.setProgress(0.01); /* show progress as 1% to start off with */
        String INPUT_GZIP_FILE = (new File(this.genome.getPathToGenomeDirectory() + File.separator + genomeFileNameTarGZ)).getAbsolutePath();
        logger.info("About to gunzip "+INPUT_GZIP_FILE);
        try {
            InputStream in = new FileInputStream(INPUT_GZIP_FILE);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);
            TarArchiveEntry entry;
            /* For humans, there are 93 files (including all the contigs)
            For simplicity, we will update the ProgressIndicator by 1% per file. If we get to 95, we will begin to update by 0.1% until
            we are done.
             */
            double percentDone=0d;
            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                /** If the entry is a directory, skip, this should never happen with the chromFa.tag.gx data anyway. **/
                if (entry.isDirectory()) {
                    continue;
                } else {
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    File outfile = new File(this.genome.getPathToGenomeDirectory() + File.separator +entry.getName());
                    logger.trace("ungzip'ping "+ entry.getName());
                    FileOutputStream fos = new FileOutputStream(outfile.getAbsolutePath(), false);
                    try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
                        while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                            dest.write(data, 0, count);
                        }
                        dest.close();
                    }
                    logger.trace("Unzipped "+entry.getName());
                    if (percentDone<90)
                        percentDone +=1.0d;
                    else
                        percentDone += 0.1d;
                    progress.setProgress(percentDone/100.0);
                }
            }
            progress.setProgress(1.0);
            OK=true;
            tarIn.close();
            logger.info("Untar completed successfully for "+INPUT_GZIP_FILE);
            this.status="chromFa.tar.gz successfully extracted.";
        } catch (IOException e) {
            logger.error("Unable to decompress "+INPUT_GZIP_FILE);
            logger.error(e,e);
            progress.setProgress(0.0);
            this.status="extraction could not be completed.";
        }
        return null;
    }

}
