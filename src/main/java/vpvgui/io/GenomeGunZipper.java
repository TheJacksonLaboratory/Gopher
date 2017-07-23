package vpvgui.io;

//import htsjdk.samtools.reference.FastaSequenceIndexCreator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for g-unzipping and untarring a downloaded genome file.
 * Created by robinp on 7/13/17.
 */
public class GenomeGunZipper {
    /** Path to the directory where we will download and decompress the genome file. */
    private String genomeDirectoryPath=null;
    /** This is the basename of the compressed genome file that we download from UCSC. */
    private static final String genomeFileNameTarGZ = "chromFa.tar.gz";
    /** This is the basename of the decompressed but still tar'd genome file that we download from UCSC. */
    private static final String genomeFileNameTar = "chromFa.tar";
    /** Size of buffer for reading the g-zip'd files.*/
    private static final int BUFFER_SIZE=1024;

    /**
     * @param directoryPath Path to the direcotry where chromFa.tar.gz was downloaded.
     */
    public GenomeGunZipper(String directoryPath) {
        this.genomeDirectoryPath=directoryPath;
    }

    /**
     * We use this method to check if we need to g-unzip the genome files. (We only check for the
     * presence of chr1.fa--this will break if species without chr1 are analyzed).
     * @return true if the chromFGa.tar.gz file has been previously extracted
     */
    public boolean alreadyExtracted() {
        File f = new File(this.genomeDirectoryPath + File.separator + "chr1.fa");
        return f.exists();
    }


    /** This function uses the apache librarty to transform the chromFa.tar.gz file into the individual chromosome files.*/
    public void extractTarGZ() {
        if (alreadyExtracted())
            return;
        String INPUT_GZIP_FILE = (new File(this.genomeDirectoryPath + File.separator + genomeFileNameTarGZ)).getAbsolutePath();
        try {
            InputStream in = new FileInputStream(INPUT_GZIP_FILE);
            GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
            TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn);
            TarArchiveEntry entry;

            while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                /** If the entry is a directory, skip, this should never happen with the chromFa.tag.gx data anyway. **/
                if (entry.isDirectory()) {
                    continue;
                } else {
                    int count;
                    byte data[] = new byte[BUFFER_SIZE];
                    File outfile = new File(this.genomeDirectoryPath + File.separator +entry.getName());
                    System.out.println("[INFO] ungzip'ping "+ entry.getName());
                    FileOutputStream fos = new FileOutputStream(outfile.getAbsolutePath(), false);
                    try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE)) {
                        while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
                            dest.write(data, 0, count);
                        }
                        dest.close();
                    }
                }
            }
            tarIn.close();
            System.out.println("[INFO] Untar completed successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
