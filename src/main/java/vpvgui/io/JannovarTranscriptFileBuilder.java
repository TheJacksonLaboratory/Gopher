package vpvgui.io;

import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.Immutable;
import de.charite.compbio.jannovar.JannovarException;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.datasource.DataSourceFactory;
import de.charite.compbio.jannovar.datasource.DatasourceOptions;
import de.charite.compbio.jannovar.datasource.InvalidDataSourceException;
//import de.charite.compbio.jannovar.cmd.download.JannovarDownloadOptions;
import de.charite.compbio.jannovar.impl.util.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import de.charite.compbio.jannovar.Jannovar;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import vpvgui.gui.ErrorWindow;
import vpvgui.gui.MessageConsole;

/**
 * This class uses Jannovar to download and build a serialized transcript definition file
 * that contains the coordinates and gene symbols etc for all transcripts/genes.
 * Created by peter on 08.05.17.
 */
public class JannovarTranscriptFileBuilder {

    //private DownloadOptions options;
   // private JannovarDownloadOptions options;

    private File downloaddir=null;
    /** should be one of hg19,hg38,mm10 */
    private String genomebuild;
    /** should be UCSC, may allow others in the future */
    private String genomedatabase=null;

    private boolean reportProgress=false;


    List<String> datasources= new ArrayList<>();;



    public JannovarTranscriptFileBuilder( String genome, File dirpath){
        downloaddir=dirpath;
        setGenome(genome);
        runJannovar();
    }

    private void setGenome(String genome) {
        String fields[]=genome.split("-");
        if (fields.length != 2) {
            ErrorWindow.display("Error","Did not recognize genome build (should never happen):"+genome);
            return;
        }
        genomedatabase=fields[0].toLowerCase();
        genomebuild=fields[1].toLowerCase();
    }


    private void runJannovar() {
        final double wndwWidth = 300.0d;
        //Label updateLabel = new Label("Running tasks...");

        Stage taskUpdateStage = new Stage(StageStyle.UTILITY);


        Task longTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                VBox messageConsol = MessageConsole.getMessageConsole();

                VBox updatePane = new VBox();
                updatePane.setPadding(new Insets(10));
                updatePane.setSpacing(5.0d);
                updatePane.getChildren().addAll(messageConsol);

                taskUpdateStage.setScene(new Scene(messageConsol));
                taskUpdateStage.show();
                System.out.println("IN STARTED TO START TASK");
                //progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                String dbase=String.format("%s/%s",genomebuild,genomedatabase);
                String argv[] = {"download","-d",dbase,"--download-dir",downloaddir.getAbsolutePath()};
                updateMessage("Downloading and building Jannovar transcript file (may take several minutes)");


                System.out.println("About  TO START Jannovar");

                Jannovar.main(argv);
                System.out.println("DONE Jannovar");
                /*for (int i = 1; i <= max; i++) {
                    if (isCancelled()) {
                        break;
                    }
                    //updateProgress(i, max);
                    //updateMessage("Task part " + String.valueOf(i) + " complete");

                    Thread.sleep(100);
                }*/
                return null;
            }
        };

        longTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                taskUpdateStage.hide();
            }
        });
        //progress.progressProperty().bind(longTask.progressProperty());
       // updateLabel.textProperty().bind(longTask.messageProperty());

        taskUpdateStage.show();
        System.out.println("ABOUT TO START TASK");
        new Thread(longTask).start();
        /* todo check absolute path is not null
        String dbase=String.format("%s/%s",genomebuild,genomedatabase);
        String argv[] = {"download","-d",dbase,"--download-dir",downloaddir.getAbsolutePath()};
        Jannovar.main(argv);
*/

    }


}
