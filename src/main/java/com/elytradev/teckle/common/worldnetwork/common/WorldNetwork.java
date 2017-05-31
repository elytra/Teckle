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
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.network.messages.TravellerDataMessage;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldNetwork implements IWorldNetwork {

    public UUID id;
    public World world;

    protected HashMap<BlockPos, WorldNetworkNode> networkNodes = new HashMap<>();
    protected HashBiMap<NBTTagCompound, WorldNetworkTraveller> travellers = HashBiMap.create();
    private List<WorldNetworkTraveller> travellersToUnregister = new ArrayList<>();

    public WorldNetwork(World world, UUID id, boolean skipRegistration) {
        this.world = world;
        if (id == null) {
            this.id = UUID.randomUUID();
        } else {
            this.id = id;
        }
        if (!skipRegistration)
            WorldNetworkDatabase.registerWorldNetwork(this);
    }

    public WorldNetwork(World world, UUID id) {
        this(world, id, false);
    }

    @Override
    public void registerNode(WorldNetworkNode node) {
        TeckleMod.LOG.debug(this + "/Registering a node, " + node);
        if (!networkNodes.containsKey(node.position))
            networkNodes.put(node.position, node);
        else
            networkNodes.replace(node.position, node);
        node.network = this;
        TeckleMod.LOG.debug(this + "/Registered node, " + node);
    }

    @Override
    public void unregisterNode(WorldNetworkNode node) {
        unregisterNodeAtPosition(node.position);
    }

    @Override
    public void unregisterNodeAtPosition(BlockPos nodePosition) {
        TeckleMod.LOG.debug(this + "/Unregistering a node at, " + nodePosition);
        if (networkNodes.containsKey(nodePosition))
            networkNodes.remove(nodePosition);
        TeckleMod.LOG.debug(this + "/Unregistered node at, " + nodePosition);
    }

    @Override
    public WorldNetworkNode getNodeFromPosition(BlockPos pos) {
        return networkNodes.get(pos);
    }

    @Override
    public boolean isNodePresent(BlockPos nodePosition) {
        return networkNodes.containsKey(nodePosition);
    }

    @Override
    public Stream<WorldNetworkNode> nodeStream() {
        return networkNodes.values().stream();
    }

    @Override
    public List<WorldNetworkNode> getNodes() {
        return networkNodes.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<BlockPos> getNodePositions() {
        return Arrays.asList((BlockPos[]) networkNodes.keySet().toArray());
    }

    @Override
    public void registerTraveller(WorldNetworkTraveller traveller, boolean send) {
        traveller.network = this;
        travellers.put(traveller.data, traveller);

        if (send)
            new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, traveller).sendToAllWatching(world, traveller.currentNode.position);
    }

    @Override
    public void unregisterTraveller(WorldNetworkTraveller traveller, boolean immediate, boolean send) {
        if (!immediate) {
            travellersToUnregister.add(traveller);
        } else {
            travellers.remove(traveller.data);

            if (traveller.currentNode != null && getNodeFromPosition(traveller.currentNode.position) != null)
                getNodeFromPosition(traveller.currentNode.position).unregisterTraveller(traveller);
        }

        if (send) {
            new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, traveller).sendToAllWatching(world, traveller.currentNode.position);
        }
    }

    @Override
    public void unregisterTraveller(NBTTagCompound data, boolean immediate, boolean send) {
        if (!travellers.containsKey(data))
            return;

        WorldNetworkTraveller traveller = travellers.get(data);
        if (!immediate) {
            travellersToUnregister.add(travellers.get(data));
        } else {
            travellers.remove(data);
            if (traveller.currentNode != null && getNodeFromPosition(traveller.currentNode.position) != null)
                getNodeFromPosition(traveller.currentNode.position).unregisterTraveller(traveller);
        }

        if (send) {
            new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, traveller).sendToAllWatching(world, traveller.currentNode.position);
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public WorldNetwork merge(IWorldNetwork otherNetwork) {
        int expectedSize = networkNodes.size() + otherNetwork.getNodes().size();
        TeckleMod.LOG.debug("Performing a merge of " + this + " and " + otherNetwork
                + "\n Expecting a node count of " + expectedSize);
        WorldNetwork mergedNetwork = new WorldNetwork(this.world, null);
        this.transferNetworkData(mergedNetwork);
        otherNetwork.transferNetworkData(mergedNetwork);
        TeckleMod.LOG.debug("Completed merge, resulted in " + mergedNetwork);
        return mergedNetwork;
    }

    @Override
    public void transferNetworkData(IWorldNetwork to) {
        List<WorldNetworkTraveller> travellersToMove = new ArrayList<>();
        travellersToMove.addAll(this.travellers.values());
        this.travellers.clear();
        List<WorldNetworkNode> nodesToMove = new ArrayList<>();
        nodesToMove.addAll(this.networkNodes.values());

        for (WorldNetworkNode node : nodesToMove) {
            this.unregisterNode(node);
            to.registerNode(node);
        }

        for (WorldNetworkTraveller traveller : travellersToMove) {
            traveller.moveTo(to);
        }
    }

    @Override
    public void validateNetwork() {
        // Perform flood fill to validate all nodes are connected. Choose an arbitrary node to current from.

        TeckleMod.LOG.debug("Performing a network validation.");
        List<List<WorldNetworkNode>> networks = new ArrayList<>();
        HashMap<BlockPos, WorldNetworkNode> uncheckedNodes = new HashMap<>();
        uncheckedNodes.putAll(this.networkNodes);

        while (!uncheckedNodes.isEmpty()) {
            List<WorldNetworkNode> newNetwork = fillFromPos((BlockPos) uncheckedNodes.keySet().toArray()[0], uncheckedNodes);
            for (WorldNetworkNode checkedNode : newNetwork) {
                uncheckedNodes.remove(checkedNode.position);
            }
            networks.add(newNetwork);
        }

        // Only process a split if there's a new network that needs to be formed. RIP old network </3
        if (networks.size() > 1) {
            // Confirm all travellers that need to go are gone.
            for (WorldNetworkTraveller traveller : travellersToUnregister) {
                travellers.remove(traveller);
                getNodeFromPosition(traveller.currentNode.position).unregisterTraveller(traveller);
            }
            travellersToUnregister.clear();

            TeckleMod.LOG.debug("Splitting a network...");
            //Start from 1, leave 0 as this network.
            for (int networkNum = 1; networkNum < networks.size(); networkNum++) {
                List<WorldNetworkNode> newNetworkData = networks.get(networkNum);
                WorldNetwork newNetwork = new WorldNetwork(this.world, null);

                for (WorldNetworkNode node : newNetworkData) {
                    this.unregisterNode(node);
                    newNetwork.registerNode(node);
                }

                List<WorldNetworkTraveller> matchingTravellers = travellers.values().stream().filter(traveller -> newNetwork.isNodePresent(traveller.currentNode.position)).collect(Collectors.toList());
                for (WorldNetworkTraveller matchingTraveller : matchingTravellers) {
                    matchingTraveller.moveTo(newNetwork);
                }
            }
        }

        TeckleMod.LOG.info("Finished validation, resulted in " + networks.size() + " networks.\n Network sizes follow.");
        for (List<WorldNetworkNode> n : networks) {
            TeckleMod.LOG.debug(n.size());
        }
    }

    @Override
    public UUID getNetworkID() {
        return this.id;
    }

    private List<WorldNetworkNode> fillFromPos(BlockPos startAt, HashMap<BlockPos, WorldNetworkNode> knownNodes) {
        List<BlockPos> posStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        List<WorldNetworkNode> out = new ArrayList<>();

        posStack.add(startAt);
        iteratedPositions.add(startAt);
        while (!posStack.isEmpty()) {
            BlockPos pos = posStack.remove(0);
            if (knownNodes.containsKey(pos)) {
                TeckleMod.LOG.debug("Added " + pos + " to out.");
                out.add(knownNodes.get(pos));
            }

            for (EnumFacing direction : EnumFacing.VALUES) {
                if (!iteratedPositions.contains(pos.add(direction.getDirectionVec())) && knownNodes.containsKey(pos.add(direction.getDirectionVec()))) {
                    posStack.add(pos.add(direction.getDirectionVec()));
                    iteratedPositions.add(pos.add(direction.getDirectionVec()));
                }
            }
        }

        return out;
    }

    @Override
    public void update() {
        for (WorldNetworkTraveller traveller : travellers.values()) {
            traveller.update();
        }
        for (WorldNetworkTraveller traveller : travellersToUnregister) {
            if (traveller == null)
                continue;

            if (traveller.currentNode != WorldNetworkNode.NONE && isNodePresent(traveller.currentNode.position))
                getNodeFromPosition(traveller.currentNode.position).unregisterTraveller(traveller);
            travellers.inverse().remove(traveller);
        }

        travellersToUnregister.clear();
    }

    @Override
    public String toString() {
        return "WorldNetwork{" +
                "nodeCount=" + networkNodes.size() +
                ", travellerCount=" + travellers.size() +
                ", worldID=" + world.provider.getDimension() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldNetwork network = (WorldNetwork) o;
        return Objects.equals(networkNodes, network.networkNodes) &&
                Objects.equals(travellers, network.travellers) &&
                Objects.equals(world, network.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkNodes, travellers, world);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = new NBTTagCompound();

        compound.setUniqueId("id", id);

        // Serialize nodes first.
        compound.setInteger("nCount", networkNodes.size());
        List<WorldNetworkNode> nodes = getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            compound.setLong("n" + i, nodes.get(i).position.toLong());
            if (nodes.get(i).getNetworkTile().getCapabilityFace() != null)
                compound.setInteger("f" + i, nodes.get(i).getNetworkTile().getCapabilityFace().getIndex());
        }

        // Serialize travellers.
        int tCount = 0;
        for (int i = 0; i < travellers.size(); i++) {
            if (travellers.get(travellers.keySet().toArray()[i]) != null) {
                compound.setTag("t" + tCount, travellers.get(travellers.keySet().toArray()[i]).serializeNBT());
                tCount++;
            }
        }
        compound.setInteger("tCount", tCount);

        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound compound) {
        this.id = compound.getUniqueId("id");
        WorldNetworkDatabase.registerWorldNetwork(this);

        for (int i = 0; i < compound.getInteger("nCount"); i++) {
            BlockPos pos = BlockPos.fromLong(compound.getLong("n" + i));
            EnumFacing face = null;
            if (compound.hasKey("f" + i)) {
                face = EnumFacing.values()[compound.getInteger("f" + i)];
            }

            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos, face)) {
                IWorldNetworkTile networkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, face);
                WorldNetworkNode node = networkTile.createNode(this, pos);
                networkTile.setNode(node);
                registerNode(node);
            }
        }

        List<WorldNetworkTraveller> deserializedTravellers = new ArrayList<>();
        for (int i = 0; i < compound.getInteger("tCount"); i++) {
            WorldNetworkTraveller traveller = new WorldNetworkTraveller(new NBTTagCompound());
            traveller.network = this;
            traveller.deserializeNBT(compound.getCompoundTag("t" + i));
            deserializedTravellers.add(traveller);
        }

        for (WorldNetworkNode networkNode : Lists.newArrayList(networkNodes.values())) {
            if (networkNode.getNetworkTile() != null)
                networkNode.getNetworkTile().networkReloaded(this);
        }

        for (WorldNetworkTraveller traveller : deserializedTravellers) {
            traveller.genPath(true);
            this.registerTraveller(traveller, true);
        }
    }
}

