package vpvgui.gui.help;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.apache.log4j.Logger;
import vpvgui.gui.ErrorWindow;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Created by peterrobinson on 7/3/17.
 */
public class HelpViewFactory {
    private static final Logger logger = Logger.getLogger(HelpViewFactory.class.getName());
    private static final String READTHEDOCS_SITE = "https://readthedocs.org/projects/vpv/";


    public static void display() {
        openHelpDialog();
        /*Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        HelpView view = new HelpView();
        HelpPresenter presenter = (HelpPresenter) view.getPresenter();

        String html=getHTML();
        presenter.setData(html);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();*/
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
                "<p>VPV (<i>ViewPoint Viewer</i>) is designed to facilitate the design of probes for capture Hi-C and related" +
                " methods by visualizing the restriction fragments that surround the one or multiple transcription" +
                "start sites of a gene (a viewpoint) in the context of their genomic position and repeat content.</p>" +
                "<p>Documentation can be found at the <a href=\"https://readthedocs.org/projects/vpv/\">VPV Documentation Website</a>.</p>" +
                "</body></html>";
        return sb;

    }





    private static void openHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("VPV Help");
        alert.setHeaderText("Get help for ViewPointViewer");
        alert.setContentText(String.format("A tutorial and detailed documentation for ViewPointViewer (VPV) can be found at readthedocs: %s",READTHEDOCS_SITE));

        ButtonType buttonTypeOne = new ButtonType("Open ReadTheDocs");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne){
            if( Desktop.isDesktopSupported() )
            {
                new Thread(() -> {
                    try {
                        logger.trace(String.format("Opening Website at %s",READTHEDOCS_SITE));
                        Desktop.getDesktop().browse( new URI( READTHEDOCS_SITE ) );
                    } catch (IOException | URISyntaxException e1) {
                        e1.printStackTrace();
                    }
                }).start();
            } else {
                ErrorWindow.display("Could not open read the docs",
                        String.format("The documentation is available at %s",READTHEDOCS_SITE));
            }


            alert.close();
        } else {
            alert.close();
        }
    }

}
