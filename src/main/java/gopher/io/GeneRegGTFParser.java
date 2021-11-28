package gopher.io;

import gopher.service.model.regulatoryexome.RegulatoryElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;

/** This class is responsible for parsing the Ensembl regulatory build GTF file. This can be used to
 * create a separate "exome" like panel that contains not only coding sequences but also the regulatory
 * sequences that are located near to the genes of interest.
 * Here is a summary of GTF format from Ensembl
 * <ol>
 *     <li>seqname - name of the chromosome or scaffold; chromosome names can be given with or without the 'chr' prefix. Important note: the seqname must be one used within Ensembl, i.e. a standard chromosome name or an Ensembl identifier such as a scaffold ID, without any additional content such as species or assembly. See the example GFF output below.
 * <li>source: name of the program that generated this feature, or the data source (database or project name)</li>
 *  <li>feature: feature type name, e.g. Gene, Variation</li>
 *  <li>start: Start position of the feature, with sequence numbering starting at 1.</li>
 *  <li>end: End position of the feature, with sequence numbering starting at 1.</li>
 *  <li>score: A floating point value.</li>
 *  <li>strand: defined as + (forward) or - (reverse).</li>
 *  <li>frame: One of '0', '1' or '2'. '0' indicates that the first base of the feature is the first base of a codon, '1' that the second base is the first base of a codon, and so on.</li>
 *  <li>attribute: A semicolon-separated list of tag-value pairs, providing additional information about each feature.</li>
 * </ol>
 * @author Peter Robinson
 * @version 0.0.1
 */
public class GeneRegGTFParser {
    static Logger logger = LoggerFactory.getLogger(GeneRegGTFParser.class.getName());
    private String pathToGTFfile=null;


    private BufferedReader reader=null;
    private String currentLine=null;



    public GeneRegGTFParser(String path) {
        pathToGTFfile=path;
    }

    public void close() throws IOException {
        reader.close();
    }




    public void initGzipReader() throws IOException {
        String encoding="UTF-8";
        if (this.pathToGTFfile==null) {
            throw new IOException("Regulatory build GTF file not initialized");
        }
        InputStream fileStream = new FileInputStream(this.pathToGTFfile);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, encoding);
        this.reader = new BufferedReader(decoder);
        this.currentLine=reader.readLine();
    }

    public boolean hasNext() {
        return this.currentLine !=null;
    }

    public RegulatoryElement next() throws IOException {
        String[] A =this.currentLine.split("\t");
        this.currentLine=this.reader.readLine(); // advance iterator
        RegulatoryElement elem=null;
        String chrom=A[0];
        if (! A[1].equals("Regulatory_Build")){
            System.exit(1);
        }
        if (! A[2].equals("regulatory_region")) {
            logger.error(String.format("Unexpected element type %s",A[2]));
        }
        int from,to;
        try {
            from=Integer.parseInt(A[3]);
            to=Integer.parseInt(A[4]);
            String annot = A[8];
             elem = parseAnnot(chrom,from,to,annot);
        } catch (NumberFormatException ne) {
            ne.printStackTrace();
        }
        return elem;
    }



    /**
     * Parse the annotation line of the Ensembl regulatory build.
     * Here are some example lines
     * <pre>
     * ID=ENSR00000216363;bound_end=105767127;bound_start=105766282;description=Open chromatin region;feature_type=Open chromatin
     ID=ENSR00000223835;bound_end=40109800;bound_start=40109601;description=Predicted enhancer region;feature_type=Enhancer
     ID=ENSR00000283551;bound_end=45929000;bound_start=45927401;description=CTCF binding site;feature_type=CTCF Binding Site
     ID=ENSR00000097936;bound_end=72404800;bound_start=72404601;description=Predicted enhancer region;feature_type=Enhancer
     </pre>
     * @param chrom chromosome
     * @param from start position
     * @param to end position
     * @param annot annotation
     */
    private RegulatoryElement parseAnnot(String chrom,int from, int to, String annot) {
        String[] B =annot.split(";");
        String id=null;
                String feature_type=null;
        for (String b:B) {
            if (b.startsWith("ID=")) {
                id=b.substring(3);
            } else if (b.startsWith("feature_type=")) {
                feature_type=b.substring(13);
            }
        }
        return new RegulatoryElement(chrom,from,to,id,feature_type);
    }



}
