package vpvgui.gui.entrezgenetable;

/**
 * Created by peter on 03.06.17.
 */
import javafx.event.ActionEvent;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import vpvgui.model.Model;
import vpvgui.model.project.JannovarGeneGenerator;

public class PopupController  implements Initializable {

    @FXML private TextArea geneSymbolArea;
    @FXML private Button geneButton;
    @FXML private Button uploadGenesButton;
    @FXML private Button acceptGenesButton;

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
        geneSymbolArea.setWrapText(true);
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

    /** Transfer the genes to the model.
     *
     * @param e
     */
    public void fetchGeneSymbols(ActionEvent e) {
        this.geneListString = this.geneSymbolArea.getText();
        String[] symbols = parseGeneSymbols(this.geneListString);
        if (this.model!=null)
            this.model.setGeneSymbols(symbols);
        e.consume();
        geneSymbolArea.clear();
        parseGeneSymbolsWithJannovar(symbols);
    }


    private void parseGeneSymbolsWithJannovar(String[] symb) {
        JannovarGeneGenerator jgg = new JannovarGeneGenerator(this.model.getSettings().getTranscriptsFileTo());
        jgg.checkGenes(symb);
    }




    private String[] parseGeneSymbols(String str) {
        String fields[] = str.split("[,;\\s]");
        for (int i=0;i<fields.length;++i) {
            fields[i]=fields[i].trim().toUpperCase();
        }
        return fields;
    }


    public void uploadGenes(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            System.err.println("[ERROR] COuld not open genes file -TODO throw exception");
            return;
        }
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br =new BufferedReader(new FileReader(file));
            String line =null;
            while ((line=br.readLine())!=null) {
                //System.out.println(line);
                sb.append(line);
            }
        } catch (IOException err) {
            err.printStackTrace();
        }
        String[] symbols = parseGeneSymbols(sb.toString());
        if (this.model!=null)
            this.model.setGeneSymbols(symbols);
        e.consume();
        geneSymbolArea.clear();
        parseGeneSymbolsWithJannovar(symbols);

    }




}