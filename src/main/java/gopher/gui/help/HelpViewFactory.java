package gopher.gui.help;

import gopher.io.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Optional;

/**
 * A helper class that displays the Read-the-docs documentation for VPV in a JavaFX webview browser
 * @author Peter Robinson
 * @version 0.2.1 (2017-11-02)
 */
public class HelpViewFactory {
    private static final Logger logger = Logger.getLogger(HelpViewFactory.class.getName());
    private static final String READTHEDOCS_SITE = "http://gopher.readthedocs.io/en/latest/";


    public static void display() {
        openHelpDialog();
    }


    private static String getHTML() {
        return "<html><body><h3>GOPHER Help</h3>" +
                "<p><i>GOPHER</i> is designed to facilitate the design of probes for capture Hi-C and related" +
                " methods by visualizing the restriction fragments that surround the one or multiple transcription" +
                "start sites of a gene (a viewpoint) in the context of their genomic position and repeat content.</p>" +
                "<p>Documentation can be found at the <a href=\"https://readthedocs.org/projects/gopher/\">GOPHER Documentation Website</a>.</p>" +
                "</body></html>";

    }





    private static void openHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("GOPHER Help");
        alert.setHeaderText("Get help for GOPHER");
        alert.setContentText(String.format("A tutorial and detailed documentation for GOPHER can be found at readthedocs: %s",READTHEDOCS_SITE));

        ButtonType buttonTypeOne = new ButtonType("Open ReadTheDocs");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne){
          openBrowser();
          alert.close();
        } else {
            alert.close();
        }
    }


    /**
     * Open a JavaFW Webview window and confirmDialog our read the docs help documentation in it.
     */
    private static void openBrowser() {
        try{
            Stage window;
            window = new Stage();
            WebView web = new WebView();
            WebEngine webEngine = web.getEngine();
            webEngine.setUserDataDirectory(new File(Platform.getWebEngineUserDataDirectory(), HelpViewFactory.class.getCanonicalName()));
            webEngine.load(READTHEDOCS_SITE);
            Scene scene = new Scene(web);
            window.setScene(scene);
            window.show();
        } catch (Exception e){
            logger.error(String.format("Could not open browser to show RTD: %s",e.toString()));
            e.printStackTrace();
        }
    }

}
