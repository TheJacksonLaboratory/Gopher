package vpvgui.gui.entrezgenetable;

import javafx.scene.Scene;
import javafx.stage.Stage;
import vpvgui.model.Model;

import java.io.File;

/**
 * Coordinate processing of the uploaded genes (target genes) and their comparison with the refGene.txt.gz file. THe main
 * GUI will call {@link #displayFromFile(Model, File)} if the dialog is opened from an example file from the Help menu
 * and otherwise will call {@link #display(Model)} to allow the user to choose a file with a FileChooser.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2017-11-19)
 */
public class EntrezGeneViewFactory {

    /** This is intended to be used to confirmDialog example sets of genes chosen from the Help menu. */
    public static void displayFromFile(Model model, File file){
        Stage window;
        String windowTitle = "Enter Gene List";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        EntrezGeneView view = new EntrezGeneView();
        EntrezGenePresenter presenter = (EntrezGenePresenter) view.getPresenter();
        presenter.setModel(model);
        presenter.uploadGenesFromFile(file);
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

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /** This causes the gene upload window to be displayed with an introductory text. */
    public static void display(Model model) {
        Stage window;
        String windowTitle = "Enter Gene List";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        EntrezGeneView view = new EntrezGeneView();
        EntrezGenePresenter presenter = (EntrezGenePresenter) view.getPresenter();
        presenter.setModel(model);
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
        String html=getHTML();
        presenter.setData(html);
        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    private static String getHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>Enter genes</h3>");
        sb.append("<p>");
        sb.append("Use the <b><TT>Upload</TT></b> button to load a file containing the target genes for a Capture Hi-C experiment. ");
        sb.append("The file must have one gene on a line and use HGNC gene symbols");
        sb.append("</p>");
        sb.append("<p>");
        sb.append("Following this, click on <b><TT>Validate</TT></b> to check whether your list contains valid HGNC gene symbols ");
        sb.append("(This step may take 10-20 seconds to complete). ");
        sb.append("If your gene list contains invalid gene symbols, revise your file before proceeding.");
        sb.append("</p>");
        sb.append("<p>");
        sb.append("Finally, click on <b><TT>Accept</TT></b> to import the corresponding genes and transcripts into. ");
        sb.append("the ViewPoint Viewer app.");
        sb.append("</p>");
        sb.append("</body></html>");
        return sb.toString();

    }


}
