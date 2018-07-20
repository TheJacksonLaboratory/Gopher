package gopher.model;

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
    public final static double MIN_GC_CONTENT=0.35;
    /** Maximum allowable GC content (in percent) */
    public final static double MAX_GC_CONTENT=0.65;


    public final static int TILING_FACTOR = 1;

    public final static int MINIMUM_FRAGMENT_SIZE = 120;
    /** Maximum allowable mean kmer alignability. */
    public final static int MAXIMUM_KMER_ALIGNABILITY = 10;

    public final static int MARGIN_SIZE = 250;

    public final static Boolean ALLOW_SINGLE_MARGIN=true;
    public final static Boolean ALLOW_PATCHING=false;

    public final static int MIN_BAIT_NUMBER = 1;

    public final static int MAX_BAIT_NUMBER = 3;

    public final static int DEFAULT_BAIT_LENGTH=120;

}
