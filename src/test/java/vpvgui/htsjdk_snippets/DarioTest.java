package vpvgui.htsjdk_snippets;

import org.junit.Test;
import vpvgui.io.JannovarTranscriptFileBuilder;

import java.io.File;

/**
 * Created by phansen on 7/10/17.
 */
public class DarioTest {



    @Test public void doAnalysis() {

        File destinationdirectory = new File("/home/peter/IdeaProjects/git_vpv_workspace/VPV");
        JannovarTranscriptFileBuilder builder = new JannovarTranscriptFileBuilder("UCSC-hg19", destinationdirectory);

    }



}
