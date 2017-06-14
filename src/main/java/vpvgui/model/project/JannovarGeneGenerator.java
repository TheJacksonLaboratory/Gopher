package vpvgui.model.project;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.JannovarException;
import de.charite.compbio.jannovar.cmd.HelpRequestedException;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.TranscriptModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by peter on 13.06.17.
 */
public class JannovarGeneGenerator {

    private String jannovarSerPath=null;

    /** {@link JannovarData} with the information */
    protected JannovarData jannovarData = null;

    /** {@link ReferenceDictionary} with genome information. */
    protected ReferenceDictionary refDict = null;

    /** Map of Chromosomes, used in the annotation. */
    protected ImmutableMap<Integer, Chromosome> chromosomeMap = null;

    /**
     * Deserialize the transcript definition file
     *
     * @param pathToDataFile
     *            String with the path to the data file to deserialize
     * @throws JannovarException
     *             when there is a problem with the deserialization
     * @throws HelpRequestedException
     *             when the user requested the help page
     */
    protected void deserializeTranscriptDefinitionFile(String pathToDataFile)
            throws JannovarException, HelpRequestedException {
        this.jannovarData = new JannovarDataSerializer(pathToDataFile).load();
        this.refDict = this.jannovarData.getRefDict();
        this.chromosomeMap = this.jannovarData.getChromosomes();
    }

    public JannovarGeneGenerator(String pathToSerializedFile) {
        this.jannovarSerPath=pathToSerializedFile;
        System.out.println("Jan="+jannovarSerPath);
    }

    public void checkGenes(String[] genelst) {
        if (this.jannovarSerPath==null) {
            System.err.println("Path to serialized Jannovar file is not defined -- set it and retry!");
            return;
        }
        Set geneset = new HashSet<>();
        for (String s:genelst) {geneset.add(s);}
        List<TranscriptModel> transcriptlist=new ArrayList<>();

        try {
            deserializeTranscriptDefinitionFile(this.jannovarSerPath);
            com.google.common.collect.ImmutableMap<String,TranscriptModel> mp =this.jannovarData.getTmByAccession();

            for (TranscriptModel tm : mp.values()) {
                String symbol = tm.getGeneSymbol();
                if ( geneset.contains(symbol) ){
                    transcriptlist.add(tm);
                }
            }
        } catch (JannovarException e) {
            e.printStackTrace();
        }
        for (TranscriptModel tm:transcriptlist) {
            System.err.println(tm);
        }
    }


}
