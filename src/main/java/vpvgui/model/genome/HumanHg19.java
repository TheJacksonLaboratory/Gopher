package vpvgui.model.genome;

import vpvgui.gui.ErrorWindow;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class HumanHg19 extends Genome implements Serializable {
    static final long serialVersionUID = 1L;

    private static final String [] chromosomes={"chr1","chr2","chr3","chr4","chr5","chr6","chr7","chr8","chr9",
    "chr10","chr11","chr12","chr13","chr14","chr15","chr16","chr17","chr18","chr19","chr20","chr21","chr22",
    "chrX","chrY"};


    public HumanHg19(String directoryPath) {
        super(directoryPath);
    }

    public HumanHg19() {

    }

    @Override
    public String getGenomeBuild() { return "hg19"; }

    /** @return true if the genome files have been previously downloaded to the indicated path. */
    @Override
    public boolean checkDownloadComplete(String path) {
        this.pathToGenomeDirectory=path;
        if (gZippedGenomeFileDownloaded()) {
            return true; // i.e., we found the chromFa.tar.gz file
        }
        logger.trace("Did not find chromFa.tar.gz. Will check for individual unpacked files.");
        // Now look for the unpacked files (the user may have deleted chromFa.tar.gz)
       return alreadyExtracted();
    }

    /**
     * We use this method to check if we need to g-unzip the genome files. (We only check for the
     * presence of chr1.fa--this will break if species without chr1 are analyzed).
     * @return true if the chromFGa.tar.gz file has been previously extracted
     */
    public  boolean alreadyExtracted() {
        List<String> missingChromosomes = new ArrayList<>();
        for (String chr : chromosomes) {
            File f = new File(this.pathToGenomeDirectory+ File.separator + chr);
            if(! f.exists() ) {
                missingChromosomes.add(f.getAbsolutePath());
            }
        }
        if (missingChromosomes.size()==0) {
            return true; // we found all expected chromosomes
        } else {
            String errmsg = missingChromosomes.stream().collect(Collectors.joining("; "));
            ErrorWindow.display("Could not extract all chromosomes",
                    String.format("Unable to extract %s ",errmsg));
            return false; // at least one chromosome was not extracted.
        }
    }

    public boolean isCanonical(String file){
        return false; //todo refactor
    }

}
