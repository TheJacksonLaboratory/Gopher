package vpvgui.io;

//import htsjdk.samtools.reference.FastaSequenceIndexCreator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;

/**
 * This class is responsible for g-unzipping and indexing a downloaded genome file.
 * Created by robinp on 7/13/17.
 */
public class GenomeIndexer {

    private String genomeDirectoryPath=null;

    private static final String genomeFileNameTarGZ = "chromFa.tar.gz";
    private static final String genomeFileNameTar = "chromFa.tar";

    private static final int BUFFER_SIZE=1024;


    /**
     *
     * @param directoryPath Path to the direcotry where chromFa.tar.gz was downloaded.
     */
    public GenomeIndexer(String directoryPath) {
        this.genomeDirectoryPath=directoryPath;
    }

    /** @return true if the chromFGa.tar.gz file has been previously extract (note we only check for the
     * presence of chr1.fa--this will break if species without chr1 are analyzed).
     * @return
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

    private boolean fastaFAIalreadyExists(String path) {
        File f=new File(String.format("%s.fai",path));
        return f.exists();
    }

    /** Create FAI indices for all FASTA files in {@link #genomeDirectoryPath} (only if needed). */
    public void indexFastaFiles() {
        final File folder = new File(this.genomeDirectoryPath);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                continue;
            } else if (!fileEntry.getPath().endsWith(".fa")) {
                continue;
            } else if (fastaFAIalreadyExists(fileEntry.getAbsolutePath())) {
                continue;
            } else {
                    /* if we get here, we have a FASTA file ending with ".fa" that has not yet been indexed */
                createFASTAindex(fileEntry.getAbsolutePath());
            }
        }
    }


   /**
    * The fasta.fai is the fasta index,.
    * <ol><li>Column 1: The contig name.</li>
    * <li>Column 2: The number of bases in the contig</li>
    * <li>Column 3: The byte index of the file where the contig sequence begins.</li>
    * <li>Column 4: bases per line in the FASTA file</li>
    * <li>Column 5: bytes per line in the FASTA file</li>
    * </ol>
    * For instance, the fai for human chr15 is
    *  <pre>
    *     chr15	102531392	7	50	51
    *  </pre>
    * Note that for the UCSC files, there is one sequence per file, and thus the fai files have only one line.
    * Also note that I did not use the HTSJDK indexer because it is not available in the versions of the library in maven
    * central. We can later use their implementation if this changes.
    * @param path Path to the original FASTA file. We will add ".fai" to this as a suffix.
    *
    */
   public void createFASTAindex(String path) {
       try {
           RandomAccessFile file = new RandomAccessFile(path, "r");
           file.seek(0);
           String header= file.readLine();
           if (!header.startsWith(">")) {
               System.err.println("[ERROR] FASTA header line did not start with >: "+header);
               return;
           }
           String contigname=header.substring(1);
           long offset=file.getFilePointer(); /* this is the offset in bytes of the first sequence line. */
           String line=file.readLine();
           int basesPerLine=line.length();
           long offset2=file.getFilePointer();
           long bytesPerLine=offset2-offset;
           long n_bases=basesPerLine;
           while ((line=file.readLine())!=null) {
               n_bases += line.length();
           }
           file.close();
           String outname=String.format("%s%s.fai",path,File.separator);
           BufferedWriter bw = new BufferedWriter(new FileWriter(outname));
           bw.write(String.format("%s\t%d\t%d\t%d\t%d",contigname,n_bases,offset,basesPerLine,bytesPerLine));
           bw.close();
       } catch (FileNotFoundException e) {
           e.printStackTrace();
           System.err.println("Error: Could not find FASTA file:"+path);

       }catch (IOException e) {
           e.printStackTrace();
           System.err.println("Error: IO Problem reading file:"+path);
       }
    }


}
