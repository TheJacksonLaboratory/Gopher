package gopher.gui.factories;

import gopher.io.Platform;
import gopher.service.GopherService;
import gopher.service.model.GopherModel;
import gopher.service.model.dialog.RestrictionEnzymeResult;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static javafx.application.Platform.runLater;


public class QCCheckFactory implements Initializable {
    static Logger logger = LoggerFactory.getLogger(QCCheckFactory.class.getName());
    @FXML
    private WebView wview;

    private WebEngine webEngine;

    @FXML private Button cancelButon;
    @FXML private Button continueButton;
    @FXML private Label warning;

    private boolean wasCanceled=true;

    private final GopherService gopherService;

    private static final String HTML_HEADER = "<html><head>%s</head><body><h1>Parameter QC</h1>";
    private static final String HTML_FOOTER = "</body></html>";
    private static NumberFormat dformater= NumberFormat.getInstance(Locale.US);
    /** This will be set to false if files are missing and we should not go on with the analysis. */
    private static boolean qc_ok=true;


    public void initialize(URL location, ResourceBundle resources) {
        webEngine = wview.getEngine();
        webEngine.setUserDataDirectory(new File(Platform.getWebEngineUserDataDirectory(), getClass().getCanonicalName()));
    }

    public QCCheckFactory(GopherService gopherService) {
        this.gopherService = gopherService;
    }

    public boolean loadQcDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Check the settings you entered");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        WebView wv = new WebView();
        webEngine = wv.getEngine();
        webEngine.setUserDataDirectory(new File(Platform.getWebEngineUserDataDirectory(), getClass().getCanonicalName()));
        String html = getHTML();
        webEngine.loadContent(html);
        dialogPane.setContent(wv);
        runLater(wv::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return true;// Optional.of(new Boolean(true));
            }
            return false;
        });
        Optional<Boolean> optionalResult = dialog.showAndWait();
        return optionalResult.get();
//       dialogPane.setContent(restrictionVBox);
//        runLater(restrictionVBox::requestFocus);
//        dialog.setResultConverter((ButtonType button) -> {
//            if (button == ButtonType.OK) {
//                return new RestrictionEnzymeResult(this.chosen);
//            }
//            return null;
//        });
//        Optional<RestrictionEnzymeResult> optionalResult = dialog.showAndWait();
//        if (optionalResult.isPresent()) {
//            return optionalResult.get().chosenEzymes();
//        } else {
//            return List.of();
//        }
    }

    public void setData(String html) {
        webEngine.loadContent(html);
    }

    @FXML public void cancelButtonClicked(ActionEvent e) {
        e.consume();
        wasCanceled=true;
    }

    @FXML public void continueButtonClicked(ActionEvent e) {
        e.consume();
        wasCanceled=false;
    }


    public boolean wasCanceled() { return  this.wasCanceled;}

    public void setLabel(String text) {
        warning.setStyle("-fx-text-alignment: right; -fx-font-size: 14pt; -fx-text-fill: red; ");
        warning.setText(text);
    }
    public boolean showQCCheck(GopherModel model) {
        Stage window;
        String windowTitle = "GOPHER Parameter Check";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close() );
        window.setTitle(windowTitle);

      //  QCCheckView view = new QCCheckView();
       // QCCheckPresenter presenter = (QCCheckPresenter) view.getPresenter();

        String html= getHTML();
        if (!qc_ok) {
            html=getErrorHTML();
        }
        setData(html);

        if (model.getViewPointList() != null && model.getViewPointList().size()>0) {
            setLabel("Warning: this step will overwrite current Viewpoints");
        }

        //window.setScene(new Scene(view.getView()));
        window.showAndWait();
        return (! wasCanceled() && qc_ok);
    }

    private String getHTML() {
        String dataQC = validateData();
        String paramQC = validateParams();
        return String.format("%s%s%s%s",String.format(HTML_HEADER,getCSSblock()),dataQC,paramQC,HTML_FOOTER);
    }



    private String validateParams() {
        StringBuilder sb = new StringBuilder();
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Design parameters</th></tr>");
        sb.append("</thead>");
        sb.append("<tr><td>Upstream size</td>");
        if (gopherService.getSizeUp()<100) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",gopherService.getSizeUp()));
        } else {
            sb.append(String.format("<td>%d</td>",gopherService.getSizeUp()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Downstream size</td>");
        if (gopherService.getSizeDown()<100) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",gopherService.getSizeDown()));
        } else {
            sb.append(String.format("<td>%d</td>",gopherService.getSizeDown()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Minimum digest size</td>");
        if (gopherService.getMinFragSize()<120) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>",gopherService.getMinFragSize()));
        } else {
            sb.append(String.format("<td>%d</td>",gopherService.getMinFragSize()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Minimum GC content</td>");
        if (gopherService.getMinGCcontent()<0.25) {
            sb.append(String.format("<td class=\"red\">%.1f%%</td>",gopherService.getMinGCContentPercent()));
        } else {
            sb.append(String.format("<td>%.1f%%</td>",gopherService.getMinGCContentPercent()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Maximum GC content</td>");
        if (gopherService.getMaxGCcontent()>0.70) {
            sb.append(String.format("<td class=\"red\">%.1f%%</td>",gopherService.getMaxGCContentPercent()));
        } else {
            sb.append(String.format("<td>%.1f%%</td>",gopherService.getMaxGCContentPercent()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Maximum kmer alignability</td>");
        if (gopherService.getMaxMeanKmerAlignability()>10) {
            sb.append(String.format("<td class=\"red\">%d</td>",gopherService.getMaxMeanKmerAlignability()));
        } else {
            sb.append(String.format("<td>%d</td>",gopherService.getMaxMeanKmerAlignability()));
        }
        sb.append("</tr>");
        String approach = gopherService.getApproach().toString();
        sb.append(String.format("<tr><td>Design approach</td><td>%s</td></tr>",approach));
        sb.append("<tr><td>Probe length</td>");
        if (gopherService.getProbeLength()<120) {
            sb.append(String.format("<td class=\"red\">%d nt (unusually short).</td>", gopherService.getProbeLength()));
        } else {
            sb.append(String.format("<td>%d</td>", gopherService.getProbeLength()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Minimum probe (bait) count</td>");
        sb.append(String.format("<td>%d</td>", gopherService.getMinBaitCount()));
        sb.append("</tr>");
        sb.append("<tr><td>Patched viewpoints (simple only)</td>");
        if (gopherService.getAllowPatching()) {
            sb.append("<td>yes</td>");
        } else {
            sb.append("<td>no</td>");
        }
        sb.append("</tr>");
        sb.append("<tr><td>Unbalanced margins</td>");
        if (gopherService.getAllowUnbalancedMargins()) {
            sb.append("<td>yes</td>");
        } else {
            sb.append("<td>no</td>");
        }
        sb.append("</tr>");


        sb.append("<tr><td>Margin size</td>");
        if (gopherService.getMarginSize() > 250 || gopherService.getMarginSize() < 120) {
            sb.append(String.format("<td class=\"red\">%d nt</td>",gopherService.getMarginSize()));
        } else {
            sb.append(String.format("<td>%d</td>",gopherService.getMarginSize()));
        }
        sb.append("</tr>");
        sb.append("</tbody>\n</table>");


        sb.append("<p>Click <b><tt>Cancel</tt></b> to go back and adjust parameters. Red values are outside of the usual " +
                "recommended range but may be appropriate depending on experimental needs and goals. " +
                "Click <b><tt>OK</tt></b> to generate Capture Hi-C probes.</p>");

        return sb.toString();
    }


    /**
     * Perform a Q/C check of the data that we are using to create the Viewpoints, mainly whether the data is
     * complete.
     * @return HTML string with summary of Q/C
     */
    private String validateData() {
        qc_ok=true;  // reset
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Data sources</th></tr>");
        sb.append("</thead>");
        sb.append("<tbody>");
        sb.append("<tr><td>Genome</td>");
        if (gopherService.getGenomeDirectoryPath()==null) {
            sb.append("<td class=\"red\">Path to genome directory not initialized.</td>");
            qc_ok=false;
        } else {
            String g = String.format("%s (at %s).",gopherService.getGenomeBuild(), gopherService.getGenomeDirectoryPath());
            sb.append(String.format("<td>%s</td>",g));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Initialization</td>");
        if (gopherService.isGenomeUnpacked() && gopherService.isGenomeIndexed()) {
            sb.append("<td>Genome correctly extracted and indexed.</td>");
        } else {
            String msg;
            if (gopherService.isGenomeUnpacked()) {
                msg="Genome was not correctly indexed";
            } else {
                msg="Genome has not been unpacked";
            }
            qc_ok=false;
            sb.append(String.format("<td class=\"red\">%s</td>",msg));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Alignability map</td>");
        String alignabilityMap = gopherService.getAlignabilityMapPathIncludingFileNameGz();

        if (alignabilityMap!=null) {
            int i=alignabilityMap.lastIndexOf(File.separator);
            if (i>0) {
                alignabilityMap=alignabilityMap.substring(i+1);
            }
            sb.append("<td>").append(alignabilityMap).append("</td>");
        } else {
            sb.append("<td class=\"red\">Alignability map not found</td>");
            qc_ok=false;
        }
        sb.append("</tr>");
        sb.append("<tr><td>Restriction enzyme</td>");
        if (gopherService.getChosenEnzymelist()==null || gopherService.getChosenEnzymelist().isEmpty()) {
            sb.append("<td class=\"red\">Restriction enzyme not initialized.</td>");
            qc_ok=false;
        } else {
            sb.append(String.format("<td>%s</td>", gopherService.getAllSelectedEnzymeString()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Project file</td>");
        if (gopherService.getProjectName()==null) {
            sb.append("<td class=\"red\">Project file name not initialized.</td>");
            qc_ok=false;
        } else {
            sb.append(String.format("<td>%s</td>", gopherService.getProjectName()));
        }
        sb.append("</tr>");
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Enrichment targets</th></tr>");
        sb.append("</thead>");
        sb.append("<tr><td>Target file</td>");
        if (gopherService.getRefGenePath()==null) {
            sb.append("<td class=\"red\">Targets not initialized.</td>");
            qc_ok=false;
        } else {
            sb.append(String.format("<td>%s</td>", gopherService.getRefGenePath()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Gene list</td>");
        if (gopherService.getGopherGeneList().isEmpty()) {
            sb.append("<td class=\"red\">Gene list not initialized.</td>");
            qc_ok=false;
        } else {
            sb.append(String.format("<td>%d chosen genes from %d in transcriptome</td>",
                    gopherService.getChosenGeneCount(), gopherService.getTotalRefGeneCount()));
        }
        sb.append("</tr>");
        sb.append("<tr><td>Unique TSS</td>");
        if (gopherService.getGopherGeneList().isEmpty()) {
            sb.append("<td class=\"red\">TSS list not initialized.</td>");
            qc_ok=false;
        } else {
            sb.append(String.format("<td>%d unique transcription starts (from total of %d)</td>",
                    gopherService.getUniqueChosenTSScount(), gopherService.getUniqueTSScount()));
        }
        sb.append("</tr>");
        return sb.toString();
    }

    private String getErrorHTML() {
        StringBuilder sb=new StringBuilder();
        sb.append("<table class=\"vpvTable\">");
        sb.append("<thead>");
        sb.append("<tr><th colspan=\"2\">Errors</th></tr>");
        sb.append("</thead>");
        sb.append("<tbody>");
        if (gopherService.getGenomeDirectoryPath()==null) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Path to genome directory not initialized.</td></tr>");
        }
        if (! gopherService.isGenomeUnpacked()) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Genome was not unpacked</td></tr>");
        }
        if (! gopherService.isGenomeIndexed()) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Genome was not correctly indexed</td></tr>");
        }
        String alignabilityMap = gopherService.getAlignabilityMapPathIncludingFileNameGz();

        if (alignabilityMap==null) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Alignability map not found</td></tr>");
        }
        if (gopherService.getChosenEnzymelist()==null || gopherService.getChosenEnzymelist().isEmpty()) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Restriction enzyme not initialized.</td></tr>");
        }
        if (gopherService.getProjectName()==null) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Project file name not initialized.</td></tr>");
        }
        if (gopherService.getRefGenePath()==null) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Targets not initialized.</td></tr>");
        }
        if (gopherService.getGopherGeneList()==null|| gopherService.getGopherGeneList().size()==0) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">Gene list not initialized.</td></tr>");
        }
        if (gopherService.getGopherGeneList()==null|| gopherService.getGopherGeneList().size()==0) {
            sb.append("<tr colspan=\"2\"><td class=\"red\">TSS list not initialized.</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p>Please initialize GOPHER with the required items before creating viewpoints.</p>");
        return String.format("%s%s%s",String.format(HTML_HEADER,getCSSblock()),sb.toString(),HTML_FOOTER);
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
