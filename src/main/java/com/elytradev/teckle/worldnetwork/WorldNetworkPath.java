package com.elytradev.teckle.worldnetwork;

import java.util.List;

/**
 * More or less a wrapper for a list that makes code easier to understand.
 */
public class WorldNetworkPath {

    private List<WorldNetworkNode> path;
    private int index = 0;

    // Private constructor, only create via static method.
    private WorldNetworkPath() {

    }

    //TODO: Static method to construct a path from two nodes in a network.

    /**
     * Move to the next path node.
     *
     * @return the next node.
     */
    public WorldNetworkNode next() {
        index++;
        WorldNetworkNode currentNode = path.get(index);
        return currentNode != null ? currentNode : WorldNetworkNode.NONE;
    }

    /**
     * Move to the previous path node.
     *
     * @return the previous node.
     */
    public WorldNetworkNode prev() {
        index--;
        WorldNetworkNode currentNode = path.get(index);
        return currentNode != null ? currentNode : WorldNetworkNode.NONE;
    }

}
