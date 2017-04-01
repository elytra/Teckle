package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

        HashMap<BlockPos, EndpointData> endpoints = new HashMap<>();

        List<Tuple<BlockPos, Integer>> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();

        nodeStack.add(new Tuple<>(startPos, 0));
        while (!nodeStack.isEmpty()) {
            Tuple<BlockPos, Integer> tuple = nodeStack.remove(nodeStack.size() - 1);
            BlockPos pos = tuple.getFirst();
            Integer cost = tuple.getSecond();
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pos.add(direction.getDirectionVec());
                EnumFacing injectionFacing = getFacingFromVector(pos.subtract(neighbourPos));
                if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(position))
                    continue;

                boolean hasEndpointAndFacing = endpoints.containsKey(neighbourPos) ? endpoints.get(neighbourPos).side.equals(injectionFacing) : true;
                if (hasEndpointAndFacing && iteratedPositions.contains(neighbourPos))
                    continue;

                WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                if (neighbourNode.canAcceptTraveller(traveller, getFacingFromVector(pos.subtract(neighbourPos)))) {
                    nodeStack.add(new Tuple<>(neighbourPos, cost + 1));
                    iteratedPositions.add(neighbourPos);

                    if (isValidEndpoint(traveller, pos, neighbourPos)) {
                        endpoints.put(pos, new EndpointData(pos, getFacingFromVector(pos.subtract(neighbourPos)), cost + 1));
                    }
                }
            }
        }

        List<EndpointData> endpointDatas = endpoints.values().stream().sorted(Comparator.comparingInt(o -> o.cost)).collect(Collectors.toList());
        WorldNetworkPath path = WorldNetworkPath.createPath(traveller, network.getNodeFromPosition(startPos), network.getNodeFromPosition(endpointDatas.get(0).pos));
        traveller.triedEndpoints.add(endpointDatas.get(0));
        traveller.activePath = path;
        traveller.previousNode = WorldNetworkNode.NONE;
        traveller.currentNode = this;
        traveller.nextNode = path.next();
    }

    /**
     * Finds the next valid node for the given traveller, if no nodes are available sends it back to it's source.
     *
     * @param traveller
     */
    public void findNodeForTraveller(WorldNetworkTraveller traveller) {
        List<Tuple<BlockPos, Integer>> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        HashMap<BlockPos, EndpointData> endpoints = new HashMap<>();

        nodeStack.add(new Tuple<>(traveller.currentNode.position, 0));
        while (!nodeStack.isEmpty()) {
            Tuple<BlockPos, Integer> tuple = nodeStack.remove(nodeStack.size() - 1);
            BlockPos pos = tuple.getFirst();
            Integer cost = tuple.getSecond();
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pos.add(direction.getDirectionVec());
                EnumFacing injectionFacing = getFacingFromVector(pos.subtract(neighbourPos));

                if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(position) || traveller.triedEndpoints.contains(network.getNodeFromPosition(neighbourPos)))
                    continue;

                boolean hasEndpointAndFacing = endpoints.containsKey(neighbourPos) ? endpoints.get(neighbourPos).side.equals(injectionFacing) : true;
                if (hasEndpointAndFacing && iteratedPositions.contains(neighbourPos))
                    continue;

                WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                if (neighbourNode.canAcceptTraveller(traveller, getFacingFromVector(pos.subtract(neighbourPos)))) {
                    nodeStack.add(new Tuple<>(neighbourPos, cost + 1));
                    iteratedPositions.add(neighbourPos);

                    if (isValidEndpoint(traveller, pos, neighbourPos)) {
                        endpoints.put(pos, new EndpointData(pos, getFacingFromVector(pos.subtract(neighbourPos)), cost + 1));
                    }
                }
            }
        }

        List<EndpointData> endpointDatas = endpoints.values().stream().sorted(Comparator.comparingInt(o -> o.cost)).collect(Collectors.toList());
        WorldNetworkPath path = WorldNetworkPath.createPath(traveller, traveller.currentNode, network.getNodeFromPosition(endpointDatas.get(0).pos));
        traveller.triedEndpoints.add(endpointDatas.get(0));
        traveller.activePath = path;
        traveller.previousNode = WorldNetworkNode.NONE;
        traveller.currentNode = this;
        traveller.nextNode = path.next();
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
