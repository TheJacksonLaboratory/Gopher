package vpvgui;

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
import vpvgui.framework.Injector;
import vpvgui.gui.splash.SplashPresenter;
import vpvgui.gui.splash.SplashView;
import vpvgui.gui.splash.SwitchScreens;
import vpvgui.model.Model;

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
    /**
     * A reference to the Model; we will write the current settings to file in
     * the {@link #stop} method by means of a method in the Model class.
     */
    private Model model;

    public static final String APPLICATION_ICON = "img/vpvicon.png";

    private Stage primarystage = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        updateLog4jConfiguration();
        //logger.info("Starting VPV Gui");
        this.primarystage = primaryStage;
        Image image = new Image(ViewPointViewer.class.getResourceAsStream("/img/vpvicon.png"));
        primaryStage.setTitle("ViewPoint Viewer");
        primaryStage.getIcons().add(image);
        // get dimensions of users screens to use as Maximum width/height
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        int xdim=(int)primScreenBounds.getWidth();
        int ydim=(int)primScreenBounds.getHeight();
        SwitchScreens switchscreens=new SwitchScreens(this.primarystage);
        switchscreens.setBounds(xdim,ydim);
        loadSplashScreen(switchscreens);

    }

    /** ToDo probably need to write settings in the VPVMainPresenter class! */
    @Override
    public void stop() throws Exception {
        //logger.info("Closing VPV Gui");
        System.err.println("CLOSING STOP()");
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * This sets the location of the log4j log file to the user's .vpvgui directory.
     */
    private void updateLog4jConfiguration() {
        File dir = getVPVDir();
        String logpath = (new File(dir + File.separator + "vpvgui.log")).getAbsolutePath();
        Properties props = new Properties();
        try {
            InputStream configStream = ViewPointViewer.class.getResourceAsStream("/log4j.properties");
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
        this.primarystage.setTitle("ViewPoint Viewer");

        this.primarystage.setMinWidth(400);
        this.primarystage.setWidth(600);
        this.primarystage.setMaxWidth(600);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), splashpresenter.getRootPane());
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);


        primarystage.setScene(scene);
        primarystage.show();

        fadeIn.play();
    }



}

