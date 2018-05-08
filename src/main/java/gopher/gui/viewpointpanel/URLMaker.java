package gopher.gui.viewpointpanel;


import org.apache.log4j.Logger;
import gopher.model.Model;
import gopher.model.viewpoint.ViewPoint;

/**
 * This class makes URLs for displaying the viewpoints.
 * The URLs need to be different for each organism since each organism has a different
 * selection of data.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.2 (2018-02-19)
 */
public class URLMaker {
    private static final Logger logger = Logger.getLogger(URLMaker.class.getName());
    private String genomebuild=null;
    /** This variable will be initialized to the number of pixels that we want the UCSC image to be. */
    private int xdim;

    private final String cuttingSite;

    /** We will make the maximum width of the UCSC image 1600. If the user's screen is smaller, we will shrink the image. */
    private static final int UCSC_DEFAULT_WIDTH = 1600;
    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private static final int OFFSET = 200;




    URLMaker(Model model){
        this.genomebuild=model.getGenomeBuild();
        xdim=Math.min(UCSC_DEFAULT_WIDTH,model.getXdim());
        cuttingSite=model.getFirstRestrictionEnzymeString();
        logger.trace(String.format("setting genomebuild to %s with default image width of %d",genomebuild,xdim));
    }


    String getImageURL(ViewPoint vp, String highlight) {
        final String trackType="hgRenderTracks";
        String url = getDefaultURL(vp,trackType,highlight);
        if (this.genomebuild.equals("hg19")) {
            url = String.format("%s&%s", url, getURLFragmentHg19());
        } else if (this.genomebuild.equals("hg38")) {
                url = String.format("%s&%s",url,getURLFragmentHg38());
        } else if (this.genomebuild.equals("mm9")) {
            url = String.format("%s&%s",url,getURLFragmentMm9());
        } else if (this.genomebuild.equals("mm10")) {
            url = String.format("%s&%s",url,getURLFragmentMm10());
        }
        return url;
    }


    String getURL(ViewPoint vp,String highlights) {
        final String trackType="hgTracks";
        String url = getDefaultURL(vp,trackType,highlights);
        if (this.genomebuild.equals("hg19")){
            url=String.format("%s&%s",url,getURLFragmentHg19());
        }  else if (this.genomebuild.equals("hg38")) {
            url = String.format("%s&%s",url,getURLFragmentHg38());
        }  else if (this.genomebuild.equals("mm9")) {
            url = String.format("%s&%s",url,getURLFragmentMm9());
        } else if (this.genomebuild.equals("mm10")) {
            url=String.format("%s&%s",url,getURLFragmentMm10());
        }
        logger.trace(String.format("URL for UCSC %s", url));
        return url;
    }
    /** These are the things to hide and show to get a nice hg19 image. */
    private String getURLFragmentHg19() {
        return "gc5Base=full&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&refGene=full";
    }

    /** These are the things to hide and show to get a nice hg19 image. */
    private String getURLFragmentHg38() {
        return "gc5Base=full&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&refGene=full&OmimAvSnp=hide";
    }
    /** These are the things to hide and show to get a nice mm9 image. */
    private String getURLFragmentMm9() {
        return "gc5Base=full&knownGene=hide&refGene=full&stsMapMouseNew=hide&xenoRefGene=hide&ensGene=hide&pubs=hide&intronEst=hide&snp128=hide&oreganno=full";
    }

    /** These are the things to hide and show to get a nice mm9 image. */
    private String getURLFragmentMm10() {
        return "gc5Base=full&knownGene=hide&refGene=full&stsMapMouseNew=hide&xenoRefGene=hide&ensGene=hide&pubs=hide&intronEst=hide&snp142Common=hide&oreganno=full";
    }


    /**
     * This is how: {@code hideTracks=1&<trackName>=full|dense|pack|hide}
     * @param vp The {@link ViewPoint} object to be displayed on the UCSC browser
     * @param trackType either "hgTracks" (interactive browser) or "hgRenderTracks" (static image)
     * @return A URL for the UCSC Genome browser
     */
    private String getDefaultURL(ViewPoint vp, String trackType,String highlights) {
        int posFrom, posTo;
        posFrom = vp.getMinimumDisplayPosition() - OFFSET;
        posTo = vp.getMaximumDisplayPosition() + OFFSET;
        String chrom = vp.getReferenceID();
        if (!chrom.startsWith("chr"))
            chrom = "chr" + chrom; /* TODO MAKE THIS ROBUST! */
        String targetItem = vp.getTargetName();
        String url = String.format("http://genome.ucsc.edu/cgi-bin/%s?db=%s&position=%s%%3A%d-%d&hgFind.matches=%s&%s&pix=%d",
                trackType, genomebuild, chrom, posFrom, posTo, targetItem, highlights,xdim);
        if (cuttingSite!=null && cuttingSite.length()>0) {
            url=String.format("%s&oligoMatch=pack&hgt.oligoMatch=%s",url,cuttingSite);
        }
        return url;
    }

}