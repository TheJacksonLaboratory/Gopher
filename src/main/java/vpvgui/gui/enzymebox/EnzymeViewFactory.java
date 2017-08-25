package vpvgui.gui.enzymebox;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import vpvgui.model.RestrictionEnzyme;

import java.util.List;

public class EnzymeViewFactory {
    static Logger logger = Logger.getLogger(EnzymeViewFactory.class.getName());

    /**
     *
     * @param enzymes List of all restrictions enzymes from which the user needs to choose one or more.
     */
    public static List<RestrictionEnzyme> getChosenEnzymes(List<RestrictionEnzyme> enzymes) {
        logger.trace("Getting chosen enzymes");
        EnzymeBoxView view = new EnzymeBoxView();
        EnzymeBoxPresenter presenter = (EnzymeBoxPresenter) view.getPresenter();
        presenter.initializeEnzymes(enzymes);

        Stage window;
        String windowTitle = "Restriction Enzymes";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
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
        return presenter.getChosenEnzymes();
    }



}
