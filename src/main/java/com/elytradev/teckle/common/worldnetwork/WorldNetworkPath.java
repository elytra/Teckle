package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller.getFacingFromVector;

/**
 * More or less a wrapper for a list that makes code easier to understand.
 */
public class WorldNetworkPath {

    public static final WorldNetworkPath NOT_POSSIBLE = new WorldNetworkPath(null, null, null);

    private List<PathNode> path;
    private int index = -1;
    private WorldNetworkTraveller traveller;
    private WorldNetworkNode startNode, endNode;

    // Private constructor, only create via static method.
    private WorldNetworkPath(WorldNetworkTraveller traveller, WorldNetworkNode startNode, WorldNetworkNode endNode) {
        path = new ArrayList<PathNode>();
        index = -1;

        this.traveller = traveller;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    /**
     * Create a path through the following nodes for the given traveller.
     * Assumes the given end node can be reached.
     *
     * @param traveller the traveller to create the path for.
     * @param startNode the starting position.
     * @param endNode   the end position.
     * @return a network path to the destination.
     */
    public static WorldNetworkPath createPath(WorldNetworkTraveller traveller, WorldNetworkNode startNode, WorldNetworkNode endNode) {
        WorldNetworkPath path = new WorldNetworkPath(traveller, startNode, endNode);
        path.generateNodes();
        return path;
    }

    /**
     * Generates nodes for a path.
     */
    private void generateNodes() {
        ArrayList<PathNode> nodeStack = new ArrayList<PathNode>();
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();

        PathNode destinationPathNode = createNodesAround(nodeStack, positions, new PathNode(null, startNode, 0));
        while (destinationPathNode == null && !nodeStack.isEmpty()) {
            PathNode lowestTotalNode = nodeStack.remove(0);
            destinationPathNode = createNodesAround(nodeStack, positions, lowestTotalNode);
        }
        this.endNode = destinationPathNode.realNode;
        PathNode currentNode = destinationPathNode;

        while (currentNode != null) {
            path.add(0, currentNode);
            currentNode = currentNode.from;
        }
    }

    /**
     * Generates PathNodes for the given node and adds them to the stack, ignores positions given.
     *
     * @param nodeStack current node stack.
     * @param positions positions to ignore.
     * @param node      node to create around.
     * @return a node representing the destination if found.
     */
    private PathNode createNodesAround(ArrayList<PathNode> nodeStack, ArrayList<BlockPos> positions, PathNode node) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = node.realNode.position.add(facing.getDirectionVec());
            if (positions.contains(neighbourPos) || !node.realNode.network.isNodePresent(neighbourPos))
                continue;

            WorldNetworkNode neighbourNode = node.realNode.network.getNodeFromPosition(neighbourPos);
            if (neighbourNode.canAcceptTraveller(traveller, getFacingFromVector(node.realNode.position.subtract(neighbourPos)))) {
                PathNode pathNode = new PathNode(node, neighbourNode, node.costFromStart + 1);
                if (pathNode.costToDestination == 0) {
                    return pathNode;
                }

                addToStack(nodeStack, pathNode);
            }
        }

        return null;
    }

    /**
     * Add the given node to the given stack at a sorted position.
     *
     * @param nodeStack the stack to add to.
     * @param newNode   the new node to add to the stack.
     */
    private void addToStack(ArrayList<PathNode> nodeStack, PathNode newNode) {
        if (nodeStack.isEmpty() || nodeStack.get(0).totalCost >= newNode.totalCost) {
            nodeStack.add(0, newNode);
        } else {
            int addToIndex = nodeStack.size();
            for (int i = 0; i < nodeStack.size(); i++) {
                PathNode nodeAtI = nodeStack.get(i);

                if (nodeAtI.totalCost >= newNode.totalCost) {
                    addToIndex = i;
                    break;
                }
            }

            nodeStack.add(addToIndex, newNode);
        }
    }

    /**
     * Move to the next path node.
     *
     * @return the next node.
     */
    public WorldNetworkNode next() {
        index++;
        WorldNetworkNode currentNode = path.get(index).realNode;
        return currentNode != null ? currentNode : WorldNetworkNode.NONE;
    }

    /**
     * Move to the previous path node.
     *
     * @return the previous node.
     */
    public WorldNetworkNode prev() {
        index--;
        WorldNetworkNode currentNode = path.get(index).realNode;
        return currentNode != null ? currentNode : WorldNetworkNode.NONE;
    }

    /**
     * Used to store data about the usefulness of a node for making a path.
     */
    private class PathNode {
        PathNode from;
        WorldNetworkNode realNode;
        int costFromStart, costToDestination, totalCost;

        public PathNode(PathNode from, WorldNetworkNode realNode, int costFromStart) {
            this.from = from;
            this.realNode = realNode;
            this.costFromStart = costFromStart;
            calculateCost();
        }

        public void calculateCost() {
            BlockPos distanceFromDestination = realNode.position.subtract(WorldNetworkPath.this.endNode.position);
            costToDestination = Math.abs(distanceFromDestination.getX() + distanceFromDestination.getY() + distanceFromDestination.getZ());
            totalCost = costFromStart + costToDestination;
        }
    }
}
