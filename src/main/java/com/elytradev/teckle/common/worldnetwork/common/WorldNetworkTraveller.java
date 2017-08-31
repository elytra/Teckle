/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.network.messages.TravellerDataMessage;
import com.elytradev.teckle.common.network.messages.TravellerMoveMessage;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.EndpointData;
import com.elytradev.teckle.common.worldnetwork.common.pathing.PathNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.WorldNetworkPath;
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * A piece of tagCompound travelling to a node in the network.
 */
public class WorldNetworkTraveller implements ITickable, INBTSerializable<NBTTagCompound> {

    public static final WorldNetworkTraveller NONE = new WorldNetworkTraveller(new NBTTagCompound());
    public static HashMap<WorldNetworkEntryPoint, List<Tuple<EndpointData, Boolean>>> roundRobinMap = Maps.newHashMap();

    public IWorldNetwork network;
    public WorldNetworkNode previousNode = WorldNetworkNode.NONE, currentNode = WorldNetworkNode.NONE, nextNode = WorldNetworkNode.NONE;
    public WorldNetworkPath activePath;
    // The current distance travelled between our previous node, and the increment node.
    public float travelledDistance = 0F;
    public NBTTagCompound data;
    public List<ImmutablePair<WorldNetworkNode, EnumFacing>> triedEndpoints = new ArrayList<>();
    public HashMap<String, IDropAction> dropActions = new HashMap<>();
    protected WorldNetworkEntryPoint entryPoint;
    private BiPredicate<WorldNetworkNode, EnumFacing> endpointPredicate = (o0, o1) -> true;

    public WorldNetworkTraveller(NBTTagCompound data) {
        this.entryPoint = null;

        this.data = data;
        if (!this.data.hasKey("idLeast"))
            this.data.setUniqueId("id", UUID.randomUUID());
    }

    public WorldNetworkTraveller(WorldNetworkEntryPoint entryPoint, NBTTagCompound data) {
        this.network = entryPoint.getNetwork();
        this.entryPoint = entryPoint;

        this.data = data;
        if (!this.data.hasKey("idLeast"))
            this.data.setUniqueId("id", UUID.randomUUID());
    }

    public static EnumFacing getFacingFromVector(Vec3i vec) {
        for (EnumFacing facing : EnumFacing.VALUES)
            if (vec.equals(facing.getDirectionVec()))
                return facing;

        return EnumFacing.DOWN;
    }

    public BiPredicate<WorldNetworkNode, EnumFacing> getEndpointPredicate() {
        return endpointPredicate;
    }

    public void setEndpointPredicate(BiPredicate<WorldNetworkNode, EnumFacing> endpointPredicate) {
        this.endpointPredicate = endpointPredicate;
    }

    public WorldNetworkEntryPoint getEntryPoint() {
        return entryPoint;
    }

    public EnumFacing getFacingVector() {
        return getFacingFromVector(nextNode.getPosition().subtract(currentNode.getPosition())).getOpposite();
    }

    public void genPath(boolean attemptReroute) {
        List<PathNode> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();

        nodeStack.add(new PathNode(null, currentNode, null));
        while (!nodeStack.isEmpty()) {
            PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pathNode.realNode.getPosition().add(direction.getDirectionVec());
                if (!network.isNodePresent(neighbourPos, direction.getOpposite()) ||
                        iteratedPositions.contains(neighbourPos) ||
                        endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite())) {
                    continue;
                }
                WorldNetworkNode neighbourNode = network.getNode(neighbourPos, direction.getOpposite());
                if (neighbourNode.canAcceptTraveller(this, direction.getOpposite())) {
                    if (!endpoints.containsKey(neighbourPos)) {
                        endpoints.put(neighbourPos, new HashMap<>());
                    }
                    if (isValidEndpoint(this, pathNode.realNode.getPosition(), neighbourPos) && endpointPredicate.test(neighbourNode, direction.getOpposite())) {
                        endpoints.get(neighbourPos).put(direction.getOpposite(),
                                new EndpointData(new PathNode(pathNode, network.getNode(neighbourPos, direction.getOpposite()), direction.getOpposite()),
                                        direction.getOpposite()));
                    } else if (entryPoint.getPosition().equals(neighbourPos) && entryPoint.getNetwork().equals(network)) {
                        PathNode nextNode = new PathNode(pathNode, entryPoint.getEndpoint(), direction.getOpposite());
                        nextNode.cost = Integer.MAX_VALUE;
                        endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData(nextNode, direction.getOpposite()));
                    } else {
                        if (network.getNode(neighbourPos, direction.getOpposite()).canConnectTo(direction.getOpposite())) {
                            nodeStack.add(new PathNode(pathNode, network.getNode(neighbourPos, direction.getOpposite()), direction.getOpposite()));
                            iteratedPositions.add(neighbourPos);
                        }
                    }
                }
            }
        }

        List<EndpointData> sortedEndpointData = new ArrayList<>();
        for (HashMap.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
            sortedEndpointData.addAll(entry.getValue().values());
        }
        sortedEndpointData.sort(Comparator.comparingInt(o -> o.cost));

        if (sortedEndpointData.isEmpty() && attemptReroute) {
            WorldNetworkPath lastPath = this.activePath;
            triedEndpoints.clear();
            genPath(false);
            if (Objects.equals(this.activePath, lastPath)) {
                nodeStack = new ArrayList<>();
                iteratedPositions = new ArrayList<>();
                endpoints = new HashMap<>();

                nodeStack.add(new PathNode(null, currentNode, null));
                while (!nodeStack.isEmpty()) {
                    PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
                    for (EnumFacing direction : EnumFacing.VALUES) {
                        BlockPos neighbourPos = pathNode.realNode.getPosition().add(direction.getDirectionVec());
                        if (!network.isNodePresent(neighbourPos, direction.getOpposite()) ||
                                iteratedPositions.contains(neighbourPos) ||
                                endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite())) {
                            continue;
                        }

                        WorldNetworkNode neighbourNode = network.getNode(neighbourPos, direction.getOpposite());
                        PathNode neighbourPathNode = new PathNode(pathNode, neighbourNode, direction.getOpposite());
                        if (neighbourNode.canAcceptTraveller(this, direction.getOpposite())) {
                            if (!endpoints.containsKey(neighbourPos)) {
                                endpoints.put(neighbourPos, new HashMap<>());
                            }

                            if (!isValidEndpoint(this, pathNode.realNode.getPosition(), neighbourPos) || !endpointPredicate.test(neighbourNode, direction.getOpposite())) {
                                nodeStack.add(new PathNode(pathNode, network.getNode(neighbourPos, direction.getOpposite()), direction.getOpposite()));
                                endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData(neighbourPathNode, direction.getOpposite()));
                            }

                            iteratedPositions.add(neighbourPos);
                        }
                    }
                }

                sortedEndpointData.clear();
                for (HashMap.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
                    sortedEndpointData.addAll(entry.getValue().values());
                }
                sortedEndpointData.sort((o1, o2) -> o2.cost - o1.cost);
            } else {
                return;
            }
        }

        if (sortedEndpointData.isEmpty())
            return;

        WorldNetworkPath path = WorldNetworkPath.createPath(this, sortedEndpointData.get(0));
        this.previousNode = path.next();
        this.currentNode = path.next();
        this.nextNode = path.next();
        this.activePath = path;
        this.travelledDistance = -0.10F;
        this.network = currentNode.getNetwork();
    }

    /**
     * Attempt to generate an initial path for this item, return false if not possible.
     *
     * @return
     */
    public boolean genInitialPath() {
        BlockPos startPos = this.entryPoint.getPosition().add(entryPoint.getOutputFace().getDirectionVec());
        if (!network.isNodePresent(startPos))
            return false;

        List<PathNode> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();

        nodeStack.add(new PathNode(null, network.getNode(startPos, entryPoint.getOutputFace().getOpposite()), entryPoint.getOutputFace().getOpposite()));
        while (!nodeStack.isEmpty()) {
            PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pathNode.realNode.getPosition().add(direction.getDirectionVec());
                if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(entryPoint.getPosition()) ||
                        iteratedPositions.contains(neighbourPos) ||
                        endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite())) {
                    continue;
                }

                WorldNetworkNode neighbourNode = network.getNode(neighbourPos, direction.getOpposite());
                if (neighbourNode == null) {
                    neighbourNode = network.getNode(neighbourPos, direction.getOpposite());
                }

                if (neighbourNode.canAcceptTraveller(this, direction.getOpposite())) {
                    if (isValidEndpoint(this, pathNode.realNode.getPosition(), neighbourPos) && endpointPredicate.test(neighbourNode, direction.getOpposite())) {
                        if (!endpoints.containsKey(neighbourPos)) {
                            endpoints.put(neighbourPos, new HashMap<>());
                        }
                        endpoints.get(neighbourPos).put(direction.getOpposite(),
                                new EndpointData(new PathNode(pathNode, network.getNode(neighbourPos, direction.getOpposite()), direction.getOpposite()),
                                        direction.getOpposite()));
                    } else {
                        if (network.getNode(neighbourPos, direction.getOpposite()).canConnectTo(direction.getOpposite())) {
                            nodeStack.add(new PathNode(pathNode, network.getNode(neighbourPos, direction.getOpposite()), direction.getOpposite()));
                            iteratedPositions.add(neighbourPos);
                        }
                    }
                }
            }
        }

        List<EndpointData> sortedEndpointData = new ArrayList<>();
        for (HashMap.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
            sortedEndpointData.addAll(entry.getValue().values());
        }
        sortedEndpointData.sort(Comparator.comparingInt(o -> o.cost));
        // Round robin checks, kind of messy but works.
        if (!sortedEndpointData.isEmpty()) {
            int lowestCost = sortedEndpointData.get(0).cost;
            List<EndpointData> lowestCostingEndpoints = sortedEndpointData.stream()
                    .filter(endpointData -> endpointData.cost == lowestCost).collect(Collectors.toList());
            List<Tuple<EndpointData, Boolean>> endpointDataToRemove = new ArrayList<>();

            if (lowestCostingEndpoints.size() > 1) {
                // More than one endpoint of equal value.

                List<Tuple<EndpointData, Boolean>> roundRobinData;
                if (roundRobinMap.containsKey(entryPoint)) {
                    roundRobinData = roundRobinMap.get(entryPoint);

                    for (EndpointData lowestCostingEndpoint : lowestCostingEndpoints) {
                        if (!roundRobinData.stream().anyMatch(endpointDataBooleanTuple -> endpointDataBooleanTuple.getFirst().equals(lowestCostingEndpoint))) {
                            roundRobinData.add(new Tuple<>(lowestCostingEndpoint, false));
                        }
                    }
                } else {
                    roundRobinData = new ArrayList<>();
                    roundRobinMap.put(entryPoint, roundRobinData);
                }
                // Quick clean of the data, remove any dormant nodes.
                roundRobinData.removeIf(endpointDataBooleanTuple -> !endpointDataBooleanTuple.getFirst().node.realNode.getNetwork().isNodePresent(endpointDataBooleanTuple.getFirst().pos));

                for (Tuple<EndpointData, Boolean> roundRobinDatum : roundRobinData) {
                    if (lowestCostingEndpoints.contains(roundRobinDatum.getFirst()) && roundRobinDatum.getSecond()) {
                        endpointDataToRemove.add(roundRobinDatum);
                    }
                }

                // Same amount of lowest endpoints as there are to remove, clear the list of round robin data.
                if (lowestCostingEndpoints.size() == endpointDataToRemove.size()) {
                    for (Tuple<EndpointData, Boolean> endpointDataBooleanTuple : endpointDataToRemove) {
                        if (sortedEndpointData.indexOf(endpointDataBooleanTuple.getFirst()) == 0) {
                            // Prevents the initial round robin destination from getting used twice.
                            roundRobinData.remove(endpointDataBooleanTuple);
                            roundRobinData.add(new Tuple<>(endpointDataBooleanTuple.getFirst(), true));
                            continue;
                        }

                        roundRobinData.remove(endpointDataBooleanTuple);
                    }
                } else {
                    sortedEndpointData.removeIf(endpointData -> endpointDataToRemove.stream().anyMatch(endpointDataBooleanTuple -> endpointData.equals(endpointDataBooleanTuple.getFirst())));

                    // Mark the one being used as satisfied.
                    try {
                        EndpointData data = sortedEndpointData.get(0);

                        if (data != null) {
                            roundRobinData.removeIf(endpointDataBooleanTuple -> endpointDataBooleanTuple.getFirst().equals(data));
                            roundRobinData.add(new Tuple<>(data, true));
                        }
                    } catch (Exception e) {
                        // Not a big deal... eat the exception.
                    }
                }
            }
        }

        WorldNetworkPath path = null;
        if (sortedEndpointData.isEmpty()) {
            if (this.network.equals(entryPoint.getNetwork())) {
                return false;
            }
        } else {
            PathNode node = sortedEndpointData.get(0).node;
            while (node.from != null) {
                node = node.from;
            }

            node.from = new PathNode(null, entryPoint, null);
            path = WorldNetworkPath.createPath(this, sortedEndpointData.get(0));
        }

        this.activePath = path;
        this.previousNode = path.next();
        this.currentNode = path.next();
        this.nextNode = path.next();
        this.travelledDistance = -0.25F;
        this.currentNode.registerTraveller(this);

        return true;
    }

    public boolean isValidEndpoint(WorldNetworkTraveller traveller, BlockPos from, BlockPos endPoint) {
        EnumFacing face = getFacingFromVector(endPoint.subtract(from));
        ImmutablePair<WorldNetworkNode, EnumFacing> endpoint =
                new ImmutablePair<>(network.getNode(endPoint, face.getOpposite()),
                        face.getOpposite());
        return !traveller.triedEndpoints.contains(endpoint)
                && network.isNodePresent(endPoint)
                && network.getNode(endPoint, face.getOpposite()).isEndpoint()
                && network.getNode(endPoint, face.getOpposite()).canAcceptTraveller(traveller, getFacingFromVector(from.subtract(endPoint)));
    }

    @Override
    public void update() {
        if (!network.isNodePresent(currentNode.getPosition())) {
            // Unregister before dropping because drop actions will empty our nbt.
            this.network.unregisterTraveller(this, false, true);
            dropActions.values().forEach(action -> action.dropToWorld(WorldNetworkTraveller.this));
            return;
        }

        if (!currentNode.isLoaded())
            return;

        if (travelledDistance >= 0.5F) {
            if (!network.isNodePresent(nextNode.getPosition()) || !nextNode.isEndpoint() && !nextNode.canAcceptTraveller(this, getFacingVector())) {
                EnumFacing injectionFace = getFacingFromVector(activePath.getEnd().realNode.getPosition().subtract(activePath.getEnd().from.realNode.getPosition())).getOpposite();
                triedEndpoints.add(new ImmutablePair<>(activePath.getEnd().realNode, injectionFace));
                quickRepath();
            } else if (travelledDistance >= 1F) {
                if (nextNode.isEndpoint()) {
                    if (travelledDistance >= 1.25F) {
                        travelledDistance = 0F;
                        EnumFacing injectionFace = getFacingFromVector(nextNode.getPosition().subtract(currentNode.getPosition())).getOpposite();
                        boolean didInject = ((WorldNetworkEndpoint) nextNode).inject(this, injectionFace);

                        if (!didInject) {
                            new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.getWorld(), this.currentNode.getPosition());
                            triedEndpoints.add(new ImmutablePair<>(nextNode, injectionFace));
                            previousNode.unregisterTraveller(this);
                            currentNode.unregisterTraveller(this);
                            genPath(true);
                            currentNode.registerTraveller(this);
                            travelledDistance = -1.15F;
                            TravellerDataMessage message = new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.getPosition(), previousNode.getPosition());
                            message.travelledDistance = travelledDistance;
                            message.sendToAllWatching(this.network.getWorld(), this.currentNode.getPosition());
                        } else {
                            network.unregisterTraveller(this, false, true);
                        }
                    }
                } else if (nextNode.getPosition().equals(activePath.getEnd().realNode.getPosition())) {
                    if (travelledDistance >= 1.25F) {
                        previousNode.unregisterTraveller(this);
                        currentNode.unregisterTraveller(this);
                        genPath(true);
                        new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.getWorld(), currentNode.getPosition());
                        travelledDistance = -1.1F;
                        TravellerDataMessage message = new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.getPosition(), previousNode.getPosition());
                        message.travelledDistance = travelledDistance;
                        message.sendToAllWatching(this.network.getWorld(), this.currentNode.getPosition());
                    }
                } else {
                    travelledDistance = 0;
                    previousNode = currentNode;
                    currentNode = nextNode;
                    nextNode = activePath.next();

                    previousNode.unregisterTraveller(this);
                    currentNode.registerTraveller(this);
                }
            }
        }

        travelledDistance += 1F / 10F;
    }

    public void quickRepath() {
        previousNode.unregisterTraveller(this);
        currentNode.unregisterTraveller(this);
        genPath(true);
        currentNode.registerTraveller(this);
        new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.getWorld(), currentNode.getPosition());
        if (travelledDistance > 0.5F)
            travelledDistance = 0.5F;
        TravellerDataMessage message = new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.getPosition(), previousNode.getPosition());
        message.travelledDistance = travelledDistance;
        message.sendToAllWatching(this.network.getWorld(), this.currentNode.getPosition());
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setFloat("travelled", travelledDistance);
        tagCompound.setTag("data", data);
        tagCompound.setLong("entrypoint", entryPoint.getPosition().toLong());
        tagCompound.setLong("curnode", currentNode.getPosition().toLong());
        tagCompound.setLong("prevnode", previousNode.getPosition().toLong());
        tagCompound.setLong("nextnode", nextNode.getPosition().toLong());

        tagCompound.setInteger("entrypointface", entryPoint.getCapabilityFace() == null ? -1
                : entryPoint.getCapabilityFace().getIndex());
        tagCompound.setInteger("curnodeface", currentNode.getCapabilityFace() == null ? -1
                : currentNode.getCapabilityFace().getIndex());
        tagCompound.setInteger("prevnodeface", previousNode.getCapabilityFace() == null ? -1
                : previousNode.getCapabilityFace().getIndex());
        tagCompound.setInteger("nextnodeface", nextNode.getCapabilityFace() == null ? -1
                : nextNode.getCapabilityFace().getIndex());

        tagCompound.setInteger("tried", triedEndpoints.size());
        for (int i = 0; i < triedEndpoints.size(); i++) {
            ImmutablePair<WorldNetworkNode, EnumFacing> triedEndpoint = triedEndpoints.get(i);
            if (triedEndpoint.getLeft() == null)
                continue;
            tagCompound.setLong("triedp" + i, triedEndpoint.getLeft().getPosition().toLong());
            tagCompound.setInteger("triedf" + i, triedEndpoint.getRight().getIndex());
            tagCompound.setInteger("triedcf" + i, triedEndpoint.getLeft().getCapabilityFace() == null ? -1
                    : triedEndpoint.getLeft().getCapabilityFace().getIndex());
        }

        tagCompound.setInteger("actions", dropActions.size());
        for (int i = 0; i < dropActions.size(); i++) {
            tagCompound.setString("action" + i, (String) dropActions.keySet().toArray()[i]);
        }

        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        EnumFacing entryPointFace = nbt.getInteger("entrypointface") > -1 ? EnumFacing.values()[nbt.getInteger("entrypointface")] : null;
        EnumFacing prevNodeFace = nbt.getInteger("prevnodeface") > -1 ? EnumFacing.values()[nbt.getInteger("prevnodeface")] : null;
        EnumFacing curNodeFace = nbt.getInteger("curnodeface") > -1 ? EnumFacing.values()[nbt.getInteger("curnodeface")] : null;
        EnumFacing nextNodeFace = nbt.getInteger("nextnodeface") > -1 ? EnumFacing.values()[nbt.getInteger("nextnodeface")] : null;

        BlockPos entryPointPos = BlockPos.fromLong(nbt.getLong("entrypoint"));
        BlockPos prevNodePos = BlockPos.fromLong(nbt.getLong("prevnode"));
        BlockPos curNodePos = BlockPos.fromLong(nbt.getLong("curnode"));
        BlockPos nextNodePos = BlockPos.fromLong(nbt.getLong("nextnode"));

        travelledDistance = nbt.getFloat("travelled");
        data = nbt.getCompoundTag("data");
        entryPoint = (WorldNetworkEntryPoint) network.getNode(entryPointPos, entryPointFace);
        previousNode = network.getNode(prevNodePos, prevNodeFace);
        currentNode = network.getNode(curNodePos, curNodeFace);
        nextNode = network.getNode(nextNodePos, nextNodeFace);

        for (int i = 0; i < nbt.getInteger("tried"); i++) {
            EnumFacing triedCF = nbt.getInteger("triedcf" + i) > -1 ? EnumFacing.values()[nbt.getInteger("triedcf")] : null;
            triedEndpoints.add(new ImmutablePair<>(network.getNode(BlockPos.fromLong(nbt.getLong("triedp" + i)), triedCF),
                    EnumFacing.values()[data.getInteger("triedf")]));
        }

        for (int i = 0; i < nbt.getInteger("actions"); i++) {
            String key = nbt.getString("action" + i);
            if (!dropActions.containsKey(key)) {
                dropActions.put(key, DropActions.ACTIONS.get(key));
            }
        }
    }

    @Override
    public WorldNetworkTraveller clone() {
        WorldNetworkTraveller traveller = new WorldNetworkTraveller(this.data.copy());
        traveller.data.setUniqueId("id", UUID.randomUUID());
        traveller.network = network;
        traveller.currentNode = currentNode;
        traveller.activePath = activePath;
        traveller.entryPoint = entryPoint;
        traveller.nextNode = nextNode;
        traveller.previousNode = previousNode;
        traveller.travelledDistance = travelledDistance;
        traveller.triedEndpoints.addAll(triedEndpoints);

        return traveller;
    }

    public void moveTo(IWorldNetwork newNetwork) {
        IWorldNetwork prevNetwork = this.network;

        prevNetwork.unregisterTraveller(this, true, false);
        this.network = newNetwork;
        this.network.registerTraveller(this, false);

        if (!network.isNodePresent(entryPoint.getPosition())) {
            //TODO: Handle purgatory.
        }

        if (!network.isNodePresent(currentNode.getPosition())) {
            dropActions.values().forEach(action -> action.dropToWorld(WorldNetworkTraveller.this));
            prevNetwork.unregisterTraveller(this, true, false);
            this.network.unregisterTraveller(data, true, false);
            return;
        } else {
        }
        if (!network.isNodePresent(nextNode.getPosition())) {
            genPath(true);
            return;
        }

        TravellerMoveMessage message = new TravellerMoveMessage(this);
        message.sendToAllWatching(network.getWorld(), currentNode.getPosition());
    }
}
