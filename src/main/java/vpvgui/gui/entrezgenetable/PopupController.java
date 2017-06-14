package vpvgui.gui.entrezgenetable;

/**
 * Created by peter on 03.06.17.
 */
import javafx.event.ActionEvent;
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
import vpvgui.model.Model;

public class PopupController  implements Initializable {

    @FXML private TextField geneSymbolTF;
    @FXML private Button geneButton;

    @FXML private Label instructions;

    /** A reference to the Model. We will use it to add genes information to
     * the model.
     */
    private Model model=null;


    /** This will hold the string with the list of genes entered by the user. */
    private String geneListString=null;


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
        System.err.println("INITIALIZE IN PopupController");

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


    public void setModel(Model mod){this.model=mod;}

    /**
     * Closes the stage of this view
     */
    private void closeStage() {
        if(stage!=null) {
            stage.close();
        }
    }


    public void fetchGeneSymbols(ActionEvent e) {
        this.geneListString = this.geneSymbolTF.getText();
        String[] symbols = parseGeneSymbols(this.geneListString);
        if (this.model!=null)
            this.model.setGeneSymbols(symbols);
        e.consume();
    }

    private String[] parseGeneSymbols(String str) {
        String fields[] = str.split("[,;\\s]");
        for (int i=0;i<fields.length;++i) {
            fields[i]=fields[i].trim().toUpperCase();
        }
        return fields;
    }

}