package gopher.gui.splash;


import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import gopher.Gopher;
import gopher.gui.popupdialog.PopupFactory;
import gopher.io.Platform;
import gopher.model.Model;
import gopher.util.SerializationManager;
import gopher.util.Utils;
import gopher.gui.gophermain.GopherMainPresenter;
import gopher.gui.gophermain.GopherMainView;

import java.io.IOException;


/** This is a functor class with a callback to switch screens when the user has chosen
 * which viewpoint to work on.
 * @author Peter Robinson
 * @version 0.1.1 (2 December, 2017)
 */
public class SwitchScreens {
    private static final Logger logger = Logger.getLogger(SwitchScreens.class.getName());
    /** A reference to the primary stage of the application. */
    private Stage primarystage;
    /** The width of the current screen, which is calculated in {@link gopher.gui.viewpointpanel.ViewPointView}. 800 is a default value,
     * but the variable will always be set to the current value of the user's screen.*/
    private int screenWidth=800;
    /** The height of the current screen, which is calculated in {@link gopher.gui.viewpointpanel.ViewPointView}. 600 is a default value,
     * but the variable will always be set to the current value of the user's screen.*/
    private int screenHeight=600;


    public SwitchScreens(Stage stage){
        this.primarystage= stage;
    }

    void createNewProject(String name){
        GopherMainView appView = new GopherMainView();
        GopherMainPresenter presenter = (GopherMainPresenter) appView.getPresenter();
        presenter.setPrimaryStageReference(this.primarystage);
        Model model = new Model();
        model.setApproach("simple");
        model.setProjectName(name);
        model.setXdim(screenWidth);
        model.setYdim(screenHeight);
        logger.trace(String.format("Setting dimensions to x=%d y=%d",screenWidth,screenHeight ));
        presenter.setModelInMainAndInAnalysisPresenter(model);
        presenter.initializeNewModelInGui();
        setupStage(appView,name);

    }

    void openExistingModel(String name) {
        GopherMainView appView = new GopherMainView();
        GopherMainPresenter presenter = (GopherMainPresenter) appView.getPresenter();
        presenter.setPrimaryStageReference(this.primarystage);
        String filepath = Platform.getAbsoluteProjectPath(name);
        Model model;
        try {
            model = SerializationManager.deserializeModel(filepath);
        } catch (IOException e) {
            PopupFactory.displayException("IOException",String.format("I/O problem while attempting to deserialize %s",filepath),e);
            return;
        } catch (ClassNotFoundException e) {
            PopupFactory.displayException("ClassNotFoundException",String.format("Could not find class while attempting to deserialize %s",filepath),e);
            return;
        } catch (Exception e) {
            PopupFactory.displayException("Exception",String.format("Exception while attempting to deserialize %s",filepath),e);
            return;
        }
        if (model == null) {
            logger.error(String.format("Unable to deserialize model from %s at %s", name, filepath));
            PopupFactory.displayError("Null pointer", String.format("Unable to deserialize model from %s at %s", name, filepath));
            return;
        }
        if (model.getHttpProxy() != null) {
            Utils.setSystemProxyAndPort(model.getHttpProxy(),model.getHttpProxyPort());
        }
        model.setXdim(screenWidth);
        model.setYdim(screenHeight);
        presenter.setModelInMainAndInAnalysisPresenter(model);
        logger.trace("Deserialized model "+ model.getProjectName());
        if (model.viewpointsInitialized()) {
            presenter.refreshViewPoints();
        }
        setupStage(appView,name);
    }



    private void setupStage(GopherMainView appView, String name) {
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.centerOnScreen();
        this.primarystage.setTitle(String.format("GOPHER: %s",name));
        this.primarystage.show();
    }



    /**
     * This function needs to be called before creating a new model or opening an old one. The dimensions of the
     * user's monitor are calculated in {@link Gopher} and transmitted to this class, whence they
     * are provided to the {@link Model} object. The Model objects uses these dimensions only to set the width of
     * the image that is obtained from the UCSC Genome browser (all other dimensions should be set automatically
     * using the JavaFX code). If for any reason these dimensions are not set, the default values of
     * {@link #screenHeight} and {@link #screenWidth} are used.
     * @param x X dimension of the user's screen
     * @param y Y dimension of the user's screen
     */
    public void setBounds(int x, int y) {
        this.screenHeight=y;
        this.screenWidth=x;
    }



}
