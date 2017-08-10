package vpvgui.gui.analysisPane;

import vpvgui.model.Model;
import vpvgui.model.project.ViewPoint;

import java.util.List;

public class ViewPointAnalysisSummaryHTMLGenerator {

    private Model model=null;


    private static final String HTML_HEADER = "<html><body><h3>Viewpoint Viewer</h3>";
    private static final String HTML_FOOTER = "</body></html>";

    public ViewPointAnalysisSummaryHTMLGenerator(Model model) {
        this.model=model;
    }



    public String getHTML() {

        List<ViewPoint> vplist=model.getViewPointList();
        int nviewpoints=vplist.size();
        int ngenes=model.getVPVGeneList().size();
        long total_size=0;
        long total_active_frags=0;
        double total_score=0d;
        for (ViewPoint vp:vplist) {
            total_size += vp.getEndPos()-vp.getStartPos();
            total_active_frags += vp.getNumOfSelectedFrags();
            total_score += vp.getScore();
            /* todo -- other quality parameters!! */
        }
        double avg_n_frag=(double)total_active_frags/nviewpoints;
        double avg_size=(double)total_size/nviewpoints;
        double avg_score=(double)100*total_score/nviewpoints;
        String lst=String.format("<ul><li>Number of genes: %d</li>"+
        "<li>Number of viewpoints: %d</li>"+
        "<li>Average number of active fragments per viewpoint: %.1f</li>"+
         "<li>Average viewpoint score: %.2f%%</li>"+
        "<li>Average viewpoint size: %.1f nucleotides</li>"+
                "<li>Total number of probe nucleotides: %d</li></ul>",
                ngenes,nviewpoints,avg_n_frag,avg_score,avg_size,this.model.getTotalNumberOfProbeNucleotides());

        return String.format("%s\n%s\n%s\n",HTML_HEADER,lst,HTML_FOOTER);
    }



}
