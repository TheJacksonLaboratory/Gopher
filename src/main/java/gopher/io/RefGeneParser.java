package gopher.io;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gopher.service.model.GopherGene;
import gopher.gui.factories.PopupFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Parse the refGene.txt.gz file from UCSC. The format is the same for all of the genome builds we are considering.
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
 * <p>Note that we skip all gene models located on random chromosomes because we do not want to create probes for
 * random chromosome contigs at this time.</p>
 * <p> The class produces a list of {@link GopherGene} objects that represent the genes found in the UCSC files.
 * These objects convert the coordinate system in the UCSC datbase file (which is 0-start, half-open) to one-based fully closed (both endpoints
 * included, which is the way the data are shown on the UCSC browser).</p>
 * @author Peter Robinson
 * @author Peter Hansen
 * @version 0.2.4 (2017-11-26)
 */
public class RefGeneParser {
    private final static Logger logger = LoggerFactory.getLogger(RefGeneParser.class.getName());
    /** All genes in the refGene file are converted into GopherGene objects. These will be used to match
     * the gene list uploaded by the user. Key: A gene symbol (e.g., FBN1), value, the corresponding {@link GopherGene}.
     * This map should contain all symbols in the refGene file*/
    private final Map<String, GopherGene> geneSymbolMap;
    /** This  map contains keys like APOC3_chr11_116700608, so that if genes have positions on  in the genome, each position is chosen.
     * This map should contain all symbols in the refGene file.
     *  */
    private final Map<String, GopherGene> gene2chromosomePosMap;
    /** The set of gene symbols that we could not find in the {@code refGene.txt.gz} file--and ergo,that we regard as being invalid because
     * they are using nonstandard gene symbols.*/
    private Set<String> invalidGeneSymbols=ImmutableSet.of();
    /** The set of gene symbols that we could find in  the {@code refGene.txt.gz} file--and ergo,that we regard as being valid.
     * These are the genes chosen by the user. */
    private Set<String> validGeneSymbols=ImmutableSet.of();
    /** Total number of genes in the refGene.txt.gz file (number of unique gene symbols). */
    private int n_totalGenes;
    /** Total number of transcription start sites in the refGene.txt.gz file. */
    private int n_totalTSS;
    /** Total number of TSS chosen by the user. */
    private int n_chosenTSS;
    /** index of the coding sequence start position in the UCSC file. */
    private static final int CDS_START_IDX=6;
    /** index of the coding sequence end position in the UCSC file. */
    private static final int CDS_END_IDX=7;


    /**
     * @param path Path to the {@code refGene.txt.gz} file.
     */
    public RefGeneParser(String path) {
        geneSymbolMap =new HashMap<>();
        gene2chromosomePosMap =new HashMap<>();
        parse(path);
    }

    /** Parse the {@code refGene.txt.gz} file. Note that we parse zero-based numbers here.
     * A side-effect of the parsing is that we get the total number of genes and transcription
     * start sites contained in the {@code refGene.txt.gz} file. These counts are stored
     * in the variables {@link #n_totalTSS} and {@link #n_totalGenes} and can be retrieved
     * by the functions {@link #getTotalTSScount()} and {@link #getTotalNumberOfRefGenes()}.*/
    private void parse(String path) {
        try {
            InputStream fileStream = new FileInputStream(path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream);
            BufferedReader br = new BufferedReader(decoder);
            String line;
            while ((line=br.readLine())!=null) {
                String[] A =line.split("\t");
                String accession=A[1];
                String chrom=A[2];
                if (chrom.contains("_")) { continue;}
                if (chrom.contains("random")) { continue; } /* do not take gene models on random contigs. */
                String strand=A[3];
                Integer gPos;
                // The UCSC database files have 0-based, closed start, open end numbers
                // we want to return 1-0based, fully closed position (both endpoints included).
                if (strand.equals("+")) {
                    gPos = Integer.parseInt(A[4]) + 1;
                } else {
                    gPos = Integer.parseInt(A[5]);
                }
                // if the CDS is indicated at the same position for start and end,
                // then the transcript is non-coding
                boolean isNoncoding = A[CDS_START_IDX].equals(A[CDS_END_IDX]);
                String name2=A[12]; // this is the gene symbol
                //String key = name2.concat(chrom);
                String key=String.format("%s_%s_%d",name2,chrom,gPos);
                GopherGene gene;
                if (gene2chromosomePosMap.containsKey(key)) {
                    gene = gene2chromosomePosMap.get(key);
                } else {
                    gene = new GopherGene(accession, name2, isNoncoding, chrom, strand);
                    geneSymbolMap.put(name2,gene);
                    gene2chromosomePosMap.put(key,gene);
                }
                gene.addGenomicPosition(gPos);

            }
            br.close();
        } catch (IOException e) {
            logger.error("Error while attempting to parse the RefGene file from UCSC:"+path);
            logger.error("IOException: {}", e.getMessage());
        }
        n_totalGenes=geneSymbolMap.size();
        n_totalTSS=0;
        // Now collect the unique transcription start site positions.
        gene2chromosomePosMap.values().forEach(vpvGene -> n_totalTSS += vpvGene.n_viewpointstarts());
    }

    /** @return a list of all gene symbols of all protein-coding genes in the {@code refGene.txt.gz} file.*/
    public List<String>  getAllProteinCodingGeneSymbols() {
        ImmutableList.Builder<String> builder=new ImmutableList.Builder<>();
        ImmutableSet.Builder<String> setbuilder=new ImmutableSet.Builder<>();
        for (Map.Entry<String,GopherGene> entry : this.geneSymbolMap.entrySet()) {
            if (entry.getValue().isCoding()) {
                builder.add(entry.getKey());
                setbuilder.add(entry.getKey());
            }
        }
        this.validGeneSymbols=setbuilder.build();
        return builder.build();
    }


    /** @return A sorted list of those symbols uploaded by the user that could NOT be found in the {@code refGene.txt.gz} file.*/
    public List<String> getInvalidGeneSymbols() {
        if (invalidGeneSymbols==null)
            return ImmutableList.of(); //
        List<String> lst = new ArrayList<>(invalidGeneSymbols);
        Collections.sort(lst);
        return lst;
    }

    /**
     * Return a sorted list of validated genes symbols. If none of the gene symbols are valid, we return an empty list.
     * This will not allow the GUI to start to calculate viewpoints.
     * @return A sorted list of those symbols uploaded by the user that could be found in the {@code refGene.txt.gz} file.*/
    public List<String> getValidGeneSymbols() {
        if (validGeneSymbols==null)
            return ImmutableList.of(); //
        List<String> lst = new ArrayList<>(validGeneSymbols);
        Collections.sort(lst);
        return lst;
    }

    /** The user uploads a list of gene symbols. This function checks this list against the gene symbols that
     * are contained in the corresponding {@code refGene.txt.gz} file. Each uploaded symbol that is found in the
     * {@code refGene.txt.gz}  file is placed in {@link #geneSymbolMap} as the key,
     * and the corresponding {@link GopherGene} object is placed in the Set {@link #validGeneSymbols},
     * and all uploaded gene symbols that are not found there are placed in the Set {@link #invalidGeneSymbols}.
     * @param genelst List of gene symbols uploaded by the user.
     */
    public void checkGenes(List<String> genelst) {
        if (genelst==null) {
            logger.error("Attempt to check genelist with null pointer");
            PopupFactory.displayError("Unable to check gene list", "Attempt to check genelist with null pointer");
            return;
        }
        ImmutableSet.Builder<String> validSetBuilder = new ImmutableSet.Builder<>();
        ImmutableSet.Builder<String> invalidSetBuilder = new ImmutableSet.Builder<>();
        for (String sym:genelst) {
            if (this.geneSymbolMap.containsKey(sym)) { // it's only checked, if a gene is in refGene or not -> use geneSymbolMap
                validSetBuilder.add(sym);
            } else {
                invalidSetBuilder.add(sym);
            }
        }
        this.invalidGeneSymbols=invalidSetBuilder.build();
        this.validGeneSymbols=validSetBuilder.build();
    }

    /**
     * Iterate through all {@link GopherGene} objects and return a list of only those {@link GopherGene}s that
     * represent valid gene symbols uploaded by the user
     * @return List of VPVGenes representing valid uploaded gene symbols.
     */
    public List<GopherGene> getGopherGeneList() {
        ImmutableList.Builder<GopherGene> builder = new ImmutableList.Builder<>();
        this.n_chosenTSS=0;
        for (GopherGene g: gene2chromosomePosMap.values()) {
            if (this.validGeneSymbols.contains(g.getGeneSymbol())) {
                builder.add(g);
                this.n_chosenTSS += g.n_viewpointstarts();
            }
        }
        return builder.build(); // must contain objects RNU6-2 on chr1 as well for RNU6-2 on chr10 -> use gene2chromosomePosMap
    }

    /** @return the total number of {@link GopherGene} objects created from parsing the {@code refGene.txt.gz} file. */
    public int getTotalNumberOfRefGenes() { return this.n_totalGenes; }

    public int getNumberOfRefGenesChosenByUser() { return this.validGeneSymbols.size(); }

    /** Calculates the total number of distinct start points (transcript start points), which would correspond to the
     * number of viewpoints we will design for this gene. Intended for unit testing.
     * @return number of distinct transcription start sites over all VPVGenes.
     */
    public int getTotalTSScount() {
        return n_totalTSS;
    }

    /** Calculates the total number of distinct start points (transcript start points), which would correspond to the
     * number of viewpoints we will design for this gene. Intended for unit testing.
     * @return number of distinct transcription start sites over all VPVGenes.
     */
    public int getCountOfChosenTSS() {
        return n_chosenTSS;
    }

}
