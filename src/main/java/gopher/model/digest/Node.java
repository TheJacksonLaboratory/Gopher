package gopher.model.digest;

import gopher.model.viewpoint.Segment;

/** A node in the binary tree we will use for finding active segments */
class Node {
    Segment segment;
    Node left;
    Node right;

    Node(Segment seg) {
        this.segment = seg;
        right = null;
        left = null;
    }



}
