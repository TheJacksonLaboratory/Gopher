package vpvgui.gui.entrezgenetable;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import vpvgui.framework.Signal;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.Model;
import vpvgui.model.project.JannovarGeneGenerator;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by peter on 01.07.17.
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

    private int maxDistToGenomicPosUp =10000;
    private int maxDistToGenomicPosDown = 10000;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isvalidated=false;
    }

   public void setSignal(Consumer<Signal> signal) {
        this.signal = signal;
    }






    /*
    public HashMap<String, Object> getResult() {
        return this.result;
    }*/

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
        JannovarGeneGenerator jgg = new JannovarGeneGenerator(this.model.getSettings().getTranscriptsFileTo());
        /* key is a gene symbol,and value is a listof corresponding transcripts. */
        this.validGenes2TranscriptsMap = jgg.checkGenes(this.symbols);
        List<String>  validGeneSymbols = jgg.getValidGeneSymbols();
        List<String> invalidGeneSymbols= jgg.getInvalidGeneSymbols();
        int n_transcripts = getNTranscripts(validGenes2TranscriptsMap);
        String html = getValidatedGeneListHTML(validGeneSymbols, invalidGeneSymbols,validGenes2TranscriptsMap.size(), n_transcripts);
        setData(html);
        isvalidated=true;
    }

    private int getNTranscripts( Map<String,List<TranscriptModel>> mp) {
        int n=0;
        for (String s : mp.keySet()) {
            List<TranscriptModel> lst = mp.get(s);
            n += lst.size();
        }
        return n;
    }


    private void parseGeneSymbolsWithJannovar(String[] symb) {
        if (this.model==null) {
            ErrorWindow.display("Error","Model was null (report to developers)");
            return;
        }
        if (this.model.getSettings()==null) {
            ErrorWindow.display("Error","Settings object was null (report to developers)");
            return;
        }
        String transcriptfile=this.model.getSettings().getTranscriptsFileTo();
        if (transcriptfile==null) {
            ErrorWindow.display("Error retrieving Jannovar transcript file","Generate Jannovar transcript file before loading genes");
            return;
        }
        JannovarGeneGenerator jgg = new JannovarGeneGenerator(this.model.getSettings().getTranscriptsFileTo());
        //jgg.checkGenes(symb);
    }


    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }



    private String joinWithNL(String[] a) {
        if (a==null || a.length==0)
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append(a[0]);
        for (int i=1;i<a.length;i++) {
            sb.append("\n  ---"+a[i]);
        }
        return sb.toString();
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

    /** TODO -- stimmt nicht fuer Maus */
    private String getChromosomeString(int c) {
         if (c>0 && c<23) {
            return String.format("chr%d",c);
        } else if (c==23) {
            return "chrX";
        } else if (c==24) {
            return "chrY";
        } else if (c==25) {
            return "chrM";
        } else {
            return "???(Could not parse chromosome)";
        }
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
                    pos = iv.getBeginPos();
                } else {
                    pos = iv.getEndPos();
                }
                ViewPoint vp = new ViewPoint(referenceSequenceID,pos,maxDistToGenomicPosUp,maxDistToGenomicPosDown);
                vpvgene.addViewPoint(vp);
            }
            vpvgenelist.add(vpvgene);
            this.model.setVPVGenes(vpvgenelist);
        }
        System.out.println("added "+ vpvgenelist.size() + " vpv genes");
        signal.accept(Signal.DONE);
    }



}
