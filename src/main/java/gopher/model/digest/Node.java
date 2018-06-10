package gopher.model.digest;

import gopher.model.viewpoint.Segment;

/** A node in the binary tree we will use for finding active segments. This class works essentially
 * like a C struct and is intended to be used only by {@link BinaryTree}. */
class Node {
    final Segment segment;
    Node left;
    Node right;

    Node(Segment seg) {
        this.segment = seg;
        right = null;
        left = null;
    }



}
