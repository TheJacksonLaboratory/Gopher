package gopher.gui.settings;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Properties;


/**
 * Class to confirmDialog the current user-chosen settings for analysis.
 * @author Peter Robinson
 * @version 0.0.3 (2017-10-20).
 */
public class SettingsViewFactory {


    public static void showSettings(Properties properties) {
        Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        SettingsView view = new SettingsView();
        SettingsPresenter presenter = (SettingsPresenter) view.getPresenter();
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
        String html=getHTML(properties);
        presenter.setData(html);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /**
     * @param properties Various items from the settings.
     * @return String containing HTML to confirmDialog in WebView pane
     */
    private static String getHTML(Properties properties) {
        String[] nonNumericProps = new String[] {
                "genome_build",  "path_to_downloaded_genome_directory", "genome_unpacked",
                "genome_indexed", "transcript_url", "target_genes_path", "refgene_path"
        };
        String[] nonNumericPropNames = new String[] {
                "Genome build", "Path to downloaded genome", "Genome decompressed",
                "Genome indexed", "Transcripts", "Path to target genes file",
                "Path to reference gene"
        };
        String[] numericProps = new String[] {
                "getFragNumUp", "fragNumDown", "getMinSizeUp", "getMaxSizeUp", "getMinSizeDown",
                "getMaxSizeDown", "getMinFragSize", "maxRepeatContent"
        };
        String[] numericPropNames = new String[] {
                "# Fragments upstream", "# Fragments downstream", "Minimum size upstream",
                "Maximum size upstream", "Minimum size downstream", "Maximum size downstream",
                "Minimum digest size", "Maximum repeat content"
        };
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>");
        sb.append(properties.getProperty("project_name"));
        sb.append("</h3><p><ul style=\"list-style: none;\">");
        for (int i = 0; i < nonNumericProps.length; i++) {
            sb.append(String.format("<li>%s: %s</li>", nonNumericPropNames[i],
                    properties.getProperty(nonNumericProps[i])));
        }
        sb.append(String.format("<li>%s: %s</li>", "Restriction enzymes",
                properties.getProperty("restriction_enzymes")));
        for (int i = 0; i < numericProps.length; i++) {
            sb.append(String.format("<li>%s: %s</li>",numericPropNames[i],
                    properties.getProperty(numericProps[i])));
        }

        sb.append("</ul></p>");
        sb.append("</body></html>");
        return sb.toString();
    }


}
