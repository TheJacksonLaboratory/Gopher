package vpvgui.model.regulatoryexome;

import org.apache.log4j.Logger;

public class RegulatoryElement {
    static Logger logger = Logger.getLogger(RegulatoryExomeBuilder.class.getName());

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

    public boolean isLocatedWithinThreshold(int i,int threshold) {
        return Math.abs(i-to)<threshold || Math.abs(i-from) < threshold;
    }
}


