package vpvgui.model.genome;



import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class HumanHg19 extends Genome implements Serializable {
    /** serialization version ID */
    static final long serialVersionUID = 1L;
    private static final String[] chromosomes = {"chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9",
            "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr20", "chr21",
            "chr22", "chrX", "chrY"};


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
}
