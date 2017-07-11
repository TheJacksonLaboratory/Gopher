package vpvgui.io.model;

import de.charite.compbio.jannovar.reference.GenomeInterval;
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

    
    @BeforeClass
    public static void setup() throws Exception {

        /* set parameters for gene and TSS extraction */

        transcriptFile= "/home/peter/IdeaProjects/git_vpv_workspace/VPV/hg19_ucsc.ser"; // /home/peter/IdeaProjects/git_vpv_workspace/VPV/hg19_ucsc.ser
        geneFile="/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/genelistsample.txt"; // "/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/CaptureC_gonad_gene_list_edit2.txt"

        /* set initial viewpoint patrameters */

        cuttingPatterns = new String[]{"GATC"};
        maxDistToGenomicPosUp = 10000;
        maxDistToGenomicPosDown=10000;
        fasta = new File("/home/peter/storage_1/agilent_project/data/genomes/hg19/ucsc.hg19.fasta");
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
            String referenceSequenceID=getChromosomeString(tm.getChr());
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
                    genomicPos = iv.getBeginPos();
                } else {
                    genomicPos = iv.getEndPos();
                }
                System.out.println(symbol);
                ViewPoint vp = new ViewPoint(referenceSequenceID,genomicPos,maxDistToGenomicPosUp,maxDistToGenomicPosDown,cuttingPatterns,fastaReader);
                vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif,  minSizeUp, maxSizeUp, minSizeDown, maxSizeDown, minFragSize, maxRepFrag);
                vpvgene.addViewPoint(vp);
            }
            vpvGeneList.add(vpvgene);
        }

        /* iterate over all genes and viewpoints within and print to BED format */

        for (int i = 0; i < vpvGeneList.size(); i++) {
            for (int j = 0; j < vpvGeneList.get(i).getviewPointList().size(); j++) {

                // viewpoint
                String getReferenceSequenceID = vpvGeneList.get(i).getReferenceSequenceID();
                Integer vpStaPos = vpvGeneList.get(i).getviewPointList().get(j).getStartPos();
                Integer vpEndPos = vpvGeneList.get(i).getviewPointList().get(j).getEndPos();
                String geneSymbol = vpvGeneList.get(i).getGeneSymbol();
                System.out.println(getReferenceSequenceID + "\t" + vpStaPos + "\t" + vpEndPos + "\t" + geneSymbol);

                // selected fragments of the viewpoint
                ArrayList<Segment> selectedRestSegList = vpvGeneList.get(i).getviewPointList().get(j).getSelectedRestSegList(cuttingMotif);
                for (int k = 0; k < selectedRestSegList.size(); k++) {
                    Integer rsStaPos = selectedRestSegList.get(k).getStartPos();
                    Integer rsEndPos = selectedRestSegList.get(k).getEndPos();
                    System.out.println(getReferenceSequenceID + "\t" + rsStaPos + "\t" + rsEndPos + "\t" + geneSymbol + "\t" + k);
                }

            }
        }
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
        if (c>0 && c<23) {
            return String.format("chr%d",c);
        } else if (c==10) {
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
