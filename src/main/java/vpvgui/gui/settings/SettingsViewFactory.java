package vpvgui.gui.settings;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vpvgui.model.Settings;


/**
 * Created by peter on 01.07.17.
 */
public class SettingsViewFactory {


    public static void showSettings(Settings settings) {
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
        String html=getHTML(settings);
        presenter.setData(html);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /*
        * Formats the list properties in human-readable format, checking for an empty list.
        */
    private static String toStringHelper(String listName, ObservableList<String> lst) {
        if (lst==null || lst.isEmpty()) {
            return (String.format(""));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: (%d)\n", listName, lst.size()));
        for (String s : lst) {
            sb.append(String.format("\t%s\n", s));
        }
        return sb.toString();
    }

    private static String getHTML(Settings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>VPV Settings</h3>");
        sb.append("<p><ul>");
        sb.append(String.format("<li>Project name: %s</li>",settings.getProjectName()));

        sb.append(String.format("<li>Genome file source: %s</li>", settings.getGenomeFileURL()));
        sb.append(String.format("<li>Transcripts file source: %s</li>", settings.getTranscriptsJannovarName()));
        sb.append(String.format("<li>Genome file destination: %s</li>", settings.getGenomeFileBasename()));
        sb.append(String.format("<li>Transcripts file destination: %s</li>", settings.getTranscriptsFileTo()));
        sb.append("<li>"+toStringHelper("Restriction Enzymes", settings.getRestrictionEnzymesList())+"</li>");
        sb.append("<li>"+toStringHelper("Target Genes", settings.getTargetGenesList())+"</li>");

        sb.append("</ul></p>");
        sb.append("</body></html>");
        return sb.toString();

    }


}
