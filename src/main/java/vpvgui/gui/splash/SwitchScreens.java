package vpvgui.gui.splash;


import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import vpvgui.io.Platform;
import vpvgui.model.Model;
import vpvgui.util.SerializationManager;
import vpvgui.vpvmain.VPVMainPresenter;
import vpvgui.vpvmain.VPVMainView;


/** This is a functor class with a callback to switch screens when the user has chosen
 * which viewpoint to work on.
 */
public class SwitchScreens {
    private static final Logger logger = Logger.getLogger(SwitchScreens.class.getName());
    /** A reference to the primary stage of the application. */
    private Stage primarystage = null;
    /** A reference to the model of the application. */
    private Model model=null;



    public SwitchScreens(Stage stage){
        this.primarystage= stage;
    }

    public void createNewProject(String name){
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
        this.primarystage.setMinWidth(1000);
        this.primarystage.setWidth(1600);
        Model model = new Model();
        model.setProjectName(name);
        model.setDefaultValues();
        presenter.setModel(model);
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.setTitle(String.format("Viewpoint Viewer: %s",name));
        setStageDimensions();
        this.primarystage.show();
    }

    public void openExistingModel(String name) {
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
        this.primarystage.setMinWidth(1000);
        this.primarystage.setWidth(1600);
        String filepath = Platform.getAbsoluteProjectPath(name);
        logger.trace("About to deserialize model at "+ filepath);
        Model model = SerializationManager.deserializeModel(filepath);
        presenter.setModelInMainAndInAnalysisPresenter(model);
        logger.trace("Deserialized model "+ model.toString());
        if (presenter==null){
            logger.fatal("Presenter was null ponter");
            return;
        }

        if (model==null) {
            logger.fatal("Model was null ponter");
            return;
        }
        if (model.viewpointsInitialized()) {
            presenter.refreshViewPoints();
        }
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.setTitle(String.format("Viewpoint Viewer: %s",name));
        setStageDimensions();
        this.primarystage.show();
    }

    private void setStageDimensions() {
        if (this.primarystage==null) return;
        this.primarystage.setMinWidth(1000);
        this.primarystage.setWidth(1400);
        this.primarystage.setMinHeight(800);
        this.primarystage.setHeight(1000);
    }


}
