package gopher.service.model.digest;


import com.google.common.collect.ImmutableList;
import gopher.exception.GopherException;
import gopher.gui.factories.PopupFactory;
import gopher.service.GopherService;
import gopher.service.model.RestrictionEnzyme;
import gopher.service.model.viewpoint.Segment;
import gopher.service.model.viewpoint.ViewPoint;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to perform in silico digestion of genome FASTA files. This class produces an export file that
 * can be used by downstream analysis programs.
 * <p>The format of the output program is as follows:</p>
 * <ol>
 *     <li>Chromosome</li>
 *     <li>Digest_Start_Position  (one-based, inclusive)</li>
 *     <li>Digest_End_Position  (one-based, inclusive)</li>
 *     <li>Digest_Number (can be used to search for adjacent fragments)</li>
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
 * <p>
 * This means we will use the following format TODO update
 * <pre>
 * Genome:testgenome       Restriction_Enzyme1:BgIII [A^GATCT]     Restriction_Enzyme2:None        Hicup digester version 0.5.10
 * Chromosome      Digest_Start_Position Fragment_End_Position   Digest_Number RE1_Fragment_Number     5'_Restriction_Site     3'_Restriction_Site
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
public class DigestCreationTask extends Task<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DigestCreationTask.class.getName());
    /**
     * List of restriction enzyme objects representing the enzymes that were used in the capture Hi-C experiment.
     */
    private final List<RestrictionEnzyme> restrictionEnzymeList;
    /**
     * key: index of enzyme; value: name of enzyme (Note: usually, we just have one enzyme!). Symmetrical with {@link #enzyme2number}).
     */
    private Map<Integer, RestrictionEnzyme> number2enzyme;
    /**
     * key: name of enzyme; value: index of enzyme (Note: usually, we just have one enzyme!). Symmetrical with {@link #number2enzyme}).
     */
    private Map<RestrictionEnzyme, Integer> enzyme2number;
    /**
     * Path to the combined FASTA file with all (or all canonical) chromosomes.
     */
    private final String genomeFastaFilePath;
    /**
     * size of margin of fragments used for calculating GC and fivePrimeRepeatContent content.
     */
    private final int marginSize;
    /**
     * Name of output file.
     */
    private final String outfilename;
    /**
     * Header of the output file.
     */
    private final String HEADER = String.join("\t", DetailedDigest.headerFields());
    /**
     * Binary tree of active {@link Segment}.
     */
    private BinaryTree btree;

    /**
     * This constructor extraqcts several items from the Model:
     * Margin size (which is used to calculate GC and fivePrimeRepeatContent content);
     * The list of chosen restriction enzymes;
     * THe list of chosen viewpoints.
     *
     * @param outfile name of output file
     * @param model   Reference to the model
     */
    public DigestCreationTask(String outfile, GopherService model) {
        this.marginSize = model.getMarginSize();
        this.restrictionEnzymeList = model.getChosenEnzymelist();
        this.genomeFastaFilePath = model.getGenomeFastaFile();
        outfile += model.getProjectName(true); // remove ".ser" suffix
        outfile += "_";
        outfile += model.getGenomeBuild();
        outfile += "_";
        outfile +=  model.getAllSelectedEnzymeString().replaceAll(";", "_"); // mutliple enzymes would be separated by semicolon
        outfile += "_digests.txt";
        outfilename = outfile;
        LOGGER.trace(outfilename);
        LOGGER.trace(String.format("Digest Factory initialize with FASTA file=%s", this.genomeFastaFilePath));
        extractChosenSegments(model.getViewPointList());
    }


    /**
     *  The coordinates of the fragments correspond to coordinates in digest file
     *  segments are one based, digests in file are one based
     * @param vplist A list of Viewpoints that contain at least one selected digest.
     */
    private void extractChosenSegments(List<ViewPoint> vplist) {
        this.btree = new BinaryTree();
        for (ViewPoint vp : vplist) {
            List<Segment> seglist = vp.getActiveSegments();
            for (Segment seg : seglist) {
                btree.add(seg);
            }
        }
    }

    public Void call() {
        updateTitle("Creating Digest file");
        updateMessage("Creating binary tree of selected fragments...");
        LOGGER.trace(String.format("We got a total of %d chosen segments in the binary tree",
                this.btree.getN_nodes()));
        try {
            digestGenome();
        } catch (GopherException ge) {
            PopupFactory.displayException("Digest error", "Exception encountered while processing digest", ge);
        }
        return null;
    }


    /**
     * @throws GopherException If an null restriction enzyme is passed that does not match  of the allowed enzymes
     */
    private void digestGenome() throws GopherException {
        this.number2enzyme = new HashMap<>();
        this.enzyme2number = new HashMap<>();
        int n = 0;
        for (RestrictionEnzyme re : this.restrictionEnzymeList) {
            if (isCancelled()) // true if user has cancelled the task
                return;
            if (re == null) {
                throw new GopherException("Got null restriction enzyme");
            } else {
                n++;
                number2enzyme.put(n, re);
                enzyme2number.put(re, n);
            }
        }
        try {
            /**
             * File handle for the output of the restriction fragments.
             */
            BufferedWriter out = new BufferedWriter(new FileWriter(outfilename));
            out.write(HEADER + "\n");
            List<DetailedDigest>  detailedDigestList = cutChromosomes(this.genomeFastaFilePath);
            out.close();
            LOGGER.error("Completed writing digest file {}", outfilename);
        } catch (Exception e) {
            e.printStackTrace();
            throw new GopherException(String.format("Could not digest chromosomes: %s", e));
        }
    }


    private int counter = 1;

    /**
     * This will cut all of the chromosomes in the multi-FASTA chromosome file.
     */
    private List<DetailedDigest>  cutChromosomes(String chromosomeFilePath) throws Exception {
        updateProgress(1, 100);
        List<DetailedDigest> allDetailedDigestList = new ArrayList<>();
        try (IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(chromosomeFilePath))) {
            String msg = String.format("Indexing Fasta file %s", chromosomeFilePath);
            LOGGER.trace(msg);
            updateProgress(10, 100);
            ReferenceSequence refseq;
            long current = 15;
            while ((refseq = fastaReader.nextSequence()) != null) {
                if (isCancelled()) // true if user has cancelled the task
                    return List.of();
                String seqname = refseq.getName();
                // note fastaReader uses the one-based numbering scheme.
                String sequence = fastaReader.getSequence(seqname).getBaseString();
                LOGGER.trace(String.format("Cutting %s (length %d)", seqname, sequence.length()));
                updateMessage(String.format("Digesting %s", seqname));
                if (current > 90) {
                    long diff = 100 - current;
                    current += 0.3 * diff;
                } else {
                    current += 5;
                }
                updateProgress(current, 100);
                List<DetailedDigest> detailedDigestList = cutOneChromosome(seqname, sequence);
                allDetailedDigestList.addAll(detailedDigestList);
            }
        } catch (Exception e) {
            throw new GopherException(String.format("Could not find FAI file for %s [%s]", chromosomeFilePath, e));
        }
        updateProgress(100, 100);
        return allDetailedDigestList;
    }

    /**
     * @param scaffoldName name of chromosome or alt scaffold
     * @param sequence     DNA sequence of the chromosome
     */
    private List<DetailedDigest> cutOneChromosome(String scaffoldName, String sequence) {
        ImmutableList.Builder<Digest> builder = new ImmutableList.Builder<>();
        for (Map.Entry<RestrictionEnzyme, Integer> ent : enzyme2number.entrySet()) {
            int enzymeNumber = ent.getValue();
            RestrictionEnzyme enzyme = ent.getKey();
            String cutpat = enzyme.getPlainSite();
            int offset = enzyme.getOffset();
            Pattern pattern = Pattern.compile(cutpat, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sequence);
            /* one-based position of first nucleotide in the entire subsequence returned by fasta reader */
            while (matcher.find()) {
                /* Note that we are trying to match the 1-based positions in SegmentFactory.
                In SegmentFactory, we use the HTSJDK IndexedFastaSequenceFile/Reader, which
                gives back one-based positions. Here, we are using a Java string, and so we
                need to add the "1" ourselves.
                 */
                int pos = matcher.start() + offset + 1;
                builder.add(new Digest(enzymeNumber, pos));
            }
        }
        ImmutableList<Digest> fraglist = ImmutableList.sortedCopyOf(builder.build());
        String previousCutEnzyme = "None";
        int previousCutPosition = 1; // start of chromosome
        //Header
        List<DetailedDigest> detailedDigestList = new ArrayList<>();
        int n = 0;
        for (Digest f : fraglist) {
            int startpos = previousCutPosition;
            int endpos = f.position - 1; // f.position is the 1-based first coordinate of the next fragment
            // Note: to get subsequence, decrement startpos by one to get zero-based numbering
            // leave endpos as is--it is one past the end in zero-based numbering.
            String subsequence = sequence.substring(startpos - 1, endpos);
            Result result = getGcAndRepeat(subsequence, this.marginSize);


            boolean selected = false;
            int baitNumUp = 0;
            int baitNumDown = 0;
            if (btree.containsNode(scaffoldName, startpos)) {
                selected = true;
                Segment seg = btree.getNode(scaffoldName, startpos).segment;
                baitNumUp = seg.getBaitNumUp();
                baitNumDown = seg.getBaitNumDown();
            }
            detailedDigestList.add(new DetailedDigest(scaffoldName,
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
                    selected,
                    baitNumUp,
                    baitNumDown));
            if (counter % 1000 == 0) {
                updateMessage(String.format("Digesting %s [%d digests so far]", scaffoldName, counter));
            }
            counter++;
            previousCutEnzyme = number2enzyme.get(f.enzymeNumber).getName();
            previousCutPosition = f.position;
        }
        // output last digest also
        // No cut ("None") at end of chromosome
        int endpos = sequence.length();
        int startpos = (previousCutPosition + 1);
        // Note: to get subsequence, decrement startpos by one to get zero-based numbering
        // leave endpos as is--it is one past the end in zero-based numbering.

        String subsequence = sequence.substring(startpos - 1, endpos);
        Result result = getGcAndRepeat(subsequence, marginSize);
        boolean selected = false;
        int baitNumUp = 0;
        int baitNumDown = 0;
        if (btree.containsNode(scaffoldName, startpos)) {
            selected = true;
            Segment seg = btree.getNode(scaffoldName, startpos).segment;
            baitNumUp = seg.getBaitNumUp();
            baitNumDown = seg.getBaitNumDown();
        }
        detailedDigestList.add(new DetailedDigest(scaffoldName,
                (previousCutPosition + 1),
                endpos,
                (++n),
                previousCutEnzyme,
                "None",
                result.getLen(),
                result.getFivePrimeGcContent(),
                result.getThreePrimeGcContent(),
                result.getFivePrimeRepeatContent(),
                result.getThreePrimeRepeatContent(),
                selected,
                baitNumUp,
                baitNumDown));
        return detailedDigestList;
    }


    /**
     * This is a convenience class for calculating and organizing results of G/C and repeat analysis.
     */
    static class Result {
        private final int len;
        /**
         * G?C content in the 5' portion of the fragment (as defined by the margin size).
         */
        private double fivePrimeGcContent;
        /**
         * G/C content in the 3' portion of the fragment (as defined by the margin size).
         */
        private double threePrimeGcContent;
        /**
         * Repeat content in the 5' portion of the fragment (as defined by the margin size).
         */
        private double fivePrimeRepeatContent;
        /**
         * Repeat content in the 3' portion of the fragment (as defined by the margin size).
         */
        private double threePrimeRepeatContent;

        private Result(int length, int GCcount5, int GCcount3, int repeatCount5, int repeatCount3) {
            this.len = length;
            if (len == 0) {
                return;
            }
            fivePrimeGcContent = (double) GCcount5 / len;
            threePrimeGcContent = (double) GCcount5 / len;
            fivePrimeRepeatContent = (double) repeatCount5 / len;
            threePrimeRepeatContent = (double) repeatCount3 / len;
        }

        int getLen() {
            return len;
        }

        double getFivePrimeGcContent() {
            return fivePrimeGcContent;
        }

        double getThreePrimeGcContent() {
            return threePrimeGcContent;
        }

        double getFivePrimeRepeatContent() {
            return fivePrimeRepeatContent;
        }

        double getThreePrimeRepeatContent() {
            return threePrimeRepeatContent;
        }
    }

    private Result getGcAndRepeat(String subsequence, int marginSize) {
        int len = subsequence.length();
        int repeat5 = 0;
        int repeat3 = 0;
        int gc5 = 0;
        int gc3 = 0;
        for (int i = 0; i < len; ++i) {
            switch (subsequence.charAt(i)) {
                case 'a':
                case 't':
                    if (i < marginSize) repeat5++;
                    if ((len - marginSize) <= i) repeat3++;
                    break;
                case 'c':
                case 'g':
                    if (i < marginSize) repeat5++;
                    if ((len - marginSize) <= i) repeat3++;
                    // don't break because we also need to count G/C here
                case 'C':
                case 'G':
                    if (i < marginSize) gc5++;
                    if ((len - marginSize) <= i) gc3++;
                    break;
            }
        }
        return new Result(len, gc5, gc3, repeat5, repeat3);
    }


}
