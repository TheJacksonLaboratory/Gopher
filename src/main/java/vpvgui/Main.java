package vpvgui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;


/**
 * App for calculating and displaying viewpoints for Capture Hi C.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

	//File file = new File(Main.class.ClassLoader.getResource("/data/enzymelist.tab").getFile());
	//System.err.println("File"+file);
        Parent root = FXMLLoader.load(getClass().getResource("/vpvgui.fxml"));

	System.err.println("MAIN: " + getClass().getResource("/vpvgui.fxml"));
       

        Scene scene = new Scene(root, 800, 600);
        scene.setRoot(root);
        primaryStage.setScene(scene );
        primaryStage.setTitle("VPV: Capture Hi-C Viewpoint Viewer");
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
