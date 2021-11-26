package gopher.model.regulatoryexome;

import gopher.model.viewpoint.ViewPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to store data from the Ensembl regulatory build and to calculate whether
 * any given element is located close enough to the transcription start site of a target gene, a function
 * called {@link #isLocatedWithinThreshold(ViewPoint, int, int)} that uses a potentially different distance
 * threshold for upstream and downstream
 * @author Peter Robinson
 * @version 0.1.3 (2017-11-12)
 */
public class RegulatoryElement {
    static Logger logger = LoggerFactory.getLogger(RegulatoryExomeBuilder.class.getName());

    private RegulationCategory category;
    private String chrom;
    private int from;
    private int to;
    private String id;

    public RegulatoryElement(String chrom, int f, int t, String identifier, String cat) {
        if (cat.equals("Enhancer")) {
            this.category = RegulationCategory.ENHANCER;
        } else if (cat.equalsIgnoreCase("Open chromatin")) {
            this.category = RegulationCategory.OPEN_CHROMATIN;
        } else if (cat.equalsIgnoreCase("Promoter Flanking Region")) {
            this.category = RegulationCategory.PROMOTER_FLANKING_REGION;
        } else if (cat.equalsIgnoreCase("CTCF Binding Site")) {
            this.category = RegulationCategory.CTCF_BINDING_SITE;
        } else if (cat.equalsIgnoreCase("Promoter")) {
            this.category = RegulationCategory.PROMOTER;
        } else if (cat.equalsIgnoreCase("TF binding site")) {
            this.category = RegulationCategory.TF_BINDING_SITE;
        } else {
            logger.error("DID NOT RECOGNIZE CATEGORY " + cat);
        }
        this.chrom = chrom;
        this.to = t;
        this.from = f;
        this.id = identifier;
    }

    public RegulationCategory getCategory() {
        return category;
    }

    public String getChrom() {
        return chrom;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getId() {
        return id;
    }

    public boolean isLocatedWithinThreshold(ViewPoint vp, int upstreamThreshold, int downstreamThreshold) {
        int genomicPos=vp.getGenomicPos(); // the transcription start site (usually)--center point of the viewpoint
        int upstreamBoundary,downstreamBoundary;
        char strand='-';
        if (vp. isPositiveStrand()) {
            upstreamBoundary=genomicPos-upstreamThreshold;
            downstreamBoundary=genomicPos+downstreamThreshold;
        } else {
            upstreamBoundary=genomicPos-downstreamThreshold;
            downstreamBoundary=genomicPos+upstreamThreshold;
        }

        return (upstreamBoundary<=to && downstreamBoundary>=to) || (upstreamBoundary<=from && downstreamBoundary>=from);
    }
}


