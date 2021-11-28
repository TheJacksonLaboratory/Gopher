package gopher.service;


import gopher.service.model.RestrictionEnzyme;
import gopher.service.model.GopherModel;
import gopher.service.model.viewpoint.ViewPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * This class makes URLs for displaying the viewpoints.
 * The URLs need to be different for each organism since each organism has a different
 * selection of data.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.1.2 (2018-02-19)
 */
public class URLMaker {
    private static final Logger logger = LoggerFactory.getLogger(URLMaker.class);
    private final String genomebuild;
    /** This variable will be initialized to the number of pixels that we want the UCSC image to be. */
    private final int xdim;
    /** A String such as DpnII or DpnII,HindIII that will be used to display all cuttings sites on UCSC. */
    private final String enzymeString;
    /** We will make the maximum width of the UCSC image 1600. If the user's screen is smaller, we will shrink the image. */
    private static final int UCSC_DEFAULT_WIDTH = 1600;
    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private static final int OFFSET = 200;


    public URLMaker(GopherService model, int width) {
        this.genomebuild=model.getGenomeBuild();
        xdim=width;
        this.enzymeString = model.getChosenEnzymelist().stream().map(RestrictionEnzyme::getName).collect(Collectors.joining(","));
        logger.trace(String.format("setting genomebuild to %s with default image width of %d",genomebuild,xdim));
    }

    public URLMaker(GopherService model){
        this(model, UCSC_DEFAULT_WIDTH);
    }


    public String getImageURL(ViewPoint vp, double zoomfactor, String highlight) {
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


    public String getURL(ViewPoint vp, double zoomfactor, String highlights) {
        final String trackType="hgTracks";
        String url = getDefaultURL(vp, zoomfactor, trackType, highlights);
        url = switch (this.genomebuild) {
            case "hg19" -> String.format("%s&%s", url, getURLFragmentHg19());
            case "hg38" -> String.format("%s&%s", url, getURLFragmentHg38());
            case "mm9" -> String.format("%s&%s", url, getURLFragmentMm9());
            case "mm10" -> String.format("%s&%s", url, getURLFragmentMm10());
            case "xenTro9" -> String.format("%s&%s", url, getURLFragmentXenTro9());
            case "danRer10" -> String.format("%s&%s", url, getURLFragmentDanRer10());
            default -> getDefaultURL(vp, zoomfactor, trackType, highlights);
        };
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