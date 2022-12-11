package gopher.service.model.digest;

import gopher.service.model.RestrictionEnzyme;
import gopher.service.model.viewpoint.ViewPoint;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This is intended to be used to assay the quality of baited digests for all protein coding
 * genes and for each of the 19 restriction enzymes in turn.
 */
public class SingleEnzymeDigestCreationTask extends Task<List<DetailedDigest>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleEnzymeDigestCreationTask.class.getName());
    /**
     * Path to the combined FASTA file with all (or all canonical) chromosomes.
     */
    private final String genomeFastaFilePath;
    /**
     * Object that creates restiction digests.
     */
    private final GenomeDigester digester;


    public SingleEnzymeDigestCreationTask(RestrictionEnzyme enzyme, String genomeFasta, List<ViewPoint> vplist, int marginSize) {
        this.genomeFastaFilePath = genomeFasta;
        digester = new GenomeDigester(enzyme, genomeFasta, vplist, marginSize);
    }


    @Override
    protected List<DetailedDigest> call() throws Exception {
        updateProgress(1, 100);
        List<DetailedDigest> allDetailedDigestList = new ArrayList<>();
        try (IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(genomeFastaFilePath))) {
            String msg = String.format("Indexing Fasta file %s", genomeFastaFilePath);
            long referenceLength = fastaReader.getSequenceDictionary().getReferenceLength();
            long cumulativeLength = 0;
            LOGGER.trace(msg);
            updateProgress(10, 100);
            ReferenceSequence refseq;
            while ((refseq = fastaReader.nextSequence()) != null) {
                if (isCancelled()) // true if user has cancelled the task
                    return null;
                String seqname = refseq.getName();
                String sequence = fastaReader.getSequence(seqname).getBaseString();
                cumulativeLength += sequence.length();
                updateMessage(String.format("Digesting %s", seqname));
                // how much of all chromosomes have we digested so far?
                long current = 100 * cumulativeLength / referenceLength;
                updateProgress(current, 100);
                List<DetailedDigest> detailedDigestList = digester.cutOneChromosome(seqname, sequence);
                allDetailedDigestList.addAll(detailedDigestList);
            }
        }
        return allDetailedDigestList;
    }


}
