package vpvgui.model.project;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.JannovarException;
import de.charite.compbio.jannovar.cmd.HelpRequestedException;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import vpvgui.gui.ErrorWindow;

import java.util.*;

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


    private Set<String> invalidGeneSymbols=null;
    private Set<String> validGeneSymbols=null;

    public List<String> getInvalidGeneSymbols() {
        List<String> lst = new ArrayList<>();
        lst.addAll(invalidGeneSymbols);
        Collections.sort(lst);
        return lst;
    }

    public List<String> getValidGeneSymbols() {
        List<String> lst = new ArrayList<>();
        lst.addAll(validGeneSymbols);
        Collections.sort(lst);
        return lst;
    }


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
       // System.out.println("Jan="+jannovarSerPath);
    }

    public Map<String,List<TranscriptModel>> checkGenes(List<String> genelst) {
        if (this.jannovarSerPath==null) {
            System.err.println("Path to serialized Jannovar file is not defined -- set it and retry!");
            return null;
        }
        if (genelst==null) {
            ErrorWindow.display("Error", "Attempt to validate an empty gene list. "+
                    "Please upload file with gene symbols prior to validation");
            return null;
        }
        this.invalidGeneSymbols = new HashSet<>();
        this.validGeneSymbols = new HashSet<>();
        Set<String> geneset = new HashSet<>();
        for (String s:genelst) {geneset.add(s);}
        /** key: gene symbol, value: lkist of corresponding transcripts. */
        Map<String,List<TranscriptModel>> transcriptmap =new HashMap<>();

        try {
            deserializeTranscriptDefinitionFile(this.jannovarSerPath);
            com.google.common.collect.ImmutableMap<String,TranscriptModel> mp =this.jannovarData.getTmByAccession();

            for (TranscriptModel tm : mp.values()) {
                String symbol = tm.getGeneSymbol();
                if ( geneset.contains(symbol) ){
                    if (transcriptmap.containsKey(symbol)) {
                        List<TranscriptModel> lst = transcriptmap.get(symbol);
                        lst.add(tm);
                        validGeneSymbols.add(symbol);
                    } else {
                        List<TranscriptModel> lst = new ArrayList<>();
                        lst.add(tm);
                        transcriptmap.put(symbol,lst);
                    }
                }
            }
            /* When we get here, we have identified transcripts models for all of our
            * valid gene symbols. There maybesymbols that are not valid that we can
            * not parse with Jannovar. Put them into the invalid map so they can be displayed on the GUI
             */
            for (String gs:geneset) {
                if (transcriptmap.containsKey(gs)) {
                    // OK; do nothing
                }  else {
                    invalidGeneSymbols.add(gs);
                }
            }
        } catch (JannovarException e) {
            e.printStackTrace();
        }
       /* for (String tm:transcriptmap.keySet()) {
            System.err.println(tm);
        }*/
        return transcriptmap;
    }


}
