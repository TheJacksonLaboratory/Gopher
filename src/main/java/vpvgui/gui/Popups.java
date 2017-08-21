package vpvgui.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.apache.log4j.Logger;

import java.util.Optional;

public class Popups {
    static Logger logger = Logger.getLogger(Popups.class.getName());


    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptText  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */
    public static String getStringFromUser(String windowTitle, String promptText, String labelText) {
        TextInputDialog dialog = new TextInputDialog(promptText);
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.setContentText(labelText);

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);

    }



    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptText  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */
    public static Integer getIntegerFromUser(String windowTitle, String promptText, String labelText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.setContentText(labelText);
        dialog.getEditor().setPromptText(promptText);
        dialog.getEditor().setPrefColumnCount(15);
       // javafx.application.Platform.runLater(() -> dialog.getb
        //dialog.req

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                return Integer.parseInt(result.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptText  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */

    public static Integer getIntegerFromUser2(String windowTitle, String promptText, String labelText) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);

        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);


        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText(promptText);


        grid.add(new Label(labelText), 0, 0);
        grid.add(username, 1, 0);


// Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

// Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
            logger.trace(String.format("Got new value %s",newValue));
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the button field by default.
        // So that text field shows prompt.
        Platform.runLater(() -> loginButton.requestFocus());

       /* dialog.setResultConverter(dialogButton -> {
            Integer i=null;
            if (dialogButton == loginButtonType) {
                try {
                    return Integer.parseInt(username.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });*/
        Optional<Integer> i = dialog.showAndWait();
        if (i.isPresent())
            return i.get();
        else
            return null;
    }



    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptText  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */
    public static Double getDoubleFromUser(String windowTitle, String promptText, String labelText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getEditor().setPromptText(promptText);
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.setContentText(labelText);

        Optional<String> result = dialog.showAndWait();
        String r=result.orElse(null);
        if (r==null) return null;
        try {
            return Double.parseDouble(r);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
