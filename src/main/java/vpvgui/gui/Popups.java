package vpvgui.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
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
    public static Integer getIntegerFromUserOLD(String windowTitle, String promptText, String labelText) {
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
     * @param promptValue  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */

    public static Integer getIntegerFromUser(String windowTitle, Integer promptValue, String labelText) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField userdata = new TextField();
        userdata.setPromptText(String.format("%d",promptValue));


        grid.add(new Label(labelText), 0, 0);
        grid.add(userdata, 1, 0);

        ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk,buttonTypeCancel);


        // Enable/Disable OK button depending only after a valid integer has been entered
        Node okButton = dialog.getDialogPane().lookupButton(buttonTypeOk);
        okButton.setDisable(true);
        userdata.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean invalid=true;
            try {
                Integer dummy = Integer.parseInt(userdata.getText());
                invalid=false; /* We will only reach this line if the userdata can be parsed as an Integer. */
            } catch (NumberFormatException e) {
                /* do nothing */
            }
            okButton.setDisable(invalid);
        });

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(new Callback<ButtonType, Integer>() {
            @Override
            public Integer call(ButtonType b) {
                if (b == buttonTypeOk) {
                    return Integer.parseInt(userdata.getText());
                }
                return null;
            }
        });

        // Request focus on the button field by default.
        // So that text field shows prompt.
        Platform.runLater(() -> okButton.requestFocus());
        Optional<Integer> i = dialog.showAndWait();
        return i.orElseGet(null);
    }


    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptValue  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */

    public static Double getDoubleFromUser(String windowTitle, Double promptValue, String labelText) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField userdata = new TextField();
        userdata.setPromptText(String.format("%.1f",promptValue));


        grid.add(new Label(labelText), 0, 0);
        grid.add(userdata, 1, 0);

        ButtonType buttonTypeOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk,buttonTypeCancel);


        // Enable/Disable OK button depending only after a valid integer has been entered
        Node okButton = dialog.getDialogPane().lookupButton(buttonTypeOk);
        okButton.setDisable(true);
        userdata.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean invalid=true;
            try {
                Double dummy = Double.parseDouble(userdata.getText());
                invalid=false; /* We will only reach this line if the userdata can be parsed as an Integer. */
            } catch (NumberFormatException e) {
                /* do nothing */
            }
            okButton.setDisable(invalid);
        });

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(new Callback<ButtonType, Double>() {
            @Override
            public Double call(ButtonType b) {
                if (b == buttonTypeOk) {
                    return Double.parseDouble(userdata.getText());
                }
                return null;
            }
        });

        // Request focus on the button field by default.
        // So that text field shows prompt.
        Platform.runLater(() -> okButton.requestFocus());
        Optional<Double> i = dialog.showAndWait();
        return i.orElseGet(null);
    }



    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptValue  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */
    public static Double getDoubleFromUserOLD(String windowTitle, Double promptValue, String labelText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getEditor().setPromptText(String.format("%.1f",promptValue));
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.setContentText(labelText);
        /*Platform.runLater(() -> okButton.requestFocus());*/
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
