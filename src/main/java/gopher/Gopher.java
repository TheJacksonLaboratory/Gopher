package gopher;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import gopher.framework.Injector;
import gopher.gui.splash.SplashPresenter;
import gopher.gui.splash.SplashView;
import gopher.gui.splash.SwitchScreens;
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
        updateLog4jConfiguration();
        //logger.info("Starting Gopher Gui");
        this.primarystage = primaryStage;
        Image image = new Image(Gopher.class.getResourceAsStream("/img/gophericon.png"));
        primaryStage.setTitle("GOPHER");
        primaryStage.getIcons().add(image);
        if (isMacintosh()) {
            try {
                URL iconURL = Gopher.class.getResource("/img/gophericon.png");
                java.awt.Image macimage = new ImageIcon(iconURL).getImage();
                com.apple.eawt.Application.getApplication().setDockIconImage(macimage);
            } catch (Exception e) {
                // Not for Windows or Linux. Just skip it!
            }
        }
        // get dimensions of users screens to use as Maximum width/height
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int xdim=(int)primScreenBounds.getWidth();
        int ydim=(int)primScreenBounds.getHeight();
        SwitchScreens switchscreens=new SwitchScreens(this.primarystage);
        switchscreens.setBounds(xdim,ydim);
        loadSplashScreen(switchscreens);

    }

    @Override
    public void stop() {
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        Locale.setDefault(new Locale("en", "US"));
        launch(args);
    }

    /**
     * This sets the location of the log4j log file to the user's .gopher directory.
     */
    private void updateLog4jConfiguration() {
        File dir = getGopherDir();
        String logpath = (new File(dir + File.separator + "gopher.log")).getAbsolutePath();
        Properties props = new Properties();
        try {
            InputStream configStream = Gopher.class.getResourceAsStream("/log4j.properties");
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

    /**
     * This will load the splash screen where a user can choose an existing viewpoint or create a new one.
     * @param switchscreen An object that will switch the main screen when the user has chosen the viewpoint to be worked on
     */
    private void loadSplashScreen(SwitchScreens switchscreen) {
        SplashView splashview = new SplashView();
        SplashPresenter splashpresenter = (SplashPresenter) splashview.getPresenter();
        splashpresenter.setSwitchScreen(switchscreen);
        Scene scene = new Scene(splashview.getView());
        this.primarystage.setTitle("GOPHER");
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), splashpresenter.getRootPane());
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);


        primarystage.setScene(scene);
        primarystage.show();

        fadeIn.play();
    }

    /**
     * @return true if the current platform is a Mac.
     */
    private boolean isMacintosh() {
        String osName = System.getProperty("os.name").toLowerCase();
        return (osName.contains("mac"));
    }



}

