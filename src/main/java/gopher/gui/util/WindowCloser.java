package gopher.gui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;

/**
 * A simple convenience class that returns one of three answers if the user
 * tries to close the app. Cancel, Save and close, Close without saving.
 */
public class WindowCloser {

    private boolean save;
    private boolean quit;


    public WindowCloser(){}

    public void display(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning: Unsaved work!");
        alert.setHeaderText("Project has unsaved work.");
        alert.setContentText("Save unsaved work before closing?");

        ButtonType saveAndQuitButton = new ButtonType("Save and quit");
        ButtonType justQuitButton = new ButtonType("Just quit");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveAndQuitButton, justQuitButton, buttonTypeCancel);
        DialogPane dp = alert.getDialogPane();
        dp.getButtonTypes().stream()
                .map(dp::lookupButton)
                .forEach(node -> ButtonBar.setButtonUniformSize(node, false));

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) {
            save=false;
            quit=false;
        } else if (result.get() == saveAndQuitButton){
            save=true;
            quit=true;
        } else if (result.get() == justQuitButton) {
            save=false;
            quit=true;
        } else {
           save=false;
           quit=false;
        }
    }

    public boolean save() {
        return save;
    }

    public boolean quit() {
        return quit;
    }
}
