package gopher.model.digest;

import gopher.model.viewpoint.Segment;

/**
 * A simple implementation of a binary tree that is intended to be used to store the
 * selected restriction fragments. When we create the digest file, we need to find these
 * fragments quickly in order to know whether any given restriction fragment is selected or not.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class BinaryTree {

    private Node root;

    private int n_nodes;

    BinaryTree(){
        n_nodes=0;
    }

    /**
     * This is used to add a selected {@link Segment}.
     * @param seg The segment to be aded
     */
    public void add(Segment seg) {
        root = addRecursive(root, seg);
    }

    /**
     * @return number of nodes of the binary tree
     */
    public int getN_nodes() {
        return n_nodes;
    }

    private Node addRecursive(Node current, Segment segment) {
        if (current == null) {
            n_nodes++;
            return new Node(segment);
        }
        if (segment.preceeds( current.segment)) {
            current.left = addRecursive(current.left, segment);
        } else if (current.segment.preceeds(segment) ) {
            current.right = addRecursive(current.right, segment);
        } else {
            // value already exists -- should never happen in this application.
            return current;
        }
        return current;
    }

    /** @return true iff the chromosome and position correspond to one of the selected {@link gopher.model.viewpoint.Segment}s.*/
    boolean containsNode(String chrom, int pos) {
        return containsNodeRecursive(root, chrom,pos);
    }

    private boolean containsNodeRecursive(Node current, String chrom, int pos) {
        if (current == null) {
            return false;
        } else if (chrom.equals(current.segment.getReferenceSequenceID()) &&
            current.segment.getStartPos().equals(pos) ){
            return true;
        } else {
            return current.segment.preceeds(chrom, pos)
                    ? containsNodeRecursive(current.right, chrom, pos)
                    : containsNodeRecursive(current.left, chrom, pos);
        }
    }

    /** @return corresponding Node iff the chromosome and position correspond to one of the selected {@link gopher.model.viewpoint.Segment}s otherwise return null*/
    Node getNode(String chrom, int pos) {
        return getNodeRecursive(root,chrom,pos);
    }

    private Node getNodeRecursive(Node current, String chrom, int pos) {
        if (current == null) {
            return null;
        } else if (chrom.equals(current.segment.getReferenceSequenceID()) &&
                current.segment.getStartPos().equals(pos) ){
            return current;
        } else return current.segment.preceeds(chrom, pos)
                ? getNodeRecursive(current.right, chrom, pos)
                : getNodeRecursive(current.left, chrom, pos);
    }
}
