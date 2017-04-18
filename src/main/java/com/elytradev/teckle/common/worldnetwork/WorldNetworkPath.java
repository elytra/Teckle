package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.concrete.Marshallable;
import com.elytradev.teckle.common.TeckleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * More or less a wrapper for a list that makes code easier to understand.
 */
public class WorldNetworkPath implements Marshallable {

    public static final WorldNetworkPath NOT_POSSIBLE = new WorldNetworkPath(null, null);

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

    private WorldNetworkPath(WorldNetworkTraveller traveller, EndpointData endNode) {
        path = new ArrayList<PathNode>();
        index = -1;

        this.traveller = traveller;
        this.endNode = endNode;
    }

    /**
     * Create a path through the following nodes for the given traveller.
     * Assumes the given end node can be reached.
     *
     * @param traveller the traveller to create the path for.
     * @param endNode   the end position.
     * @return a network path to the destination.
     */
    public static WorldNetworkPath createPath(WorldNetworkTraveller traveller, EndpointData endNode) {
        WorldNetworkPath path = new WorldNetworkPath(traveller, endNode);
        path.path.add(endNode.node);
        PathNode from = endNode.node.from;
        while (from != null) {
            path.path.add(0, from);
            // the best code ive ever written right here.
            from = from.from;
        }
        return path;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<PathNode> getPath() {
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
     * Move to the increment path node.
     *
     * @return the increment node.
     */
    public WorldNetworkNode next() {
        index++;
        WorldNetworkNode currentNode = null;
        try {
            currentNode = path.get(index).realNode;
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to get next node in path on " + FMLCommonHandler.instance().getEffectiveSide());
        }
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

        for (PathNode pathNode : path) {
            buf.writeLong(pathNode.realNode.position.toLong());
        }
    }

    @Override
    public void readFromNetwork(ByteBuf buf) {
        int size = ByteBufUtils.readVarInt(buf, 3);

        for (int i = 0; i < size; i++) {
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            WorldNetworkNode networkNode = new WorldNetworkNode(null, pos);
            if (i == size - 1) {
                networkNode = new DummyWorldNetworkEndpoint(null, pos);
            }
            PathNode from = null;
            if (i != 0) {
                from = path.get(i - 1);
            }

            path.add(new PathNode(from, networkNode));
        }
    }

    public PathNode getEnd() {
        return path.get(path.size() - 1);
    }

}
