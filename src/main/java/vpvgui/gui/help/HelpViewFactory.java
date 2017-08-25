package vpvgui.gui.help;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Created by peterrobinson on 7/3/17.
 */
public class HelpViewFactory {




    public static void display() {
        Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        HelpView view = new HelpView();
        HelpPresenter presenter = (HelpPresenter) view.getPresenter();

        String html=getHTML();
        presenter.setData(html);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /*
        * Formats the list properties in human-readable format, checking for an empty list.
        */
    private static String toStringHelper(String listName, ObservableList<String> lst) {
        if (lst==null || lst.isEmpty()) {
            return ("");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: (%d)\n", listName, lst.size()));
        for (String s : lst) {
            sb.append(String.format("\t%s\n", s));
        }
        return sb.toString();
    }

    private static String getHTML() {
        String sb = "<html><body><h3>VPV Help</h3>" +
                "<p>VPV (<i>ViewPoint Viewer</i>) is designed to facilitate the design of Viewpoints for capture Hi-C and related" +
                " methods by visualizing the restriction fragments that surround the one or multiple transcription" +
                "start sites of a gene in the context of their genomic position and repat content." +
                " Users can choose from several restriction enzymes and set a number of parameters that are explained below.</p>" +
                "<p><b>Genome Build</b>VPV currently supports hg37, hg38 and mm10. Choosing a genome build with the pull down menu" +
                "will determine which files are downloaded by the Genome and Transcripts buttons.</p>" +
                "</body></html>";
        return sb;

    }

}
