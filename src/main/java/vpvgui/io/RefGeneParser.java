package vpvgui.io;

import de.charite.compbio.jannovar.JannovarException;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.apache.log4j.Logger;
import vpvgui.model.project.VPVGene;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Parse the refGene.txt.gz file from UCSC. The format is the same for all of the Genomebuilds we are considering.
 * <ol>
 *     <li>bin (e.g., 197) No meaning forus, itis used to index the UCSC files</li>
 *     <li>name (e.g., NM_001353310)</li>
 *     <li>chrom (e.g., chr8)</li>
 *     <li>strand (e.g., -)</li>
 *     <li>txStart (e.g., 130851838)</li>
 *     <li>txEnd (e.g.,131028943)</li>
 *     <li>cdsStart (e.g., 130854387)</li>
 *     <li>cdsEnd (e.g.,130892694) </li>
 *     <li>exonCount (e.g., 13)</li>
 *     <li>exonStarts</li>
 *     <li>exonEnds</li>
 *     <li>score (e.g., 0)</li>
 *     <li>name2 (e.g., FAM49B)</li>
 *     <li>cdsStartStat</li>
 *     <li>cdsEndStat</li>
 *     <li>exonFrames</li>
 * </ol>
 * @author Peter Robinson
 * @version 0.0.2 (2017-07-23)
 */
public class RefGeneParser {

    static Logger logger = Logger.getLogger(RefGeneParser.class.getName());
    /** All genes in the refGenefile are converted into VPVGene objects. These will be used to match
     * the gene list uploaded by the user. */
    private Map<String, VPVGene> genemap=null;

    /** The set of gene symbols that we could not find in Jannovar--and ergo,that we regard as being invalid because
     * they are using nonstandard gene symbols.
     */
    private Set<String> invalidGeneSymbols=null;
    /** The set of gene symbols that we could find in Jannovar--and ergo,that we regard as being valid.
     */
    private Set<String> validGeneSymbols=null;

    public RefGeneParser(String path) {
        genemap=new HashMap<>();
        parse(path);
    }

    /** Parse the refGene.txt.gz file. Note that we parse zero-based numbers here. */
    private void parse(String path) {
        try {
            InputStream fileStream = new FileInputStream(path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream);
            BufferedReader br = new BufferedReader(decoder);
            String line;
            while ((line=br.readLine())!=null) {
                //System.out.println(line);
                String A[]=line.split("\t");
                String accession=A[1];
                String chrom=A[2];
                String strand=A[3];
                Integer gPos;
                if (strand.equals("+")) {
                    gPos = Integer.parseInt(A[4]);
                } else {
                    gPos = Integer.parseInt(A[5])-1;
                }
                String name2=A[12];
                //System.out.println(accession +"; "+chrom+"; "+strand+"; "+gPos+"; "+name2);
                VPVGene gene=null;
                if (genemap.containsKey(name2)) {
                    gene = genemap.get(name2);
                } else {
                    gene = new VPVGene(accession, name2);
                    gene.setChromosome(chrom);
                    if (strand.equals("+")){
                        gene.setForwardStrand();
                    } else if (strand.equals("-")){
                        gene.setReverseStrand();
                    } else {
                        System.err.println("[ERROR] did not recognize strand \""+ strand + "\"");
                        System.exit(1); /* todo throw some exception. */
                    }
                    genemap.put(name2,gene);
                }
                gene.addGenomicPosition(gPos);
            }
            br.close();
        } catch (IOException e) {
            logger.error("Error while attempting to parse the RefGene file from UCSC:"+path);
            logger.error(e,e);
        }
    }




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

    /** The user uploads a list of gene symbols. This function checks this list against the gene symbols that
     * are contained in the corresponding Jannovar serialized file. For each symbol, if it is found in the
     * jannovar serilaized file, the symbol is placed in a {@code Map<String,VPVGene>} as the key,
     * and the corresponding {@link VPVGene} object is placed as the value (thelatter has all the transcript start sites).
     * This map is returned for all valid (found) gene symbols. The valid gene symbols
     * (those found in the refGene.txt.gz file) are also placed in the Set {@link #validGeneSymbols},
     * and all uploaded gene symbols that are not found there are placed in the Set {@link #invalidGeneSymbols}.
     * @param genelst List of gene symbols uploaded by the user.
     * @return map of valid symbols and  {@link VPVGene} objects
     */
    public void checkGenes(List<String> genelst) {
        if (genelst==null) {
            System.err.println("[ERROR] First initialize the gene list and then runm this test");
            System.exit(1); /* todo add exception here. */
        }
        this.invalidGeneSymbols = new HashSet<>();
        this.validGeneSymbols = new HashSet<>();
        Set<String> geneset = new HashSet<>();
        for (String s:genelst) {geneset.add(s);}
        /** key: gene symbol, value: list of corresponding transcripts. */
        Map<String,VPVGene> transcriptmap =new HashMap<>();
        for (String sym:genelst) {
            if (this.genemap.containsKey(sym)) {
                validGeneSymbols.add(sym);
                transcriptmap.put(sym, this.genemap.get(sym));
            } else {
                invalidGeneSymbols.add(sym);
            }
        }
    }

    /**
     * Iterate through all {@link VPVGene} objects and return a list of only those {@link VPVGene}s that
     * represent valid gene symbols uploaded by the user
     * @return List of VPVGenes representing valid uploaded gene symbols.
     */
    public List<VPVGene> getVPVGeneList() {
        List<VPVGene> genelist=new ArrayList<>();
        for (VPVGene g: genemap.values()) {
            if (this.validGeneSymbols.contains(g.getGeneSymbol()))
            genelist.add(g);
        }
        return genelist;
    }

    /** @return the total number of {@link vpvgui.model.project.VPVGene} objects created from parsing the
     * refGene.txt.gz file.
     */
    public int n_totalRefGenes() { return this.genemap.size(); }

    /** Calculates the total number of distinct start points (transcript start points), which would correspond to the
     * number of viewpoints we will design for this gene. Intended for unit testing.
     * @return number of distinct transcription start sites over all VPVGenes.
     */
    public int n_totalTSSstarts() {
        int n=0;
        for (VPVGene vpg :genemap.values()) {
            n+=vpg.n_viewpointstarts();
        }
        return n;
    }

}
