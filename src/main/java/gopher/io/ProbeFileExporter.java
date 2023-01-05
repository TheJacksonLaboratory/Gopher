package gopher.io;

import gopher.service.model.viewpoint.Bait;
import gopher.service.model.viewpoint.Segment;
import gopher.service.model.viewpoint.ViewPoint;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(gopher.io.BEDFileExporter.class.getName());

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
        this.ProbeFileBedFormat = String.format("%s_ProbeFile.bed",outPrefix);
        /* remove trailing slash if necessary. */
        if (dirpath.endsWith(File.separator)) {
            dirpath=dirpath.substring(0,dirpath.length()-1);
        }
        this.directoryPath=dirpath;
    }

    private String getFullPath(String fname) {
        return String.format("%s%s%s",this.directoryPath,File.separator,fname);
    }

    public void printProbeFileInAgilentFormat(Integer probe_length, List<ViewPoint> viewpointlist, String genomeBuild, String IndexedFastaSequenceFilePath) throws IOException {

        IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(IndexedFastaSequenceFilePath));

        PrintStream out_probe_file_bed = new PrintStream(new FileOutputStream(getFullPath(ProbeFileBedFormat)));

        FileOutputStream fos = new FileOutputStream(getFullPath(ProbeFileAgilentFormatZip));
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zipOutAgillent = new ZipOutputStream(bos);
        zipOutAgillent.putNextEntry(new ZipEntry(ProbeFileAgilentFormat));

        zipOutAgillent.write("TargetID\tProbeID\tSequence\tReplication\tStrand\tCoordinates\n".getBytes());
        // TODO: The following code is not nice but it works. It would be better to derive first the list of unique
        // TODO: segments and then export probes from this list.
        // TODO: BUT NOTE, THE PROBES IN THE EXPORTED PROBE FILE FOR AGILENT NEED TO BE SORTED BY
        // TODO: REFERENCE ID AND STARTING COORDINATE!

        // use a hashMap of Integer sets to get rid of duplicated probes
        HashMap<String, ArrayList<Integer>> uniqueBaits = new HashMap<>();
        HashMap<String, String> coordsTogeneNames = new HashMap<>();

        for (ViewPoint vp : viewpointlist) {
            LOGGER.error("ProbeFileExporter vp {}", vp);
            if (vp.getNumOfSelectedFrags() == 0) { continue; }
            for(Segment seg : vp.getActiveSegments()) {

                if(0 == seg.getBaitNumTotal()) { continue; }

                for(Bait b : seg.getBaitsForUpstreamMargin()) {
                    coordsTogeneNames.put(b.getContigStartPosKey(),vp.getTargetName());
                    if(!uniqueBaits.containsKey(b.getRefId())) {
                        ArrayList<Integer> staPosList = new ArrayList<>();
                        staPosList.add(b.getStartPos());
                        uniqueBaits.put(b.getRefId(),staPosList);
                    } else {
                        uniqueBaits.get(b.getRefId()).add(b.getStartPos());
                    }
                }
                for(Bait b : seg.getBaitsForDownstreamMargin()) {
                    coordsTogeneNames.put(b.getContigStartPosKey(), vp.getTargetName());
                    if(!uniqueBaits.containsKey(b.getRefId())) {
                        ArrayList<Integer> staPosList = new ArrayList<>();
                        staPosList.add(b.getStartPos());
                        uniqueBaits.put(b.getRefId(), staPosList);
                    } else {
                        uniqueBaits.get(b.getRefId()).add(b.getStartPos());
                    }
                }
            }
        }

        List<String> sortedKeyList = new ArrayList<>(uniqueBaits.keySet());
        Collections.sort(sortedKeyList);
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("ddMMyy");
        String dateToStr = format.format(curDate);
        for(Object key : sortedKeyList) {
            String refID = key.toString();
            // convert ArrayList to set to get rid of duplicated baits
            Set<Integer> foo = new HashSet<>(uniqueBaits.get(refID));
            // convert back to ArrayList for sorting
            ArrayList<Integer> sortedStartPositions = new ArrayList<>(foo);

            Collections.sort(sortedStartPositions);
            for (Integer baitStartPosition : sortedStartPositions) {
                LOGGER.error("sortedPosition {}", baitStartPosition);
                // build ProbeID
                String probeID = "probe_";
                probeID += dateToStr;
                probeID += "_";
                probeID += genomeBuild;
                probeID += "_";
                probeID += refID;
                probeID += "_";
                probeID += (baitStartPosition - 1);
                probeID += "_";
                String key3 = refID + ":" + baitStartPosition;
                probeID += coordsTogeneNames.get(key3);
                // get sequence
                // note that in HTSJDK, the start and stop positions in the following function
                // are both inclusive, 1-based start/stop of region.
                // the probeID, in contrast, is using the zero-based coordinates
                ReferenceSequence sequence = fastaReader.getSubsequenceAt(refID, baitStartPosition, baitStartPosition + probe_length - 1);
                String printToZip = refID + "\t" + probeID + "\t" + sequence.getBaseString().toUpperCase() + "\t" + 1 + "\t" + "+" + "\t" + refID + ":" + baitStartPosition + "-" + (baitStartPosition + 120 - 1) + "\n";
                zipOutAgillent.write(printToZip.getBytes());
                out_probe_file_bed.println(refID + "\t" + (baitStartPosition - 1) + "\t" + (baitStartPosition + probe_length - 2) + "\t" + probeID); // start and end 0-based
            }
        }
        zipOutAgillent.closeEntry();
        zipOutAgillent.close();
    }
}