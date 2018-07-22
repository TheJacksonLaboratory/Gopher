package gopher.gui.qcCheckPane;

import javafx.scene.Scene;
import javafx.stage.Stage;

import gopher.model.Model;

import java.io.File;
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
        String windowTitle = "Gopher Parameter Check";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close() );
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

    private static String getHTML(Model model) {
        String dataQC = validateData(model);
        String paramQC=validateParams(model);
        return String.format("%s%s%s%s",String.format(HTML_HEADER,getCSSblock()),dataQC,paramQC,HTML_FOOTER);
    }



    private static String validateParams(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Design parameters</th></tr>");
        sb.append("</thead>");
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
        sb.append("<tr><td>Maximum kmer alignability</td>");
        if (model.getMaxMeanKmerAlignability()>10) {
            sb.append(String.format("<td class=\"red\">%d</td>",model.getMaxMeanKmerAlignability()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getMaxMeanKmerAlignability()));
        }
        sb.append("</tr>");
        String approach=model.getApproach().toString();
        sb.append(String.format("<tr><td>Design approach</td><td>%s</td></tr>",approach));
        sb.append("<tr><td>Probe length</td>");
        if (model.getProbeLength()<120) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",model.getProbeLength()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getProbeLength()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Minimum bait count</td>");
        sb.append(String.format("<td>%d</td>",model.getMinBaitCount()));
        sb.append("</tr>");
        sb.append("<tr><td>Patched viewpoints (simple only)</td>");
        if (model.getAllowPatching()) {
            sb.append("<td>yes</td>");
        } else {
            sb.append("<td>no</td>");
        }
        sb.append("</tr>");
        sb.append("<tr><td>Unbalanced margins</td>");
        if (model.getAllowSingleMargin()) {
            sb.append("<td>yes</td>");
        } else {
            sb.append("<td>no</td>");
        }
        sb.append("</tr>");


        sb.append("<tr><td>Margin size</td>");
        if (model.getMarginSize()>250 || model.getMarginSize()<120) {
            sb.append(String.format("<td class=\"red\">%d nt</td>",model.getMarginSize()));
        } else {
            sb.append(String.format("<td>%d</td>",model.getMarginSize()));
        }
        sb.append("</tr>");
        sb.append("</tbody>\n</table>");


        sb.append("<p>Click <b><tt>Cancel</tt></b> to go back and adjust parameters. Red values are outside of the usual " +
                "recommended range but may be appropriate depending on experimental needs and goals. " +
                "Click <b><tt>Continue</tt></b> to generate Capture Hi-C probes.</p>");

        return sb.toString();
    }


    /**
     * Perform a Q/C check of the data that we are using to create the Viewpoints, mainly whether the data is
     * complete.
     * @param model Model of the probe design
     * @return HTML string with summary of Q/C
     */
    private static String validateData(Model model) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Data sources</th></tr>");
        sb.append("</thead>");
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
            String msg;
            if (model.isGenomeUnpacked()) {
                msg="Genome was not correctly indexed";
            } else {
                msg="Genome has not been unpacked";
            }
            sb.append(String.format("<td class=\"red\">%s</td>",msg));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Alignability map</td>");
        String alignabilityMap = model.getAlignabilityMapPathIncludingFileNameGz();
        int i=alignabilityMap.lastIndexOf(File.separator);
        if (i>0) {
            alignabilityMap=alignabilityMap.substring(i+1);
        }
        if (alignabilityMap!=null) {
            sb.append("<td>"+alignabilityMap+"</td>");
        } else {
            sb.append("<td class=\"red\">Alignability map not found</td>");
        }
        sb.append("</tr>");
        sb.append("<tr><td>Restriction enzyme</td>");
        if (model.getChosenEnzymelist()==null || model.getChosenEnzymelist().isEmpty()) {
            sb.append("<td class=\"red\">Restriction enzyme not initialized.</td>");
        } else {
            sb.append(String.format("<td>%s</td>",model.getAllSelectedEnzymeString()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Project file</td>");
        if (model.getProjectName()==null) {
            sb.append("<td class=\"red\">Project file name not initialized.</td>");
        } else {
            sb.append(String.format("<td>%s</td>",model.getProjectName()));
        }
        sb.append("</tr>");
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Enrichment targets</th></tr>");
        sb.append("</thead>");
        sb.append("<tr><td>Target file</td>");
        if (model.getRefGenePath()==null) {
            sb.append("<td class=\"red\">Targets not initialized.</td>");
        } else {
            sb.append(String.format("<td>%s</td>",model.getRefGenePath()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Gene list</td>");
        if (model.getGopherGeneList()==null|| model.getGopherGeneList().size()==0) {
            sb.append("<td class=\"red\">Gene list not initialized.</td>");
        } else {
            sb.append(String.format("<td>%d chosen genes from %d in transcriptome</td>",model.getChosenGeneCount(), model.getTotalRefGeneCount()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Unique TSS</td>");
        if (model.getGopherGeneList()==null|| model.getGopherGeneList().size()==0) {
            sb.append("<td class=\"red\">TSS list not initialized.</td>");
        } else {
            sb.append(String.format("<td>%d unique transcription starts (from total of %d)</td>",model.getUniqueChosenTSScount(),model.getUniqueTSScount()));
        }
        sb.append("</tr>");
        return sb.toString();
    }


    /**
     * @return a block of CSS code intended for the blue-beige Table of data on the design.
     */
    private static String getCSSblock() {
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
