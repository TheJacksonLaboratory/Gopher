package vpvgui.gui.splash;


import javafx.scene.Scene;
import javafx.stage.Stage;
import vpvgui.io.Platform;
import vpvgui.model.Model;
import vpvgui.util.SerializationManager;
import vpvgui.vpvmain.VPVMainPresenter;
import vpvgui.vpvmain.VPVMainView;


/** This is a functor class with a callback to switch screens when the user has chosen
 * which project to work on.
 */
public class SwitchScreens {
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
        Model model = presenter.getModel();
        model.setProjectName(name);
        model.setDefaultValues();
        presenter.setModel(model);
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.setTitle(String.format("Viewpoint Viewer: %s",name));
        this.primarystage.show();
    }

    public void openExistingModel(String name) {
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
        this.primarystage.setMinWidth(1000);
        this.primarystage.setWidth(1600);
        String filepath = Platform.getAbsoluteProjectPath(name);
        Model model = SerializationManager.deserializeModel(filepath);
        presenter.setModel(model);
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.setTitle(String.format("Viewpoint Viewer: %s",name));
        this.primarystage.show();
    }


}
