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

/**
 * This class uses Jannovar to download and build a serialized transcript definition file
 * that contains the coordinates and gene symbols etc for all transcripts/genes.
 * Created by peter on 08.05.17.
 */
public class JannovarTranscriptFileBuilder {

    //private DownloadOptions options;
   // private JannovarDownloadOptions options;

    private File downloaddir=null;

    private String genomebuild;

    private boolean reportProgress=false;


    List<String> datasources= new ArrayList<>();;



    public JannovarTranscriptFileBuilder( String genome, File dirpath){
        downloaddir=dirpath;
        genomebuild=genome;
        runJannovar();

    }

    private void runJannovar() {
        String argv[] = {"download","-d","hg19/ucsc","--download-dir",downloaddir.getAbsolutePath()};
        Jannovar.main(argv);


    }


}
