package vpvgui.io;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import de.charite.compbio.jannovar.Jannovar;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import vpvgui.gui.ErrorWindow;

/**
 * This class uses Jannovar to download and build a serialized transcript definition file
 * that contains the coordinates and gene symbols etc for all transcripts/genes.
 * @author  Peter Robinson
 * @version 0.0.2 (2017-06-10)
 */
public class JannovarTranscriptFileBuilder {
    private File downloaddir=null;
    /** should be one of hg19,hg38,mm10 */
    private String genomebuild=null;
    /** should be UCSC, may allow others in the future */
    private String genomedatabase=null;
    /** SHould be set to the name of the serialized file, e.g., {@code hg19_ucsc.ser}. */
    private String jannovarFileName=null;

    private boolean reportProgress=false;


    List<String> datasources= new ArrayList<>();;



    public JannovarTranscriptFileBuilder(String genome, File dirpath){
        downloaddir=dirpath;
        setGenome(genome);
        setJannovarFileName();
    }

    /** Initializes ther path where jannovar while be downloaded to. The path consists of the
     * absolute paht to the directory chosen by the user and then a file name such as
     * {@code hg38_ucsc.ser}.
     */
    private void setJannovarFileName() {
        if (genomedatabase.equals("ucsc")) {
            if (genomebuild.equals("hg19")) {
                this.jannovarFileName = this.downloaddir.getAbsolutePath() + File.separator + "hg19_ucsc.ser";
            } else if (genomebuild.equals("hg38")) {
                this.jannovarFileName =  this.downloaddir.getAbsolutePath() + File.separator + "hg38_ucsc.ser";
            } else if (genomebuild.equals("mm10")) {
                this.jannovarFileName =  this.downloaddir.getAbsolutePath() + File.separator + "mm10_ucsc.ser";
            } else if (genomebuild.equals("mm9")) {
                this.jannovarFileName=this.downloaddir.getAbsolutePath() + File.separator + "mm9_ucsc.ser";
            }
        }
    }

    private void setGenome(String genome) {
        genomedatabase="ucsc";
        genomebuild=genome.toLowerCase();
    }

    /** Start a task to let the Jannovar download run.*/
    public void runJannovar() {
        Stage taskUpdateStage = new Stage(StageStyle.UTILITY);
        Scene scene = new Scene(new Group());
        taskUpdateStage.setTitle("Jannovar download");
        taskUpdateStage.setWidth(400);
        taskUpdateStage.setHeight(180);
        HBox hbox = new HBox();
        Label label = new Label("Downloading Jannovar-based transcript/gene models.\nThis will take a few minutes.\nThis dialog will close when the download is completed");
        hbox.getChildren().add(label);
        ((Group)scene.getRoot()).getChildren().add(hbox);

        taskUpdateStage.setScene(scene);

        final String jannovarpath=this.jannovarFileName;

        Task longTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                File f = new File(jannovarpath);
                if (f.exists())
                    return null; /* Don't download if File already exists!! */
                String dbase=String.format("%s/%s",genomebuild,genomedatabase);
                String argv[] = {"download","-d",dbase,"--download-dir",downloaddir.getAbsolutePath()};
                Jannovar.main(argv);
                return null;
            }
        };

        longTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                taskUpdateStage.hide();
            }
        });
        taskUpdateStage.show();
        new Thread(longTask).start();
    }

    public String getSerializedFilePath() {
        return this.jannovarFileName;
    }


}
