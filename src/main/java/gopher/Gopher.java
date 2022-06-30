package gopher;

import gopher.gui.factories.PopupFactory;
import gopher.io.Platform;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import gopher.service.model.GopherModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * App for calculating and displaying viewpoints for Capture Hi C.
 * @author Peter Robinson
 * @author Peter Hansen
 * @author Daniel Danis
 * @author Hannah Blau
 * @version 0.3.1 (2017-11-12)
 */
public class Gopher extends Application {
    static Logger LOGGER = LoggerFactory.getLogger(Gopher.class.getName());
    /**
     * A reference to the Model; we will write the current settings to file in
     * the {@link #stop} method by means of a method in the Model class.
     */
    private GopherModel model;

    public static final String APPLICATION_ICON = "img/gophericon.png";


    @Override
    public void start(Stage primaryStage) {
        try { // ensure that directory for Gopher global settings exists, or die
            Platform.createGopherDir();
        } catch (IOException e) {
            PopupFactory.displayException("Error", e.getMessage() + "\nGopher will now exit", e);
            javafx.application.Platform.exit();
        }
        LOGGER.info("Starting Gopher Gui");

        Image image = new Image(Gopher.class.getResourceAsStream("/img/gophericon.png"));
        primaryStage.setTitle("GOPHER");
        primaryStage.getIcons().add(image);

        // get dimensions of users screens to use as Maximum width/height
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int xdim=(int)primScreenBounds.getWidth();
        int ydim=(int)primScreenBounds.getHeight();
    }



}

