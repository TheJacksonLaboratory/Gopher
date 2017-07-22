package vpvgui.io;

import java.io.*;

/**
 * This class creates a FASTA index for the FASTA file passed to the constructor.
 * Note that in the future it would be better to use HTSJDK, but the FASTA Indexing function
 * is not available with the current version in maven central.
 * * The fasta.fai is the fasta index,.
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
 * central. We can later use their implementation if this changes. The class should create a fasta index equivalent to
 * what would be produced by <pre>$ samtools faidx test.fa</pre>.
 * @author Peter Robinson
 * @version 0.0.1 (7/22/17).
 */
public class FASTAIndexer {

    private String path=null;

    private String contigname=null;
    private long n_bases;
    private long byte_index;
    private long bases_per_line;
    private long bytes_per_line;

    public String getContigname() {
        return contigname;
    }

    public long getN_bases() {
        return n_bases;
    }

    public long getByte_index() {
        return byte_index;
    }

    public long getBases_per_line() {
        return bases_per_line;
    }

    public long getBytes_per_line() {
        return bytes_per_line;
    }

    public String getPath() {

        return path;
    }

    /**
     *
     * @param path Path to the FASTA file to be indexed.
     */
    public FASTAIndexer(String path){
        this.path=path;
    }


    public void writeFASTAIndex()  throws IOException{
            String outname=String.format("%s%s.fai",path, File.separator);
            BufferedWriter bw = new BufferedWriter(new FileWriter(outname));
            bw.write(String.format("%s\t%d\t%d\t%d\t%d",contigname,n_bases,byte_index,bases_per_line,bytes_per_line));
            bw.close();
    }


    /**
    * Extract data for a FASTA index from the file passed to the constructor that is stored
     * in {@link #path}.
     */
    public void createFASTAindex() throws IOException {
        RandomAccessFile file = new RandomAccessFile(this.path, "r");
        file.seek(0);
        String header = file.readLine();
        if (!header.startsWith(">")) {
            System.err.println("[ERROR] FASTA header line did not start with >: " + header);
            return;
        }
        this.contigname = header.substring(1);
        this.byte_index = file.getFilePointer(); /* this is the offset in bytes of the first sequence line. */
        String line = file.readLine();
        this.bases_per_line = line.length();
        long offset = file.getFilePointer();
        this.bytes_per_line = offset - this.byte_index;
        this.n_bases = bases_per_line;
        while ((line = file.readLine()) != null) {
            this.n_bases += line.length();
        }
        file.close();
    }

}
