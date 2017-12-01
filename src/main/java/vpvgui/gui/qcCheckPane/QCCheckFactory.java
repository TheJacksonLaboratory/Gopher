package vpvgui.gui.qcCheckPane;

import javafx.scene.Scene;
import javafx.stage.Stage;

import vpvgui.model.Model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * This class coordinates the creation of the HTML code for the parameter check window.
 * @author Peter Robinson
 * @version 0.0.2 (2017-11-01)
 */
public class QCCheckFactory {
    private static final String HTML_HEADER = "<html><head>%s</head><body><h1>Parameter QC</h1>";
    private static final String HTML_FOOTER = "</body></html>";
    private static NumberFormat dformater= NumberFormat.getInstance(Locale.US);

    public static boolean showQCCheck(Model model) {
        Stage window;
        String windowTitle = "VPV Parameter Check";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        QCCheckView view = new QCCheckView();
        QCCheckPresenter presenter = (QCCheckPresenter) view.getPresenter();
        presenter.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });
        String html= getHTML(model);
        presenter.setData(html);

        if (model.getViewPointList() != null && model.getViewPointList().size()>0) {
            presenter.setLabel("Warning: this step will overwrite current Viewpoints");
        }

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
        return (! presenter.wasCanceled());
    }

    public static String getHTML(Model model) {
        String dataQC = validateData(model);
        String paramQC=validateParams(model);
        String designQC=validateDesignParams(model);
        String expla=getExplanations();
        return String.format("%s%s%s%s%s%s",String.format(HTML_HEADER,getCSSblock()),dataQC,paramQC,designQC,expla,HTML_FOOTER);
    }


    private static String getExplanations() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Explanations</h3>");
        sb.append("<p>Values shown in red may be incorrect and should be carefully examined by users. Click <tt>Cancel</tt> to "+
        "go back and correct entries or <tt>Continue</tt> to generate viewpoints.</p>");
        sb.append("<h4>Data</h4>");
        sb.append("<p class=\"ex\">Use the VPVGui Genome: <tt>Download</tt> button to set the location of the genome directory. "+
                " If the directory is empty (or does not contain UCSC genome files) then VPVGui will download the genome file to "+
                " this directory. Use the Decompress genome: <tt>Start</tt> button to extract the genome file (which is provided" +
                " as a gzipped archive file). Use the Index genome FASTA: <tt>Start</tt> button to create index files (*.fai) " +
                " for the FASTA files that are extracted from the gzipped file.</p>");
        sb.append("<p class=\"ex\">Similarly, the Transcripts: <tt>Download</tt> button will download the indicated transcript definition file to the" +
                " indicated directory unless a transcript file is already present. Be careful not to mix up assemblies; for instance, " +
                "using a transcript file for hg38 will lead to incorrect results if used for a genome file for hg19.</p> ");
        sb.append("<h4>Parameters</h4>");
        sb.append("<p class=\"ex\">Upstream and Downstream are the lengths with respect to the transcript start sites that will be included in" +
                " the search for viewpoints and fragments. All fragments that are located within or overlap with these positions will be included" +
                " in the probe design created by VPVGui if they fulfil the remaining criteria listed below.<p>");
        sb.append("<p class=\"ex\">The minimum probe size refers to the size of the restriction fragment. Any candidate fragment smaller than this " +
                "size is rejected because it is unlikely to enrich well in the capture Hi-C procedure and may be difficult to map.</p>");
        sb.append("<p class=\"ex\">The minimum and maximum GC content parameters determine the minimum and maximum allowed content of G and C bases" +
                " for a fragment. Values above and below these criteria are difficult to enrich and sequence and are therefore rejected from the" +
                " probe design.</p>");
        sb.append("<p class=\"ex\">The maximum repeat content parameter uses the determination of repeat sequences of the UCSC Genome Browser " +
                "(which in turn is based on repeatmasker). Fragments with a higher repeat content than this value are reject because they" +
                " are difficult to map.</p>");
        sb.append("<h4>Design Parameters</h4>");
        sb.append("<p class=\"ex\">The probe length is the length of probes that will be ordered and usually is determined by the" +
                " technology of the capture probe vendor. Ensure that the value you use matches the length that will be manufactured" +
                " by the chosen capture probe vendor. The tiling factor refers to the number of probes that on" +
                " average cover any given nucletide of the target region.</p>");
        return sb.toString();
    }




    private static String validateDesignParams(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Panel design parameters</h3>");
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead><tr>");
        sb.append("<th>Item</th><th>Result</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");
        sb.append("<tr><td>Probe length</td>");
        if (model.getProbeLength()<120) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",model.getProbeLength()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getProbeLength()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Tiling factor</td>");
        sb.append(String.format("<td>%d</td>",model.getTilingFactor()));
        sb.append("</tr>");
        sb.append("<tr><td>Margin size</td>");
        if (model.getMarginSize()>250 || model.getMarginSize()<120) {
            sb.append(String.format("<td class=\"red\">%d nt</td>",model.getMarginSize()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getMarginSize()));
        }
        sb.append("</tr>");
        sb.append("</tbody>\n</table>");

        return sb.toString();
    }


    private static String validateParams(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Viewpoint parameters</h3>");
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead><tr>");
        sb.append("<th>Item</th><th>Result</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");
        sb.append("<tr><td>Upstream size</td>");
        if (model.getSizeUp()<100) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",model.getSizeUp()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getSizeUp()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Downstream size</td>");
        if (model.getSizeDown()<100) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",model.getSizeDown()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getSizeDown()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Minimum fragment size</td>");
        if (model.getMinFragSize()<120) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",model.getMinFragSize()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getMinFragSize()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Minimum GC content</td>");
        if (model.getMinGCcontent()<0.25) {
            sb.append(String.format("<td class=\"red\">%.1f%%</td>",model.getMinGCContentPercent()));
        } else {
            sb.append(String.format("<td>%.1f%%</td>",model.getMinGCContentPercent()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Maximum GC content</td>");
        if (model.getMaxGCcontent()>0.70) {
            sb.append(String.format("<td class=\"red\">%.1f%%</td>",model.getMaxGCContentPercent()));
        } else {
            sb.append(String.format("<td>%.1f%%</td>",model.getMaxGCContentPercent()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Maximum repeat content</td>");
        if (model.getMaxRepeatContent()>0.65) {
            sb.append(String.format("<td class=\"red\">%.1f%%</td>",model.getMaxRepeatContentPercent()));
        } else {
            sb.append(String.format("<td>%.1f%%</td>",model.getMaxRepeatContentPercent()));
        }
        sb.append("</tr>");
        String approach=model.getApproach().toString();
        sb.append(String.format("<tr><td>Design approach</td><td>%s</td></tr>",approach));

        sb.append("</tbody>\n</table>");

        return sb.toString();
    }



    private static String validateData(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Data section</h3>");
//        sb.append("<p>This section provides a summary of the input data required for VPVGui.</p>");
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead><tr>");
        sb.append("<th>Item</th><th>Result</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");
        sb.append("<tr><td>Genome</td>");
        if (model.getGenomeDirectoryPath()==null) {
            sb.append("<td class=\"red\">Path to genome directory not initialized.</td>");
        } else {
            String g = String.format("%s (at %s).",model.getGenomeBuild(),model.getGenomeDirectoryPath());
            sb.append(String.format("<td>%s</td>",g));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Initialization</td>");
        if (model.isGenomeUnpacked() && model.isGenomeIndexed()) {
            sb.append("<td>Genome correctly extracted and indexed.</td>");
        } else {
            String msg="";
            if (model.isGenomeUnpacked()) {
                msg="Genome was not correctly indexed";
            } else {
                msg="Genome has not been unpacked";
            }
            sb.append(String.format("<td class=\"red\">%s</td>",msg));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Gene list</td>");
        if (model.getVPVGeneList()==null|| model.getVPVGeneList().size()==0) {
            sb.append("<td class=\"red\">Gene list not initialized.</td>");
        } else {
            sb.append(String.format("<td>%d chosen genes from %d in transcriptome</td>",model.getChosenGeneCount(), model.getTotalRefGeneCount()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Unique TSS</td>");
        if (model.getVPVGeneList()==null|| model.getVPVGeneList().size()==0) {
            sb.append("<td class=\"red\">TSS list not initialized.</td>");
        } else {
            sb.append(String.format("<td>%d unique transcription starts (from total of %d)</td>",model.getUniqueChosenTSScount(),model.getUniqueTSScount()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Restriction enzyme</td>");
        if (model.getChosenEnzymelist()==null || model.getChosenEnzymelist().isEmpty()) {
            sb.append("<td class=\"red\">Restriction enzyme not initialized.</td>");
        } else {
            sb.append(String.format("<td>%s</td>",model.getRestrictionEnzymeString()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Transcripts</td>");
        if (model.getRefGenePath()==null) {
            sb.append("<td class=\"red\">Transcript list not initialized.</td>");
        } else {
            sb.append(String.format("<td>%s</td>",model.getRefGenePath()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Project file</td>");
        if (model.getProjectName()==null) {
            sb.append("<td class=\"red\">Project file name not initialized.</td>");
        } else {
            sb.append(String.format("<td>%s</td>",model.getProjectName()));
        }
        sb.append("</tr>");
        sb.append("</tbody>\n</table>");

        return sb.toString();
    }


    /**
     * @return a block of CSS code intended for the blue-beige Table of data on the design.
     */
    public static String getCSSblock() {
        return "<style>\n" +
                "h1 {\n" +
                "\tfont-size: 16;\n" +
                "  font-weight: bold;\n" +
                "  color: #1C6EA4;\n" +
                "}\n" +
                "h3 {\n" +
                "\tfont-size: 16;\n" +
                "  font-weight: italic;\n" +
                "  color: #1C6EA4;\n" +
                "}\n" +
                "h4 {\n" +
                "\tfont-size: 12;\n" +
                "  font-weight: italic;\n" +
                "  color: #1C6EA4;\n" +
                "}\n" +
                "p.ex {\n" +
                "\tfont-size: 9;\n" +
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
                "table.vpvTable td.red {\n" +
                "  color: red;\n" +
                "  font-weight: bold;\n" +
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


}
