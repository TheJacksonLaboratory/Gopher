package gopher.io;

import gopher.model.genome.Genome;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.log4j.Logger;

import java.io.*;

public class AlignabilityMapDecompressor  extends Task<Void> {
    private static final Logger logger = Logger.getLogger(AlignabilityMapDecompressor.class.getName());

    /** Model of the current genome, e.g., hg19 */
    private final Genome genome;
    /** This is the basename of the compressed alignabilty map file that we download from X. */
    private final String alignabilityMapFileNameTarGZ;
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
    public AlignabilityMapDecompressor(Genome genom, ProgressIndicator pi) {
        this.progress=pi;
        this.genome=genom;
        //this.alignabilityMapFileNameTarGZ=this.genome.getGenomeBasename();
        this.alignabilityMapFileNameTarGZ = "wgEncodeCrgMapabilityAlign100mer.bedgraph.gz";
        logger.trace(String.format("alignabilityMapFileNameTarGZ=%s",alignabilityMapFileNameTarGZ));
        //this.genomeFileNameTar=this.alignabilityMapFileNameTarGZ.replaceAll(".gz","");
    }


    public String getStatus() { return status; }

    /**
     * We use this method to check if we need to g-unzip the genome files.
     * @return true if the hg19.fa file is found (and thus, the chromFa.tar.gx has been previously extracted)
     */
    private boolean alreadyExtracted() {
        File f = new File(this.genome.getPathToGenomeDirectory() + File.separator + "wgEncodeCrgMapabilityAlign100mer.bedgraph");
        logger.trace("Checking for existence of file " + f.getAbsolutePath());
        return f.exists();
    }


    /**
     * This function uses the list of canonical chromosomes from the {@link #genome} and writes only
     * the corresponding chromosomes to an output file that is called hg19.fa (for instance -- for
     * the hg19 build).
     * @throws IOException if the genome fasta file cannot be g-unzipped
     */
    private void extractAlignabilityMap() throws IOException {
        updateProgress(0.01); /* show progress as 1% to start off with */
        String INPUT_GZIP_FILE = (new File(this.genome.getPathToGenomeDirectory() + File.separator + alignabilityMapFileNameTarGZ)).getAbsolutePath();
        logger.info("About to gunzip "+ INPUT_GZIP_FILE +
                " ([path to genome directory=" + genome.getPathToGenomeDirectory() +
                " and genome filename=" + alignabilityMapFileNameTarGZ);
        boolean needToCreateDirectory=true;
        int blocks = genome.getNumberOfCanonicalChromosomes() + 1;
        double currentProgress=0.0D;
        updateProgress(currentProgress);
        try {
            InputStream in = new FileInputStream(INPUT_GZIP_FILE);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);

            String dirpath = this.genome.getPathToGenomeDirectory();
            String outputBedGraphFileName = genome.getGenomeBuild() + ".100mer.alignabilityMap.bedgraph";
            File outfile = new File( dirpath + File.separator + outputBedGraphFileName);
            FileOutputStream fos = new FileOutputStream(outfile.getAbsolutePath(), false);
            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);

            genome.setAlignabilityUnpacked(true);

            int count = 0;
            byte data[] = new byte[BUFFER_SIZE];
            while ((count = gzipIn.read(data, 0, BUFFER_SIZE)) != -1) {
                dest.write(data, 0, count);
            }
            dest.close();
            logger.trace("Unzipped bedgraph");
            updateProgress(1.0);
            this.status = String.format("Extracted alignability map");
        } catch (IOException e) {
            logger.error("Unable to decompress " + INPUT_GZIP_FILE);
            logger.error(e,e);
            updateProgress(0.0);
            this.status="Extraction could not be completed.";
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
            extractAlignabilityMap();
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
