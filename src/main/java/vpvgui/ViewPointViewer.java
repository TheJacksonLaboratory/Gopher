package vpvgui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import vpvgui.framework.Injector;
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

    static Logger logger = Logger.getLogger(ViewPointViewer.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception{
        logger.info("Starting VPV Gui");
        updateLog4jConfiguration();
        VPVMainView appView = new VPVMainView();
        Scene scene = new Scene(appView.getView());
        primaryStage.setTitle("HPO Phenote");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    @Override
    public void stop() throws Exception {
        logger.info("Closing VPV Gui");
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
            System.out.println("Error: Cannot laod configuration file ");
        }
        logger.info("Resetting log file location to "+logpath);
        LogManager.resetConfiguration();
        props.setProperty("log4j.appender.logfile.file", logpath);
        PropertyConfigurator.configure(props);
    }
}

