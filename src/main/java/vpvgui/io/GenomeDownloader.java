package vpvgui.io;

import javafx.scene.control.ProgressIndicator;
import org.apache.log4j.Logger;
import vpvgui.exception.DownloadFileNotFoundException;

import vpvgui.gui.popupdialog.PopupFactory;
import vpvgui.model.DataSource;
import vpvgui.model.genome.Genome;
import vpvgui.model.genome.HumanHg19;
import vpvgui.model.genome.HumanHg38;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GenomeDownloader {
    static Logger logger = Logger.getLogger(GenomeDownloader.class.getName());
    /** genome build symbol, e.g., hg19, mm10. */
    private String genomebuild=null;
    /** Representation of the genome we will download. */
    private Genome genome=null;
    /** URL to download the genome from UCSC. */
    private String url=null;

    private String currentStatus="uninitialized";

    private boolean successful=false;



    public GenomeDownloader(String build) {
        this.genomebuild=build;
        logger.debug("Constructor of GenomeDownloader, build="+build);
        try {
            this.url=getGenomeURL(build);
            this.genome=getGenome(build);
            logger.debug("Setting url to "+url);
        } catch (DownloadFileNotFoundException e){
            logger.error(e,e);
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
        logger.trace(String.format("Starting download of %s to %s",directory,url));
        downloadTask.setOnSucceeded(e -> {
            logger.trace("Finished downloading genome file to "+directory);
        });
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
        if (gb.equals("hg19")) {
            return  DataSource.createUCSChg19().getGenomeURL();
        } else if (gb.equals("hg38")) {
            return DataSource.createUCSChg38().getGenomeURL();
        } else if (gb.equals("mm9")) {
            return DataSource.createUCSCmm9().getGenomeURL();
        } else if (gb.equals("mm10")) {
            return DataSource.createUCSCmm10().getGenomeURL();
        } else {
            throw new DownloadFileNotFoundException(String.format("Attempt to get URL for unknown genome build: %s.", gb));
        }
    }


    private Genome getGenome(String genomebuild) throws DownloadFileNotFoundException {
        if (genomebuild.equals("hg19")) {
            return new HumanHg19();
        } else if (genomebuild.equals("hg38")) {
            return new HumanHg38();
        } else if (genomebuild.equals("mm9")) {
            throw new DownloadFileNotFoundException(String.format("Not yet implemented: genome for build: %s.", genomebuild));
        } else if (genomebuild.equals("mm10")) {
            throw new DownloadFileNotFoundException(String.format("Not yet implemented: genome for build: %s.", genomebuild));
        } else {
            throw new DownloadFileNotFoundException(String.format("Attempt to get Genome object for unknown genome build: %s.", genomebuild));
        }
    }


    private String getGenomeBasename() {
        return this.genome.getGenomeBasename();
    }

}
