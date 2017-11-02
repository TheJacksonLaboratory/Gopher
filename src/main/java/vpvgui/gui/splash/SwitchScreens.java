package vpvgui.gui.splash;


import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import vpvgui.gui.ErrorWindow;
import vpvgui.io.Platform;
import vpvgui.model.Model;
import vpvgui.util.SerializationManager;
import vpvgui.util.Utils;
import vpvgui.vpvmain.VPVMainPresenter;
import vpvgui.vpvmain.VPVMainView;

import java.io.IOException;


/** This is a functor class with a callback to switch screens when the user has chosen
 * which viewpoint to work on.
 * @author Peter Robinson
 * @version 0.0.2 (14 September, 2017)
 */
public class SwitchScreens {
    private static final Logger logger = Logger.getLogger(SwitchScreens.class.getName());
    /** A reference to the primary stage of the application. */
    private Stage primarystage = null;
    /** A reference to the model of the application. */
    private Model model=null;
    /** The width of the current screen, which is calculated in {@link vpvgui.gui.viewpointpanel.ViewPointView}. 800 is a default value,
     * but the variable will always be set to the current value of the user's screen.*/
    private int screenWidth=800;
    /** The height of the current screen, which is calculated in {@link vpvgui.gui.viewpointpanel.ViewPointView}. 600 is a default value,
     * but the variable will always be set to the current value of the user's screen.*/
    private int screenHeight=600;


    public SwitchScreens(Stage stage){
        this.primarystage= stage;
    }

    public void createNewProject(String name){
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
//        this.primarystage.setMinWidth(1000);
//        this.primarystage.setWidth(1600);
        Model model = new Model();
        model.setProjectName(name);
        model.setDefaultValues();
        model.setXdim(screenWidth);
        model.setYdim(screenHeight);
        logger.error(String.format("Setting dimensions to x=%d y=%d",screenWidth,screenHeight ));
        presenter.setModel(model);
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.centerOnScreen();
        this.primarystage.setTitle(String.format("Viewpoint Viewer: %s",name));
        // The commented line is causing null pointer during creation of the new project when the hidden file is not
        // yet present in the user's home directory.
        // This is not necessary, since we're adjusting the bounds 7 lines above this one. Is that correct?
        // setModelXYBounds();
        this.primarystage.show();
    }

    public void openExistingModel(String name) {
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
        String filepath = Platform.getAbsoluteProjectPath(name);
        try {
            this.model = SerializationManager.deserializeModel(filepath);
        } catch (IOException e) {
            ErrorWindow.displayException("IOException",String.format("I/O problem while attempting to deserialize %s",filepath),e);
            return;
        } catch (ClassNotFoundException e) {
            ErrorWindow.displayException("ClassNotFoundException",String.format("Could not find class while attempting to deserialize %s",filepath),e);
            return;
        } catch (Exception e) {
            ErrorWindow.displayException("Exception",String.format("Exception while attempting to deserialize %s",filepath),e);
            return;
        }
        if (this.model == null) {
            logger.error(String.format("Unable to deserialize model from %s at %s", name, filepath));
            ErrorWindow.display("Null pointer", String.format("Unable to deserialize model from %s at %s", name, filepath));
            return;
        }
        if (this.model.getHttpProxy() != null) {
            Utils.setSystemProxyAndPort(model.getHttpProxy(),model.getHttpProxyPort());
        }

        presenter.setModelInMainAndInAnalysisPresenter(model);
        logger.trace("Deserialized model "+ model.getProjectName());
        if (presenter==null){
            logger.fatal("Presenter was null ponter");
            return;
        }
        if (model.viewpointsInitialized()) {
            presenter.refreshViewPoints();
        }
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.centerOnScreen();
        this.primarystage.setTitle(String.format("Viewpoint Viewer: %s",name));
        setModelXYBounds();
        this.primarystage.show();
    }

    /**
     *
     * @param x X dimension of the user's screen
     * @param y Y dimension of the user's screen
     */
    public void setBounds(int x, int y) {
        this.screenHeight=y;
        this.screenWidth=x;
    }

    private void setModelXYBounds() {
        model.setXdim(screenWidth);
        model.setYdim(screenHeight);
    }


}
