package gopher.gui.entrezgenetable;

import gopher.model.Model;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.Reader;

/**
 * Coordinate processing of the uploaded genes (target genes) and their comparison with the refGene.txt.gz file. THe main
 * GUI will call {@link #displayFromFile(Model, Reader)} if the dialog is opened from an example file from the Help menu
 * and otherwise will call {@link #display(Model)} to allow the user to choose a file with a FileChooser.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2017-11-19)
 */
public class EntrezGeneViewFactory {

    /** This is intended to be used to confirmDialog example sets of genes chosen from the Help menu. */
    public static void displayFromFile(Model model, Reader reader) {
        Stage window;
        String windowTitle = "Enter Gene List";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close() );
        window.setTitle(windowTitle);

        EntrezGeneView view = new EntrezGeneView();
        EntrezGenePresenter presenter = (EntrezGenePresenter) view.getPresenter();
        presenter.setModel(model);
        presenter.uploadGenesFromFile(reader);
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
        window.setOnCloseRequest( event -> window.close() );
        window.setTitle(windowTitle);

        EntrezGeneView view = new EntrezGeneView();
        EntrezGenePresenter presenter = (EntrezGenePresenter) view.getPresenter();
        presenter.setModel(model);
        presenter.setSignal(signal -> {
            switch (signal) {
                case DONE:
                case CANCEL:
                    window.close();
                    break;
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
        sb.append("The file must have one gene on a line and use HGNC gene symbols. Alternatively copy the list of genes with the " +
                "<b><TT>Clipboard</TT></b> button.");
        sb.append("</p>");
        sb.append("<p>");
        sb.append("Then click on <b><TT>Validate</TT></b> to check whether your list contains valid HGNC gene symbols ");
        sb.append("If your gene list contains invalid gene symbols, revise your file before proceeding.");
        sb.append("Finally, click on <b><TT>Accept</TT></b> or <b><TT>Cancel</TT></b>. ");
        sb.append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

}
