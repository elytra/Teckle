package com.elytradev.teckle.worldnetwork;

import com.google.common.collect.HashBiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by darkevilmac on 3/25/2017.
 */
public class WorldNetwork implements ITickable {

    protected static List<WorldNetwork> NETWORKS = new ArrayList<>();
    protected HashBiMap<BlockPos, WorldNetworkNode> networkNodes = HashBiMap.create();
    protected List<WorldNetworkTraveller> travellers = new ArrayList<>();
    protected World world;

    public WorldNetwork(World world) {
        this.world = world;

        NETWORKS.add(this);
    }

    @SubscribeEvent
    public static void onTickEvent(TickEvent.WorldTickEvent e) {
        if (NETWORKS.isEmpty() || e.phase.equals(TickEvent.Phase.START))
            return;

        List<WorldNetwork> emptyNetworks = new ArrayList<>();
        for (WorldNetwork network : NETWORKS) {
            if (network.networkNodes.isEmpty()) {
                if (!emptyNetworks.contains(network))
                    emptyNetworks.add(network);

                System.out.println("Found dead network " + network);
                continue;
            }
            network.update();
        }

        for (WorldNetwork emptyNetwork : emptyNetworks) {
            NETWORKS.remove(emptyNetwork);
        }
    }

    public void registerNode(WorldNetworkNode node) {
        System.out.println(this + "/Registering a node, " + node);
        networkNodes.put(node.position, node);
        node.network = this;
        System.out.println(this + "/Registered node, " + node);
    }

    public void unregisterNode(WorldNetworkNode node) {
        System.out.println(this + "/Unregistering a node, " + node);
        networkNodes.remove(node.position);
        System.out.println(this + "/Unregistered node, " + node);
    }

    public void unregisterNodeAtPosition(BlockPos nodePosition) {
        if (isNodePresent(nodePosition))
            unregisterNode(getNodeFromPosition(nodePosition));
    }

    public WorldNetworkNode getNodeFromPosition(BlockPos pos) {
        return networkNodes.get(pos);
    }

    public boolean isNodePresent(BlockPos nodePosition) {
        return networkNodes.containsKey(nodePosition);
    }

    public List<WorldNetworkNode> getNodes() {
        return Arrays.asList((WorldNetworkNode[]) networkNodes.values().toArray());
    }

    public List<BlockPos> getNodePositions() {
        return Arrays.asList((BlockPos[]) networkNodes.keySet().toArray());
    }

    public void registerTraveller(WorldNetworkTraveller traveller) {
        travellers.add(traveller);
    }

    public void unregisterTraveller(WorldNetworkTraveller traveller) {
        travellers.remove(traveller);
    }

    public World getWorld() {
        return world;
    }

    public WorldNetwork merge(WorldNetwork otherNetwork) {
        System.out.println("Performing a merge of " + this + " and " + otherNetwork);
        WorldNetwork mergedNetwork = new WorldNetwork(this.world);

        mergedNetwork.networkNodes.putAll(this.networkNodes);
        mergedNetwork.networkNodes.putAll(otherNetwork.networkNodes);
        mergedNetwork.travellers.addAll(this.travellers);
        mergedNetwork.travellers.addAll(otherNetwork.travellers);

        // Update network variable for all nodes.
        mergedNetwork.networkNodes.forEach((pos, node) -> {
            if (node.network != null) {
                node.network.unregisterNode(node);
            }

            mergedNetwork.registerNode(node);
            node.network = mergedNetwork;
        });
        mergedNetwork.travellers.forEach(traveller -> {
            if (traveller.network != null) {
                traveller.network.unregisterTraveller(traveller);
            }

            mergedNetwork.registerTraveller(traveller);
            traveller.network = mergedNetwork;
        });

        return mergedNetwork;
    }

    /**
     * Checks that the network's connections are fully valid, performs a split if needed.
     */
    public void validateNetwork() {
        // Perform flood fill to validate all nodes are connected. Choose an arbitrary node to start from.

        System.out.println("Performing a network validation.");
        List<List<WorldNetworkNode>> networks = new ArrayList<>();
        HashMap<BlockPos, WorldNetworkNode> uncheckedNodes = new HashMap<>();
        uncheckedNodes.putAll(this.networkNodes);

        while (!uncheckedNodes.isEmpty()) {
            List<BlockPos> nodeStack = new ArrayList<>();
            List<BlockPos> checkedPositions = new ArrayList<>();
            List<WorldNetworkNode> newNetwork = new ArrayList<>();

            BlockPos uncheckedNodeKey = (BlockPos) uncheckedNodes.keySet().toArray()[0];
            nodeStack.add(uncheckedNodes.get(uncheckedNodeKey).position);
            checkedPositions.add(nodeStack.get(0));
            while (!nodeStack.isEmpty()) {
                BlockPos nodePos = nodeStack.remove(0);

                if (uncheckedNodes.containsKey(nodePos)) {
                    newNetwork.add(uncheckedNodes.remove(nodePos));
                    for (EnumFacing direction : EnumFacing.VALUES) {
                        if (!checkedPositions.contains(nodePos.add(direction.getDirectionVec())))
                            nodeStack.add(nodePos.add(direction.getDirectionVec()));
                    }
                }
            }

            networks.add(newNetwork);
        }

        // Only process a split if there's a new network that needs to be formed. RIP old network </3
        if (networks.size() > 1) {
            System.out.println("Splitting a network...");
            //Start from 1, leave 0 as this network.
            for (int networkNum = 1; networkNum < networks.size(); networkNum++) {
                List<WorldNetworkNode> newNetworkData = networks.get(networkNum);
                WorldNetwork newNetwork = new WorldNetwork(this.world);
                for (WorldNetworkNode node : newNetworkData) {
                    this.unregisterNode(node);
                    newNetwork.registerNode(node);

                    // Move travellers if needed.
                    // TODO: Find travellers a new entry point.
                    if (!node.getTravellers().isEmpty()) {
                        for (WorldNetworkTraveller traveller : node.getTravellers()) {
                            traveller.network.unregisterTraveller(traveller);
                            newNetwork.registerTraveller(traveller);
                        }
                    }
                }
            }
        }

        System.out.println("Finished validation, resulted in " + networks.size() + " networks.");
    }

    @Override
    public void update() {
        for (WorldNetworkTraveller traveller : travellers) {
            traveller.update();
        }
    }
}

