package gopher.model.digest;

/**
 * This represents one "cut" of a genomic digest, i.e., one position that is cut by a specific enzyme.
 * This can be used to generate a digest map which is an ordered list of cuts.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2
 */
public class Digest implements Comparable<Digest> {
    /** The digests are numbered from 1 to N; this makes it easy to test for adjacent fragments. */
    final int enzymeNumber;
    /** The start (5') position of the digest (restriction fragment). */
    public final int position;

    Digest(int enzymeNr, int pos) {
        enzymeNumber=enzymeNr;
        position=pos;
    }


    @Override
    public int compareTo(Digest o) {
        return position - o.position;
    }
}
