package vpvgui.model.genome;



import java.io.Serializable;
import java.util.*;

public class HumanHg19 extends Genome implements Serializable {
    /** serialization version ID */
    static final long serialVersionUID = 3L;
    private static final String[] chromosomes = {"chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9",
            "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr20", "chr21",
            "chr22", "chrX", "chrY"};

    private static String genomeBasename="chromFa.tar.gz";

    public HumanHg19(String directoryPath) {
        super(directoryPath);
        init();
    }

    public HumanHg19() {
        super();
        init();
    }

    private void init() {
        Arrays.stream(chromosomes).forEach(s -> {
            valid.add(s);
        });
    }

    @Override
    public String getGenomeBuild() {
        return "hg19";
    }
    @Override
    public String getGenomeBasename(){return genomeBasename;};
    @Override
    public  int getNumberOfCanonicalChromosomes() { return chromosomes.length; }
}
