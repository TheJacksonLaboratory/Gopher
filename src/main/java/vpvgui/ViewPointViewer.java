package vpvgui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import vpvgui.framework.Injector;
import vpvgui.model.Model;
import vpvgui.vpvmain.VPVMainPresenter;
import vpvgui.vpvmain.VPVMainView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static vpvgui.io.Platform.getVPVDir;


/**
 * App for calculating and displaying viewpoints for Capture Hi C.
 */
public class ViewPointViewer extends Application {

    //static Logger logger = Logger.getLogger(ViewPointViewer.class.getName());
    /** A reference to the Model; we will write the current settings to file in
     * the {@link #stop} method by means of a method in the Model class. */
    private Model model;

    public static final String APPLICATION_ICON = "img/vpvicon.png";

    private boolean isSplashScreenShowing=false;

    @Override
    public void start(Stage primaryStage) throws Exception{
        updateLog4jConfiguration();
        //logger.info("Starting VPV Gui");
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter=(VPVMainPresenter)appView.getPresenter();
        Image image  = new Image(ViewPointViewer.class.getResourceAsStream("/img/vpvicon.png"));
        this.model=presenter.getModel();
        Scene scene = new Scene(appView.getView());
        primaryStage.setTitle("ViewPoint Viewer");

        primaryStage.setMinWidth(1000);
        primaryStage.setWidth(1600);
        primaryStage.getIcons().add(image);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    @Override
    public void stop() throws Exception {
        //logger.info("Closing VPV Gui");
        Model.writeSettingsToFile(this.model);
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** This sets the location of the log4j log file to the user's .vpvgui directory.*/
    private void updateLog4jConfiguration() {
        File dir=getVPVDir();
        String logpath=(new File(dir+File.separator+"vpvgui.log")).getAbsolutePath();
        Properties props = new Properties();
        try {
            InputStream configStream = ViewPointViewer.class.getResourceAsStream( "/log4j.properties");
            props.load(configStream);
            configStream.close();
        } catch (IOException e) {
            System.out.println("Error: Cannot load configuration file.");
        }
       // logger.info("Resetting log file location to "+logpath);
        LogManager.resetConfiguration();
        props.setProperty("log4j.appender.logfile.file", logpath);
        PropertyConfigurator.configure(props);
    }
}

