package vpvgui.io;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    static Logger logger = Logger.getLogger(GeneRegGTFParser.class.getName());
    private String pathToGTFfile=null;
    private List<Element> regulatoryElements;



    public GeneRegGTFParser(String path) {
        pathToGTFfile=path;
        regulatoryElements=new ArrayList<>();
    }






    public void parse() {
        int n_lines=0;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(pathToGTFfile));
            String line=null;
            while ((line=reader.readLine())!=null) {
               // System.out.println(line);
                String A[]=line.split("\t");
                String chrom=A[0];
                if (! A[1].equals("Regulatory_Build")){
                    System.exit(1);
                }
                if (! A[2].equals("regulatory_region")) {
                    logger.fatal(String.format("Unexpected element type %s",A[2]));
                }
                Integer from,to;
                try {
                    from=Integer.parseInt(A[3]);
                    to=Integer.parseInt(A[4]);
                    String annot = A[8];
                    parseAnnot(chrom,from,to,annot);
                    n_lines++;
                } catch (NumberFormatException ne) {
                    ne.printStackTrace();
                }

            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        logger.trace("parsed " +n_lines + " regulatory elements");
        logger.trace(String.format("%d are near target",regulatoryElements.size()));
        for (Element e:regulatoryElements) {
            System.out.println(e);
        }
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
     * @param chrom
     * @param from
     * @param to
     * @param annot
     */
    private void parseAnnot(String chrom,int from, int to, String annot) {
        System.out.println(annot);
        String B[]=annot.split(";");
        String id=null;
                String feature_type=null;
        for (String b:B) {
            if (b.startsWith("ID=")) {
                id=b.substring(3);
            } else if (b.startsWith("feature_type=")) {
                feature_type=b.substring(13);
            }
        }
        Element elem = new Element(chrom,from,to,id,feature_type);
        addIfRegulatoryElementNearToTarget(elem,100000);
    }



    public void addIfRegulatoryElementNearToTarget(Element elem, int threshold) {
        String chrom="17";
        int pos=7_687_538; // TP53 for GRCh38
        if (elem.chrom.equals(chrom) && Math.abs(elem.from-pos)<threshold) {
            regulatoryElements.add(elem);
        }
    }


    /** These are the categories of regulatory elements in the Ensembl GTF. */
    enum RegulationCategory {ENHANCER, OPEN_CHROMATIN,PROMOTER_FLANKING_REGION, CTCF_BINDING_SITE, PROMOTER, TF_BINDING_SITE};

    static class Element {

        public RegulationCategory category;
        public String chrom;
        public int from;
        public int to;
        public String id;

        public Element(String chrom,int f, int t, String identifier,String cat) {
            if (cat.equals("Enhancer")) {
                this.category=RegulationCategory.ENHANCER;
            } else if (cat.equalsIgnoreCase("Open chromatin")) {
                this.category=RegulationCategory.OPEN_CHROMATIN;
            } else if (cat.equalsIgnoreCase("Promoter Flanking Region")) {
                this.category=RegulationCategory.PROMOTER_FLANKING_REGION;
            } else if (cat.equalsIgnoreCase("CTCF Binding Site")) {
                this.category=RegulationCategory.CTCF_BINDING_SITE;
            }else if (cat.equalsIgnoreCase("Promoter")) {
                this.category=RegulationCategory.PROMOTER;
            }else if (cat.equalsIgnoreCase("TF binding site")) {
                this.category=RegulationCategory.TF_BINDING_SITE;
            }  else{
                logger.fatal("DID NOT RECOGNIZE CATEGORY "+cat);
                System.exit(1);
            }
            this.chrom=chrom;
            this.to=t;
            this.from=f;
            this.id=identifier;


        }

    }



}
