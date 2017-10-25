package vpvgui.gui.analysisPane;

import org.apache.log4j.Logger;
import vpvgui.model.Design;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.ViewPoint;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ViewPointAnalysisSummaryHTMLGenerator {
    private static final Logger logger = Logger.getLogger(ViewPointAnalysisSummaryHTMLGenerator.class.getName());
    private Model model=null;


    private static final String HTML_HEADER = "<html><body><h3>Viewpoint Viewer</h3>";
    private static final String HTML_FOOTER = "</body></html>";
    private NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    public ViewPointAnalysisSummaryHTMLGenerator(Model model) {
        this.model=model;
        logger.trace("Consructing HTML generator with model="+this.model.toString());
    }



    public String getHTML() {
        logger.trace(String.format("Getting HTML ViewPoint summary code for model=%s",this.model.toString()));
        if (model==null){
            logger.error("model was null");
            return "";
        }
        Design design = new Design(this.model);
        design.calculateDesignParameters();
        int nviewpoints=design.getN_viewpoints();
        int ngenes=design.getN_genes();

        long total_active_frags=design.getN_unique_fragments();
        double avg_n_frag=design.getAvgFragmentsPerVP();
        double avg_size=design.getAvgVPsize();
        double avg_score=design.getAvgVPscore();
        int n_totalNucleotidesInProbes=design.getN_nucleotides_in_probes();
        int totalEffectiveNucleotides=design.totalEffectiveSize();
        String lst = String.format("<ul><li>Number of genes: %d</li>" +
                        "<li>Number of viewpoints: %d</li>" +
                        "<li>Number of unique fragments: %d</li>" +
                        "<li>Average number of active fragments per viewpoint: %.1f</li>" +
                        "<li>Average viewpoint score: %.2f%%</li>" +
                        "<li>Average viewpoint size: %.1f nucleotides</li>" +
                        "<li>Total margin size: %s nucleotides</li>" +
                        "<li>Tiling factor: %d x</li>" +
                        "<li>Total effective size: %s kb</li>" +
                        "</ul>",
                ngenes,
                nviewpoints,
                total_active_frags,
                avg_n_frag,
                100*avg_score,
                avg_size,
                dformater.format(n_totalNucleotidesInProbes),
                model.getTilingFactor(),
                dformater.format(totalEffectiveNucleotides/100));

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
