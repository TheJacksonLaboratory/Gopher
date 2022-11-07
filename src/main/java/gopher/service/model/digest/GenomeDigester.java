package gopher.service.model.digest;

import com.google.common.collect.ImmutableList;
import gopher.service.GopherService;
import gopher.service.model.RestrictionEnzyme;
import gopher.service.model.viewpoint.Segment;
import gopher.service.model.viewpoint.ViewPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenomeDigester {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenomeDigester.class.getName());
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
    private BinaryTree btree;

    public GenomeDigester(GopherService model) {
        this.marginSize = model.getMarginSize();
        this.restrictionEnzymeList = model.getChosenEnzymelist();
        this.genomeFastaFilePath = model.getGenomeFastaFile();
        LOGGER.trace(String.format("GenomeDigester initializes with FASTA file=%s", this.genomeFastaFilePath));
        extractChosenSegments(model.getViewPointList());
    }


    public GenomeDigester(RestrictionEnzyme enzyme, String genomeFasta, List<ViewPoint> vplist, int marginSize) {
        this.restrictionEnzymeList = List.of(enzyme);
        this.genomeFastaFilePath = genomeFasta;
        this.marginSize = marginSize;
        extractChosenSegments(vplist);
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

    /**
     * @param scaffoldName name of chromosome or alt scaffold
     * @param sequence     DNA sequence of the chromosome
     * @throws IOException can be thrown by the BufferedWriter.
     */
    public List<DetailedDigest> cutOneChromosome(String scaffoldName, String sequence) throws IOException {
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
            Integer baitNumUp = 0;
            Integer baitNumDown = 0;
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
        Integer baitNumUp = 0;
        Integer baitNumDown = 0;
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
