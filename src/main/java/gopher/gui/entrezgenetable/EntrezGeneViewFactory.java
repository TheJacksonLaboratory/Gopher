package gopher.gui.entrezgenetable;

import gopher.service.model.GopherModel;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.Reader;

/**
 * Coordinate processing of the uploaded genes (target genes) and their comparison with the refGene.txt.gz file. THe main
 * GUI will call {@link #displayFromFile(GopherModel, Reader)} if the dialog is opened from an example file from the Help menu
 * and otherwise will call {@link #display(GopherModel)} to allow the user to choose a file with a FileChooser.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.2 (2017-11-19)
 */
public class EntrezGeneViewFactory {

    /** This is intended to be used to confirmDialog example sets of genes chosen from the Help menu. */
    public static void displayFromFile(GopherModel model, Reader reader) {
        Stage window;
        String windowTitle = "Target Gene List";
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
                case CANCEL:
                    window.close();
                    break;
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }
        });

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /** This causes the gene upload window to be displayed with an introductory text. */
    public static void display(GopherModel model) {
        Stage window;
        String windowTitle = "Target Gene List";
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
        return "<html><body><h3>Enter genes</h3>" +
        "<p>" +
        "Use the <b><TT>Upload</TT></b> button to load a file containing the target genes for a Capture Hi-C experiment. " +
        "The file must have one gene on a line and use HGNC gene symbols. Alternatively use the " +
                "<b><TT>Clipboard</TT></b> button to load the gene symbols from text in the system clipboard." +
        "</p>" +
        "<p>" +
        "Then click on <b><TT>Validate</TT></b> to check whether your list contains valid HGNC gene symbols " +
        "If your gene list contains invalid gene symbols, revise your file before proceeding." +
        "Finally, click on <b><TT>Accept</TT></b>. Click on <b><TT>Cancel</TT> return to the main window without any changes.</b>. " +
        "</p>" +
        "</body></html>";
    }

}
