package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.concrete.Marshallable;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

import static com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller.getFacingFromVector;

/**
 * More or less a wrapper for a list that makes code easier to understand.
 */
public class WorldNetworkPath implements Marshallable {

    public static final WorldNetworkPath NOT_POSSIBLE = new WorldNetworkPath(null, null, null);

    private List<PathNode> path;
    private int index = -1;
    private WorldNetworkTraveller traveller;
    private WorldNetworkNode startNode;
    private EndpointData endNode;

    // Default constructor to calm concrete down.
    public WorldNetworkPath() {
        path = new ArrayList<PathNode>();
        index = -1;
    }

    private WorldNetworkPath(WorldNetworkTraveller traveller, WorldNetworkNode startNode, EndpointData endNode) {
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
    public static WorldNetworkPath createPath(WorldNetworkTraveller traveller, WorldNetworkNode startNode, EndpointData endNode) {
        WorldNetworkPath path = new WorldNetworkPath(traveller, startNode, endNode);
        path.generateNodes();
        return path;
    }

    /**
     * Get all positions in the path, used for data sync.
     *
     * @return the data sorted from beginning to end.
     */
    public List<BlockPos> pathPositions() {
        List<BlockPos> out = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            out.add(path.get(i).realNode.position);
        }

        return out;
    }

    /**
     * Generates nodes for a path.
     */
    private void generateNodes() {
        ArrayList<PathNode> nodeStack = new ArrayList<PathNode>();
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();

        PathNode destinationPathNode = createNodesAround(nodeStack, positions, new PathNode(null, startNode, 0, true));
        while (destinationPathNode == null && !nodeStack.isEmpty()) {
            PathNode lowestTotalNode = nodeStack.remove(0);
            destinationPathNode = createNodesAround(nodeStack, positions, lowestTotalNode);
        }

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
                PathNode pathNode = new PathNode(node, neighbourNode, node.costFromStart + 1, true);
                if (pathNode.costToDestination == 0 && getFacingFromVector(node.realNode.position.subtract(neighbourPos)).equals(endNode.side)) {
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
     * Move to the increment path node.
     *
     * @return the increment node.
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

    @Override
    public void writeToNetwork(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, path.size(), 3);

        for (int i = 0; i < path.size(); i++) {
            buf.writeLong(path.get(i).realNode.position.toLong());
        }
    }

    @Override
    public void readFromNetwork(ByteBuf buf) {
        int size = ByteBufUtils.readVarInt(buf, 3);

        for (int i = 0; i < size; i++) {
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            WorldNetworkNode networkNode = new WorldNetworkNode(null, pos);
            if (i == size - 1) {
                networkNode = new WorldNetworkEndpoint(null, pos);
            }

            path.add(new PathNode(i == 0 ? null : path.get(i - 1), networkNode, 0, false));
        }
    }

    /**
     * Used to store tagCompound about the usefulness of a node for making a path.
     */
    private class PathNode {
        PathNode from;
        WorldNetworkNode realNode;
        int costFromStart, costToDestination, totalCost;

        public PathNode(PathNode from, WorldNetworkNode realNode, int costFromStart, boolean doCostCalc) {
            this.from = from;
            this.realNode = realNode;
            this.costFromStart = costFromStart;
            if (doCostCalc)
                calculateCost();
        }

        public void calculateCost() {
            BlockPos distanceFromDestination = realNode.position.subtract(WorldNetworkPath.this.endNode.pos);
            costToDestination = Math.abs(distanceFromDestination.getX() + distanceFromDestination.getY() + distanceFromDestination.getZ());
            totalCost = costFromStart + costToDestination;
        }
    }
}
