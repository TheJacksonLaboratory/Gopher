package gopher;

import gopher.gui.popupdialog.PopupFactory;
import gopher.io.Platform;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import gopher.framework.Injector;
import gopher.controllers.SplashController;
import gopher.controllers.SwitchScreens;
import gopher.model.Model;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

import static gopher.io.Platform.getGopherDir;


/**
 * App for calculating and displaying viewpoints for Capture Hi C.
 * @author Peter Robinson
 * @author Peter Hansen
 * @author Daniel Danis
 * @author Hannah Blau
 * @version 0.3.1 (2017-11-12)
 */
public class Gopher extends Application {

    //static Logger logger = Logger.getLogger(Gopher.class.getName());
    /**
     * A reference to the Model; we will write the current settings to file in
     * the {@link #stop} method by means of a method in the Model class.
     */
    private Model model;

    public static final String APPLICATION_ICON = "img/gophericon.png";

    private Stage primarystage = null;

    @Override
    public void start(Stage primaryStage) {
        try { // ensure that directory for Gopher global settings exists, or die
            Platform.createGopherDir();
        } catch (IOException e) {
            PopupFactory.displayException("Error", e.getMessage() + "\nGopher will now exit", e);
            javafx.application.Platform.exit();
        }
//        updateLog4jConfiguration();
        //logger.info("Starting Gopher Gui");
        this.primarystage = primaryStage;
        Image image = new Image(Gopher.class.getResourceAsStream("/img/gophericon.png"));
        primaryStage.setTitle("GOPHER");
        primaryStage.getIcons().add(image);

        // get dimensions of users screens to use as Maximum width/height
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int xdim=(int)primScreenBounds.getWidth();
        int ydim=(int)primScreenBounds.getHeight();
//        SwitchScreens switchscreens=new SwitchScreens(this.primarystage);
//        switchscreens.setBounds(xdim,ydim);
       // loadSplashScreen(switchscreens);

    }

    @Override
    public void stop() {
        Injector.forgetAll();
    }

    public static void main2(String[] args) {
        String jre_version = System.getProperty("java.specification.version");
        if (!jre_version.equals("1.8")) {
            JOptionPane.showMessageDialog(null,
                    "Your current Java version "
                            + jre_version
                            + " is not supported and GOPHER may not work correctly.\n"
                            + "Please install Java version 17",
                    "Java version warning", JOptionPane.WARNING_MESSAGE);
        }
        Locale.setDefault(new Locale("en", "US"));
        launch(args);
    }

//    /**
//     * This sets the location of the log4j log file to the user's .gopher directory.
//     */
//    private void updateLog4jConfiguration() {
//        File dir = getGopherDir();
//        String logpath = (new File(dir + File.separator + "gopher.log")).getAbsolutePath();
//        Properties props = new Properties();
//        try {
//            InputStream configStream = Gopher.class.getResourceAsStream("/log4j.properties");
//            props.load(configStream);
//            configStream.close();
//        } catch (IOException e) {
//            System.out.println("Error: Cannot load configuration file.");
//        }
//        // logger.info("Resetting log file location to "+logpath);
//        //LogManager.resetConfiguration();
//        props.setProperty("log4j.appender.logfile.File", logpath);
//        System.setProperty("logfile.name",logpath);
//        //PropertyConfigurator.configure(props);
//    }

//    /**
//     * This will load the splash screen where a user can choose an existing viewpoint or create a new one.
//     * @param switchscreen An object that will switch the main screen when the user has chosen the viewpoint to be worked on
//     */
//    private void loadSplashScreen(SwitchScreens switchscreen) {
//        SplashView splashview = new SplashView();
//        SplashController splashpresenter = (SplashController) splashview.getPresenter();
//        splashpresenter.setSwitchScreen(switchscreen);
//        Scene scene = new Scene(splashview.getView());
//        this.primarystage.setTitle("GOPHER");
//        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), splashpresenter.getRootPane());
//        fadeIn.setFromValue(0);
//        fadeIn.setToValue(1);
//        fadeIn.setCycleCount(1);
//
//
//        primarystage.setScene(scene);
//        primarystage.show();
//
//        fadeIn.play();
//    }

//    /**
//     * @return true if the current platform is a Mac.
//     */
//    private boolean isMacintosh() {
//        String osName = System.getProperty("os.name").toLowerCase();
//        return (osName.contains("mac"));
//    }
//


}

