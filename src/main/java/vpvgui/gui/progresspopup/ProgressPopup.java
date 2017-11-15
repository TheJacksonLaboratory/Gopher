package vpvgui.gui.progresspopup;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressPopup {


    private final ProgressIndicator pb = new ProgressIndicator();

    private String progressTitle=null;

    private String progressLabel=null;

    private Stage window=null;


    public void startProgress(final Task task) throws InterruptedException {
        Label label=new Label(progressLabel);
        FlowPane root = new FlowPane();
        root.setPadding(new Insets(10));
        root.setHgap(10);
        root.getChildren().addAll(label,pb);
        Scene scene = new Scene(root, 400, 100);
        window = new Stage();
        window.setTitle(this.progressTitle);
        window.setScene(scene);
        window.show();
//        task.setOnSucceeded(event -> {
//            window.close();
//        });
        Thread thread = new Thread(task);
        thread.start();
    }


    public ProgressPopup(String title, String label) {

        progressTitle=title;
        progressLabel=label;
    }

    public ProgressIndicator getProgressIndicator(){return  this.pb; }

    public void close() {
        if (window!=null) window.close();
    }



}
