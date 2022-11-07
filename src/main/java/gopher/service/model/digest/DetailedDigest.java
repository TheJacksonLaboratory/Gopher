package gopher.service.model.digest;


/**
 * A POJO for the information about restriction digests and their baits (i.e., probes for capture Hi-C).
 * Note that all numbering is one-based inclusive (BED convention).
 * The fields are self-explanatory except that digestNumber is essentially a primary key with AUTO INCREMENT,
 * restrictionSite5prime (and ...3prime) are the restriction sites at both ends of the digest (sometimes,
 * multiple enzymes can be used), enrichment status indicates whether a bait was used to enrich this
 * digest in capture Hi-C, and nProbes5prime/nProbes3prime indicates the number of Baits (probes) at the
 * 5' and 3' ends of the digest (generally, these numbers will be zero or one).
 * @author  Peter N Robinson
 */
public record DetailedDigest(String chromosome,
                             int digestStartPosition,
                             int digestEndPosition,
                             int digestNumber,
                             String restrictionSite5prime,
                             String restrictionSite3prime,
                             int digestLength,
                             double gcContent5prime,
                             double gcContent3prime,
                             double repeatContent5prime,
                             double repeatContent3prime,
                             boolean enrichmentStatus,
                             int nProbes5prime,
                             int nProbes3prime) {
    @Override
    public
    String toString() {
        return String.format("%s\t%d\t%d\t%d\t%s\t%s\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%s\t%d\t%d\n",
                chromosome(),
                digestStartPosition(),
                digestEndPosition(),
                digestNumber(),
                restrictionSite5prime(),
                restrictionSite3prime(),
                digestLength(),
                gcContent5prime(),
                gcContent3prime(),
                repeatContent5prime(),
                repeatContent3prime(),
                enrichmentStatus() ? "T" : "F",
                nProbes5prime(),
                nProbes3prime());
    }
}
