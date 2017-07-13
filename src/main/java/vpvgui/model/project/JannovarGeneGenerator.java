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
 * This class uses the Jannovar transcript definition file (e.g., hg38_ucsc.ser) to extract a list of
 * TranscriptModel objects. The function {@link #checkGenes} returns a Map whose key is a gene symbol and
 * whose value is a list of TranscriptModel objects.
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

    /** Map from integer that represents chromosome to the actual chromosome name (e.g., chrX) */
    private Map<Integer,String> chrId2Name=null;


    private void extractChromosomeNames() {
        chrId2Name = new HashMap<>();
        for (Chromosome c:chromosomeMap.values()) {
            int id=c.getChrID();
            String name=c.getChromosomeName();
            chrId2Name.put(id,name);
        }
    }

    /**
     *
     * @param id integer representation of a chromosome, e.g., 1 or 23
     * @return corresponding String, e.g., chr1 or chrX.
     */
    public String chromosomeId2Name(int id) {
        return this.chrId2Name.get(id);
    }


    private Set<String> invalidGeneSymbols=null;
    private Set<String> validGeneSymbols=null;

    /** @return A list of those symbols uploaded by the user that could NOT be found in the Jannovar serialized file.*/
    public List<String> getInvalidGeneSymbols() {
        List<String> lst = new ArrayList<>();
        lst.addAll(invalidGeneSymbols);
        Collections.sort(lst);
        return lst;
    }

    /** @return A list of those symbols uploaded by the user that could be found in the Jannovar serialized file.*/
    public List<String> getValidGeneSymbols() {
        List<String> lst = new ArrayList<>();
        lst.addAll(validGeneSymbols);
        Collections.sort(lst);
        return lst;
    }


    /**
     * Deserialize the transcript definition file, and initialize a map of
     * mappings from chromosome ids (simple integers) to chromosome names,
     * e.g., chrX or chr4.
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
        extractChromosomeNames();
    }

    public JannovarGeneGenerator(String pathToSerializedFile) {
        this.jannovarSerPath=pathToSerializedFile;
       System.out.println("JannovarGeneGenerator jannovar ser path="+jannovarSerPath);
    }

    /** The user uploads a list of gene symbols. This function checks this list against the gene symbols that
     * are contained in the corresponding Jannovar serialized file. For each symbol, if it is found in the
     * jannovar serilaized file, the symbol is placed in a {@code Map<String,List<TranscriptModel>>} as the key,
     * and all of the corresponding Jannovar TranscriptModels are placed in the List as the value. This map
     * is returned for all valid (found) gene symbols. The valid gene symbols (those found in the Jannovar
     * serialized file) are also placed in the Set {@link #validGeneSymbols}, and all uploaded gene symbols that
     * are not found in the Jannovar serialized file are placed in the Set {@link #invalidGeneSymbols}.
     * @param genelst List of gene symbols uploaded by the user.
     * @return
     */
    public Map<String,List<TranscriptModel>> checkGenes(List<String> genelst) {
        if (this.jannovarSerPath==null) {
            System.err.println("Path to serialized Jannovar file is not defined -- set it and retry!");
            return null;
        }
        if (genelst==null) {
            System.err.println("[ERROR] First initialize the gene list and then runm this test");
            System.exit(1);
        }
        this.invalidGeneSymbols = new HashSet<>();
        this.validGeneSymbols = new HashSet<>();
        Set<String> geneset = new HashSet<>();
        for (String s:genelst) {geneset.add(s);}
        /** key: gene symbol, value: list of corresponding transcripts. */
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
            * valid gene symbols. There may be symbols that are not valid that we can
            * not parse with Jannovar. Put them into the invalid map so they can be displayed on the GUI
             */
            for (String gs:geneset) {
                if ( ! transcriptmap.containsKey(gs)) {
                    invalidGeneSymbols.add(gs);
                }
            }
        } catch (JannovarException e) {
            e.printStackTrace();
        }
       for (String tm:transcriptmap.keySet()) {
            System.err.println(tm);
            System.err.println(transcriptmap.get(tm));
        }
        return transcriptmap;
    }


}
