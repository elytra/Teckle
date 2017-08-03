package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Predicate;

public class PositionData {

    private static Map<Integer, Map<BlockPos, PositionData>> POOL = Maps.newHashMap();

    public int dimension;
    public BlockPos pos;
    private Map<UUID, List<NodeContainer>> nodeContainers = Maps.newHashMap();

    PositionData(int dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    /**
     * Gets position data for the specified block position in the specified dimension,
     * creates one if it's not already present.
     *
     * @param dimension
     * @param position
     * @return
     */
    public static PositionData getPositionData(int dimension, BlockPos position) {
        if (!POOL.containsKey(Integer.valueOf(dimension)))
            POOL.put(Integer.valueOf(dimension), Maps.newHashMap());

        Map<BlockPos, PositionData> dimensionPool = POOL.get(Integer.valueOf(dimension));
        if (!dimensionPool.containsKey(position))
            dimensionPool.put(position, new PositionData(dimension, position));

        return dimensionPool.get(position);
    }

    public List<NodeContainer> getNodeContainers(UUID key) {
        return nodeContainers.get(key);
    }

    public boolean add(NodeContainer container) {
        return add(container.getNetwork().getNetworkID(), container);
    }

    public boolean add(UUID networkID, NodeContainer container) {
        if (!nodeContainers.containsKey(networkID))
            nodeContainers.put(networkID, Lists.newArrayList());

        return nodeContainers.get(networkID).add(container);
    }

    public List<NodeContainer> removeNetwork(UUID networkID) {
        return nodeContainers.remove(networkID);
    }

    public boolean removeNodeContainer(UUID key, NodeContainer nodeContainer) {
        if (!nodeContainers.containsKey(key))
            nodeContainers.put(key, Lists.newArrayList());

        return nodeContainers.get(key).remove(nodeContainer);
    }

    public Set<UUID> networkIDS() {
        return nodeContainers.keySet();
    }

    public Collection<List<NodeContainer>> allNodeContainers() {
        return nodeContainers.values();
    }

    public Set<Map.Entry<UUID, List<NodeContainer>>> entrySet() {
        return nodeContainers.entrySet();
    }

    public boolean removeIf(UUID key, Predicate<NodeContainer> predicate) {
        return nodeContainers.get(key).removeIf(predicate);
    }

    public NodeContainer add(IWorldNetwork network, WorldNetworkNode node) {
        this.nodeContainers.putIfAbsent(network.getNetworkID(), Lists.newArrayList());
        List<NodeContainer> nodeContainers = this.nodeContainers.get(network.getNetworkID());

        NodeContainer container = new NodeContainer();
        container.setNetwork(network);
        container.setNode(node);
        container.setPos(pos);
        container.setFacing(node.getCapabilityFace());

        nodeContainers.removeIf(oldContainer -> Objects.equals(oldContainer, container));
        nodeContainers.add(container);
        return container;
    }
}
