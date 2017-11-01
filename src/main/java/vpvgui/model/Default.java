package vpvgui.model;

/**
 * This class holds default values for various items.
 * @author Peter Robinson
 * @version 0.1.3 (2017-11-01)
 */
public class Default {

    public final static int MAXIMUM_ALLOWED_REPEAT_OVERLAP=20;

    public final static int PROBE_LENGTH=120;


    public final static int SIZE_DOWNSTREAM = 1500;

    public final static int SIZE_UPSTREAM = 5000;


    /** Minimum allowable GC content (in percent) */
    public final static double MIN_GC_CONTENT=25.0;
    /** Maximum allowable GC content (in percent) */
    public final static double MAX_GC_CONTENT=65.0;


    public final static int TILING_FACTOR = 1;

    public final static int MINIMUM_FRAGMENT_SIZE = 120;
    /** Maximum allowable repeat content (in percent). */
    public final static double MAXIMUM_REPEAT_CONTENT = 70.0;

    public final static int MARGIN_SIZE = 250;

    public final static String GENOME_BUILD = "hg19";

}
