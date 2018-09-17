package gopher.io;

import gopher.exception.GopherException;
import gopher.model.GopherGene;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * The intended use case for this file is to import a BED6 file that contains single positions of interest,
 * for instance, GWAS hits. We transform each line of the file into a GopherGene whose TSS
 * is identical with the start position of the BED file.
 */
public class BedFileParser {
    private static Logger logger = Logger.getLogger(BedFileParser.class);
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
    private Set<String> invalidGeneSymbols=null;
    /** The set of gene symbols that we could find in  the {@code refGene.txt.gz} file--and ergo,that we regard as being valid.
     * These are the genes chosen by the user. */
    private Set<String> validGeneSymbols=null;
    /** Total number of genes in the refGene.txt.gz file (number of unique gene symbols). */
    private int n_totalGenes;
    private int n_chosenGenes;
    private int n_totalTSS;
    private int n_chosenTSS;

    private final static int MINIMUM_NUMBER_OF_BED_FIELDS=6;

    public BedFileParser(String path) throws GopherException {
        geneSymbolMap =new HashMap<>();
        gene2chromosomePosMap =new HashMap<>();
        parse(path);
    }

    /** Parse */
    private void parse(String path) throws GopherException {
        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue; // skip empty lines that might be at the end of the file
                }
                String A[] = line.split("\t");
                if (A.length < MINIMUM_NUMBER_OF_BED_FIELDS) {
                    throw new GopherException(String.format("Malformed BED6 file line : %s (at least %d fields required but we got %d",
                            line,MINIMUM_NUMBER_OF_BED_FIELDS,A.length));
                }
                String chrom=A[0];
                int pos;
                try {
                    pos=Integer.parseInt(A[1])+1; // convert to one-based
                } catch(NumberFormatException n) {
                    throw new GopherException(String.format("Malformed BED6 line. Could not parse start pos (%s): %s",A[1],line));
                }
                String accession=A[3]; // something like rs123456 or custom name
                String strand=A[5];
                if (! strand.equals("+") && ! strand.equals("-")) {
                    throw new GopherException(String.format("Malformed BED6 line. Strand was %s. Line=%s",strand,line));
                }
                boolean isNoncoding=false; // needed for interface but not used
                GopherGene gene= new GopherGene(accession, accession, isNoncoding, chrom, strand);
                if (geneSymbolMap.containsKey(accession)) {
                    throw new GopherException(String.format("Error -- attempt to enter multiple BED6 lines with same name (%s)", accession));
                }
                geneSymbolMap.put(accession,gene);
                gene2chromosomePosMap.put(accession,gene);
                gene.addGenomicPosition(pos);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Iterate through all {@link GopherGene} objects and return a list of only those {@link GopherGene}s that
     * represent valid gene symbols uploaded by the user
     * @return List of VPVGenes representing valid uploaded gene symbols.
     */
    public List<GopherGene> getGopherGeneList() {
        List<GopherGene> genelist=new ArrayList<>();
        this.n_chosenTSS=0;
        for (GopherGene g: gene2chromosomePosMap.values()) {
            genelist.add(g);
            this.n_chosenTSS += g.n_viewpointstarts();
        }
        return genelist;
    }


}
