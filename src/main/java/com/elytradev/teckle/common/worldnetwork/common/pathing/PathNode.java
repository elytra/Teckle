package com.elytradev.teckle.common.worldnetwork.common.pathing;

import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;

/**
 * Used to store tagCompound about the usefulness of a node for making a path.
 */
public class PathNode {
    public int cost;
    public PathNode from;
    public WorldNetworkNode realNode;

    public PathNode(PathNode from, WorldNetworkNode realNode) {
        this.from = from;
        this.realNode = realNode;
        this.cost = from != null ? from.cost + 1 : 0;
    }

}
