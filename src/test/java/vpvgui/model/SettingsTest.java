package vpvgui.model;

import javafx.collections.ObservableList;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Hannah Blau (blauh)
 *         created on 6/8/17.
 */
public class SettingsTest {

    @Test
    public void testToString() throws Exception {
        Settings s = Settings.factory();
        System.out.print(s);

        s.setProjectName("FancyProject");
        s.setGenomeFileFrom("http://hgdownload.soe.ucsc.edu/goldenPath/hg19/bigZips/chromFa.tar.gz");
        s.setTranscriptsFileFrom("http://hgdownload.cse.ucsc.edu/goldenPath/hg19/database/refGene.txt.gz");
        s.setRepeatsFileFrom("http://hgdownload.soe.ucsc.edu/goldenPath/hg19/repeats.tar.gz");
        s.setGenomeFileTo("/Users/blauh/mygenome");
        s.setTranscriptsFileTo("/Users/blauh/mytranscripts");
        s.setRepeatsFileTo("/Users/blauh/myrepeats");
        ObservableList<String> rel = s.getRestrictionEnzymesList();
        ObservableList<String> tgl = s.getTargetGenesList();
        for (int i = 0; i < 10; i++) {
            rel.add("RestrictionEnzyme" + i);
            tgl.add("TargetGene" + i);
        }
        System.out.print(s);
    }

    @Test
    public void testFactory() throws Exception {
    }

    @Test
    public void testSaveToFile() throws Exception {
    }

}