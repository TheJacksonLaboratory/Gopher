package vpvgui.gui.analysisPane;

import vpvgui.model.Model;
import vpvgui.model.project.ViewPoint;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ViewPointAnalysisSummaryHTMLGenerator {

    private Model model=null;


    private static final String HTML_HEADER = "<html><body><h3>Viewpoint Viewer</h3>";
    private static final String HTML_FOOTER = "</body></html>";
    private NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    public ViewPointAnalysisSummaryHTMLGenerator(Model model) {
        this.model=model;
    }



    public String getHTML() {

        List<ViewPoint> vplist=model.getViewPointList();
        int nviewpoints=vplist.size();
        int ngenes=model.getVPVGeneList().size();
        long total_size=0;
        long total_active_frags=0;
        long total_margin_size=0;
        double total_score=0d;
        for (ViewPoint vp:vplist) {
            total_size += vp.getEndPos()-vp.getStartPos();
            total_active_frags += vp.getNumOfSelectedFrags();
            total_score += vp.getScore();
            total_margin_size += vp.getTotalMarginSize();
            /* todo -- other quality parameters!! */
        }
        double avg_n_frag=(double)total_active_frags/nviewpoints;
        double avg_size=(double)total_size/nviewpoints;
        double avg_score=(double)100*total_score/nviewpoints;
        String totalEffectiveNucleotides=getTotalEffectiveNucleotides(total_margin_size,model.getTilingFactor());
        String lst = String.format("<ul><li>Number of genes: %d</li>" +
                        "<li>Number of viewpoints: %d</li>" +
                        "<li>Average number of active fragments per viewpoint: %.1f</li>" +
                        "<li>Average viewpoint score: %.2f%%</li>" +
                        "<li>Average viewpoint size: %.1f nucleotides</li>" +
                        "<li>Total margin size: %s nucleotides</li>" +
                        "<li>Total effective size: %s</li>" +
                        "</ul>",
                ngenes,
                nviewpoints,
                avg_n_frag,
                avg_score,
                avg_size,
                dformater.format(total_margin_size),
                totalEffectiveNucleotides);

        return String.format("%s\n%s\n%s\n", HTML_HEADER, lst, HTML_FOOTER);
    }

    private String getTotalEffectiveNucleotides(long total_margin_size, double tilingFactor) {
        long total = (long) (total_margin_size * tilingFactor);
        if (total >= 1e6) {
            double Mb = (double) total / 1e6;
            return String.format("%s nucleotides (%.2f Mb), with tiling factor %.1f", dformater.format(total), Mb, tilingFactor);
        } else {
            double Kb = (double) total / 1e3;
            return String.format("%s nucleotides (%.2f kb), with tiling factor %.1f", dformater.format(total), Kb, tilingFactor);
        }
    }


}
