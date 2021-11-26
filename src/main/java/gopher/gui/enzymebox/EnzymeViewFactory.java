package gopher.gui.enzymebox;

import javafx.scene.Scene;
import javafx.stage.Stage;
import gopher.model.Model;
import gopher.model.RestrictionEnzyme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EnzymeViewFactory {
    private static Logger logger = LoggerFactory.getLogger(EnzymeViewFactory.class.getName());

    /**
     * Initialize the Enyzme list to show any previously chosen enzyme with a check,
     * and return the enzymes that the user chooses.
     * @param model
     */
    public static List<RestrictionEnzyme> getChosenEnzymes(Model model) {
        List<RestrictionEnzyme> allEnzymes = model.getRestrictionEnymes();
        List<RestrictionEnzyme> chosenEnzymes = model.getChosenEnzymelist();
        logger.trace("Getting chosen enzymes");
        EnzymeBoxView view = new EnzymeBoxView();
        EnzymeBoxPresenter presenter = (EnzymeBoxPresenter) view.getPresenter();
        presenter.initializeEnzymes(allEnzymes,chosenEnzymes);

        Stage window;
        String windowTitle = "Restriction Enzymes";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close());
        window.setTitle(windowTitle);

        presenter.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });


        window.setScene(new Scene(view.getView()));
        window.showAndWait();
        logger.trace(String.format("Presented n chosen %d", presenter.getChosenEnzymes().size()));
        return presenter.getChosenEnzymes();
    }



}
