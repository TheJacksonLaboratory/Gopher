package vpvgui.io.model;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.project.JannovarGeneGenerator;
import vpvgui.model.project.Segment;
import vpvgui.model.project.VPVGene;
import vpvgui.model.project.ViewPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by peterrobinson on 7/11/17.
 */
public class TestViewPointCreation {


    /* declare parameters for gene and TSS extraction */

    private static String transcriptFile;
    private static String geneFile;

    /* declare initial viewpoint patrameters */

    private static String[] cuttingPatterns;
    private static int maxDistToGenomicPosUp;
    private static int maxDistToGenomicPosDown;
    private static File fasta;
    private static IndexedFastaSequenceFile fastaReader;

    /* declare viewpoint parameters as requested by Darío */

    private static Integer fragNumUp;
    private static Integer fragNumDown;
    private static String cuttingMotif;
    private static Integer minSizeUp;
    private static Integer maxSizeUp;
    private static Integer minSizeDown;
    private static Integer maxSizeDown;
    private static Integer minFragSize;
    private static double maxRepFrag;
    private static Integer marginSize;

    /* declare other parameters */

    private static String outPath;
    private static String outPrefix;


    @BeforeClass
    public static void setup() throws Exception {

        /* set parameters for gene and TSS extraction */

        transcriptFile= "/home/peter/IdeaProjects/git_vpv_workspace/VPV/mm9_ucsc.ser"; // /home/peter/IdeaProjects/git_vpv_workspace/VPV/hg19_ucsc.ser
        geneFile="/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/CaptureC_gonad_gene_list_edit2_plus_only.txt"; // "/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/CaptureC_gonad_gene_list_edit2.txt"

        /* set initial viewpoint patrameters */

        cuttingPatterns = new String[]{"GATC"};
        maxDistToGenomicPosUp = 10000;
        maxDistToGenomicPosDown = 10000;
        fasta = new File("/home/peter/storage_1/agilent_project/data/genomes/mm9/Genome_Fasta_Files/mm9.fa");
        fastaReader = new IndexedFastaSequenceFile(fasta);

        /* set viewpoint parameters as requested by Darío */

        fragNumUp = 4;
        fragNumDown = 4;
        cuttingMotif = "GATC";
        minSizeUp = 1500;
        maxSizeUp = 5000;
        minSizeDown = 1500;
        maxSizeDown = 5000;
        minFragSize = 130;
        maxRepFrag = 0.9;
        marginSize = 250;

        /* set other parameters */

        outPath = "/home/peter/IdeaProjects/git_vpv_workspace/VPV/";
        outPrefix = "gonad";
    }


    /* Der folgende Code kann benutzt werden, um Darios
    Probes zu entwerfen und ist eigentlich kein Text, aber das
    JUNit Framework bietet die Moeglichkeit, Code so starten zu koennen
    ohne den restlichen Code zu veraendern.
    Du muesstest bitte die Pfade, die unten mit ??? markiert sind,
    entsprechend anpassen
     */
    @Test public void createProbesTest() throws Exception {

        /* fill list of gene symbols from file */

        ArrayList<String> symbols = new ArrayList<String>();
        Scanner s = new Scanner(new File(geneFile));
        while (s.hasNext()) {
            symbols.add(s.next());
        }
        s.close();

        /* get Jannovar annotation and create ViewPoint objects */

        Map<String,List<TranscriptModel>> validGenes2TranscriptsMap=null;
        List<VPVGene> vpvGeneList;

        if (transcriptFile==null) {
            ErrorWindow.display("Error retrieving Jannovar transcript file","Generate Jannovar transcript file before loading genes.");
            return;
        }

        JannovarGeneGenerator jgg = new JannovarGeneGenerator(transcriptFile);
        /* key is a gene symbol,and value is a listof corresponding transcripts. */
        validGenes2TranscriptsMap = jgg.checkGenes(symbols);
        List<String> validGeneSymbols = jgg.getValidGeneSymbols();
        List<String> invalidGeneSymbols= jgg.getInvalidGeneSymbols();
        int n_transcripts = getNTranscripts(validGenes2TranscriptsMap);


        vpvGeneList = new ArrayList<>();
        for (String symbol : validGenes2TranscriptsMap.keySet()) {
            List<TranscriptModel> transcriptList=validGenes2TranscriptsMap.get(symbol);
            TranscriptModel tm = transcriptList.get(0);
            String referenceSequenceID=getChromosomeStringMouse(tm.getChr());
            String id = tm.getGeneID();
            VPVGene vpvgene=new VPVGene(id,symbol);
            vpvgene.setChromosome(referenceSequenceID);
            if (tm.getStrand().isForward()) {
                vpvgene.setForwardStrand();
            } else {
                vpvgene.setReverseStrand();
            }
            for (TranscriptModel tmod: transcriptList) {
                GenomeInterval iv = tmod.getTXRegion();
                Integer genomicPos = null;
                if (tm.getStrand().isForward()) {
                    /* Add 1 to convert from zero-based to one-based numbering */
                    genomicPos = iv.getGenomeBeginPos().getPos()+1;
                } else {
                    /* The last position of a minus-strand gene is the start position,
                     * and the numbering is then one-based once it is flipped. Need to use
                     * the withStrand to convert positions of minus-strand genes.
                     */
                    genomicPos = iv.withStrand(Strand.FWD).getGenomeEndPos().getPos();
                }
                System.out.println(symbol);
                ViewPoint vp = new ViewPoint(referenceSequenceID,genomicPos,maxDistToGenomicPosUp,maxDistToGenomicPosDown,cuttingPatterns,fastaReader);
               /*
                public void generateViewpointLupianez(Integer fragNumUp, Integer fragNumDown, String motif, Integer minSizeUp,
                Integer maxSizeUp, Integer minSizeDown, Integer maxSizeDown, Integer minFragSize, double maxRepFrag, Integer marginSize) {
                @PeterHansen--ich habe unten "marginSize" ergänzt, es hatte gefehlt und bei mir nicht kompiliert!
                */
               Integer marginSize=42;
                vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif,  minSizeUp, maxSizeUp, minSizeDown, maxSizeDown,
                        minFragSize, maxRepFrag,marginSize);
                vpvgene.addViewPoint(vp);
            }
            vpvGeneList.add(vpvgene);
        }

        /* iterate over all genes and viewpoints within and print to BED format */

        printRestFragsToBed(outPath, outPrefix, vpvGeneList);


        /* calculate summary statistics */

        Integer numberOfResolvedViewpoints = 0;
        Integer numberOfUnresolvedViewpoints = 0;
        for (int i = 0; i < vpvGeneList.size(); i++) {
            for (int j = 0; j < vpvGeneList.get(i).getviewPointList().size(); j++) {
                if(vpvGeneList.get(i).getviewPointList().get(j).getResolved()) {
                    numberOfResolvedViewpoints++;
                } else {
                    numberOfUnresolvedViewpoints++;
                    String geneSymbol = vpvGeneList.get(i).getGeneSymbol();
                    String strand = new String();
                    if(vpvGeneList.get(i).isForward()) {
                        strand = "+";
                    } else {
                        strand = "-";
                    }
                    System.out.println(geneSymbol + "\t" + strand);
                }
            }
         }
         System.out.println("numberOfResolvedViewpoints: " + numberOfResolvedViewpoints);
         System.out.println("numberOfUnresolvedViewpoints: " + numberOfUnresolvedViewpoints);
    }


    private void printRestFragsToBed(String outPath, String outPrefix, List<VPVGene> vpvGeneList) throws FileNotFoundException {

        /* prepare output files for writing */

        String fileName = outPrefix + "_viewpoints.bed";
        PrintStream out_viewpoints = new PrintStream(new FileOutputStream(outPath+fileName));
        String description = new String();
        description = outPrefix + "_viewpoints";
        out_viewpoints.println("track name='" + description + "' description='" + description + "'");

        fileName = outPrefix + "_fragments.bed";
        PrintStream out_fragments = new PrintStream(new FileOutputStream(outPath+fileName));
        description = new String();
        description = outPrefix + "_fragments";
        out_fragments.println("track name='" + description + "' description='" + description + "'");

        fileName = outPrefix + "_fragment_margins.bed";
        PrintStream out_fragment_margins = new PrintStream(new FileOutputStream(outPath+fileName));
        description = new String();
        description = outPrefix + "_fragment_margins";
        out_fragment_margins.println("track name='" + description + "' description='" + description + "'");

        fileName = outPrefix + "_genomic_positions.bed";
        PrintStream out_genomic_positions = new PrintStream(new FileOutputStream(outPath+fileName));
        description = new String();
        description = outPrefix + "_genomic_positions";
        out_genomic_positions.println("track name='" + description + "' description='" + description + "'");


        for (int i = 0; i < vpvGeneList.size(); i++) {
            for (int j = 0; j < vpvGeneList.get(i).getviewPointList().size(); j++) {


                // print viewpoint
                String getReferenceSequenceID = vpvGeneList.get(i).getReferenceSequenceID();
                Integer vpStaPos = vpvGeneList.get(i).getviewPointList().get(j).getStartPos();
                Integer vpEndPos = vpvGeneList.get(i).getviewPointList().get(j).getEndPos();
                Integer vpGenomicPos = vpvGeneList.get(i).getviewPointList().get(j).getGenomicPos();
                String geneSymbol = vpvGeneList.get(i).getGeneSymbol();
                Integer viewPointScore=0;
                if(vpvGeneList.get(i).getviewPointList().get(j).getResolved()) {
                    viewPointScore=1;
                }
                viewPointScore=vpvGeneList.get(i).getviewPointList().get(j).getNumOfSelectedFrags();
                out_viewpoints.println(getReferenceSequenceID + "\t" + vpStaPos + "\t" + vpEndPos + "\t" + geneSymbol + "\t" + viewPointScore);

                out_genomic_positions.println(getReferenceSequenceID + "\t" + vpGenomicPos + "\t" + (vpGenomicPos+1) + "\t" + geneSymbol + "\t" + viewPointScore);

                // print selected fragments of the viewpoint
                ArrayList<Segment> selectedRestSegList = vpvGeneList.get(i).getviewPointList().get(j).getSelectedRestSegList(cuttingMotif);
                for (int k = 0; k < selectedRestSegList.size(); k++) {

                    Integer rsStaPos = selectedRestSegList.get(k).getStartPos();
                    Integer rsEndPos = selectedRestSegList.get(k).getEndPos();
                    out_fragments.println(getReferenceSequenceID + "\t" + rsStaPos + "\t" + rsEndPos + "\t" + geneSymbol + "_fragment_" + k);

                    // print margins of selected fragments
                    for(int l = 0; l<selectedRestSegList.get(k).getSegmentMargins(marginSize).size(); l++) {
                        Integer fmStaPos = selectedRestSegList.get(k).getSegmentMargins(marginSize).get(l).getStartPos();
                        Integer fmEndPos = selectedRestSegList.get(k).getSegmentMargins(marginSize).get(l).getEndPos();
                        out_fragment_margins.println(getReferenceSequenceID + "\t" + fmStaPos + "\t" + fmEndPos + "\t" + geneSymbol + "_fragment_" + k + "_margin");
                    }

                }
            }

        }
        out_viewpoints.close();
        out_genomic_positions.close();
        out_fragments.close();
        out_fragment_margins.close();
     }






    /** TODO -- stimmt nicht fuer Maus */
    private String getChromosomeString(int c) {
        if (c>0 && c<23) {
            return String.format("chr%d",c);
        } else if (c==23) {
            return "chrX";
        } else if (c==24) {
            return "chrY";
        } else if (c==25) {
            return "chrM";
        } else {
            return "???(Could not parse chromosome)";
        }
    }

    private String getChromosomeStringMouse(int c) {
        if (c>0 && c<20) {
            return String.format("chr%d",c);
        } else if (c==20) {
            return "chrX";
        } else if (c==21) {
            return "chrY";
        } else if (c==22) {
            return "chrM";
        } else {
            return "???(Could not parse chromosome)";
        }
    }


    private int getNTranscripts( Map<String,List<TranscriptModel>> mp) {
        int n=0;
        for (String s : mp.keySet()) {
            List<TranscriptModel> lst = mp.get(s);
            n += lst.size();
        }
        return n;
    }


}