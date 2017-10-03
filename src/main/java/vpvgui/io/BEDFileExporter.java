package vpvgui.io;

import org.apache.log4j.Logger;
import vpvgui.gui.viewpointpanel.URLMaker;
import vpvgui.model.viewpoint.Segment;
import vpvgui.model.viewpoint.ViewPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class exports BEDfiles that can be used to check the results and for ordering probes.
 * @author Peter Hansen, Peter Robinson
 * @version 0.0.2 (2017-10-2)
 */
public class BEDFileExporter {

    private static final Logger logger = Logger.getLogger(BEDFileExporter.class.getName());
    private String viewpointBEDfile=null;
    private String fragmentBEDfile=null;
    private String fragmentMarginsBEDfile=null;
    private String genomicPositionsBEDfile=null;
    private String fragmentMarginsUniqueBEDfile=null;
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
        this.fragmentBEDfile=String.format("%s_fragments.bed",prefix);
        this.fragmentMarginsBEDfile=String.format("%s_fragment_margins.bed",prefix);
        this.genomicPositionsBEDfile=String.format("%s_genomic_positions.bed",prefix);
        this.fragmentMarginsUniqueBEDfile=String.format("%s_fragment_margins_unique.bed",prefix);
        this.ucscURLfile=String.format("%s_ucscURLs.tsv",prefix);
    }

    private String getFullPath(String fname) {
        return String.format("%s%s%s",this.directoryPath,File.separator,fname);
    }



    public void printRestFragsToBed(List<ViewPoint> viewpointlist, String genomeBuild) throws FileNotFoundException {
        /* prepare output file handles for writing */
        PrintStream out_viewpoints = new PrintStream(new FileOutputStream(getFullPath(viewpointBEDfile)));
        String description = viewpointBEDfile;
        out_viewpoints.println("track name='" + description + "' description='" + description + "'");

        PrintStream out_fragments = new PrintStream(new FileOutputStream(getFullPath(fragmentBEDfile)));
        description = fragmentBEDfile;
        out_fragments.println("track name='" + description + "' description='" + description + "'");

        PrintStream out_fragment_margins = new PrintStream(new FileOutputStream(getFullPath(fragmentMarginsBEDfile)));
        description = fragmentMarginsBEDfile;
        out_fragment_margins.println("track name='" + description + "' description='" + description + "'");

        PrintStream out_genomic_positions = new PrintStream(new FileOutputStream(getFullPath(genomicPositionsBEDfile)));
        description = genomicPositionsBEDfile;
        out_genomic_positions.println("track name='" + description + "' description='" + description + "'");

        PrintStream out_fragment_margins_unique = new PrintStream(new FileOutputStream(getFullPath(fragmentMarginsUniqueBEDfile)));
        description = fragmentMarginsUniqueBEDfile;
        out_fragment_margins_unique.println("track name='" + description + "' description='" + description + "'");

        PrintStream out_ucscURL = new PrintStream(new FileOutputStream(getFullPath(ucscURLfile)));
        out_ucscURL.println("Gene\tTSS\tURL");

        Set<String> uniqueFragmentMargins = new HashSet<String>();

        URLMaker urlmaker = new URLMaker(genomeBuild);

        for (ViewPoint vp : viewpointlist) {
                // print viewpoint
                String getReferenceSequenceID = vp.getReferenceID();
                Integer vpStaPos = vp.getStartPos();
                Integer vpEndPos = vp.getEndPos();
                Integer vpGenomicPos = vp.getGenomicPos();
                String geneSymbol =  vp.getTargetName();
                Integer viewPointScore=0;
                if(vp.getResolved()) {
                    viewPointScore=1;
                }
                viewPointScore=vp.getNumOfSelectedFrags();

                out_viewpoints.println(getReferenceSequenceID + "\t" + (vpStaPos-1) + "\t" + vpEndPos + "\t" + geneSymbol + "\t" + viewPointScore);

                out_genomic_positions.println(getReferenceSequenceID + "\t" + vpGenomicPos + "\t" + (vpGenomicPos+1) + "\t" + geneSymbol + "\t" + viewPointScore);

                // print selected fragments of the viewpoint
                //List<Segment> selectedRestSegList = vp.getActiveSegments();
                //for (int k = 0; k < selectedRestSegList.size(); k++) {
                int k=0; // index of selected fragment
                for (Segment segment : vp.getActiveSegments()) {
                    k++;
                    Integer rsStaPos = segment.getStartPos();
                    Integer rsEndPos = segment.getEndPos();
                    out_fragments.println(getReferenceSequenceID + "\t" + (rsStaPos-1) + "\t" + rsEndPos + "\t" + geneSymbol + "_fragment_" + k);

                    // print margins of selected fragments
                    for(int l = 0; l<segment.getSegmentMargins().size(); l++) {
                        Integer fmStaPos = segment.getSegmentMargins().get(l).getStartPos();
                        Integer fmEndPos = segment.getSegmentMargins().get(l).getEndPos();
                        out_fragment_margins.println(getReferenceSequenceID + "\t" + (fmStaPos-1) + "\t" + fmEndPos + "\t" + geneSymbol + "_fragment_" + k + "_margin");
                        uniqueFragmentMargins.add(getReferenceSequenceID + "\t" + (fmStaPos-1) + "\t" + fmEndPos + "\t" + geneSymbol);
                    }
                }
            String url= getDefaultURL(vp,genomeBuild);
                out_ucscURL.println(String.format("%s\t%s\t%s",vp.getTargetName(),vp.getGenomicLocationString(),url));
            }
        out_viewpoints.close();
        out_genomic_positions.close();
        out_fragments.close();
        out_fragment_margins.close();
        out_ucscURL.close();

        /* print out unique set of margins for enrichment (and calculate the total length of all  margins) */
        Integer totalLengthOfMargins=0;
        for (String s : uniqueFragmentMargins) {
            out_fragment_margins_unique.println(s);
            String[] parts = s.split("\t");
            Integer sta = Integer.parseInt(parts[1]);
            Integer end = Integer.parseInt(parts[2]);
            Integer len = end - sta;
            totalLengthOfMargins = totalLengthOfMargins + len;
        }
        out_fragment_margins_unique.close();

        logger.trace("totalLengthOfMargins: " + totalLengthOfMargins);
    }



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
