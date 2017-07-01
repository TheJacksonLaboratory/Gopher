package vpvgui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vpvgui.framework.Injector;
import vpvgui.vpvmain.VPVMainView;


/**
 * App for calculating and displaying viewpoints for Capture Hi C.
 */
public class ViewPointViewer extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        VPVMainView appView = new VPVMainView();
        Scene scene = new Scene(appView.getView());
        primaryStage.setTitle("HPO Phenote");
       // final String uri = getClass().getResource("vpvmain.css").toExternalForm();
        //scene.getStylesheets().add(uri);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    @Override
    public void stop() throws Exception {
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

