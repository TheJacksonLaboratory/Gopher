package gopher.model.digest;

import gopher.model.viewpoint.Segment;

public class BinaryTree {

    private Node root;

    BinaryTree(){
    }

    public void add(Segment seg) {
        root = addRecursive(root, seg);
    }


    private Node addRecursive(Node current, Segment segment) {
        if (current == null) {
            return new Node(segment);
        }
        if (segment.preceeds( current.segment)) {
            current.left = addRecursive(current.left, segment);
        } else if (current.segment.preceeds(segment) ) {
            current.right = addRecursive(current.right, segment);
        } else {
            // value already exists
            return current;
        }
        return current;
    }


    boolean containsNode(String chrom, int pos) {
        return containsNodeRecursive(root, chrom,pos);
    }

    private boolean containsNodeRecursive(Node current, String chrom, int pos) {
        if (current == null) {
            return false;
        }
        if (chrom.equals(current.segment.getReferenceSequenceID()) &&
                current.segment.getStartPos() == pos) {
            return true;
        }
        return current.segment.preceeds(chrom,pos)
                ? containsNodeRecursive(current.right, chrom,pos)
                : containsNodeRecursive(current.left, chrom,pos);
    }
}
