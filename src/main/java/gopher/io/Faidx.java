package gopher.io;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.log4j.Logger;
import gopher.exception.UnindexableFastaFileException;
import gopher.exception.GopherException;
import gopher.model.Model;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.*;

/**
 * Coordinates the task of creating a FAI index for the genome fasta file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2018-02-15)
 */
public class Faidx extends Task<Void> {
    private static Logger logger = Logger.getLogger(Faidx.class.getName());
    /**  Path to the directory where we will search for the genome FASTA file and produce the faidx file. */
    private final String genomeDirectoryPath;
    /** Basename of the genome fasta file, e.g., hg19.fa */
    private final String genomeFastaBaseName;
    /** Basename of the genome fasta index file, e.g., hg19.fa.fai */
    private final String genomeFastaIndexBaseName;
    /** Number of canonical chromosomes, e.g., chr1 and not chr3random123. */
    private final int n_canonical_chromosomes;
    /** Key: Name of a chromosome (or in general, of a contig). Value: length in nucleotides. */
    private Map<String, Integer> contigLengths;
    /** The progress indicator on the GUI that will show progress of indexing. */
    private final ProgressIndicator progress;

    public Faidx(Model model, ProgressIndicator pi) {
        this.genomeDirectoryPath = model.getGenomeDirectoryPath();
        this.genomeFastaBaseName =model.getGenome().getGenomeFastaName();
        this.genomeFastaIndexBaseName =genomeFastaBaseName + ".fai"; // fai suffix
        logger.trace("Initializing fasta indexing at directory " + this.genomeDirectoryPath);
        this.progress = pi;
        this.n_canonical_chromosomes = model.getGenome().getNumberOfCanonicalChromosomes();
        this.contigLengths = new HashMap<>();
    }
    public Faidx(String genomeDirPath, String basename, int n_chroms) {
        this.genomeDirectoryPath=genomeDirPath;
        this.genomeFastaBaseName = basename;
        this.genomeFastaIndexBaseName =genomeFastaBaseName + ".fai"; // fai suffix
        logger.trace("Initializing fasta indexing at directory " + this.genomeDirectoryPath);
        this.n_canonical_chromosomes=n_chroms;
        this.progress=null; // signal not to use the Progress Indicator
        this.contigLengths=new HashMap<>();
    }


    /**
     * We use this method to check if we need to g-unzip the genome files.
     * @return true if the hg19.fa file is found (and thus, the chromFa.tar.gx has been previously extracted)
     */
    private boolean alreadyIndexed(String pathToFasta) {
        String PathToFai=pathToFasta + ".fai";
        File f = new File(PathToFai);
        logger.trace("checking for existence of file " + f.getAbsolutePath());
        return f.exists();
    }

    /**
     * Create FAI indices for all FASTA files in {@link #genomeDirectoryPath} (only if needed).
     * It is packaged as a Task to allow concurrency
     */
    @Override
    protected Void call() throws GopherException {
        String path = genomeDirectoryPath + File.separator + genomeFastaBaseName;
        if (alreadyIndexed(path)) {
            logger.trace("We found index for " + path +" and are skipping the FAI indexing step");
            updateProgress(1.00);
            return null;
        }
        File fasta = new File(path);
        double file_size = fasta.length();
        double currentProgress=0.0D;
        updateProgress(currentProgress);

        try {
            FileChannel fileChannel = FileChannel.open(Paths.get(fasta.getAbsolutePath()));
            int noOfBytesRead = 0;
            StringBuilder sb = new StringBuilder();

            boolean isFirstSeqLine = false;
            long currOffset = 0;
            long prevOffset = 0;
            boolean isLast = false; // True when line is expected to be the last one of sequence

            Set<String> seqNames = new HashSet<>();
            List<FastaIndexEntry> records = new ArrayList<>();
            FastaIndexEntry faidxEntry = null;

            while (noOfBytesRead != -1) {
                ByteBuffer buffer = ByteBuffer.allocate(100000);
                noOfBytesRead = fileChannel.read(buffer);
                buffer.flip();

                while (buffer.hasRemaining()) {
                    char x = (char) buffer.get();
                    currOffset++;
                    sb.append(x);
                    if (x == '\n') { // One full line read.
                        String line = sb.toString();
                        sb.setLength(0);
                        if (line.trim().isEmpty()) {
                            isLast = true;
                            continue;
                        }
                        if (line.startsWith(">")) {
                            isLast = false;
                            if (faidxEntry != null) {
                                records.add(faidxEntry);
                            }
                            updateProgress(Math.min(0.99, currOffset/file_size));
                            faidxEntry = new FastaIndexEntry();
                            faidxEntry.makeSeqNameFromRawLine(line);
                            logger.trace("Indexing " + faidxEntry.getSeqName());

                            if (seqNames.contains(faidxEntry.getSeqName())) {
                                String err = fasta.getAbsolutePath() + ": Duplicate sequence name found for " + faidxEntry.getSeqName();
                                throw new UnindexableFastaFileException(err);
                            } else {
                                seqNames.add(faidxEntry.getSeqName());
                            }
                            faidxEntry.byteOffset = currOffset;
                            isFirstSeqLine = true;
                        } else {
                            if (isLast) {
                                String err = fasta.getAbsolutePath() + ": Different line length in " + faidxEntry.getSeqName();
                                throw new UnindexableFastaFileException(err);
                            }
                            int seqLen = line.replaceAll("\\s", "").length();
                            faidxEntry.seqLength += seqLen;
                            if (isFirstSeqLine) {
                                faidxEntry.lineLength = seqLen;
                                faidxEntry.lineFullLength = (int) (currOffset - prevOffset);
                                isFirstSeqLine = false;
                            } else if (faidxEntry.lineLength != seqLen) {
                                isLast = true;
                            }
                        }
                        prevOffset = currOffset;
                    } // End of processing one full line
                } // End reading chunk of bytes
            } // End of reading channel.

            records.add(faidxEntry); // Add last record

            // Write out index
            String faipath = getGenomeFastaIndexPath();
            BufferedWriter wr = new BufferedWriter(new FileWriter(new File(faipath)));
            for (FastaIndexEntry rec : records) {
                wr.write(rec.toString() + "\n");
                // also record the contig lengths
                String contigname=rec.getSeqName();
                int len=rec.seqLength;
                this.contigLengths.put(contigname,len);
            }
            wr.close();
            updateProgress(1.0D);
        } catch (IOException ioe) {
            throw new GopherException(ioe.getMessage());
        }

        return null;
    }


    /** Update the progress bar of the GUI in a separate thread.
     * @param pr Current progress.
     */
    private void updateProgress(double pr) {
        if (progress==null) { // can run the class without a PI
            return;
        }
        javafx.application.Platform.runLater( () -> progress.setProgress(pr) );
    }

    public Map<String,Integer> getContigLengths () { return this.contigLengths; }

    /** Return true if the FASTA index is found already -- no need to repeat! */
    private boolean fastaFAIalreadyExists(String path) {
        File f=new File(String.format("%s.fai",path));
        return f.exists();
    }

    public String getGenomeFastaIndexPath() {
        return genomeDirectoryPath + File.separator + genomeFastaIndexBaseName;
    }

    public boolean genomeFileExists() {
        String path= genomeDirectoryPath + File.separator + genomeFastaBaseName;
        File f = new File(path);
        return f.exists();
    }

}

