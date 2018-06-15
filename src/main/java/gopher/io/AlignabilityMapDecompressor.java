package gopher.io;

import gopher.model.Model;
import gopher.model.genome.Genome;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
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

    private final Model model;

    private String status=null;

    private boolean OK = false;
    public boolean OK() {return OK;}

    /**
     * @param model Reference to the model for downloaded/unpacked alignability map
     */
    public AlignabilityMapDecompressor(Model model, ProgressIndicator pi) {
        this.model = model;
        this.progress=pi;
        this.genome=model.getGenome();
        this.alignabilityMapFileNameTarGZ = model.getAlignabilityMapPathIncludingFileNameGz();
        logger.trace(String.format("alignabilityMapFileNameTarGZ=%s",alignabilityMapFileNameTarGZ));

    }

    /**
     * This function uses the list of canonical chromosomes from the {@link #genome} and writes only
     * the corresponding chromosomes to an output file that is called hg19.fa (for instance -- for
     * the hg19 build).
     * @throws IOException if the genome fasta file cannot be g-unzipped
     */
    private void extractAlignabilityMap() throws IOException {

        updateProgress(0.01); /* show progress as 1% to start off with */

        String inputGzipFileName = this.model.getAlignabilityMapPathIncludingFileNameGz();

        String outputBedGraphFileName = this.model.getAlignabilityMapPathIncludingFileName();



        double currentProgress=0.0D;

        updateProgress(currentProgress);

        try {

            InputStream in = new FileInputStream(inputGzipFileName);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);

            FileOutputStream out = new FileOutputStream(outputBedGraphFileName, false);
            BufferedOutputStream bedgraphOut = new BufferedOutputStream(out, BUFFER_SIZE);

            int count = 0;
            byte data[] = new byte[BUFFER_SIZE];
            while ((count = gzipIn.read(data, 0, BUFFER_SIZE)) != -1) {
                bedgraphOut.write(data, 0, count);
            }
            bedgraphOut.close();

            logger.trace("Decompression complete");
            updateProgress(1.0);
            this.status = String.format("Decompression complete");

        } catch (IOException e) {
            logger.error("Unable to decompress " + inputGzipFileName);
            logger.error(e,e);
            updateProgress(0.0);
            this.status="Decompression could not be completed.";
            throw e;
        }
    }


    /** This function uses the apache library to transform the chromFa.tar.gz file into the individual chromosome files.
     * It is packaged as a Task to allow concurrency. */
    @Override
    protected Void call() throws IOException {
        logger.debug("About to extract alignability file");
        if (model.alignabilityMapPathIncludingFileNameExists()) {
            logger.debug("Decompression complete, returning.");
            updateProgress(100.0);
            this.status="Decompression complete";
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
