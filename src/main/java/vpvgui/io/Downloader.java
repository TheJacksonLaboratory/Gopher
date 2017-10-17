package vpvgui.io;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.log4j.Logger;
import vpvgui.exception.VPVException;
import vpvgui.gui.ErrorWindow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Downloader extends Task<Void> {

    static Logger logger = Logger.getLogger(Downloader.class.getName());

    /** An error message, if an error occured */
    protected String errorMessage=null;

    /**
     * This is the absolute path to the place (directory) where the downloaded file will be
     * saved in the local filesystem.
     */
    private File localDir=null;

    /**
     * This is the full local path of the file we will download. It should be set to be identical
     * to {@link #localDir} except for the final file base name.
     */
    private File localFilePath=null;


    /** A reference to a ProgressIndicator that must have be
     * initialized in the GUI and not within this class.
     */
    private ProgressIndicator progress=null;

    /** This is the URL of the file we want to download */
    protected String urlstring=null;

    public Downloader(File directoryPath, String url, String basename) {
        this.localDir = directoryPath;
        this.urlstring=url;
        setLocalFilePath(basename);
        makeDirectoryIfNotExist();
    }

    public Downloader(String path, String url, String basename) {
        this(new File(path),url,basename);
    }

    public Downloader(String path, String url, String basename,  ProgressIndicator pi) {
        this(path,url,basename);
        this.progress = pi;
    }

    public Downloader(File path, String url, String basename,   ProgressIndicator pi) {
        this(path,url,basename);
        this.progress = pi;
    }


    protected File getLocalFilePath() { return  this.localFilePath; }

    protected void setLocalFilePath (String bname) {
        this.localFilePath = new File(this.localDir + File.separator + bname);
        logger.debug("setLocalFilepath for download to: "+localFilePath);
    }



    public boolean hasError() {
        return this.errorMessage != null;
    }

    public String getError() {
        return this.errorMessage;
    }

    /**
     * @param url Subclasses need to set this to the URL of the resource to be downloaded. Alternatively,
     * client code needs to set it.
     */
    public void setURL(String url) {
        this.urlstring=url;
    }




    /**
     * This method downloads a file to the specified local file path. If the file already exists, it emits a warning
     * message and does nothing.
     */
    @Override
    protected Void call() throws VPVException {
        if (progress!=null)
            progress.setProgress(1.000); /* show progress as 100% */
        logger.debug("[INFO] Downloading: \"" + urlstring + "\"");

        // The error handling can be improved with Java 7.
        String err = null;

        InputStream reader;
        FileOutputStream writer;

        int threshold = 0;
        int block = 250000;
        try {
            URL url = new URL(urlstring);
            URLConnection urlc = url.openConnection();
            reader = urlc.getInputStream();
            logger.trace("URL host: "+ url.getHost() + "\n reader available="+reader.available());
            logger.trace("LocalFilePath: "+localFilePath);
            writer = new FileOutputStream(localFilePath);
            byte[] buffer = new byte[153600];
            int totalBytesRead = 0;
            int bytesRead = 0;
            int size = urlc.getContentLength();
            if (progress!=null) { progress.setProgress(0.01); }
            logger.trace("Size of file to be downloaded: "+size);
            if (size >= 0)
                block = size /100;
            //System.err.println("0%       50%      100%");
            while ((bytesRead = reader.read(buffer)) > 0) {
                //System.out.println("bytesRead="+bytesRead + ", size="+size + ", threshold="+threshold +", totalBytesRead="+totalBytesRead + " gt?=+" + ( totalBytesRead > threshold));
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
                if (size>0 && totalBytesRead > threshold) {
                    //System.err.print("=");
                    if (progress!=null) { progress.setProgress((double)totalBytesRead/size); }
                    threshold += block;
                }
            }
            logger.info("Successful download from "+urlstring+": " + (Integer.toString(totalBytesRead)) + "(" + size + ") bytes read.");
            writer.close();
        } catch (MalformedURLException e) {
            progress.setProgress(0.00);
            err = String.format("Malformed url: \"%s\"\n%s", urlstring, e.toString());
            throw new VPVException(err);
        } catch (IOException e) {
            progress.setProgress(0.00);
           // ErrorWindow.display("Error downloading","um");
            err = String.format("IO Exception reading from URL: \"%s\"\n%s", urlstring, e.toString());
            throw new VPVException(err);
        } catch (Exception e){
            progress.setProgress(0.00);
            throw new VPVException(e.getMessage());
        }
        if (err != null) {
            logger.error("Failure to download from \""+urlstring+"\"");
            logger.error(err);
            ErrorWindow.display("Error downloading",err);
            progress.setProgress(0.00);
            return null;

        }
        if (progress!=null) { progress.setProgress(1.000);/* show 100% completion */ }
        return null;
    }

    /**
     * This function creates a new directory to store the downloaded file. If the directory already exists, it
     *  does nothing.
     */
    protected void makeDirectoryIfNotExist() {
        if (localDir==null) {
            logger.error("Null pointer passed, unable to make directory.");
            return;
        }
        if (this.localDir.getParentFile().exists()) {
           return;
        } else {
            logger.info("Creating directory: "+ localDir);
            this.localDir.mkdir();
        }
    }


}
