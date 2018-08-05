package gopher.io;

import gopher.model.viewpoint.Bait;
import gopher.model.viewpoint.Segment;
import gopher.model.viewpoint.ViewPoint;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class exports probe files that can be used for ordering of probes.
 *
 * @author Peter Hansen, Peter Robinson
 * @version 0.0.1 (2018-07-06)
 *
 */
public class ProbeFileExporter {
    private static final Logger logger = Logger.getLogger(gopher.io.BEDFileExporter.class.getName());

        private final String ProbeFileAgilentFormat;
    private final String ProbeFileAgilentFormatZip;
        private final String ProbeFileBedFormat;
        private final String directoryPath; // Path to directory where the BED files will be stored. Has no path is guaranteed to have no trailing slash.

    /**
     *
     * @param dirpath The directory where we will write the probe files to
     * @param outPrefix The prefix (name) of the files.
     */
    public ProbeFileExporter(String dirpath, String outPrefix){
        // initialize the file names
        this.ProbeFileAgilentFormat = String.format("%s_agilentProbeFile.txt",outPrefix);
        this.ProbeFileAgilentFormatZip = String.format("%s_agilentProbeFile.txt.zip",outPrefix);
        this.ProbeFileBedFormat = String.format("%s_BedProbeFile.bed",outPrefix);
        /* remove trailing slash if necessary. */
        if (dirpath.endsWith(File.separator)) {
            dirpath=dirpath.substring(0,dirpath.length()-1);
        }
        this.directoryPath=dirpath;
    }

    private String getFullPath(String fname) {
        return String.format("%s%s%s",this.directoryPath,File.separator,fname);
    }

    public void printProbeFileInAgilentFormat(List<ViewPoint> viewpointlist, String genomeBuild, String IndexedFastaSequenceFilePath) throws IOException {

        Integer probe_length = 120;

        IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(IndexedFastaSequenceFilePath));

        PrintStream out_probe_file_bed = new PrintStream(new FileOutputStream(getFullPath(ProbeFileBedFormat)));

        //PrintStream out_probe_file_agilent = new PrintStream(new FileOutputStream(getFullPath(ProbeFileAgilentFormat)));


        FileOutputStream fos = new FileOutputStream(getFullPath(ProbeFileAgilentFormatZip));
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zipOutAgillent = new ZipOutputStream(bos);
        zipOutAgillent.putNextEntry(new ZipEntry(ProbeFileAgilentFormat));

        zipOutAgillent.write(String.format("TargetID\tProbeID\tSequence\tReplication\tStrand\tCoordinates\n").getBytes());



        //out_probe_file_agilent.println("TargetID\tProbeID\tSequence\tReplication\tStrand\tCoordinates");

        // TODO: The following code is not nice but it works. It would be better to derive first the list of unique
        // TODO: segments and then export probes from this list.
        // TODO: BUT NOTE, THE PROBES IN THE EXPORTED PROBE FILE FOR AGILENT NEED TO BE SORTED BY
        // TODO: REFERENCE ID AND STARTING COORDINATE!

        // use a hashMap of Integer sets to get rid of duplicated probes
        Set<String> uniqueProbes = new HashSet<>();
        HashMap<String, ArrayList<Integer>> uniqueBaits = new HashMap<String, ArrayList<Integer>>();

        for (ViewPoint vp : viewpointlist) {
            if (vp.getNumOfSelectedFrags() == 0) { continue; }
            for(Segment seg : vp.getActiveSegments()) {

                if(0 == seg.getBaitNumTotal()) { continue; }

                for(Bait b : seg.getBaitsForUpstreamMargin()) {
                    String key = b.getRefId() + ":" + b.getStartPos() + "-" + b.getEndPos(); // build key
                    uniqueProbes.add(key);
                    if(!uniqueBaits.containsKey(b.getRefId())) {
                        ArrayList<Integer> staPosList = new ArrayList<>();
                        staPosList.add(b.getStartPos());
                        uniqueBaits.put(b.getRefId(),staPosList);
                    } else {
                        uniqueBaits.get(b.getRefId()).add(b.getStartPos());
                    }
                }
                for(Bait b : seg.getBaitsForDownstreamMargin()) {
                    String key = b.getRefId() + ":" + b.getStartPos() + "-" + b.getEndPos(); // build key
                    uniqueProbes.add(key);
                    if(!uniqueBaits.containsKey(b.getRefId())) {
                        ArrayList<Integer> staPosList = new ArrayList<>();
                        staPosList.add(b.getStartPos());
                        uniqueBaits.put(b.getRefId(),staPosList);
                    } else {
                        uniqueBaits.get(b.getRefId()).add(b.getStartPos());
                    }
                }
            }
        }

        List sortedKeyList = new ArrayList(uniqueBaits.keySet());
        Collections.sort(sortedKeyList);
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("ddMMyy");
        String dateToStr = format.format(curDate);
        for(Object key : sortedKeyList) {
            String refID = key.toString();
            // convert ArrayList to set to get rid of duplicated baits
            Set<Integer> foo = new HashSet<>(uniqueBaits.get(refID));
            // convert back to ArrayList for sorting
            ArrayList<Integer> sortedPositions = new ArrayList<>(foo);

            Collections.sort(sortedPositions);
            for(int i = 0; i < sortedPositions.size(); i++) {
                // build ProbeID
                String probeID = "probe_";
                probeID += dateToStr;
                probeID += "_";
                probeID += genomeBuild;
                probeID += "_";
                probeID += refID;
                probeID += "_";
                probeID += (sortedPositions.get(i)-1);
                // get sequence
                ReferenceSequence sequence = fastaReader.getSubsequenceAt(refID, sortedPositions.get(i),sortedPositions.get(i)+probe_length-1);
                String printToZip=String.format(refID + "\t" + probeID + "\t" + sequence.getBaseString().toUpperCase() + "\t" + 1 + "\t" + "+" + "\t" + refID + ":" + (sortedPositions.get(i)) + "-" + (sortedPositions.get(i)+120-1) + "\n");
                zipOutAgillent.write(printToZip.getBytes());
                out_probe_file_bed.println(refID + "\t" + (sortedPositions.get(i)-1) + "\t" + (sortedPositions.get(i)+probe_length-2) + "\t" + probeID); // start and end 0-based

            }
        }
        zipOutAgillent.closeEntry();
        zipOutAgillent.close();
    }
}