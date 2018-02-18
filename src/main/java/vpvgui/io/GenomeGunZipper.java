package vpvgui.io;

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
 * Note that UCSC download files are not 100% consistent. The Hg38 file has a prefix (hg38.chromFa.tar.gz) and when it
 * unpacks, it does so into a new "chroms" directory. We need to create that directory in code for the un-gzipping and
 * un-tarring to work. We then also need to transmit back to the model that the genome download path has been extended
 * by "/chroms".
 * @author Peter Robinson
 * @version 0.2.3 (2018-02-17)
 */
public class GenomeGunZipper extends Task<Void>  {
    private static Logger logger = Logger.getLogger(GenomeGunZipper.class.getName());
    /** Model of the current genome, e.g., hg19, with paths and canonical chromosomes etc. */
    private final Genome genome;
    /** This is the basename of the compressed genome file that we download from UCSC. */
    private final String genomeFileNameTarGZ;
    /** This is the basename of the decompressed but still tar'd genome file that we download from UCSC. */
    private final String genomeFileNameTar;
    /** Size of buffer for reading the g-zip'd files.*/
    private static final int BUFFER_SIZE=1024;
    /** Indicator of progress of unzipping the genome tar.gz file. */
    private final ProgressIndicator progress;

    private String status=null;

    private boolean OK = false;
    public boolean OK() {return OK;}

    /**
     * @param genom Reference to the model for downloaded/unpacked/index genome data.
     */
    public GenomeGunZipper(Genome genom, ProgressIndicator pi) {
        this.progress=pi;
        this.genome=genom;
        this.genomeFileNameTarGZ=this.genome.getGenomeBasename();
        logger.trace(String.format("genomeFileNameTarGZ=%s",genomeFileNameTarGZ));
        this.genomeFileNameTar=this.genomeFileNameTarGZ.replaceAll(".gz","");
    }


    public String getStatus() { return status; }

    /**
     * We use this method to check if we need to g-unzip the genome files.
     * @return true if the hg19.fa file is found (and thus, the chromFa.tar.gx has been previously extracted)
     */
    private boolean alreadyExtracted() {
        File f = new File(this.genome.getPathToGenomeDirectory() + File.separator + genome.getGenomeFastaName());
        logger.trace("checking for existence of file " + f.getAbsolutePath());
        return f.exists();
    }


    /**
     * This function uses the list of canonical chromosomes from the {@link #genome} and writes only
     * the corresponding chromosomes to an output file that is called hg19.fa (for instance -- for
     * the hg19 build).
     * @throws IOException if the genome fasta file cannot be g-unzipped
     */
    private void extractCanonicalChromosomes() throws IOException {
        updateProgress(0.01); /* show progress as 1% to start off with */
        String INPUT_GZIP_FILE = (new File(this.genome.getPathToGenomeDirectory() + File.separator + genomeFileNameTarGZ)).getAbsolutePath();
        logger.info("About to gunzip "+INPUT_GZIP_FILE);
        boolean needToCreateDirectory=true;
        double totalprogress=0d;
        int blocks = genome.getNumberOfCanonicalChromosomes() + 1;
        double blocksize = 1D/blocks;
        double currentProgress=0.0D;
        updateProgress(currentProgress);
        try {
            InputStream in = new FileInputStream(INPUT_GZIP_FILE);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);
            TarArchiveEntry entry;
            /* For human hg38, there are 93 files (including all the contigs)
            For simplicity, we will update the ProgressIndicator by 1% per file.
            If we get to 95, we will begin to update by 0.1% until
            we are done.
             */
            String dirpath =this.genome.getPathToGenomeDirectory();
            String outputFastaFileName=genome.getGenomeBuild() + ".fa";
            File outfile = new File( dirpath+ File.separator +outputFastaFileName);
            FileOutputStream fos = new FileOutputStream(outfile.getAbsolutePath(), false);
            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                currentProgress+=blocksize;
                updateProgress(currentProgress);
                // If the entry is a directory, skip, this should never happen with the chromFa.tag.gx data anyway.
                if (entry.isDirectory()) {
                    continue;
                } else {
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    // Note that for hg38, the tar archive expands into a subdirectory called chroms.
                    // If the files begin with "chroms", the direc
                    String filename=entry.getName();
                    filename=filename.replaceAll("^\\./","");
                    if (genome.isCanonicalChromosome(filename)) {
                        logger.trace("Including chromosome "+ filename + " in output file");
                    } else {
                        logger.trace("Omitting non-canonical chromosome " +filename + " from output fasta file");
                        continue;
                    }
                    if (needToCreateDirectory && filename.startsWith("chroms")) {
                        // create a chroms directory if needed, otherwise we cannot unpack to it
                        // this means that the archive wants to unpack to chroms/chr1 etc.
                        logger.trace(String.format("Creating directory for chroms: %s",filename));
                        String createDirPath =String.format("%s%schroms",dirpath,File.separator);
                        File directory = new File(createDirPath);
                        if (! directory.exists()) {
                            logger.trace(String.format("Creating directory for chroms: %s",createDirPath));
                            directory.mkdir();
                        }
                        this.genome.setPathToGenomeDirectory(createDirPath);// this extends the genome path.
                        needToCreateDirectory=false; // only need to do this the first time.
                    }
                    if (filename.equals(genome.getGenomeBasename())) {
                        continue;
                    }
                    // Note that since filename may have the "chroms" subdirectory itself, we do not need to add it here.
                    while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                        dest.write(data, 0, count);
                    }
                    logger.trace("extracted chromosome:  "+entry.getName());
                }
            } // end of loop over Tar archive contents
            dest.close();
        } catch (IOException e) {
            logger.error("Unable to decompress "+INPUT_GZIP_FILE);
            logger.error(e,e);
            updateProgress(0.0);
            this.status="extraction could not be completed.";
            throw e;
        }
    }


    /** This function uses the apache library to transform the chromFa.tar.gz file into the individual chromosome files.
     * It is packaged as a Task to allow concurrency. */
    @Override
    protected Void call() throws IOException {
        logger.debug("About to extract canonical chromosome fasta file");
        if (alreadyExtracted()) {
            logger.debug("Found previously extracted file, returning.");
            updateProgress(100.0);
            this.status="extraction previously completed.";
            OK=true;

        } else {
            extractCanonicalChromosomes();
        }
        return null;
    }

    /** Update the progress bar of the GUI in a separate thread.
     * @param pr Current progress.
     */
    private void updateProgress(double pr) {
        javafx.application.Platform.runLater(() -> {
            if (progress == null) {
                logger.error("NULL pointer to download progress indicator");
                return;
            }
            progress.setProgress(pr);
        });
    }

}
