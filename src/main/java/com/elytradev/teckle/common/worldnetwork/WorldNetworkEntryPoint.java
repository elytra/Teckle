package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller.getFacingFromVector;

/**
 * A node used to add travellers to a network, handles initial endpoint finding, as well as finding new endpoints when one fails.
 */
public class WorldNetworkEntryPoint extends WorldNetworkNode {

    private EnumFacing facing = EnumFacing.DOWN;
    //TODO: round robin for endpoints of same cost.
    private int roundRobinTicker = 0;

    public WorldNetworkEntryPoint(WorldNetwork network, BlockPos position, EnumFacing facing) {
        this.network = network;
        this.position = position;
        this.facing = facing;
    }

    public void addTraveller(NBTTagCompound data) {
        WorldNetworkTraveller traveller = new WorldNetworkTraveller(this, data);
        createInitialPathForTraveller(traveller);
        network.registerTraveller(traveller);
    }

    public void createInitialPathForTraveller(WorldNetworkTraveller traveller) {
        BlockPos startPos = this.position.add(facing.getDirectionVec());

        if (!network.isNodePresent(startPos))
            return;

        HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();
        List<Tuple<BlockPos, Integer>> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();

        nodeStack.add(new Tuple<>(startPos, 0));
        while (!nodeStack.isEmpty()) {
            Tuple<BlockPos, Integer> tuple = nodeStack.remove(nodeStack.size() - 1);
            BlockPos pos = tuple.getFirst();
            Integer cost = tuple.getSecond();
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pos.add(direction.getDirectionVec());
                if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(position)) {
                    continue;
                }

                if (iteratedPositions.contains(neighbourPos)) {
                    continue;
                }

                if (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite())) {
                    continue;
                }

                WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                if (neighbourNode.canAcceptTraveller(traveller, direction.getOpposite())) {
                    if (isValidEndpoint(traveller, pos, neighbourPos)) {
                        if (!endpoints.containsKey(neighbourPos)) {
                            endpoints.put(neighbourPos, new HashMap<>());
                        }
                        endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData((WorldNetworkEndpoint) neighbourNode, neighbourPos, direction.getOpposite(), cost + 1));
                    } else {
                        nodeStack.add(new Tuple<>(neighbourPos, cost + 1));
                        iteratedPositions.add(neighbourPos);
                    }
                }
            }
        }

        List<EndpointData> sortedEndpointData = new ArrayList<>();
        for (HashMap.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
            sortedEndpointData.addAll(entry.getValue().values());
        }
        sortedEndpointData.sort(Comparator.comparingInt(o -> o.cost));

        System.out.println("Found " + sortedEndpointData.size() + " endpoints. Using: " + sortedEndpointData.get(0));
        WorldNetworkPath path = WorldNetworkPath.createPath(traveller, network.getNodeFromPosition(startPos), sortedEndpointData.get(0));
        traveller.triedEndpoints.add(sortedEndpointData.get(0));
        traveller.activePath = path;
        traveller.previousNode = WorldNetworkNode.NONE;
        traveller.currentNode = this;
        traveller.nextNode = path.next();

        traveller.currentNode.registerTraveller(traveller);
    }

    /**
     * Finds the increment valid node for the given traveller, if no nodes are available sends it back to it's source.
     *
     * @param traveller
     */
    public void findNodeForTraveller(WorldNetworkTraveller traveller) {
        List<Tuple<BlockPos, Integer>> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();

        nodeStack.add(new Tuple<>(traveller.currentNode.position, 0));
        while (!nodeStack.isEmpty()) {
            Tuple<BlockPos, Integer> tuple = nodeStack.remove(nodeStack.size() - 1);
            BlockPos pos = tuple.getFirst();
            Integer cost = tuple.getSecond();
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pos.add(direction.getDirectionVec());
                if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(position)) {
                    continue;
                }

                if (iteratedPositions.contains(neighbourPos)) {
                    continue;
                }

                if (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite())) {
                    continue;
                }

                WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                if (neighbourNode.canAcceptTraveller(traveller, direction.getOpposite())) {
                    if (isValidEndpoint(traveller, pos, neighbourPos)) {
                        if (!endpoints.containsKey(neighbourPos)) {
                            endpoints.put(neighbourPos, new HashMap<>());
                        }
                        endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData((WorldNetworkEndpoint) neighbourNode, neighbourPos, direction.getOpposite(), cost + 1));
                    } else {
                        nodeStack.add(new Tuple<>(neighbourPos, cost + 1));
                        iteratedPositions.add(neighbourPos);
                    }
                }
            }
        }

        List<EndpointData> sortedEndpointData = new ArrayList<>();
        for (HashMap.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
            sortedEndpointData.addAll(entry.getValue().values());
        }
        sortedEndpointData.sort(Comparator.comparingInt(o -> o.cost));
        BlockPos startPos = traveller.nextNode.position;
        WorldNetworkPath path = WorldNetworkPath.createPath(traveller, new WorldNetworkNode(network, startPos), sortedEndpointData.get(0));

        traveller.triedEndpoints.add(sortedEndpointData.get(0));
        traveller.previousNode = path.next();
        traveller.currentNode = path.next();
        traveller.nextNode = path.next();
        traveller.activePath = path;
    }

    public boolean isValidEndpoint(WorldNetworkTraveller traveller, BlockPos from, BlockPos endPoint) {
        return network.isNodePresent(endPoint)
                && network.getNodeFromPosition(endPoint).isEndpoint()
                && network.getNodeFromPosition(position).canAcceptTraveller(traveller, getFacingFromVector(from.subtract(endPoint)));
    }

    public EnumFacing getFacing() {
        return facing;
    }

}
