package gopher.gui.factories;

import gopher.service.model.dialog.RestrictionEnzymeResult;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import gopher.service.model.RestrictionEnzyme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static javafx.application.Platform.runLater;

public class EnzymeViewFactory {
    private static Logger logger = LoggerFactory.getLogger(EnzymeViewFactory.class.getName());

    /**
     * Initialize the Enyzme list to show any previously chosen enzyme with a check,
     * and return the enzymes that the user chooses.
     * @param
     */

    private List<CheckBox> boxlist;

    private int count;

    private List<RestrictionEnzyme> chosen = null;

    private Map<String, RestrictionEnzyme> enzymemap;

    public EnzymeViewFactory() {
        this.boxlist = new ArrayList<>();
        this.enzymemap = new HashMap<>();
        //this.restrictionLabel.setText("Choose one or more restriction enzymes");
        this.chosen = new ArrayList<>();
    }

    public List<RestrictionEnzyme> getChosenEnzymes(List<RestrictionEnzyme> allEnzymes,
                                                           List<RestrictionEnzyme> chosenEnzymes ) {
        logger.trace("Getting chosen enzymes");

        Dialog<RestrictionEnzymeResult> dialog = new Dialog<>();
        dialog.setTitle("Choose Enzymes");
        dialog.setHeaderText("Choose one or more restriction enzymes");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        VBox restrictionVBox = new VBox();
        if (chosenEnzymes==null) {
            logger.info("chosen enzyme list not initialized. Will set it to an empty list");
            chosenEnzymes=new ArrayList<>();
        }
        for (RestrictionEnzyme re : allEnzymes) {
            String label = re.getLabel();
            CheckBox cb = new CheckBox(label);
            enzymemap.put(re.getName(), re);
            cb.setOnAction(e -> handle(re.getName()));
            cb.setAllowIndeterminate(false);
            if (chosenEnzymes.contains(re)) {
                cb.setSelected(true);
                chosen.add(re);
                logger.trace(String.format("Set selected to %s for %s",cb.isSelected(),re.getName() ));
            }
            cb.setId("checkbx");
            boxlist.add(cb);
            restrictionVBox.getChildren().addAll(cb);
            restrictionVBox.setSpacing(8);
        }
        dialogPane.setContent(restrictionVBox);
        runLater(restrictionVBox::requestFocus);
        dialog.setResultConverter((ButtonType button) -> {
            if (button == ButtonType.OK) {
                return new RestrictionEnzymeResult(this.chosen);
            }
            return null;
        });
        Optional<RestrictionEnzymeResult> optionalResult = dialog.showAndWait();
        if (optionalResult.isPresent()) {
            return optionalResult.get().chosenEzymes();
        } else {
            return List.of();
        }
    }

    public List<RestrictionEnzyme> getChosenEnzymes() {
        logger.trace(String.format("Returning of chosen enzymes: %d",chosen.size() ));
        return this.chosen;
    }


    /**
     * This gets called every time the use chooses or deselects an enzyme. It resets and fills the
     * set {@link #chosen}, which can then be retrieved using {@link #getChosenEnzymes()}.
     *
     * @param site
     */
    private void handle(String site) {
        this.chosen = new ArrayList<>();
        logger.trace(String.format("handle %s",site ));
        for (CheckBox cb : boxlist) {
            if (cb.isSelected()) {
                String name = cb.getText(); /* this is something like "HindIII: A^AGCTT", but we need just "HindIII" */
                int i = name.indexOf(":");
                if (i > 0) {
                    name = name.substring(0, i);
                }
                RestrictionEnzyme re = enzymemap.get(name);
                if (re == null) { /* Should never happen! */
                    logger.error("We were unable to retrieve the name for enzyme " + name);
                    return;
                }
                chosen.add(re);
            }
        }
        logger.trace(String.format("Number of chosen enzymes: %d",chosen.size() ));
    }

}
