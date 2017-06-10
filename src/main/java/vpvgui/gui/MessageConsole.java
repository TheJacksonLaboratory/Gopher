package vpvgui.gui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by robinp on 6/9/17.
 */
public class MessageConsole {


     public  MessageConsole() {

    }


    public static VBox getMessageConsole() {

        TextArea ta = new TextArea();//TextAreaBuilder.create().prefWidth(800).prefHeight(600).wrapText(true).build();
        ta.setPrefWidth(800);
        ta.setPrefHeight(600);
        ta.setWrapText(true);
        Console console = new Console(ta);
        PrintStream ps = new PrintStream(console, true);
        System.setOut(ps);
        System.setErr(ps);

        Label label = new Label();
        label.setText("Jannovar....");

        VBox layout = new VBox(10);

        layout.getChildren().addAll(label, ta);

        //layout.setAlignment(Pos.BASELINE_LEFT);
        // Scene scene = new Scene(layout);
        // window.setScene(scene);
        //window.showAndWait();

        //console.close();
        return layout;

    }


    public static class Console extends OutputStream {

        private TextArea output;

        public Console(TextArea ta) {
            this.output = ta;
        }

        @Override
        public void write(int i) throws IOException {
            output.appendText(String.valueOf((char) i));
            //flush();
        }
    }


}
