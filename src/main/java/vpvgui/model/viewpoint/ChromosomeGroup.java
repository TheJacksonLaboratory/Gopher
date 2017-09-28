package vpvgui.model.viewpoint;


import org.apache.log4j.Logger;
import vpvgui.model.VPVGene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is designed to group the {@link vpvgui.model.VPVGene} objects according to
 * chromosome so that we can open a FastaReader object only once for each chromosome. We
 * will order the VPVGenes according to chromosome and then sort them according to location
 * on the chromosome.
 * @author Peter Robinson
 * @version 0.0.1 (2017-09-27).
 */
public class ChromosomeGroup {
    private static final Logger logger = Logger.getLogger(ChromosomeGroup.class.getName());
    private String referenceSequenceID=null;

    private List<VPVGene> genes=null;

    public ChromosomeGroup(String ref) {
        this.referenceSequenceID=ref;
        genes=new ArrayList<>();
    }

    public void addVPVGene(VPVGene g) { genes.add(g);}

    public List<VPVGene> getGenes() {
        Collections.sort(genes);
        return genes;
    }

    public String getReferenceSequenceID() { return referenceSequenceID;}
}
