package vpvgui.gui;

import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class Popups {



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
        TextInputDialog dialog = new TextInputDialog(promptText);
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.setContentText(labelText);

        Optional<String> result = dialog.showAndWait();
        String r=result.orElse(null);
        if (r==null) return null;
        try {
            return Integer.parseInt(r);
        } catch (NumberFormatException e) {
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
    public static Double getDoubleFromUser(String windowTitle, String promptText, String labelText) {
        TextInputDialog dialog = new TextInputDialog(promptText);
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
