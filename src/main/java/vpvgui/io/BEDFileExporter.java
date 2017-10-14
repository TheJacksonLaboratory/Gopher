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
import java.util.List;
import java.util.Set;


/**
 * This class exports BEDfiles that can be used to check the results and for ordering probes. We export three files
 * <ol>
 *     <li>Target regions -> now unique margins</li>
 *     <li>All tracks -> combine genomic positions, viewpoints, fragments, etc. in one BED file</li>
 *      <li>Summary tsv -> For each viewpoint collect all informations that are useful for sharing the results,
 * such as UCSC links, scores, number of selected fragments, etc..</li>
 * </ol>
 * The target regions file is intended to be used to generate probes, .e.g., by use of a Wizard of a probe manufacturer.
 * @author Peter Hansen, Peter Robinson
 * @version 0.0.3 (2017-10-14)
 */
public class BEDFileExporter {

    private static final Logger logger = Logger.getLogger(BEDFileExporter.class.getName());
    private String viewpointBEDfile=null;
    private String allTracksBEDfile =null;
    //private String fragmentMarginsBEDfile=null;
    private String genomicPositionsBEDfile=null;
    /** The unique fragment margins (final probe design). */
    private String targetRegionBEDfile =null;
    private String ucscURLfile=null;
    /** Path to directory where the BED files will be stored. The path is guaranteed to have no trailing slash. */
    private String directoryPath=null;


    public BEDFileExporter(String dirpath, String outPrefix){
        initFileNames(outPrefix);
        /* remove trailing slash if necessary. */
        if (dirpath.endsWith(File.separator)) {
            dirpath=dirpath.substring(0,dirpath.length()-1);
        }
        this.directoryPath=dirpath;
    }

    private void initFileNames(String prefix) {
        this.viewpointBEDfile=String.format("%s_viewpoints.bed",prefix);
        this.allTracksBEDfile =String.format("%s_allTracks.bed",prefix);
       // this.fragmentMarginsBEDfile=String.format("%s_fragment_margins.bed",prefix);
       // this.genomicPositionsBEDfile=String.format("%s_genomic_positions.bed",prefix);
        this.targetRegionBEDfile =String.format("%s_target_regions.bed",prefix);
        this.ucscURLfile=String.format("%s_ucscURLs.tsv",prefix);
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
        PrintStream out_allTracks = new PrintStream(new FileOutputStream(getFullPath(allTracksBEDfile)));
        out_allTracks.println("track name='" + allTracksBEDfile + "' description='" + allTracksBEDfile + "'");

        PrintStream out_targetRegions = new PrintStream(new FileOutputStream(getFullPath(targetRegionBEDfile)));
        out_targetRegions.println("track name='" + targetRegionBEDfile + "' description='" + targetRegionBEDfile + "'");

        PrintStream out_ucscURL = new PrintStream(new FileOutputStream(getFullPath(ucscURLfile)));
        out_ucscURL.println("Gene\tTSS\tURL");

        Set<String> uniqueFragmentMargins = new HashSet<String>(); // use a set to get rid of duplicate fragments.

        for (ViewPoint vp : viewpointlist) {
                String referenceSequenceID = vp.getReferenceID();
                Integer vpStaPos = vp.getStartPos();
                Integer vpEndPos = vp.getEndPos();
                Integer vpGenomicPos = vp.getGenomicPos();
                String geneSymbol =  vp.getTargetName();
                Integer viewPointScore=0;
                if(vp.getResolved()) {
                    viewPointScore=1;
                }
                viewPointScore=vp.getNumOfSelectedFrags();
                out_allTracks.println(String.format("%s\t%d\t%d\t%s[Viewpoint]\t%d",
                    referenceSequenceID,
                        (vpStaPos-1),
                        vpEndPos,
                    geneSymbol,
                    viewPointScore));
                out_allTracks.println(String.format("%s\t%d\t%d\t%s[TSS]\t%d",
                        referenceSequenceID,
                        vpGenomicPos,
                        (vpGenomicPos+1),
                        geneSymbol,
                        viewPointScore));

                // print selected fragments of the viewpoint
                int k=0; // index of selected fragment
                for (Segment segment : vp.getActiveSegments()) {
                    k++;
                    Integer rsStaPos = segment.getStartPos();
                    Integer rsEndPos = segment.getEndPos();
                    out_allTracks.println(referenceSequenceID + "\t" + (rsStaPos-1) + "\t" + rsEndPos + "\t" + geneSymbol + "_fragment_" + k + "\t"+ viewPointScore);

                    // print margins of selected fragments
                    for(int l = 0; l<segment.getSegmentMargins().size(); l++) {
                        Integer fmStaPos = segment.getSegmentMargins().get(l).getStartPos();
                        Integer fmEndPos = segment.getSegmentMargins().get(l).getEndPos();
                        out_allTracks.println(referenceSequenceID + "\t" + (fmStaPos-1) + "\t" + fmEndPos + "\t" + geneSymbol + "_fragment_" + k + "_margin"+ "\t"+ viewPointScore);
                        uniqueFragmentMargins.add(referenceSequenceID + "\t" + (fmStaPos-1) + "\t" + fmEndPos + "\t" + geneSymbol);
                    }
                }
            String url= getDefaultURL(vp,genomeBuild);
                out_ucscURL.println(String.format("%s\t%s\t%s",vp.getTargetName(),vp.getGenomicLocationString(),url));
            }
        out_allTracks.close();
        out_ucscURL.close();

        /* print out unique set of margins for enrichment (and calculate the total length of all  margins) */
        Integer totalLengthOfMargins=0;
        for (String s : uniqueFragmentMargins) {
            out_targetRegions.println(s);
            String[] parts = s.split("\t");
            Integer sta = Integer.parseInt(parts[1]);
            Integer end = Integer.parseInt(parts[2]);
            Integer len = end - sta;
            totalLengthOfMargins = totalLengthOfMargins + len;
        }
        out_targetRegions.close();

        logger.trace("Done output of BED files. Total Length of Margins: " + totalLengthOfMargins);
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

}
