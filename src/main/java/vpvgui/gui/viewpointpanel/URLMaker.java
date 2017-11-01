package vpvgui.gui.viewpointpanel;


import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.ViewPoint;

/**
 * This class makes URLs for displaying the viewpoints.
 * The URLs need to be different for each organism since each organism has a different
 * selection of data.
 */
public class URLMaker {
    private static final Logger logger = Logger.getLogger(URLMaker.class.getName());
    private String genomebuild=null;
      private String restrictionenzyme=null;
    /** This variable will be initialized to the number of pixels that we want the UCSC image to be. */
    private int xdim;

    /** We will make the maximum width of the UCSC image 1600. If the user's screen is smaller, we will shrink the image. */
    private static final int UCSC_DEFAULT_WIDTH = 1600;
    /* Number of nucleotides to show before and after first and last base of viewpoint. */
    private static final int OFFSET = 200;




    public URLMaker(Model model){
        this.genomebuild=model.getGenomeBuild();
        xdim=Math.min(UCSC_DEFAULT_WIDTH,model.getXdim());
        logger.trace(String.format("setting genomebuild to %s with default image width of %d",genomebuild,xdim));
    }


    public String getImageURL(ViewPoint vp, String highlight) {
        final String trackType="hgRenderTracks";
        String url = getDefaultURL(vp,trackType,highlight);
        if (this.genomebuild.equals("hg19")){
            url=String.format("%s&%s",url,getURLFragmentHg19());
        } else if (this.genomebuild.equals("mm9")) {
            url = String.format("%s&%s",url,getURLFragmentMm9());
        }
        return url;

    }


    public String getURL(ViewPoint vp,String highlights) {
        final String trackType="hgTracks";
        String url = getDefaultURL(vp,trackType,highlights);
        if (this.genomebuild.equals("hg19")){
            url=String.format("%s&%s",url,getURLFragmentHg19());
        }  else if (this.genomebuild.equals("mm9")) {
            url = String.format("%s&%s",url,getURLFragmentMm9());
        }
        return url;
    }
    /** These are the things to hide and show to get a nice hg19 image. */
    public String getURLFragmentHg19() {
        return "snp147Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&refGene=full&g=gc5base";
    }
    /** These are the things to hide and show to get a nice mm9 image. */
    public String getURLFragmentMm9() {
        return "knownGene=hide&refGene=full&stsMapMouseNew=hide&hgFind.matches=Slc12a1&xenoRefGene=hide&blastHg18KG=hide&ensGene=hide&pubs=hide&intronEST=hide&snp128=hide&oreganno=full";
    }


    /**
     * TODO needs more customization!
     * OLD KIND OF URL
     * {@code http://genome.ucsc.edu/cgi-bin/hgRenderTracks?db=hg19&position=chr3%3A189344251-189352061&hgFind.matches=TP63&highlight=hg19.chr3%3A189344451-189345117%23F08080%7Chg19.chr3%3A189346090-189346609%23ABEBC6%7Chg19.chr3%3A189347116-189348942%23FFA07A%7Chg19.chr3%3A189348942-189349556%23C39BD3%7Chg19.chr3%3A189349556-189350268%23F7DC6F%7Chg19.chr3%3A189350268-189350584%23F08080%7Chg19.chr3%3A189350584-189350934%23ABEBC6%7Chg19.chr3%3A189351345-189351861%23FFA07A&pix=1400}
     * We want to add the tracks for
     * <ol>
     *     <li>H3K27ac</li>
     *     <li>DnaIHypersensitivity</li>
     *     <li>TranscriptionFactorCHipseq</li>
     *     <li>100 vertebrate conservatoin</li>
     *     <li>multiZ alignments</li>
     *     <li>repeats</li>
     * </ol>
     * This is how: {@code hideTracks=1&<trackName>=full|dense|pack|hide}
     * @param vp The {@link ViewPoint} object to be displayed on the UCSC browser
     * @param trackType either "hgTracks" (interactive browser) or "hgRenderTracks" (static image)
     * @return
     */
    public String getDefaultURL(ViewPoint vp, String trackType,String highlights) {
        int posFrom, posTo;
        posFrom = vp.getMinimumDisplayPosition() - OFFSET;
        posTo = vp.getMaximumDisplayPosition() + OFFSET;
        String chrom = vp.getReferenceID();
        if (!chrom.startsWith("chr"))
            chrom = "chr" + chrom; /* TODO MAKE THIS ROBUST! */
        String targetItem = vp.getTargetName();
        String url = String.format("http://genome.ucsc.edu/cgi-bin/%s?db=%s&position=%s%%3A%d-%d&hgFind.matches=%s&%s&pix=%d",
                trackType, genomebuild, chrom, posFrom, posTo, targetItem, highlights,xdim);
        if (restrictionenzyme!=null) {
            url=String.format("%s&oligoMatch=pack&hgt.oligoMatch=GATC",url,restrictionenzyme);
        }
        return url;
    }

    public void setEnzyme(String re) {
        this.restrictionenzyme=re;
    }



}
