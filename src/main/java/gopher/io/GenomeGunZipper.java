package gopher.io;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;


import gopher.service.model.genome.Genome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger logger = LoggerFactory.getLogger(GenomeGunZipper.class.getName());
    /** Model of the current genome, e.g., hg19, with paths and canonical chromosomes etc. */
    private final Genome genome;
    /** This is the basename of the compressed genome file that we download from UCSC. */
    private final String genomeFileNameTarGZ;
    /** Size of buffer for reading the g-zip'd files.*/
    private static final int BUFFER_SIZE=1024;
    /** Indicator of progress of unzipping the genome tar.gz file. */
    private final ProgressIndicator progress;

    private String status=null;

    private boolean OK = false;
    public boolean OK() {return OK;}

    /**
     * @param genome Reference to the model for downloaded/unpacked/index genome data.
     */
    public GenomeGunZipper(Genome genome, ProgressIndicator pi) {
        this.progress=pi;
        this.genome=genome;
        this.genomeFileNameTarGZ=this.genome.getGenomeBasename();
        logger.trace(String.format("genomeFileNameTarGZ=%s",genomeFileNameTarGZ));
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

    /** Check if we can find the original downloaded file (e.g., hg38.chromFa.tar.gz). If not, probably the user
     * needs to download it or the user is in the wrong directory.
     * @return true if we can find the gzipped file, e.g., hg38.chromFa.tar.gz
     */
    public boolean gZippedFileExists() {
        String path = this.genome.getPathToGenomeDirectory() + File.separator + genomeFileNameTarGZ;
        File f = new File(path);
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
        logger.info("About to gunzip " + INPUT_GZIP_FILE +
                " ([path to genome directory=" + genome.getPathToGenomeDirectory() +
                " and genome filename=" + genomeFileNameTarGZ);
        boolean needToCreateDirectory=true;
        int n_extracted_chromosomes=0;
        double extracted_bytes_estimate = 3100000000D;
        double currentProgress=0.0D;
        updateProgress(currentProgress);
        try {
            InputStream in = new FileInputStream(INPUT_GZIP_FILE);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);
            TarArchiveEntry entry;
            String dirpath =this.genome.getPathToGenomeDirectory();
            String outputFastaFileName = genome.getGenomeBuild() + ".fa";
            File outfile = new File( dirpath + File.separator + outputFastaFileName);
            FileOutputStream fos = new FileOutputStream(outfile.getAbsolutePath(), false);
            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                updateProgress(Math.min(0.99, gzipIn.getBytesRead()/extracted_bytes_estimate));
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
                   // commenting the following out will include non-canonical chromosomes.
//                    if (genome.isCanonicalChromosome(filename)) {
//                        logger.trace("Including chromosome "+ filename + " in output file");
//                    } else {
//                        logger.trace("Omitting non-canonical chromosome " +filename + " from output fasta file");
//                        continue;
//                    }
                    if (filename.equals(genome.getGenomeBasename())) {
                        continue;
                    }
                    // Note that since filename may have the "chroms" subdirectory itself, we do not need to add it here.
                    while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                        dest.write(data, 0, count);
                    }
                    logger.trace("extracted chromosome:  " + entry.getName());
                    n_extracted_chromosomes++;
                }
            } // end of loop over Tar archive contents
            dest.close();
            this.status=String.format("extracted %d chromosomes",n_extracted_chromosomes);
            updateProgress(100.0);
            OK=true;
        } catch (IOException e) {
            logger.error("Unable to decompress " + INPUT_GZIP_FILE);
            // logger.error(e,e);
            updateProgress(0.0);
            this.status="extraction could not be completed.";
            throw e;
        }
    }

    private void extractCanonicalChromosomesNoTarArchive() throws IOException {
        updateProgress(0.01); /* show progress as 1% to start off with */
        String INPUT_GZIP_FILE = (new File(this.genome.getPathToGenomeDirectory() + File.separator + genomeFileNameTarGZ)).getAbsolutePath();
        logger.info("About to gunzip " + INPUT_GZIP_FILE +
                " ([path to genome directory=" + genome.getPathToGenomeDirectory() +
                " and genome filename=" + genomeFileNameTarGZ);
        boolean needToCreateDirectory=true;
        int n_extracted_chromosomes=0;
        double extracted_bytes_estimate = 3100000000D;
        double currentProgress=0.0D;
        updateProgress(currentProgress);
        try {
            InputStream in = new FileInputStream(INPUT_GZIP_FILE);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
            String dirpath =this.genome.getPathToGenomeDirectory();
            String outputFastaFileName = genome.getGenomeBuild() + ".fa";
            File outfile = new File( dirpath + File.separator + outputFastaFileName);
            FileOutputStream fos = new FileOutputStream(outfile.getAbsolutePath(), false);
            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
            int len;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((len = gzipIn.read(buffer)) > 0) {
                updateProgress(Math.min(0.99, gzipIn.getBytesRead()/extracted_bytes_estimate));
                fos.write(buffer,0,len);
            }
            dest.close();
            this.status=String.format("extracted %d chromosomes",n_extracted_chromosomes);
            updateProgress(100.0);
            OK=true;
        } catch (IOException e) {
            logger.error("Unable to decompress " + INPUT_GZIP_FILE);
            // logger.error(e,e);
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
            if(this.genome.getGenomeBuild()=="xenTro9" || this.genome.getGenomeBuild()=="danRer10") {
                logger.trace("Not a tar archive. File needs to be unzipped only.");
                extractCanonicalChromosomesNoTarArchive();
                return null;
            } else {
                extractCanonicalChromosomes();
            }
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
