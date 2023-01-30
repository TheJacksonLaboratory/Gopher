package gopher.service.model.genome;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class HumanHg19 extends Genome implements Serializable {
    private final static Logger LOGGER = LoggerFactory.getLogger(HumanHg19.class);
    /** serialization version ID */
    static final long serialVersionUID = 4L;
    private static final String[] chromosomes = {"chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9",
            "chr10", "chr11", "chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19", "chr20", "chr21",
            "chr22", "chrX", "chrY"};

    private static final String genomeBasename="chromFa.tar.gz";

    public HumanHg19(String directoryPath) {
        super(directoryPath);
        init();

    }

    public HumanHg19() {
        super();
    }
    @Override
    protected void init() {
        Arrays.stream(chromosomes).forEach(s -> {
            valid.add(s);
            chromosomeFileNames.add(String.format("%s.fa",s));
        });
    }

    @Override
    public String getGenomeBuild() {  return "hg19";  }
    @Override public String getGenomeFastaName(){return "hg19.fa"; }
    @Override
    public String getGenomeBasename(){return genomeBasename;}
    @Override
    public  int getNumberOfCanonicalChromosomes() { return chromosomes.length; }

    @Override  public boolean isCanonicalChromosome(String filename) {
        int i = filename.lastIndexOf("/");
        if (i>0) { // i.e., filename was a path with slashes
            filename = filename.substring(i+1);
        }
        return chromosomeFileNames.contains(filename);
    }
}
