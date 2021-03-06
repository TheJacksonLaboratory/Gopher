package gopher.gui.viewpointpanel;


import gopher.model.RestrictionEnzyme;
import org.apache.log4j.Logger;
import gopher.model.Model;
import gopher.model.viewpoint.ViewPoint;

import java.util.stream.Collectors;

/**
 * This class makes URLs for displaying the viewpoints.
 * The URLs need to be different for each organism since each organism has a different
 * selection of data.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.2 (2018-02-19)
 */
class URLMaker {
    private static final Logger logger = Logger.getLogger(URLMaker.class.getName());
    private String genomebuild=null;
    /** This variable will be initialized to the number of pixels that we want the UCSC image to be. */
    private int xdim;
    /** A String such as DpnII or DpnII,HindIII that will be used to display all cuttings sites on UCSC. */
    private final String enzymeString;

    /** We will make the maximum width of the UCSC image 1600. If the user's screen is smaller, we will shrink the image. */
    private static final int UCSC_DEFAULT_WIDTH = 1600;
    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private static final int OFFSET = 200;


    URLMaker(Model model, int width) {
        this.genomebuild=model.getGenomeBuild();
        xdim=width;
        this.enzymeString = model.getChosenEnzymelist().stream().map(RestrictionEnzyme::getName).collect(Collectors.joining(","));
        logger.trace(String.format("setting genomebuild to %s with default image width of %d",genomebuild,xdim));
    }

    URLMaker(Model model){
        this(model, UCSC_DEFAULT_WIDTH);
    }


    String getImageURL(ViewPoint vp, double zoomfactor,String highlight) {
        final String trackType="hgRenderTracks";
        String url = getDefaultURL(vp,zoomfactor,trackType,highlight);
        switch (genomebuild) {
            case "hg19":

            url = String.format("%s&%s", url, getURLFragmentHg19());
            break;
            case "hg38":
                url = String.format("%s&%s",url,getURLFragmentHg38());
                break;
            case "mm9":
            url = String.format("%s&%s",url,getURLFragmentMm9());
            break;
            case "mm10":
            url = String.format("%s&%s",url,getURLFragmentMm10());
            break;
            case "xenTro9":
                url = String.format("%s&%s",url,getURLFragmentXenTro9());
                break;
            case "danRer10":
                url = String.format("%s&%s",url,getURLFragmentDanRer10());
                break;
            default:
                // should never happen
                logger.error("Unable to find URL for genome build "+genomebuild);
        }
        return url;
    }


    String getURL(ViewPoint vp,double zoomfactor,String highlights) {
        final String trackType="hgTracks";
        String url = getDefaultURL(vp,zoomfactor,trackType,highlights);
        if (this.genomebuild.equals("hg19")){
            url=String.format("%s&%s",url,getURLFragmentHg19());
        }  else if (this.genomebuild.equals("hg38")) {
            url = String.format("%s&%s",url,getURLFragmentHg38());
        }  else if (this.genomebuild.equals("mm9")) {
            url = String.format("%s&%s",url,getURLFragmentMm9());
        } else if (this.genomebuild.equals("mm10")) {
            url=String.format("%s&%s",url,getURLFragmentMm10());
        }  else if (this.genomebuild.equals("xenTro9")) {
            url = String.format("%s&%s",url,getURLFragmentXenTro9());
        } else if (this.genomebuild.equals("danRer10")) {
            url=String.format("%s&%s",url,getURLFragmentDanRer10());
        }
        logger.trace(String.format("URL for UCSC %s", url));
        return url;
    }

    /** These are the things to hide and show to get a nice hg19 image. */
    private String getURLFragmentHg19() {
        return "gc5Base=dense&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&ncbiRefSeqView=pack&g=wgEncodeCrgMapabilityAlign50mer&i=wgEncodeCrgMapabilityAlign50mer";
    }

    /** These are the things to hide and show to get a nice hg19 image. */
    private String getURLFragmentHg38() {
        return "gc5Base=dense&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&ncbiRefSeqView=pack&OmimAvSnp=hide";
    }

    /** These are the things to hide and show to get a nice mm9 image. */
    private String getURLFragmentMm9() {
        return "gc5Base=dense&knownGene=hide&ncbiRefSeqView=pack&stsMapMouseNew=hide&xenoRefGene=hide&ensGene=hide&pubs=hide&intronEst=hide&snp128=hide&oreganno=full&wgEncodeCrgMapabilityAlign50mer=full";
    }

    /** These are the things to hide and show to get a nice mm9 image. */
    private String getURLFragmentMm10() {
        return "gc5Base=dense&knownGene=hide&ncbiRefSeqView=pack&stsMapMouseNew=hide&xenoRefGene=hide&ensGene=hide&pubs=hide&intronEst=hide&snp142Common=hide&oreganno=full";
    }

    /** These are the things to hide and show to get a nice xenTro9 image. */
    private String getURLFragmentXenTro9() {
        return "gc5BaseBw=dense&rmsk=dense&cons11way=hide";
    }

    /** These are the things to hide and show to get a nice danRer10 image. */
    private String getURLFragmentDanRer10() {
        return "gc5BaseBw=dense&rmsk=dense&cons11way=hide";
    }


    /**
     * This is how: {@code hideTracks=1&<trackName>=full|dense|pack|hide}
     * @param vp The {@link ViewPoint} object to be displayed on the UCSC browser
     * @param trackType either "hgTracks" (interactive browser) or "hgRenderTracks" (static image)
     * @return A URL for the UCSC Genome browser
     */
    private String getDefaultURL(ViewPoint vp, double zoomfactor,String trackType,String highlights) {
        int posFrom, posTo;
        posFrom = vp.getMinimumDisplayPosition(zoomfactor) - OFFSET;
        posTo = vp.getMaximumDisplayPosition(zoomfactor) + OFFSET;
        logger.trace("posFrom="+posFrom+", posTo="+posTo);
        String chrom = vp.getReferenceID();
        if (!chrom.startsWith("chr"))
            chrom = "chr" + chrom; /* TODO MAKE THIS ROBUST! */
        String targetItem = vp.getTargetName();
        String url = String.format("http://genome.ucsc.edu/cgi-bin/%s?db=%s&position=%s%%3A%d-%d&hgFind.matches=%s&%s&pix=%d",
                trackType, genomebuild, chrom, posFrom, posTo, targetItem, highlights,xdim);
        if (enzymeString!=null && enzymeString.length()>0) {
            url=String.format("%s&cutters=dense&hgt.cutters=%s",url,enzymeString);
        }
        return url;
    }

}
