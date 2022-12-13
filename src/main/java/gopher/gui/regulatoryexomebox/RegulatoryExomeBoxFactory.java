package gopher.gui.regulatoryexomebox;

import gopher.service.GopherService;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import gopher.exception.GopherException;
import gopher.gui.factories.EnzymeViewFactory;
import gopher.gui.factories.PopupFactory;
import gopher.gui.progresspopup.ProgressPopup;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(EnzymeViewFactory.class.getName());

    private static List<RegulationCategory> chosenCategories;
    /**
     * Initialize the Enyzme list to show any previously chosen enzyme with a check,
     * and return the enzymes that the user chooses.
     * @param service GopherService
     */
    public static void exportRegulatoryExome(GopherService service, final File exportDir) {
        Stage window;
        String windowTitle = "Regulatory Exome";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close());
        window.setTitle(windowTitle);

        ProgressPopup popup = new ProgressPopup("Exporting BED file...",
                "Calculating and exporting regulatory gene panel BED file");
        ProgressIndicator progressIndicator = popup.getProgressIndicator();
        RegulatoryExomeBuilder builder = new RegulatoryExomeBuilder(service, chosenCategories,progressIndicator);
        builder.setOnFailed(e -> {
            PopupFactory.displayError("Failure to build regulatory exome.",
                    builder.getStatus());
            LOGGER.error(builder.getStatus());
            popup.close();
        });
        builder.setOnSucceeded(e -> {
            try {
                LOGGER.trace(String.format("Will output regulatory panel BED file to %s", exportDir.getAbsolutePath()));
                builder.outputRegulatoryExomeBedFile(exportDir.getAbsolutePath());
                Properties regulatoryProperties = builder.getRegulatoryReport();
                service.setRegulatoryExomeProperties(regulatoryProperties);
            } catch (IOException ioe) {
                PopupFactory.displayException("Error", "Could not write regulatory exome panel to file", ioe);
            }
            popup.close();
        });
        popup.startProgress(builder);
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
