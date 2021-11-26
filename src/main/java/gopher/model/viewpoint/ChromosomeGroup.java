package gopher.model.viewpoint;


import gopher.model.GopherGene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is designed to group the {@link GopherGene} objects according to
 * chromosome so that we can open a FastaReader object only once for each chromosome. We
 * will order the VPVGenes according to chromosome and then sort them according to location
 * on the chromosome.
 * @author Peter Robinson
 * @version 0.0.1 (2017-09-27).
 */
public class ChromosomeGroup {
    private static final Logger logger = LoggerFactory.getLogger(ChromosomeGroup.class.getName());
    private final String referenceSequenceID;

    private final List<GopherGene> genes;

    ChromosomeGroup(String ref) {
        this.referenceSequenceID=ref;
        this.genes=new ArrayList<>();
    }

    void addGopherGene(GopherGene g) { genes.add(g);}

    public List<GopherGene> getGenes() {
        Collections.sort(genes);
        return genes;
    }

    String getReferenceSequenceID() { return referenceSequenceID;}
}
