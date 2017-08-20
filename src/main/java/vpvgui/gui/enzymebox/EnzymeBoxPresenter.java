package vpvgui.gui.enzymebox;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;
import vpvgui.framework.Signal;
import vpvgui.model.RestrictionEnzyme;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by peterrobinson on 7/11/17.
 */
public class EnzymeBoxPresenter implements Initializable {
    static Logger logger = Logger.getLogger(EnzymeBoxPresenter.class.getName());
    @FXML Label restrictionLabel;

    @FXML VBox restrictionVBox;

    @FXML Button okButton;


    private static List<CheckBox> boxlist;

    private static int count;

    private static List<RestrictionEnzyme> chosen=null;

    private static Map<String,RestrictionEnzyme> enzymemap;

    private Consumer<vpvgui.framework.Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.boxlist = new ArrayList<>();
        this.enzymemap = new HashMap<>();
        this.restrictionLabel.setText("Choose one or more restriction enzymes for Capture Hi-C");
        logger.trace("We initialized restrictionLabel to " + this.restrictionLabel.getText());
        this.chosen = new ArrayList<>();
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
            this.restrictionVBox.getChildren().addAll(cb);
        }
        logger.trace("We added " + boxlist.size() + " enzymes to boxlist");

    }


    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }





    public List<RestrictionEnzyme> getChosenEnzymes() {
        return this.chosen;
    }


    /**
     * This gets called every time the use chooses or deselects an enzyme. It resets and fills the
     * set {@link #chosen}, which can then be retrieved using {@link #getChosenEnzymes()}.
     * @param site
     */
    private void handle(String site) {
       this.chosen = new ArrayList<>();
       for (CheckBox cb : boxlist) {
           if (cb.isSelected()) {
               String name = cb.getText(); /* this is something like "HindIII: A^AGCTT", but we need just "HindIII" */
               int i=name.indexOf(":");
               if (i>0) {
                   name=name.substring(0,i);
               }
               RestrictionEnzyme re = enzymemap.get(name);
                if (re==null) { /* Should never happen! */
                    logger.error("We were unable to retrieve the name for enzyme "+name); return;
                }
               chosen.add(re);
           }
       }
    }

    @FXML public void okButtonClicked(javafx.event.ActionEvent e){
        e.consume();
        signal.accept(Signal.DONE);
    }

}
