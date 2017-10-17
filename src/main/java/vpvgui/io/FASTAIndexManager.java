package vpvgui.io;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.genome.Genome;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The purpose of this class is to encapsulte the FASTA Indexer in a Task.
 */
public class FASTAIndexManager extends Task<Void> {

    static Logger logger = Logger.getLogger(FASTAIndexManager.class.getName());
    /** Path to the directory where we will download and decompress the genome file. */
    private String genomeDirectoryPath=null;
    /** Model of the current genome we are downloading. */
    private Genome genome;

    public FASTAIndexManager(Model model, ProgressIndicator pi) {
        this.genomeDirectoryPath=model.getGenomeDirectoryPath();
        logger.trace("Initializing fasta indexing at directory "+this.genomeDirectoryPath);
        this.progress=pi;
        this.genome=model.getGenome();
        this.indexedFastaFiles=new HashMap<>();
        this.contigLengths=new HashMap<>();
    }

    /** Key: name of chromosome or contig; value-absolute path to the corresponding FASTA file on disk. */
    private Map<String,String> indexedFastaFiles;
    /** Key:Name of a chromosome (or in general, of a contig). Value: length in nucleotides */
    private Map<String,Integer> contigLengths;
    /** The progress indicator on the GUI that will show progress of indexing. */
    private ProgressIndicator progress=null;
    /** The label on the GUI that will show the status/result of the indexing operation. */


    public Map<String,String> getIndexedFastaFiles() { return this.indexedFastaFiles; }


    public String getContigName(File file) {
        String name=file.getName();
        int i=name.indexOf(".fa");
        if (i>0)
            return name.substring(0,i);
        else
            return name;
    }

    /** Create FAI indices for all FASTA files in {@link #genomeDirectoryPath} (only if needed).
     * It is packaged as a Task to allow concurrency*/
    @Override
    protected Void call() {
        final File genomeDirectory = new File(this.genomeDirectoryPath);
        int n=genomeDirectory.listFiles().length;
        double blocksize=1.0/(double)n;
        double totalprogress=0d;
        this.progress.setProgress(0.0);
        for (final File fileEntry : genomeDirectory.listFiles()) {
            String contigname=null;
            if (fileEntry.isDirectory()) {
                continue;
            } else if (fileEntry.getName().contains("random")) {
                continue; /* skip random contigs! */
            } else if (!fileEntry.getPath().endsWith(".fa")) {
                continue;
            } else if (fastaFAIalreadyExists(fileEntry.getAbsolutePath())) {
                contigname=getContigName(fileEntry);
                this.indexedFastaFiles.put(contigname,fileEntry.getAbsolutePath());
                logger.trace(String.format("Fasta file: %s (basename: %s): Size-%d ",
                        fileEntry.getAbsolutePath(),
                        contigname,
                        this.indexedFastaFiles.size()));
                continue;
            } else {
                    /* if we get here, we have a FASTA file ending with ".fa" that has not yet been indexed */
                try {
                    FASTAIndexer indexer=new FASTAIndexer(fileEntry.getAbsolutePath());
                    indexer.createFASTAindex();
                    indexer.writeFASTAIndex();
                    contigname=indexer.getContigname();
                    long len=indexer.getN_bases();
                    this.contigLengths.put(contigname,(int)len);
                    totalprogress+=blocksize;
                    progress.setProgress(totalprogress);
                } catch (IOException e) {
                    logger.error("Error encountered while indexing FASTA files");
                    logger.error(e,e);
                }
                logger.trace("Adding map entry: "+contigname+": "+fileEntry.getAbsolutePath());
                this.indexedFastaFiles.put(contigname,fileEntry.getAbsolutePath());
            }
        }
        progress.setProgress(1.0);
        return null;
    }


    public Map<String,Integer> getContigLengths () { return this.contigLengths; }




    /** Return true if the FASTA index is found already -- no need to repeat! */
    private boolean fastaFAIalreadyExists(String path) {
        File f=new File(String.format("%s.fai",path));
        return f.exists();
    }

}
