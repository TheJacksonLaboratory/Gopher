package gopher.gui.regulatoryexomebox;

import gopher.framework.Signal;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import gopher.gui.enzymebox.EnzymeBoxPresenter;
import gopher.service.model.regulatoryexome.RegulationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class RegulatoryExomeBoxPresenter implements Initializable {
    private static Logger logger = LoggerFactory.getLogger(EnzymeBoxPresenter.class.getName());
    @FXML private Label label;
    @FXML private VBox regulatoryVBox;
    @FXML private Button okButton;

    private List<CheckBox> boxlist;
    private int count;
    private List<RegulationCategory> chosen = null;
    private Map<String,RegulationCategory> categories;
    private Consumer<gopher.framework.Signal> signal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.boxlist = new ArrayList<>();
        this.categories = new LinkedHashMap<>();
        for (RegulationCategory rc : RegulationCategory.values()) {
            categories.put(rc.toString(),rc);
        }
        this.chosen = new ArrayList<>();
        Platform.runLater(() -> {
            initializeCategories();
            this.okButton.requestFocus();
        });
    }

    private void initializeCategories() {
        for (RegulationCategory rc : categories.values()) {
            String label = rc.toString();
            CheckBox cb = new CheckBox(label);
            cb.setOnAction(e -> handle(rc.toString()));
            cb.setAllowIndeterminate(false);
            cb.setSelected(true);
            chosen.add(rc);
            cb.setId("checkbx");
            boxlist.add(cb);
            this.regulatoryVBox.getChildren().addAll(cb);
            this.regulatoryVBox.setSpacing(8);
        }
    }


    public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }


    List<RegulationCategory> getChosenCategories() {
        logger.trace(String.format("Returning of chosen enzymes: %d",chosen.size() ));
        return this.chosen;
    }

    /**
     * This gets called every time the use chooses or deselects an enzyme. It resets and fills the
     * set {@link #chosen}, which can then be retrieved using {@link #getChosenCategories()}.
     *
     * @param category One of the enumeration constants in {@link RegulationCategory}.
     */
    private void handle(String category) {
        this.chosen = new ArrayList<>();
        logger.trace(String.format("handle %s",category ));
        for (CheckBox cb : boxlist) {
            if (cb.isSelected()) {
                String name = cb.getText(); /* this is something like "HindIII: A^AGCTT", but we need just "HindIII" */
                int i = name.indexOf(":");
                if (i > 0) {
                    name = name.substring(0, i);
                }
                RegulationCategory re = categories.get(name);
                if (re == null) { /* Should never happen! */
                    logger.error("We were unable to retrieve the name for enzyme " + name);
                    return;
                }
                chosen.add(re);
            }
        }
        logger.trace(String.format("Number of chosen enzymes: %d",chosen.size() ));
    }

    @FXML
    public void okButtonClicked(javafx.event.ActionEvent e) {
        e.consume();
        signal.accept(Signal.DONE);
    }


}
