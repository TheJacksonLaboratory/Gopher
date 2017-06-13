package vpvgui.gui.entrezgenetable;

/**
 * Created by peter on 03.06.17.
 */
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PopupController  implements Initializable {

    @FXML private TextField usernameTF;
    @FXML private PasswordField passwordPF;
    @FXML private Button connectBtn;

    @FXML private Label instructions;


    private Stage stage = null;
    private HashMap<String, Object> result = new HashMap<String, Object>();

    private static final String WORDS =
            "Paste a gene list into the window or use the File Chooser to select a file "+
                    " with the genes. Use valid HGNC gene symbols. Valid gene symbols will be shown" +
                    "in bold text.";


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instructions.setWrapText(true);
        instructions.setStyle("-fx-font-family: \"Comic Sans MS\"; -fx-font-size: 20; -fx-text-fill: darkred;");
        instructions.setText(WORDS);
        connectBtn.setOnAction((event)->{
            result.clear();
            result.put("username", usernameTF.getText());
            result.put("password", passwordPF.getText());
            closeStage();
        });

    }

    public HashMap<String, Object> getResult() {
        return this.result;
    }

    /**
     * setting the stage of this view
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Closes the stage of this view
     */
    private void closeStage() {
        if(stage!=null) {
            stage.close();
        }
    }

}