package vpvgui.model.project;

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

import java.io.File;

/**
 * This class retrieves the location and the DNA sequence of
 * an arbitrary DNA fragment (the fragment could be a viewpoint,
 * and the ViewPoint objects can be initialized with Fragment objects of desired).
 * It assumes that there is a directory where each chromosome is a separate file.
 */
public class Fragment {

    /** Object representing the fasta index file */
    private FastaSequenceIndex fai=null;

    /** Object representing the fasta file */
    private IndexedFastaSequenceFile fa =null;

    /** Use this to keep track of the current chromosome as we are iterating over
     variants. */
    private String currentChromosome="";

    /**
     * The constructor is passed the location of a directory that
     * contains exactly one FASTA file (ending: *.fa or *.fasta) that
     * represents the human genome together with the correcponding
     * fasta index file (fai).
     */
    private String directory;




    /**
     *
     * @param directoryPath path to the directory where the genome file is located.
     */
    public Fragment(String directoryPath) {

        this.directory=directoryPath;
    }

    public  String getDirectoryPath() { return directory; }

    public FastaSequenceIndex getFai() {
        return fai;
    }

    public String getFastaAbsolutePath() {
        return fa.toString();
    }

    public String getCurrentChromosome() {
        return currentChromosome;
    }

    public void setCurrentChromosome(String chrom) {
        this.currentChromosome=chrom;
        openChromosomeFaAndFai(chrom);

    }



    /**
     * This function is used to construct the fasta and the fai objects that
     * the HTSJDK library will use to extract the sequence. It initializes the
     * class variables {@link #fai} and {@link #fa}.
     * <p>
     */
    private void openChromosomeFaAndFai(String chrom) {
        String faPath= String.format("%s/%s.fa",this.directory,chrom);
        String faiPath=String.format("%s.fai",faPath);
        this.fai = new FastaSequenceIndex(new File(faiPath));
        this.fa = new IndexedFastaSequenceFile(new File(faPath), fai);
    }

    public String getGenomicReferenceSequence(String chrString, long from_pos, long to_pos) {
        ReferenceSequence rf = fa.getSubsequenceAt(chrString, from_pos, to_pos);
        //System.out.println("RF=" + rf);
        byte[] t = rf.getBases();
        String reference = new String(t);
        return reference.toUpperCase();
    }
}
