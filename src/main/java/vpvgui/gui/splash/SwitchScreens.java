package vpvgui.gui.splash;


import javafx.scene.Scene;
import javafx.stage.Stage;
import vpvgui.model.Model;
import vpvgui.vpvmain.VPVMainPresenter;
import vpvgui.vpvmain.VPVMainView;

import java.io.File;

import static vpvgui.io.Platform.getVPVDir;

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

    public Model createNewProject(String name){
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
        this.primarystage.setMinWidth(1000);
        this.primarystage.setWidth(1600);
        Model model = presenter.getModel();
        model.setProjectName(name);
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.show();
        return model;
    }

    public void openExistingModel(String name) {
        VPVMainView appView = new VPVMainView();
        VPVMainPresenter presenter = (VPVMainPresenter) appView.getPresenter();
        this.primarystage.setMinWidth(1000);
        this.primarystage.setWidth(1600);
        Model model = presenter.getModel();
        File dir = getVPVDir();
        String filepath = (new File(dir + File.separator + name)).getAbsolutePath();
        Model.initializeModelFromSettingsFile(filepath,model);
        Scene scene = new Scene(appView.getView());
        this.primarystage.setScene(scene);
        this.primarystage.show();
    }


}
