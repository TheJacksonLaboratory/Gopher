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


    private static final String HTML_HEADER = "<html><head>%s</head><body><h3>Viewpoint Viewer</h3>";
    private static final String HTML_FOOTER = "</body></html>";
    private static NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    public ViewPointAnalysisSummaryHTMLGenerator(Model model) {
        this.model=model;
        logger.trace("Constructing HTML generator with model="+this.model.toString());
    }




    public static String getCSSblock() {
        return "<style>\n" +
               " .datagrid table { border-collapse: collapse; text-align: left; width: auto; } \n" +
                ".datagrid {font: normal 12px/150% Arial, Helvetica, sans-serif; background: #fff; \n" +
                "border: 1px solid #006699; " +
                "border-radius: 3px; }\n" +
                ".datagrid table td, .datagrid table th { padding: 3px 10px; }\n" +
                ".datagrid table thead th {background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #006699), color-stop(1, #00557F) );\n" +
                "background-color:#006699; color:#FFFFFF; font-size: 15px; font-weight: bold; border-left: 1px solid #0070A8; } " +
                ".datagrid table thead th:first-child { border: none; }" +
                ".datagrid table tbody td { color: #00496B; border-left: 1px solid #E1EEF4;font-size: 12px;font-weight: normal; }" +
                ".datagrid table tbody .alt td { background: #E1EEF4; color: #00496B; }" +
                ".datagrid table tbody td:first-child { border-left: none; }" +
                ".datagrid table tbody tr:last-child td { border-bottom: none; }" +
                "padding: 2px 8px; margin: 1px;color: #FFFFFF;border: 1px solid #006699; " +
                "border-radius: 3px; " +
                "filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#006699', endColorstr='#00557F');" +
                "background-color:#006699; }.datagrid table tfoot ul.active, " +
//                ".datagrid table tfoot ul a:hover { text-decoration: none;border-color: #006699; color: #FFFFFF; " +
//                "background: none; background-color:#00557F;}div.dhtmlx_window_active, div.dhx_modal_cover_dv { position: fixed !important; }\n "+
                "</style>";
    }





    public String getHTML() {
        logger.trace(String.format("Getting HTML ViewPoint summary code for model=%s",this.model.toString()));
        if (model==null){
            logger.error("model was null");
            return "";
        }
        Design design = new Design(this.model);
        design.calculateDesignParameters();
       String lst = getHTMLTable(design,model.getTilingFactor());

        return String.format("%s\n%s\n%s\n", String.format(HTML_HEADER,getCSSblock()), lst, HTML_FOOTER);
    }


    /*
     <div class="datagrid"><table>
<thead><tr><th>header</th><th>header</th><th>header</th><th>header</th></tr></thead>
<tfoot><tr><td colspan="4"><div id="paging"><ul><li><a href="#"><span>Previous</span></a></li><li><a href="#" class="active"><span>1</span></a></li><li><a href="#"><span>2</span></a></li><li><a href="#"><span>3</span></a></li><li><a href="#"><span>4</span></a></li><li><a href="#"><span>5</span></a></li><li><a href="#"><span>Next</span></a></li></ul></div></tr></tfoot>
<tbody><tr><td>data</td><td>data</td><td>data</td><td>data</td></tr>
<tr class="alt"><td>data</td><td>data</td><td>data</td><td>data</td></tr>
<tr><td>data</td><td>data</td><td>data</td><td>data</td></tr>
<tr class="alt"><td>data</td><td>data</td><td>data</td><td>data</td></tr>
<tr><td>data</td><td>data</td><td>data</td><td>data</td></tr>
</tbody>
</table></div>
     */
    private static String getHTMLTable(Design design,int tilingFactor) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"datagrid\"><table>");
        sb.append("<thead><tr>");
        sb.append("<th>Total counts</th><th>Viewpoint characteristics</th><th>Size</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");

        int nviewpoints=design.getN_viewpoints();
        int ngenes=design.getN_genes();
        long total_active_frags=design.getN_unique_fragments();
        double avg_n_frag=design.getAvgFragmentsPerVP();
        double avg_size=design.getAvgVPsize();
        double avg_score=design.getAvgVPscore();
        int n_totalNucleotidesInProbes=design.getN_nucleotides_in_probes();
        int totalEffectiveNucleotides=design.totalEffectiveSize();

        sb.append(String.format("<tr class=\"alt\"><td>Number of genes: %d</td>"+
                "<td>Average number of active fragments per viewpoint: %.1f</td><td>Total margin size: %s nucleotides</td></tr>",
                ngenes,avg_n_frag, dformater.format(n_totalNucleotidesInProbes)));
        sb.append(String.format("<tr><td>Number of viewpoints: %d</td><td>Average viewpoint score: %.2f%%</td><td>Tiling factor: %d x</td></tr>",
                nviewpoints,100*avg_score,tilingFactor));
        sb.append(String.format("<tr class=\"alt\"><td>Number of unique fragments: %d</td><td>Average viewpoint size: %.1f nucleotides</td><td>Total effective size: %s kb</td></tr>",
                total_active_frags, avg_size,  dformater.format(totalEffectiveNucleotides/100)));
        sb.append("</tbody>\n</table></div>");
        return sb.toString();


    }




}
