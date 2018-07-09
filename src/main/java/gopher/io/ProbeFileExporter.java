package gopher.io;

import gopher.model.viewpoint.AlignabilityMap;
import gopher.model.viewpoint.Bait;
import gopher.model.viewpoint.Segment;
import gopher.model.viewpoint.ViewPoint;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

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
        private final String directoryPath; // Path to directory where the BED files will be stored. Has no path is guaranteed to have no trailing slash.

    /**
     *
     * @param dirpath The directory where we will write the probe files to
     * @param outPrefix The prefix (name) of the files.
     */
    public ProbeFileExporter(String dirpath, String outPrefix){
        // initialize the file names
        this.ProbeFileAgilentFormat =String.format("%s_agilentProbeFile.bed",outPrefix);
        /* remove trailing slash if necessary. */
        if (dirpath.endsWith(File.separator)) {
            dirpath=dirpath.substring(0,dirpath.length()-1);
        }
        this.directoryPath=dirpath;
    }

    private String getFullPath(String fname) {
        return String.format("%s%s%s",this.directoryPath,File.separator,fname);
    }

    public void printProbeFileAgilentInFormat(List<ViewPoint> viewpointlist, String genomeBuild, String IndexedFastaSequenceFilePath) throws FileNotFoundException {

        IndexedFastaSequenceFile fastaReader = new IndexedFastaSequenceFile(new File(IndexedFastaSequenceFilePath));

        PrintStream out_probe_file = new PrintStream(new FileOutputStream(getFullPath(ProbeFileAgilentFormat)));
        out_probe_file.println("TargetID\tProbeID\tSequence\tReplication\tStrand\tCoordinates");

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
                        ArrayList<Integer> staPosList = new ArrayList<Integer>();
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
                        ArrayList<Integer> staPosList = new ArrayList<Integer>();
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
        SimpleDateFormat format = new SimpleDateFormat("ddmmyy");
        String dateToStr = format.format(curDate);
        for(Object key : sortedKeyList) {
            String refID = key.toString();
            // convert ArrayList to set to get rid of duplicated baits
            Set<Integer> foo = new HashSet<Integer>(uniqueBaits.get(refID));
            // convert back to ArrayList for sorting
            ArrayList<Integer> sortedPositions = new ArrayList<Integer>(foo);

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
                ReferenceSequence sequence = fastaReader.getSubsequenceAt(refID, sortedPositions.get(i),sortedPositions.get(i)+120-1);
                out_probe_file.println(refID + "\t" + probeID + "\t" + sequence.getBaseString().toUpperCase() + "\t" + 1 + "\t" + "+" + "\t" + refID + ":" + (sortedPositions.get(i)) + "-" + (sortedPositions.get(i)+120-1));
            }
        }

        out_probe_file.close();

    }


}
