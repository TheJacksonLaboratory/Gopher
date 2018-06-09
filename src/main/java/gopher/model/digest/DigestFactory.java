package gopher.model.digest;


import com.google.common.collect.ImmutableList;
import gopher.exception.GopherException;
import gopher.model.Model;
import gopher.model.RestrictionEnzyme;
import gopher.model.viewpoint.Segment;
import gopher.model.viewpoint.ViewPoint;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class to perform in silico digestion of genome FASTA files. This class produces an export file that
 * can be used by downstream analysis programs.
 * <p>The format of the output program is as follows:</p>
 * <ol>
 *     <li>Chromosome</li>
 *     <li>Fragment_Start_Position  (one-based, inclusive)</li>
 *     <li>Fragment_End_Position  (one-based, inclusive)</li>
 *     <li>Fragment_Number (can be used to search for adjacent fragments)</li>
 *     <li>5'_Restriction_Site</li>
 *     <li>3'_Restriction_Site</li>
 *     <li>Length</li>
 *     <li>GC content of 5' margin</li>
 *     <li>GC content of 3' margin</li>
 *     <li>Repeat content of 5' margin</li>
 *     <li>Repeat content of 3' margin</li>
 *     <li>Active/inactive flag</li>
 *     <li>Number of probes (upstream;downstream)</li>
 * </ol>

 *This means we will use the following format TODO update
 * <pre>
 * Genome:testgenome       Restriction_Enzyme1:BgIII [A^GATCT]     Restriction_Enzyme2:None        Hicup digester version 0.5.10
 * Chromosome      Fragment_Start_Position Fragment_End_Position   Fragment_Number RE1_Fragment_Number     5'_Restriction_Site     3'_Restriction_Site
 * chrUn_KI270745v1        1       1861    1       1       None    Re1
 * chrUn_KI270745v1        1862    29661   2       2       Re1     Re1
 * chrUn_KI270745v1        29662   35435   3       3       Re1     Re1
 * chrUn_KI270745v1        35436   40296   4       4       Re1     Re1
 * chrUn_KI270745v1        40297   41891   5       5       Re1     None
 * </pre>
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @author <a href="mailto:peter.hansen@charite.de">Peter Hansen</a>
 * @version 0.1.2
 */
public class DigestFactory {
    private static final Logger logger = LogManager.getLogger(DigestFactory.class.getName());
    /** List of restriction enzyme objects representing the enzymes that were used in the capture Hi-C experiment. */
    private final List<RestrictionEnzyme> restrictionEnzymeList;
    /** key: index of enzyme; value: name of enzyme (Note: usually, we just have one enzyme!). Symmetrical with {@link #enzyme2number}).*/
    private Map<Integer,RestrictionEnzyme> number2enzyme;
    /** key: name of enzyme; value: index of enzyme (Note: usually, we just have one enzyme!). Symmetrical with {@link #number2enzyme}).*/
    private Map<RestrictionEnzyme,Integer> enzyme2number;
    /** Path to the combined FASTA file with all (or all canonical) chromosomes. */
    private final String genomeFastaFilePath;
    /** File handle for the output of the restriction fragments. */
    private BufferedWriter out = null;
    /** size of margin of fragments used for calculating GC and fivePrimeRepeatContent content. */
    private final int marginSize;
    /** Name of output file. */
    private final String outfilename;
    /** Fields of the header of the output file. */
    private final String[] headerFields = {
            "Chromosome",
            "Fragment_Start_Position",
            "Fragment_End_Position",
            "Fragment_Number",
            "5'_Restriction_Site",
            "3'_Restriction_Site",
            "Length",
            "5'_GC_Content",
            "3'_GC_Content",
            "5'_Repeat_Content",
            "3'_Repeat_Content",
            "Selected",
            "5'_Probes",
            "3'_Probes"
    };
    /** Header of the output file. */
    private final String HEADER= Arrays.stream(headerFields).collect(Collectors.joining("\t"));
    /** Binary tree of active {@link Segment}.*/
    private BinaryTree btree;


    /**
     * This constructor extraqcts several items from the Model:
     * Margin size (which is used to calculate GC and fivePrimeRepeatContent content);
     * The list of chosen restriction enzymes;
     * THe list of chosen viewpoints.
     * @param genomeFastaFile path to the combined FASTA file with all (or all canonical) chromosomes.
     * @param outfile name of output file
     * @param model Reference to the model
     */
    public DigestFactory(String genomeFastaFile, String outfile, Model model) {
        int msize = model.getMarginSize();
        this.restrictionEnzymeList = model.getChosenEnzymelist();
        this.genomeFastaFilePath=genomeFastaFile;
        outfilename=outfile;
        logger.trace(String.format("FragmentFactory initialize with FASTA file=%s",genomeFastaFile));
        marginSize=msize;
        extractChosenSegments(model);
    }


    private void extractChosenSegments(Model model) {
        btree = new BinaryTree();
        //A list of Viewpoints that contain at least one selected digest.
        List<ViewPoint> vplist = model.getActiveViewPointList();
        for (ViewPoint vp : vplist) {
            String chrom = vp.getReferenceID();
            List<Segment> seglist = vp.getActiveSegments();
            for (Segment seg : seglist) {
                btree.add(seg);
            }
        }
    }



    /** @return the path of the multi-chromosome, single FASTA file used to calculate the digest. */
    String getGenomeFastaFilePath() {
        return genomeFastaFilePath;
    }


    /**
     *
     * @param enzymes A list of strings representing the enzymes.
     * @throws GopherException If an invalid String is passed that does not match  of the allowed enzymes
     */
    public void digestGenome(List<String> enzymes) throws GopherException {
        this.number2enzyme =new HashMap<>();
        this.enzyme2number=new HashMap<>();
        int n=0;
        for (String enzym : enzymes) {
            RestrictionEnzyme re = restrictionEnzymeList.stream().
                    filter( x ->  enzym.equalsIgnoreCase(x.getName()) ).
                    findFirst().orElse(null);
            if (re==null) {
                throw new GopherException(String.format("Did not recognize restriction enzyme \"%s\"",enzym));
            } else {
                n++;
                number2enzyme.put(n,re);
                enzyme2number.put(re,n);
            }
        }
        try {
            out = new BufferedWriter(new FileWriter(outfilename));
            out.write(HEADER + "\n");
            cutChromosomes(this.genomeFastaFilePath, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GopherException(String.format("Could not digest chromosomes: %s", e.toString()));
        }

    }




    private int counter=1;
    /** This will cut all of the chromosomes in the multi-FASTA chromosome file. */
    private void cutChromosomes(String chromosomeFilePath, BufferedWriter out) throws Exception {
        logger.trace(String.format("cutting chromosomes %s",chromosomeFilePath ));
        IndexedFastaSequenceFile fastaReader;
        try {
             fastaReader = new IndexedFastaSequenceFile(new File(chromosomeFilePath));
        } catch (Exception e) {
            throw  new GopherException(String.format("Could not find FAI file for %s [%s]",chromosomeFilePath,e.toString()));
        }

        ReferenceSequence refseq;
        while ((refseq=fastaReader.nextSequence())!=null) {
            String seqname = refseq.getName();
            // note fastaReader refers to one-based numbering scheme.
            String sequence = fastaReader.getSequence(seqname).getBaseString();
            //ReferenceSequence refseq = fastaReader.nextSequence();
            logger.trace(String.format("Cutting %s (length %d)",seqname,sequence.length() ));
            cutOneChromosome(seqname, sequence);
        }

    }

    /**
     *
     * @param scaffoldName name of chromosome or alt scaffold
     * @param sequence DNA sequence of the chromosome
     * @throws IOException can be thrown by the BufferedWriter.
     */
    private void cutOneChromosome(String scaffoldName,String sequence) throws IOException {
        ImmutableList.Builder<Digest> builder = new ImmutableList.Builder<>();
        for (Map.Entry<RestrictionEnzyme,Integer> ent : enzyme2number.entrySet()) {
            int enzymeNumber = ent.getValue();
            RestrictionEnzyme enzyme = ent.getKey();
            String cutpat = enzyme.getPlainSite();
            int offset = enzyme.getOffset();
            Pattern pattern = Pattern.compile(cutpat,Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sequence);
            /* one-based position of first nucleotide in the entire subsequence returned by fasta reader */
            while (matcher.find()) {
                int pos = matcher.start() + offset; /* one-based position of first nucleotide after the restriction enzyme cuts */
                if (counter%1000==0) {
                    System.out.println(String.format("Added %d th digest",counter ));
                }
                builder.add(new Digest(enzymeNumber,pos));
            }
        }
        ImmutableList<Digest> fraglist = ImmutableList.sortedCopyOf(builder.build());
        String previousCutEnzyme="None";
        Integer previousCutPosition=0; // start of chromosome
        //Header

         int n=0;
        for (Digest f:fraglist) {
            int startpos= (previousCutPosition+1);
            int endpos = f.position;
            // Note: to get subsequence, decrement startpos by one to get zero-based numbering
            // leave endpos as is--it is one past the end in zero-based numbering.
            String subsequence=sequence.substring(startpos-1,endpos);
            Result result = getGcAndRepeat(subsequence, this.marginSize);
            boolean selected = btree.containsNode(scaffoldName,f.position);
            out.write(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%s\t%d\t%d\n",
                    scaffoldName,
                    startpos,
                    endpos,
                    (++n),
                    previousCutEnzyme,
                    number2enzyme.get(f.enzymeNumber).getName(),
                    result.getLen(),
                    result.getFivePrimeGcContent(),
                    result.getThreePrimeGcContent(),
                    result.getFivePrimeRepeatContent(),
                    result.getThreePrimeRepeatContent(),
                    selected ? "T" : "F",
                    -1,
                    -1));
            previousCutEnzyme=number2enzyme.get(f.enzymeNumber).getName();
            previousCutPosition=f.position;
        }
        // output last digest also
        // No cut ("None") at end of chromosome
        int endpos = sequence.length();
        int startpos= (previousCutPosition+1);
        // Note: to get subsequence, decrement startpos by one to get zero-based numbering
        // leave endpos as is--it is one past the end in zero-based numbering.
        String subsequence=sequence.substring(startpos-1,endpos);
        Result result = getGcAndRepeat(subsequence,marginSize);
        out.write(String.format("%s\t%d\t%d\t%d\t%s\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%s\t%d\t%d\n",
                scaffoldName,
                (previousCutPosition+1),
                endpos,
                (++n),
                previousCutEnzyme,
                "None",
                result.getLen(),
                result.getFivePrimeGcContent(),
                result.getThreePrimeGcContent(),
                result.getFivePrimeRepeatContent(),
                result.getThreePrimeRepeatContent(),
                "?-selected",
                -1,
                -1));
    }


    /**
     * This is a convenience class for calculating and organizing results of G/C and repeat analysis.
     */
    static class Result {
        private int len;
        /** G?C content in the 5' portion of the fragment (as defined by the margin size). */
        private double fivePrimeGcContent;
        /** G/C content in the 3' portion of the fragment (as defined by the margin size). */
        private double threePrimeGcContent;
        /** Repeat content in the 5' portion of the fragment (as defined by the margin size). */
        private double fivePrimeRepeatContent;
        /** Repeat content in the 3' portion of the fragment (as defined by the margin size). */
        private double threePrimeRepeatContent;

        private Result(int length, int GCcount5, int GCcount3, int repeatCount5, int repeatCount3) {
            this.len=length;
            if (len==0) { return; }
            fivePrimeGcContent =(double)GCcount5/len;
            threePrimeGcContent = (double)GCcount5/len;
            fivePrimeRepeatContent =(double)repeatCount5/len;
            threePrimeRepeatContent = (double)repeatCount3/len;
        }

        int getLen() { return len; }
        double getFivePrimeGcContent() { return fivePrimeGcContent; }
        double getThreePrimeGcContent() { return threePrimeGcContent; }
        double getFivePrimeRepeatContent() { return fivePrimeRepeatContent; }
        double getThreePrimeRepeatContent() { return threePrimeRepeatContent; }
    }

    private Result getGcAndRepeat(String subsequence, int marginSize) {
        int len=subsequence.length();
        int repeat5=0;
        int repeat3=0;
        int gc5=0;
        int gc3=0;
        for (int i=0;i<len;++i) {
            switch (subsequence.charAt(i)) {
                case 'a' :
                case 't' :
                    if (i<marginSize) repeat5++;
                    if  ((len-marginSize)<=i) repeat3++;
                    break;
                case 'c' :
                case 'g' :
                    if (i<marginSize) repeat5++;
                    if  ((len-marginSize)<=i) repeat3++;
                    // don't break because we also need to count G/C here
                case 'C' :
                case 'G' :
                    if (i<marginSize) gc5++;
                    if  ((len-marginSize)<=i) gc3++;
                    break;
            }
        }
        return new Result(len,gc5,gc3,repeat5,repeat3);
    }


}
