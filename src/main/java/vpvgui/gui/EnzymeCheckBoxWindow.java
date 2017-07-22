package vpvgui.gui;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vpvgui.model.RestrictionEnzyme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robinp on 5/11/17.
 * This class allows the user to select one or more Restriction enzymes using a CheckBox Modal Winodw.
 */
public class EnzymeCheckBoxWindow {


    private static List<CheckBox> boxlist;

    private static int count;

    private static List<RestrictionEnzyme> chosen=null;

    private static Map<String,RestrictionEnzyme> enzymemap;


    public static List<RestrictionEnzyme> display(List<RestrictionEnzyme> enzymes) {
        initializeEnzymes(enzymes);

        chosen = new ArrayList<>();
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Restriction enzymes");
        window.setMinWidth(250);
        window.setMinHeight(400);
        Label label = new Label();
        label.setText("Choose one or more restriction enzymes for Capture Hi-C");

        VBox layout = new VBox(10);

        layout.getChildren().addAll(label);
        for (CheckBox cb : boxlist) {
            layout.getChildren().addAll(cb);
        }
        layout.setAlignment(Pos.BASELINE_LEFT);
        Button okButton = new Button("OK");
        okButton.setOnAction( e -> window.close() );
        layout.getChildren().addAll(okButton);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();

        return chosen;
    }



    private static void initializeEnzymes(List<RestrictionEnzyme> enzymes) {
        boxlist = new ArrayList<>();
        enzymemap = new HashMap<>();
        for (RestrictionEnzyme re :  enzymes) {
            String label = re.getLabel();
            CheckBox cb = new CheckBox(label);
            enzymemap.put(re.getName(),re);
            cb.setOnAction( e -> handle(re.getName()));
            cb.setStyle(
                    "-fx-border-color: lightblue; "
                            + "-fx-font-size: 18;"
                            + "-fx-border-insets: -5; "
                            + "-fx-border-radius: 5;"
                            + "-fx-border-style: dotted;"
                            + "-fx-border-width: 2;"
                            + "-fx-alignment: top-left"
            );
            boxlist.add(cb);
        }
    }

    private static void handle(String site) {
       chosen = new ArrayList<>();
       for (CheckBox cb : boxlist) {
           if (cb.isSelected()) {
               String label = cb.getText();
               int index = label.indexOf(":");
               String name=label.substring(0,index);
               System.out.println("Got name="+name);
               RestrictionEnzyme re = enzymemap.get(name);
               System.out.println("Adding re"+re.getLabel());
               chosen.add(re);
           }
       }
    }



}
