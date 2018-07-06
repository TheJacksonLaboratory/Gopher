package gopher.io;

import gopher.model.viewpoint.AlignabilityMap;
import gopher.model.viewpoint.Bait;
import gopher.model.viewpoint.Segment;
import gopher.model.viewpoint.ViewPoint;
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

    public void printProbeFileAgilentInFormat(List<ViewPoint> viewpointlist, String genomeBuild) throws FileNotFoundException {
        PrintStream out_probe_file = new PrintStream(new FileOutputStream(getFullPath(ProbeFileAgilentFormat)));
        out_probe_file.println("TargetID\tProbeID\tSequence\tReplication\tStrand\tCoordinates");

        // use a hashMap of Integer sets to get rid of duplicated probes
        HashMap<String,Set<Integer>> uniqueProbes = null;
/*
        for (ViewPoint vp : viewpointlist) {
            if (vp.getNumOfSelectedFrags() == 0) { continue; }
            for(Segment seg : vp.getActiveSegments()) {

                if(0<seg.getBaitNumTotal()) { continue; }

                for(Bait bait : seg.getBaitsForUpstreamMargin()) {
                    uniqueProbes.get(bait.getRefId()).add(bait.getStartPos());
                }
                for(Bait bait : seg.getBaitsForDownstreamMargin()) {
                    uniqueProbes.get(bait.getRefId()).add(bait.getStartPos());
                }
            }
        }

        // sort keys of hashMap lexicographically
        List sortedKeyList = new ArrayList(uniqueProbes.keySet());
        Collections.sort(sortedKeyList);

        for(int i=0; i<sortedKeyList.size(); i++) {
            List sortedStartPosList = new ArrayList(uniqueProbes.get(sortedKeyList.get(i)));
            Collections.sort(sortedKeyList);
            for(int j=0; j<sortedStartPosList.size(); j++) {
                logger.trace(sortedKeyList.get(i) + "\t" + sortedStartPosList);
            }
        }

*/



        for (ViewPoint vp : viewpointlist) {
            if (vp.getNumOfSelectedFrags() == 0) { continue; }
            String targetID = vp.getReferenceID();
            Date curDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("ddmmyy");
            String dateToStr = format.format(curDate);
            String probeID = "probe_";
            probeID += dateToStr;
            probeID += "_";
            probeID += genomeBuild;
            probeID += "_";
            probeID += vp.getReferenceID();
            probeID += "_";
            probeID += vp.getStartPos();
            out_probe_file.println(targetID+ "\t" + probeID);

        }

        out_probe_file.close();
    }


}
