package gopher.gui.regulatoryexomebox;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import gopher.exception.GopherException;
import gopher.gui.enzymebox.EnzymeViewFactory;
import gopher.gui.popupdialog.PopupFactory;
import gopher.gui.progresspopup.ProgressPopup;
import gopher.service.model.GopherModel;
import gopher.service.model.regulatoryexome.RegulationCategory;
import gopher.service.model.regulatoryexome.RegulatoryExomeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Open up a dialog where the user can choose which categories of regulatory elements they would like
 * to export.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class RegulatoryExomeBoxFactory {
    private static Logger logger = LoggerFactory.getLogger(EnzymeViewFactory.class.getName());

    private static List<RegulationCategory> chosenCategories;
    /**
     * Initialize the Enyzme list to show any previously chosen enzyme with a check,
     * and return the enzymes that the user chooses.
     * @param model
     */
    public static void exportRegulatoryExome(GopherModel model, final File exportDir) {
        RegulatoryExomeBoxView view = new RegulatoryExomeBoxView();
        RegulatoryExomeBoxPresenter presenter = (RegulatoryExomeBoxPresenter) view.getPresenter();
        Stage window;
        String windowTitle = "Regulatory Exome";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

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
        chosenCategories = presenter.getChosenCategories();
        ProgressPopup popup = new ProgressPopup("Exporting BED file...",
                "Calculating and exporting regulatory gene panel BED file");
        ProgressIndicator progressIndicator = popup.getProgressIndicator();
        RegulatoryExomeBuilder builder = new RegulatoryExomeBuilder(model, chosenCategories,progressIndicator);
        builder.setOnFailed(e -> {
            PopupFactory.displayError("Failure to build regulatory exome.",
                    builder.getStatus());
            System.err.println(builder.getStatus());
            popup.close();
        });
        builder.setOnSucceeded(e -> {
            try {
                logger.trace(String.format("Will output regulatory panel BED file to %s", exportDir.getAbsolutePath()));
                builder.outputRegulatoryExomeBedFile(exportDir.getAbsolutePath());
                Properties regulatoryProperties = builder.getRegulatoryReport();
                model.setRegulatoryExomeProperties(regulatoryProperties);
            } catch (IOException ioe) {
                PopupFactory.displayException("Error", "Could not write regulatory exome panel to file", ioe);
            }
            popup.close();
        });
        try {
            popup.startProgress(builder);
        } catch (InterruptedException e) {
            PopupFactory.displayException("Error", "Could not download regulatory build", e);
        }

        return;
    }



    public static File getDirectoryForExport(Node rootNode) throws GopherException {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose directory to store regulatory exome BED file.");
        File file = dirChooser.showDialog(rootNode.getScene().getWindow());
        if (file==null || file.getAbsolutePath().equals("")) {
            throw new GopherException("Could not set directory to write regulatory exome BED file.");
        }
        return file;
    }





}
