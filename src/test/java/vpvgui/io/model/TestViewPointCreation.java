package vpvgui.io.model;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import org.junit.BeforeClass;
import org.junit.Test;
import vpvgui.gui.ErrorWindow;
import vpvgui.model.project.*;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

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

        transcriptFile= "/Users/hansep/IdeaProjects/VPV/mm9_ucsc.ser"; // /home/peter/IdeaProjects/git_vpv_workspace/VPV/hg19_ucsc.ser
        geneFile="/Users/hansep/IdeaProjects/VPV/src/test/resources/CaptureC_gonad_gene_list_edit2.txt"; // "/home/peter/IdeaProjects/git_vpv_workspace/VPV/src/test/resources/CaptureC_gonad_gene_list_edit2.txt"

        /* set initial viewpoint patrameters */

        cuttingPatterns = new String[]{"GATC"};
        maxDistToGenomicPosUp = 10000;
        maxDistToGenomicPosDown = 10000;
        fasta = new File("/Users/hansep/IdeaProjects/VPV/mm9_fasta/mm9.fa");
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
        maxRepFrag = 0.6;
        marginSize = 250;

        /* set other parameters */

        outPath = "/Users/hansep/IdeaProjects/VPV/";
        outPrefix = "gonad_RC06";
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
        System.out.println("Number of entered gene symbols:" + symbols.size());


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


        /* print invalid gene symbols to file */

        String fileName = outPrefix + "_invalid_symbols.txt";
        PrintStream out_invalid_symbols = new PrintStream(new FileOutputStream(outPath+fileName));

        for(int i = 0; i < invalidGeneSymbols.size(); i++) {
            out_invalid_symbols.println(invalidGeneSymbols.get(i));
        }
        out_invalid_symbols.close();
        System.out.println("Number of invalid gene symbols: " + invalidGeneSymbols.size());
        System.out.println("Number of valid gene symbols: " + validGeneSymbols.size());
        System.out.println("validGenes2TranscriptsMap.size(): " + validGenes2TranscriptsMap.size());


        /* read refSeq.txt to map of TSS lists */

        HashMap<String,String> geneMapOfTss = new HashMap<String,String>();
        HashMap<String,String> geneMapOfTssHelp = new HashMap<String,String>();
        Integer countSymbolsWithTssOnDifferentChromosomes = 0;
        File file = new File("/Users/hansep/IdeaProjects/VPV/mm9_fasta/refGene.txt");
        Scanner refGeneFile = new Scanner(file);
        while (refGeneFile.hasNextLine()) {
            String[] line = refGeneFile.nextLine().split("\t");
            String chr = line[2];
            String strand = line[3];
            Integer tssPos;
            if(strand.equals("+")) {
                tssPos = Integer.parseInt(line[4]);
            }
            else {
                tssPos = Integer.parseInt(line[5])-1;
            }
            String geneSymbol= line[12];
            if(chr.contains("_") && (geneSymbol.equals("Tsnax") || geneSymbol.equals("Uba1y") || geneSymbol.equals("Gm1993"))) {continue;} // skip random chromosomes
            if(geneMapOfTssHelp.containsKey(geneSymbol)) {
                String prevGeneChr = geneMapOfTssHelp.get(geneSymbol);
                if(!prevGeneChr.equals(chr)) {
                    // check if affected symbol is in our list
                    if(symbols.contains(geneSymbol)) {
                        //System.out.println(prevGeneChr);
                        //System.out.println(chr + "\t" + tssPos.toString() + "\t" + strand + "\t" + geneSymbol);
                    }
                    countSymbolsWithTssOnDifferentChromosomes++;

                }
            }
            if(chr.contains("random") && symbols.contains(geneSymbol)) {
                System.out.println(geneSymbol + "\t" + chr);
            }
            String key = chr + "\t" + tssPos.toString() + "\t" + strand + "\t" + geneSymbol;
            geneMapOfTss.put(key,key);
            geneMapOfTssHelp.put(geneSymbol,chr);
        }
        System.out.println(countSymbolsWithTssOnDifferentChromosomes);
        System.out.println("*******");

         /* create a proper HashMap with gene symbols as keys and lists of TSS as values */

        HashMap<String,ArrayList<TranscriptionStartSite>> geneMapOfTssLists = new HashMap<String,ArrayList<TranscriptionStartSite>>();
        for (String key : geneMapOfTss.keySet()) {
            String[] line =  key.split("\t");
            String chr = line[0];
            Integer pos = Integer.parseInt(line[1]);
            String strand = line[2];
            String geneSymbol = line[3];
            TranscriptionStartSite newTSS = new TranscriptionStartSite(chr,pos,strand);
            if(geneMapOfTssLists.isEmpty() || !geneMapOfTssLists.containsKey(geneSymbol)){
                ArrayList<TranscriptionStartSite> newList = new ArrayList<TranscriptionStartSite>();
                newList.add(newTSS);
                geneMapOfTssLists.put(geneSymbol,newList);
            } else {
                geneMapOfTssLists.get(geneSymbol).add(newTSS);
            }
        }

        System.out.println("*******");

        /* check how many of the entered symbols can be found in refGene.txt */

        System.out.println("totalNumberOfEnteredSymbols: " + symbols.size());

        Integer numberOfFoundSymbols = 0;
        Integer numberOfSymbolsNotFound = 0;
        for(int i=0;i<symbols.size();i++) {
            if(geneMapOfTssLists.containsKey(symbols.get(i))) {
                numberOfFoundSymbols++;
            }
            else {
                numberOfSymbolsNotFound++;
                System.out.println(symbols.get(i));
            }
        }
        System.out.println("numberOfFoundSymbols: " + numberOfFoundSymbols);
        System.out.println("numberOfSymbolsNotFound: " + numberOfSymbolsNotFound);

        System.out.println("*******");

        /* apply our viewpoint derivation method to all TSS */

        vpvGeneList = new ArrayList<>();
        for (String symbol : symbols) {
            if(!geneMapOfTssLists.containsKey(symbol)) {continue;}
            VPVGene vpvgene=new VPVGene(symbol,symbol);
            for (int i=0;i<geneMapOfTssLists.get(symbol).size();i++) {
                String referenceSequenceID = geneMapOfTssLists.get(symbol).get(i).getReferenceSequenceID();
                Integer gPos = geneMapOfTssLists.get(symbol).get(i).getPos();
                ViewPoint vp = new ViewPoint(referenceSequenceID,gPos,maxDistToGenomicPosUp,maxDistToGenomicPosDown,cuttingPatterns,fastaReader);
                vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif,  minSizeUp, maxSizeUp, minSizeDown, maxSizeDown,
                        minFragSize, maxRepFrag,marginSize);
                vpvgene.addViewPoint(vp);
                vpvgene.setChromosome(referenceSequenceID);
             }
            vpvGeneList.add(vpvgene);
        }








        /* apply our viewpoint derivation method to all TSS */
/*
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
                    /* Add 1 to convert from zero-based to one-based numbering
                    genomicPos = iv.getGenomeBeginPos().getPos()+1;
                } else {
                    /* The last position of a minus-strand gene is the start position,
                     * and the numbering is then one-based once it is flipped. Need to use
                     * the withStrand to convert positions of minus-strand genes.

                    genomicPos = iv.withStrand(Strand.FWD).getGenomeEndPos().getPos();
                }
                ViewPoint vp = new ViewPoint(referenceSequenceID,genomicPos,maxDistToGenomicPosUp,maxDistToGenomicPosDown,cuttingPatterns,fastaReader);
                vp.generateViewpointLupianez(fragNumUp, fragNumDown, cuttingMotif,  minSizeUp, maxSizeUp, minSizeDown, maxSizeDown,
                        minFragSize, maxRepFrag,marginSize);
                vpvgene.addViewPoint(vp);
            }
            vpvGeneList.add(vpvgene);
        }
        */

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

        fileName = outPrefix + "_fragment_margins_unique.txt";
        PrintStream out_fragment_margins_unique = new PrintStream(new FileOutputStream(outPath+fileName));
        description = new String();
        description = outPrefix + "_fragment_margins_unique";
        out_fragment_margins_unique.println("track name='" + description + "' description='" + description + "'");

        Set<String> uniqueFragmentMargins = new HashSet<String>();

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
                        uniqueFragmentMargins.add(getReferenceSequenceID + "\t" + fmStaPos + "\t" + fmEndPos + "\t" + geneSymbol);
                    }

                }
            }

        }
        out_viewpoints.close();
        out_genomic_positions.close();
        out_fragments.close();
        out_fragment_margins.close();

        /* print out unique set of margins for enrichment (and calculate the total length of all  margins) */
        Integer totalLengthOfMargins=0;
        for (String s : uniqueFragmentMargins) {
            out_fragment_margins_unique.println(s);
            String[] parts = s.split("\t");
            Integer sta = Integer.parseInt(parts[1]);
            Integer end = Integer.parseInt(parts[2]);
            Integer len = end - sta;
            totalLengthOfMargins = totalLengthOfMargins + len;
        }
        out_fragment_margins_unique.close();

        System.out.println("totalLengthOfMargins: " + totalLengthOfMargins);
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
