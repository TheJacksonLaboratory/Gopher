package gopher.io;

import gopher.service.model.genome.*;
import javafx.scene.control.ProgressIndicator;
import gopher.exception.DownloadFileNotFoundException;

import gopher.gui.factories.PopupFactory;
import gopher.service.model.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GenomeDownloader {
    static Logger logger = LoggerFactory.getLogger(GenomeDownloader.class.getName());
    /** genome build symbol, e.g., hg19, mm10. */
    private String genomebuild;
    /** Representation of the genome we will download. */
    private Genome genome=null;
    /** URL to download the genome from UCSC. */
    private String url=null;

    private String currentStatus="uninitialized";

    private boolean successful=false;


    public GenomeDownloader(String build) {
        this.genomebuild=build;
        logger.debug("Constructor of GenomeDownloader, build=" + build);
        try {
            this.url=getGenomeURL(build);
            this.genome=getGenome(build);
            logger.debug("Setting url to "+ url);
        } catch (DownloadFileNotFoundException e){
            logger.error("Error: {}",e.getMessage());
        }
    }
    /** @return a simple message summarizing how much work has been completed. */
    public String getStatus() {
        return currentStatus;
    }
    /** @return true if the genome was downloaded successfully or is already present.*/
    public boolean successful() {
        return successful;
    }


    /**
     * Start a thread that will download the chromFa.tar.gz file from UCSC.
     * @param directory Directory we will download to
     * @param basename Name of the file (chromFa.tar.gz)
     * @param pi Progress indicator bound to the download operation.
     */
    public void downloadGenome(String directory, String basename, ProgressIndicator pi) {
        Downloader downloadTask = new Downloader(directory, this.url, basename, pi);
        logger.trace(String.format("Starting download of %s to %s",url,directory));
        downloadTask.setOnSucceeded(e -> logger.trace("Finished downloading genome file to " + directory));
        downloadTask.setOnFailed(eh -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            downloadTask.getException().printStackTrace(pw);
            String sStackTrace = sw.toString(); // s
            logger.trace("Failed to download genome file. "+sStackTrace);
            PopupFactory.displayError("Error", sStackTrace);
        });
        Thread th = new Thread(downloadTask);
        th.setDaemon(true);
        th.start();
    }


    /**
     * This method uses {@link DataSource} as a source for collections
     * of paths and names that represent the sets of data we will need
     * to analyze any one genome.
     * @param gb The genome build
     */
    public String getGenomeURL(String gb) throws DownloadFileNotFoundException {
        return switch (gb) {
            case "hg19" -> DataSource.createUCSChg19().getGenomeURL();
            case "hg38" -> DataSource.createUCSChg38().getGenomeURL();
            case "mm9" -> DataSource.createUCSCmm9().getGenomeURL();
            case "mm10" -> DataSource.createUCSCmm10().getGenomeURL();
            case "xenTro9" -> DataSource.createUCSCxenTro9().getGenomeURL();
            case "danRer10" -> DataSource.createUCSCdanRer10().getGenomeURL();
            default -> throw new DownloadFileNotFoundException(String.format("Attempt to get URL for unknown genome build: %s.", gb));
        };
    }


    private Genome getGenome(String genomebuild) throws DownloadFileNotFoundException {
        return switch (genomebuild) {
            case "hg19" -> new HumanHg19();
            case "hg38" -> new HumanHg38();
            case "mm9" -> new MouseMm9();
            case "mm10" -> new MouseMm10();
            case "xenTro9" -> new FrogXenTro9();
            case "danRer10" -> new FishDanRer10();
            default -> throw new DownloadFileNotFoundException(String.format("Attempt to get Genome object for unknown genome build: %s.", genomebuild));
        };
    }


    private String getGenomeBasename() {
        return this.genome.getGenomeBasename();
    }

}
