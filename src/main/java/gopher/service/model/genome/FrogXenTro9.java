package gopher.service.model.genome;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by hansep on 11/27/17.
 */
public class FrogXenTro9 extends Genome implements Serializable {
    /** serialization version ID */
    static final long serialVersionUID = 3L;

    private static final String genomeBasename="xenTro9.chromFa.tar.gz";

    private static final String[] chromosomes = {"chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9",
            "chr10"};


    public FrogXenTro9(String directoryPath) {
        super(directoryPath);
        init();
    }

    public FrogXenTro9() {
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
    public String getGenomeBuild() {
        return "xenTro9";
    }
    @Override public String getGenomeFastaName(){return "xenTro9.fa"; }

    @Override
    public String getGenomeBasename(){return genomeBasename;}
    @Override
    public  int getNumberOfCanonicalChromosomes() { return chromosomes.length; }
}
