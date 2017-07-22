package vpvgui.gui.entrezgenetable;


import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import vpvgui.framework.Signal;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.Model;
import vpvgui.model.project.JannovarGeneGenerator;
import vpvgui.model.project.VPVGene;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * This is the window that appears so that the user can upload target genes. The dialog has three buttons.
 * <ol>
 *     <li>Upload causes {@link #uploadGenes(ActionEvent)} to be run, which fills the set {@link #symbols}.
 *     This set can contain valid and invalid symbols</li>
 *     <li>Validate causes {@link #validateGeneSymbols(ActionEvent)} to be run, which fills the map
 *     {@link #validGenes2TranscriptsMap} that has all valid symbols (key) with their corresponding transcripts (value), whereby
 *     only one transcriptmodel is stored per distinct transcription start site. The function also displays lists of valid and invalid
 *     gene symbols in the dialog</li>
 *     <li>Accept causes {@link #acceptGenes()} to be run, which creates a list of {@link VPVGene} objects - one for
 *     each valid symbol -- and passes this to the {@link Model}. It also causes the dialog to close</li>
 * </ol>
 * Therefore, if all goes well, the effect of this dialog is to pass a list of {@link VPVGene} objects to the model.
 * This list should then be used to create {@link vpvgui.model.project.ViewPoint} objects elsewhere in the code.
 * @author Peter Robinson
 * @version 0.0.2 (2017-07-22)
 */
public class EntrezGenePresenter implements Initializable {
    @FXML
    private Label instructions;

    @FXML
    private WebView wview;

    @FXML
    private Button uploadGenesButton;

    /** A reference to the Model. We will use it to add genes information to
     * the model.
     */
    private Model model=null;

    /** This will hold the string with the list of genes entered by the user. */
    private String geneListString=null;


    private Stage stage = null;
    //private HashMap<String, Object> result = new HashMap<String, Object>();
    /** List of the symbols prior to validation, i.e., may contain invalid symbols. */
    private List<String> symbols=null;

    private Map<String,List<TranscriptModel>> validGenes2TranscriptsMap=null;

    private List<VPVGene> vpvgenelist;


    private Consumer<Signal> signal;


    private boolean isvalidated=false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isvalidated=false;
    }

   public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }

    /** This object will find the Jannovar TranscriptModel objects that correspond to all of the
     * transcripts that correspond to a Gene symbol.
     */
   private JannovarGeneGenerator jgg =null;

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
    @FXML public void validateGeneSymbols(ActionEvent e) {
        e.consume();
        if (this.model.getSettings()==null) {
            ErrorWindow.display("Error","Settings object was null (report to developers)");
            return;
        }
        String transcriptfile=this.model.getSettings().getTranscriptsFileTo();
        if (transcriptfile==null) {
            ErrorWindow.display("Error retrieving Jannovar transcript file","Generate Jannovar transcript file before loading genes");
            return;
        }
        this.jgg = new JannovarGeneGenerator(this.model.getSettings().getTranscriptsFileTo());
        /* key is a gene symbol,and value is a listof corresponding transcripts. */
        this.validGenes2TranscriptsMap = jgg.checkGenes(this.symbols);
        List<String>  validGeneSymbols = jgg.getValidGeneSymbols();
        List<String> invalidGeneSymbols= jgg.getInvalidGeneSymbols();
        int n_transcripts = getNTranscripts(validGenes2TranscriptsMap);
        String html = getValidatedGeneListHTML(validGeneSymbols, invalidGeneSymbols,validGenes2TranscriptsMap.size(), n_transcripts);
        setData(html);
        isvalidated=true;
    }

    /** @return total number of transcripts. */
    private int getNTranscripts( Map<String,List<TranscriptModel>> mp) {
        int n=0;
        for (String s : mp.keySet()) {
            List<TranscriptModel> lst = mp.get(s);
            n += lst.size();
        }
        return n;
    }

   /** Sets the text that will be shown in the HTML View. */
    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }

    @FXML
    public void uploadGenes(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            System.err.println("[ERROR] Could not open genes file -TODO throw exception");
            return;
        }
        StringBuilder sb = new StringBuilder();
        this.symbols = new ArrayList<>();
        try {
            BufferedReader br =new BufferedReader(new FileReader(file));
            String line =null;
            while ((line=br.readLine())!=null) {
                //System.out.println(line);
                symbols.add(line.trim());
            }
        } catch (IOException err) {
            err.printStackTrace();
        }
        e.consume();
        setData(getInitialGeneListHTML(symbols));
        isvalidated=false;
    }



    private String getValidatedGeneListHTML(List<String> valid, List<String> invalid, int n_genes, int n_transcripts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<style type=\"text/css\">\n" +
                " span.bold-red {\n" +
                "    color: red;\n" +
                "    font-weight: bold;\n" +
                "}\n" +
                 " span.blu {\n" +
                 "    color: #4e89a4;\n" +
                 "    font-weight: normal;\n" +
                "}\n" +
                "</style>");
        sb.append("<body><h3>Validated gene list</h3>");
        sb.append(String.format("<p>We parsed a total number of %d genes and found a total of %d associated transcripts.</p>",n_genes,n_transcripts));
        sb.append(String.format("<p>%d of the uploaded gene symbols were valid, and %d were invalid or could not be parsed.</p>",
                valid.size(),invalid.size()));
        if (invalid.size()>0) {
            sb.append("<p>Invalid genes:<br/>");
            for (String inv : invalid) {
                sb.append("<span class=\"bold-red\">"+ inv + "</span><br/>");
            }
            sb.append("</p>");
            sb.append("<p><i>Please correct the gene symbol in the input file before proceeding. ");
            sb.append("Otherwise, the invalid gene symbol will be ignored.</i></p>");
        }
        if (valid.size()==0) {
            sb.append("<p>Error: No valid genes found!</p>");
        } else {
            sb.append("<p>Valid genes:<br/>");
            for (String v : valid) {
                sb.append("<span class=\"blu\">"+ v + "</span><br/>");
            }
            sb.append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }


    private String getInitialGeneListHTML(List<String> genes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>Uploaded gene list</h3>");
        sb.append(String.format("<p>Number of genes: %d.</p>",genes.size()));
        sb.append("<p>");
        for (String g:genes) {
            sb.append(g + "<br/>");
        }
        sb.append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /** Use the Jannovar Gene Generator object to get the correct chromosome string, e.g., chrX */
    private String getChromosomeString(int c) {
         return this.jgg.chromosomeId2Name(c);
    }

    @FXML public void acceptGenes() {
        if (!isvalidated) {
            ErrorWindow.display("Error","Please validate genes for accepting them!");
            return;
        }
        if (validGenes2TranscriptsMap==null || validGenes2TranscriptsMap.size()==0) {
            ErrorWindow.display("Error","Unable to parse even one valid transcript, please revise the gene symbol file!");
            return;
        }
        vpvgenelist = new ArrayList<>();
        for (String symbol : validGenes2TranscriptsMap.keySet()) {
            List<TranscriptModel> transcriptList=validGenes2TranscriptsMap.get(symbol);
            TranscriptModel tm = transcriptList.get(0);
            String referenceSequenceID=getChromosomeString(tm.getChr());
            String id = tm.getGeneID();
            VPVGene vpvgene=new VPVGene(id,symbol);
            vpvgene.setChromosome(referenceSequenceID);
            if (tm.getStrand().isForward()) {
                vpvgene.setForwardStrand();
            } else {
                vpvgene.setReverseStrand();
            }
            for (TranscriptModel tmod: transcriptList) {
                GenomeInterval iv = tmod.getTXRegion();
                Integer pos=null;
                if (tm.getStrand().isForward()) {
                    /* Add 1 to convert from zero-based to one-based numbering */
                    pos = iv.getGenomeBeginPos().getPos()+1;
                } else {
                    /* The last position of a minus-strand gene is the start position,
                     * and the numbering is then one-based once it is flipped. Need to use
                     * the withStrand to convert positions of minus-strand genes.
                     */
                    pos = iv.withStrand(Strand.FWD).getGenomeEndPos().getPos();
                }
                vpvgene.addGenomicPosition(pos);
            }
            vpvgenelist.add(vpvgene);
            this.model.setVPVGenes(vpvgenelist);
        }
        System.out.println("[INFO] EntrezGenePresenter: added "+ vpvgenelist.size() + " vpv genes");
        signal.accept(Signal.DONE);
    }



}
