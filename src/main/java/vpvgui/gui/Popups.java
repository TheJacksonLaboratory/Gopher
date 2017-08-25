package vpvgui.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
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
        /*Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        String  result=null;

       // ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        // dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        Button okButton=new Button("OK");
        Button cancelButton=new Button("Cancel");
        TextField userstringField = new TextField();
        userstringField.setPromptText(promptText);


        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label(labelText), 0, 0);
        grid.add(userstringField, 1, 0);
        grid.add(cancelButton,0,1);
        grid.add(okButton,1,1);

        dialog.getDialogPane().setContent(grid);
        okButton.setOnAction(e -> {final String s=userstringField.getText();result=s;} );


        Platform.runLater(() -> okButton.requestFocus());*/
        Optional<String> result = dialog.showAndWait();
        logger.trace(String.format("We got name=%s",result));
        if (result.isPresent())
            return result.get();
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
