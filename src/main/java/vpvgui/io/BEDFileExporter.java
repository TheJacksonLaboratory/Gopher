package vpvgui.io;

import org.apache.log4j.Logger;
import vpvgui.gui.viewpointpanel.URLMaker;
import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class exports BEDfiles that can be used to check the results and for ordering probes. We export three files
 * <ol>
 *     <li>Target regions; now unique margins</li>
 *     <li>All tracks; combine genomic positions, viewpoints, fragments, etc. in one BED file</li>
 *      <li>Summary tsv; For each viewpoint collect all informations that are useful for sharing the results,
 * such as UCSC links, scores, number of selected fragments, etc..</li>
 * </ol>
 * The target regions file is intended to be used to generate probes, .e.g., by use of a Wizard of a probe manufacturer.
 * @author Peter Hansen, Peter Robinson
 * @version 0.0.3 (2017-10-14)
 */
public class BEDFileExporter {

    private static final Logger logger = Logger.getLogger(BEDFileExporter.class.getName());
    private String allTracksBEDfile =null;
    private String targetRegionBEDfile =null;
    private String vpvSummaryTSVfile=null;
    private String vpvSummaryRfile=null;
    private String vpvUniqueTargetFragmentsFile=null;
    /** Path to directory where the BED files will be stored. The path is guaranteed to have no trailing slash. */
    private String directoryPath=null;

    /**
     *
     * @param dirpath The directory where we will write the BED and TSV files to
     * @param outPrefix The prefix (name) of the files.
     */
    public BEDFileExporter(String dirpath, String outPrefix){
        initFileNames(outPrefix);
        /* remove trailing slash if necessary. */
        if (dirpath.endsWith(File.separator)) {
            dirpath=dirpath.substring(0,dirpath.length()-1);
        }
        this.directoryPath=dirpath;
    }

    private void initFileNames(String prefix) {
        this.allTracksBEDfile =String.format("%s_allTracks.bed",prefix);
        this.targetRegionBEDfile =String.format("%s_targetRegions.txt",prefix);
        this.vpvSummaryTSVfile=String.format("%s_vpvSummary.tsv",prefix);
        this.vpvSummaryRfile=String.format("%s_vpvSummary.r",prefix);
        this.vpvUniqueTargetFragmentsFile=String.format("%s_unique_target_fragments.bed",prefix);
    }

    private String getFullPath(String fname) {
        return String.format("%s%s%s",this.directoryPath,File.separator,fname);
    }

    /**
     * This function is responsible for outputting data in the form of BED files and also a TSV file with URLs and some
     * other data on each viewpoint. Users can viewthe chosen fragments by uploading them to the UCSCbrowser.
     * @param viewpointlist
     * @param genomeBuild
     * @throws FileNotFoundException
     */
    public void printRestFragsToBed(List<ViewPoint> viewpointlist, String genomeBuild) throws FileNotFoundException {

        PrintStream out_targetRegions = new PrintStream(new FileOutputStream(getFullPath(targetRegionBEDfile)));
        out_targetRegions.println("track name='" + targetRegionBEDfile + "' description='" + targetRegionBEDfile + "'");

        // print tsv file that can be used to share the results of VPV
        // -----------------------------------------------------------
        PrintStream out_ucscURL = new PrintStream(new FileOutputStream(getFullPath(vpvSummaryTSVfile)));
        out_ucscURL.println("Gene\tGENOMIC_POS\tURL\tNO_SELECTED_FRAGMENTS\tSCORE\tVP_LENGTH\tACT_SEG_LENGTH\tTSS_FRAGMENT_SELECTED");
        for (ViewPoint vp : viewpointlist) {
            if(vp.getNumOfSelectedFrags()==0) {continue;}
            String url= getDefaultURL(vp,genomeBuild);
            int NO_SELECTED_FRAGMENTS = vp.getActiveSegments().size();
            String SCORE = String.format("%.2f", vp.getScore());
            out_ucscURL.println(String.format("%s\t%s\t%s\t%d\t%s\t%d\t%d\t%b",vp.getTargetName(),vp.getGenomicLocationString(),url,NO_SELECTED_FRAGMENTS,SCORE,vp.getTotalLengthOfViewpoint(),vp.getTotalLengthOfActiveSegments(),vp.isTSSfragmentChosen()));
        }
        out_ucscURL.close();

        PrintStream vpvSummaryR = new PrintStream(new FileOutputStream(getFullPath(vpvSummaryRfile)));

        String[] helpSplit=vpvSummaryRfile.split("_vpvSummary.r");
        String prefix=helpSplit[0];
        String script=getRscript(prefix);
        vpvSummaryR.println(script);

        // print file for all tracks that can be uploaded to the UCSC genome browser
        // print file for target regions that can be used as input for the SureDesign wizard
        // ---------------------------------------------------------------------------------

        PrintStream out_allTracks = new PrintStream(new FileOutputStream(getFullPath(allTracksBEDfile)));

        // print genomic positions
        out_allTracks.println("track name='" + "VPV: Genomic Positions" + "' description='" + "Genomic positions" + "' color=0,0,0" + " visibility=2");
        for (ViewPoint vp : viewpointlist) {
            if(vp.getNumOfSelectedFrags()==0) {continue;}
            out_allTracks.println(String.format("%s\t%d\t%d\t%s",
                    vp.getReferenceID(),
                    vp.getGenomicPos()-1,
                    (vp.getGenomicPos()),
                    vp.getTargetName()));
        }

        // print viewpoints
        out_allTracks.println("track name='" + "VPV: Viewpoints" + "' description='" + "Viewpoints" + "' color=0,0,0" + "' useScore=1" + " visibility=2");
        for (ViewPoint vp : viewpointlist) {
            if(vp.getNumOfSelectedFrags()==0) {continue;}
            out_allTracks.println(String.format("%s\t%d\t%d\t%s\t%d",
                    vp.getReferenceID(),
                    (vp.getStartPos()-1),
                    vp.getEndPos(),
                    vp.getTargetName(),
                    (int) Math.round(vp.getScore()*1000)));
        }

        // print restriction fragments and get unique fragment margins
        out_allTracks.println("track name='" + "VPV: Restriction fragments" + "' description='" + "Restriction fragments" + "' color=0,0,128" + " visibility=2");
        Set<String> uniqueFragmentMargins = new HashSet<String>(); // use a set to get rid of duplicate fragment margins
        HashMap<String,String> uniqueFragmentMarginsMap = new HashMap<String,String>();
        Set<String> uniqueFragments = new HashSet<String>(); // use a set to get rid of duplicate fragments

        for (ViewPoint vp : viewpointlist) {
            if(vp.getNumOfSelectedFrags()==0) {continue;}
            int k=0; // index of selected fragment
            for (Segment segment : vp.getActiveSegments()) {
                k++;
                Integer rsStaPos = segment.getStartPos();
                Integer rsEndPos = segment.getEndPos();
                out_allTracks.println(vp.getReferenceID() + "\t" + (rsStaPos-1) + "\t" + rsEndPos + "\t" + vp.getTargetName());

                // get unique margins of selected fragments and unique fragments
                for(int l = 0; l<segment.getSegmentMargins().size(); l++) {
                    Integer fmStaPos = segment.getSegmentMargins().get(l).getStartPos();
                    Integer fmEndPos = segment.getSegmentMargins().get(l).getEndPos();

                    String key = vp.getReferenceID() + ":" + (fmStaPos-1) + "-" + fmEndPos; // build key
                    if (uniqueFragmentMarginsMap.get(key) == null) { // check if region is already in hash
                        uniqueFragmentMarginsMap.put(key,vp.getTargetName());
                    } else {
                        if (!uniqueFragmentMarginsMap.get(key).contains(vp.getTargetName())) { // gene symbol is not in target region name
                            uniqueFragmentMarginsMap.put(key,uniqueFragmentMarginsMap.get(key) + "," + vp.getTargetName()); // concat old and new value
                        }
                    }

                    uniqueFragmentMargins.add(vp.getReferenceID() + "\t" + (fmStaPos-1) + "\t" + fmEndPos + "\t" + vp.getTargetName() + "_margin_" + l);
                    uniqueFragments.add(vp.getReferenceID() + "\t" + (segment.getStartPos()-1) + "\t" + segment.getEndPos() + "\t" + vp.getTargetName());
                }
            }
        }

        // print out unique set of margins as targets for enrichment
        out_allTracks.println("track name='" + "VPV: Target regions" + "' description='" + "Target regions" + "' color=0,64,128" + " visibility=2");
        Integer totalLengthOfMargins=0;
        for (String s : uniqueFragmentMargins) {
            //out_allTracks.println(s);
            //out_targetRegions.println(s);
            String[] parts = s.split("\t");
            Integer sta = Integer.parseInt(parts[1]);
            Integer end = Integer.parseInt(parts[2]);
            Integer len = end - sta;
            totalLengthOfMargins = totalLengthOfMargins + len;
        }
        totalLengthOfMargins=0;
        int target_id = 0;
        for (String key : uniqueFragmentMarginsMap.keySet()) {
            String[] parts = key.split(":");
            String ref_id = parts[0];
            String[] parts2 = parts[1].split("-");
            String sta = parts2[0];
            String end = parts2[1];
            out_allTracks.println(ref_id + "\t" + sta + "\t" + end + "\ttarget_" + target_id + ":" + uniqueFragmentMarginsMap.get(key));
            out_targetRegions.println(ref_id + "\t" + sta + "\t" + end + "\ttarget_" + target_id + ":" + uniqueFragmentMarginsMap.get(key));
            Integer len = Integer.parseInt(end) - Integer.parseInt(sta);
            totalLengthOfMargins = totalLengthOfMargins + len;
            target_id++;
        }


        out_allTracks.close();
        out_targetRegions.close();

        logger.trace("Done output of BED files. Total Length of Margins: " + totalLengthOfMargins);

        // print out unique set of target fragments to a separate file that can be used as input for diachromatic
        // ------------------------------------------------------------------------------------------------------

        PrintStream out_uniqueTargetFragments = new PrintStream(new FileOutputStream(getFullPath(vpvUniqueTargetFragmentsFile)));
        for (String s : uniqueFragments) {
            out_uniqueTargetFragments.println(s);
        }
    }


    /** @return A UCSC URL representing the target region of interest. */
    public String getDefaultURL(ViewPoint vp, String genomebuild) {
        int posFrom, posTo;
         int offset = 500;
        posFrom = vp.getStartPos() - offset;
        posTo = vp.getEndPos() + offset;
        String chrom = vp.getReferenceID();
        if (!chrom.startsWith("chr"))
            chrom = "chr" + chrom; /* TODO MAKE THIS ROBUST! */
        String targetItem = vp.getTargetName();
        String trackType="hgTracks";
        String url = String.format("http://genome.ucsc.edu/cgi-bin/%s?db=%s&position=%s%%3A%d-%d&hgFind.matches=%s&pix=1800",
                trackType, genomebuild, chrom, posFrom, posTo, targetItem);
        return url;
    }

    public String getRscript(String prefix) {
        String script="TAB <- read.table(\"" + prefix + "_vpvSummary.tsv\", sep = \"\\t\",header=T)\n\n";

        script += "cairo_pdf(\"" + prefix + "_vpvSummary.pdf\", width=7, height=7)\n";
        script += "par(mfrow=c(2,2))\n\n";

        script += "hist(\n";
        script += "\tt(TAB[\"SCORE\"]),\n";
        script += "\tbreaks=seq(0,1,0.05),\n";
        script += "\tmain=\"Viewpoint scores\",\n";
        script += "\txlab=\"Score\"\n";
        script += "\t)\n\n";

        script += "MEAN_SCORE=round(mean(TAB[,\"SCORE\"]),digits=2)\n";
        script += "legend(\"topleft\", legend=c(paste(\"Avg: \",MEAN_SCORE)),cex=0.8,bty = \"n\")\n\n\n";


        script += "X_MIN=min(TAB[\"VP_LENGTH\"])\n";
        script += "X_MAX=max(TAB[\"VP_LENGTH\"])\n\n";

        script += "hist(\n";
        script += "t(TAB[\"VP_LENGTH\"]),\n";
        script += ")\n";


        script += "dev.off()\n\n";
        return script;
    }

}
