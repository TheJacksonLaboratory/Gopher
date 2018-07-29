package gopher.gui.analysisPane;

import org.apache.log4j.Logger;
import gopher.model.Design;
import gopher.model.Model;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The analysis panel of Gopher shows a summary of the design on the top and a table with individual viewpoints on the
 * bottom part of the panel. The top part of the panel is implemented with a JavaFX WebView. This class generates
 * HTML code that shows a summary of the probe design parameters based on calculations in the {@link Design} class.
 *
 * @author Peter Robinson
 * @version 0.1.1 (2017-10-29)
 */
class ViewPointAnalysisSummaryHTMLGenerator {
    private static final Logger logger = Logger.getLogger(ViewPointAnalysisSummaryHTMLGenerator.class.getName());
    private Model model;

    private Map<String,String> listItems;


    private static final String HTML_HEADER = "<html><head>%s</head><body><h1>Panel design</h1>";
    private static final String HTML_FOOTER = "</body></html>";
    private static NumberFormat dformater = NumberFormat.getInstance(Locale.US);

    ViewPointAnalysisSummaryHTMLGenerator(Model model) {
        this.model = model;
        createListViewContent();
    }


    public Map<String,String>  createListViewContent() {
        Design design = new Design(this.model);
        design.calculateDesignParameters();
        this.listItems=new LinkedHashMap<>();

        int ngenes = design.getN_genes();
        int resolvedGenes = design.getN_resolvedGenes();
        String geneV = String.format("n=%d of which %d have \u2265 valid viewpoint",ngenes,resolvedGenes);
        listItems.put("Genes",geneV);

        int nviewpoints = design.getN_viewpoints();
        int resolvedVP = design.getN_resolvedViewpoints();
        double avVpSize = design.getAvgVPsize();
        double avgVpScore = design.getAvgVPscore();
        String vpointV = String.format("n=%d of which %d have \u2265 valid fragment. Mean size=%.1f bp. Mean score=%.1f",
                nviewpoints,resolvedVP,avVpSize,avgVpScore);
        listItems.put("Viewpoints",vpointV);

        int nfrags = design.getN_unique_fragments();
        int total_active_frags = design.getN_unique_fragments();
        double avg_n_frag = design.getAvgFragmentsPerVP();
        String fragmentV = String.format("Total unique digests=%d; active digests: %d; mean digests/viewpoint: %.1f",
                nfrags,total_active_frags,avg_n_frag);
        listItems.put("Digests",fragmentV);

        int n_balancedDigests = design.getTotalNumBalancedDigests();
        int n_unbalanced = design.getTotalNumUnbalancedDigests();
        int n_baits = design.getTotalNumOfUniqueBaits();
        String fragmentBalanceV = String.format("n=%d (Balanced digests: %d; unbalanced digests: %d)",
                n_baits,n_balancedDigests,n_unbalanced);
        listItems.put("Probes",fragmentBalanceV);
        return listItems;
    }





    /**
     * @return a block of CSS code intended for the blue-beige Table of data on the design.
     */
    private static String getCSSblock() {
        return "<style>\n" +
                "h1 {\n" +
                "\tfont-size: 16x;\n" +
                "  font-weight: bold;\n" +
                "  color: #1C6EA4;\n" +
                "}\n" +
                "table.vpvTable {\n" +
                "  border: 1px solid #1C6EA4;\n" +
                "  background-color: #EEEEEE;\n" +
                "  width: auto;\n" +
                "  text-align: left;\n" +
                "  border-collapse: collapse;\n" +
                "}\n" +
                "table.vpvTable td, table.vpvTable th {\n" +
                "  border: 1px solid #AAAAAA;\n" +
                "  padding: 3px 2px;\n" +
                "}\n" +
                "table.vpvTable tbody td {\n" +
                "  font-size: 13px;\n" +
                "}\n" +
                "table.vpvTable tr:nth-child(even) {\n" +
                "  background: #D0E4F5;\n" +
                "}\n" +
                "table.vpvTable thead {\n" +
                "  background: #1C6EA4;\n" +
                "  background: -moz-linear-gradient(top, #5592bb 0%, #327cad 66%, #1C6EA4 100%);\n" +
                "  background: -webkit-linear-gradient(top, #5592bb 0%, #327cad 66%, #1C6EA4 100%);\n" +
                "  background: linear-gradient(to bottom, #5592bb 0%, #327cad 66%, #1C6EA4 100%);\n" +
                "  border-bottom: 2px solid #444444;\n" +
                "}\n" +
                "table.vpvTable thead th {\n" +
                "  font-size: 15px;\n" +
                "  font-weight: bold;\n" +
                "  color: #FFFFFF;\n" +
                "  border-left: 2px solid #D0E4F5;\n" +
                "}\n" +
                "table.vpvTable thead th:first-child {\n" +
                "  border-left: none;\n" +
                "}\n" +
                "</style>";
    }


    String getHTML() {
        logger.trace(String.format("Getting HTML ViewPoint summary code for model=%s", this.model.getProjectName()));
        if (model == null) {
            logger.error("model was null");
            return "";
        }
        Design design = new Design(this.model);
        design.calculateDesignParameters();
        String lst = getHTMLTable(design);
        return String.format("%s\n%s\n%s\n", String.format(HTML_HEADER, getCSSblock()), lst, HTML_FOOTER);
    }


    /**
     * @param design The parameters used to generate probes (see {@link Design}).
     * @return HTML string to be displayed on the analysis panel.
     */
    private static String getHTMLTable(Design design) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead><tr>");
        sb.append("<th>Genes</th><th>Viewpoints</th><th>Digests and baits</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");

        int nviewpoints = design.getN_viewpoints();
        int ngenes = design.getN_genes();
        long total_active_frags = design.getN_unique_fragments();
        double avg_n_frag = design.getAvgFragmentsPerVP();
        double avVpSize = design.getAvgVPsize();
        int resolvedGenes = design.getN_resolvedGenes();
        double avg_score = design.getAvgVPscore();
        int n_totalNucleotidesInProbes = design.getN_nucleotides_in_unique_fragment_margins();
        int nfrags = design.getN_unique_fragments();
        int resolvedVP = design.getN_resolvedViewpoints();

        int n_baits = design.getTotalNumOfUniqueBaits();
        int n_balancedDigests = design.getTotalNumBalancedDigests();
        int n_unbalanced = design.getTotalNumUnbalancedDigests();

        sb.append(String.format("<tr><td>Number of genes: %d</td>" +
                        "<td>Number of valid viewpoints: %d</td><td>Number of unique fragments: %d</td></tr>",
                ngenes, resolvedVP, total_active_frags));

        sb.append(String.format("<tr><td>Genes with &geq; 1 viewpoint: %d</td><td>Average viewpoint score: %.2f%%</td>" +
                        "<td>Selected balanced digests: %d</td></tr>",
                resolvedGenes, 100 * avg_score, n_balancedDigests));
        sb.append(String.format("<tr><td></td><td>Average viewpoint size: %.1f bp</td>" +
                        "<td>Selected unbalanced digests: %d</td></tr>",
                avVpSize, n_unbalanced));
        sb.append(String.format("<tr><td></td>" +
                        "<td></td><td>Number of baits: %d</td></tr>",
                n_baits));
        sb.append("</tbody>\n</table>");
        return sb.toString();
    }


}
