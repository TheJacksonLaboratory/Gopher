package vpvgui.gui.enzymebox;

import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import vpvgui.model.RestrictionEnzyme;

import java.net.URL;
import java.util.*;

/**
 * Created by peterrobinson on 7/11/17.
 */
public class EnzymeBoxPresenter implements Initializable {


    private static List<CheckBox> boxlist;

    private static int count;

    private static List<RestrictionEnzyme> chosen=null;

    private static Map<String,RestrictionEnzyme> enzymemap;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.boxlist = new ArrayList<>();
        this.enzymemap = new HashMap<>();
    }

    public void initializeEnzymes(List<RestrictionEnzyme> enzymes) {
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



/*
    public static List<RestrictionEnzyme> display(List<RestrictionEnzyme> enzymes) {


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



*/

    private void handle(String site) {
       this.chosen = new ArrayList<>();
       for (CheckBox cb : boxlist) {
           if (cb.isSelected()) {
               String name = cb.getText();
               RestrictionEnzyme re = enzymemap.get(name);
               chosen.add(re);
           }
       }
    }



}
