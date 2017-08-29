package vpvgui.io;

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

public class BEDFileExporter {

    private String viewpointBEDfile=null;
    private String fragmentBEDfile=null;
    private String fragmentMarginsBEDfile=null;
    private String genomicPositionsBEDfile=null;
    private String fragmentMarginsUniqueBEDfile=null;
    /** Path to directory where the BED files will be stored. The path is guaranteed to have no trailing slash. */
    private String directoryPath=null;


    private String cuttingMotif="GATC"; /* TODO MAKE THIS DYNAMIC!!!! */


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
    }

    private String getFullPath(String fname) {
        return String.format("%s%s%s",this.directoryPath,File.separator,fname);
    }



    public void printRestFragsToBed(List<ViewPoint> viewpointlist) throws FileNotFoundException {
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

        Set<String> uniqueFragmentMargins = new HashSet<String>();

        for (ViewPoint vp : viewpointlist) {
                // print viewpoint
                String getReferenceSequenceID = vp.getReferenceID(); // vpvGeneList.get(i).getContigID();
                Integer vpStaPos = vp.getStartPos(); // vpvGeneList.get(i).getviewPointList().get(j).getStartPos();
                Integer vpEndPos = vp.getEndPos(); //vpvGeneList.get(i).getviewPointList().get(j).getEndPos();
                Integer vpGenomicPos = vp.getGenomicPos(); //vpvGeneList.get(i).getviewPointList().get(j).getGenomicPos();
                String geneSymbol =  vp.getTargetName(); //.get(i).getGeneSymbol();
                Integer viewPointScore=0;
                if(vp.getResolved()) {
                    viewPointScore=1;
                }
                viewPointScore=vp.getNumOfSelectedFrags();
                //double viewPointScore2 = vpvGeneList.get(i).getviewPointList().get(j).getViewpointScore("GATC",marginSize);
                out_viewpoints.println(getReferenceSequenceID + "\t" + vpStaPos + "\t" + vpEndPos + "\t" + geneSymbol + "\t" + viewPointScore);

                out_genomic_positions.println(getReferenceSequenceID + "\t" + vpGenomicPos + "\t" + (vpGenomicPos+1) + "\t" + geneSymbol + "\t" + viewPointScore);

                // print selected fragments of the viewpoint
                ArrayList<Segment> selectedRestSegList = vp.getSelectedRestSegList(cuttingMotif);
                for (int k = 0; k < selectedRestSegList.size(); k++) {

                    Integer rsStaPos = selectedRestSegList.get(k).getStartPos();
                    Integer rsEndPos = selectedRestSegList.get(k).getEndPos();
                    out_fragments.println(getReferenceSequenceID + "\t" + rsStaPos + "\t" + rsEndPos + "\t" + geneSymbol + "_fragment_" + k);

                    // print margins of selected fragments
                    for(int l = 0; l<selectedRestSegList.get(k).getSegmentMargins().size(); l++) {
                        Integer fmStaPos = selectedRestSegList.get(k).getSegmentMargins().get(l).getStartPos();
                        Integer fmEndPos = selectedRestSegList.get(k).getSegmentMargins().get(l).getEndPos();
                        out_fragment_margins.println(getReferenceSequenceID + "\t" + fmStaPos + "\t" + fmEndPos + "\t" + geneSymbol + "_fragment_" + k + "_margin");
                        uniqueFragmentMargins.add(getReferenceSequenceID + "\t" + fmStaPos + "\t" + fmEndPos + "\t" + geneSymbol);
                    }

                }
            }
        out_viewpoints.close();
        out_genomic_positions.close();
        out_fragments.close();
        out_fragment_margins.close();

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

        System.out.println("totalLengthOfMargins: " + totalLengthOfMargins);
    }

}
