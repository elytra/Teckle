package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.common.network.TravellerDataMessage;
import com.elytradev.teckle.common.network.TravellerMoveMessage;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.EndpointData;
import com.elytradev.teckle.common.worldnetwork.common.pathing.PathNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.WorldNetworkPath;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

/**
 * A piece of tagCompound travelling to a node in the network.
 */
public class WorldNetworkTraveller implements ITickable, INBTSerializable<NBTTagCompound> {

    public static final WorldNetworkTraveller NONE = new WorldNetworkTraveller(new NBTTagCompound());

    public WorldNetwork network;
    public WorldNetworkNode previousNode = WorldNetworkNode.NONE, currentNode = WorldNetworkNode.NONE, nextNode = WorldNetworkNode.NONE;
    public WorldNetworkPath activePath;
    public float travelledDistance = 0F;
    // The current distance travelled between our previous node, and the increment node.
    public NBTTagCompound data;
    public List<Tuple<WorldNetworkNode, EnumFacing>> triedEndpoints = new ArrayList<>();
    public HashMap<String, IDropAction> dropActions = new HashMap<>();
    protected WorldNetworkEntryPoint entryPoint;

    public WorldNetworkTraveller(NBTTagCompound data) {
        this.entryPoint = null;

        this.data = data;
        if (!this.data.hasKey("idLeast"))
            this.data.setUniqueId("id", UUID.randomUUID());
    }

    protected WorldNetworkTraveller(WorldNetworkEntryPoint entryPoint, NBTTagCompound data) {
        this.network = entryPoint.network;
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

    public WorldNetworkEntryPoint getEntryPoint() {
        return entryPoint;
    }

    public EnumFacing getFacingVector() {
        return getFacingFromVector(nextNode.position.subtract(currentNode.position)).getOpposite();
    }

    public void genPath(boolean attemptReroute) {
        List<PathNode> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();

        nodeStack.add(new PathNode(null, currentNode));
        while (!nodeStack.isEmpty()) {
            PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pathNode.realNode.position.add(direction.getDirectionVec());
                if (!network.isNodePresent(neighbourPos) ||
                        iteratedPositions.contains(neighbourPos) ||
                        (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite()))) {
                    continue;
                }

                WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                if (neighbourNode.canAcceptTraveller(this, direction.getOpposite())) {
                    if (!endpoints.containsKey(neighbourPos)) {
                        endpoints.put(neighbourPos, new HashMap<>());
                    }
                    if (isValidEndpoint(this, pathNode.realNode.position, neighbourPos)) {
                        endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData(new PathNode(pathNode, network.getNodeFromPosition(neighbourPos)), direction.getOpposite()));
                    } else if (entryPoint.position.equals(neighbourPos) && entryPoint.network.equals(network)) {
                        PathNode nextNode = new PathNode(pathNode, entryPoint.endpoint);
                        nextNode.cost = Integer.MAX_VALUE;
                        endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData(nextNode, direction.getOpposite()));
                    } else {
                        nodeStack.add(new PathNode(pathNode, network.getNodeFromPosition(neighbourPos)));
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

        if (sortedEndpointData.isEmpty() && attemptReroute) {
            WorldNetworkPath lastPath = this.activePath;
            triedEndpoints.clear();
            genPath(false);
            if (this.activePath.equals(lastPath)) {
                nodeStack = new ArrayList<>();
                iteratedPositions = new ArrayList<>();
                endpoints = new HashMap<>();

                nodeStack.add(new PathNode(null, currentNode));
                while (!nodeStack.isEmpty()) {
                    PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
                    for (EnumFacing direction : EnumFacing.VALUES) {
                        BlockPos neighbourPos = pathNode.realNode.position.add(direction.getDirectionVec());
                        if ((!network.isNodePresent(neighbourPos) ||
                                iteratedPositions.contains(neighbourPos) ||
                                (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite())))) {
                            continue;
                        }

                        WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                        PathNode neighbourPathNode = new PathNode(pathNode, neighbourNode);
                        if (neighbourNode.canAcceptTraveller(this, direction.getOpposite())) {
                            if (!endpoints.containsKey(neighbourPos)) {
                                endpoints.put(neighbourPos, new HashMap<>());
                            }

                            if (!isValidEndpoint(this, pathNode.realNode.position, neighbourPos)) {
                                nodeStack.add(new PathNode(pathNode, network.getNodeFromPosition(neighbourPos)));
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
        this.network = currentNode.network;
    }

    /**
     * Attempt to generate an initial path for this item, return false if not possible.
     *
     * @return
     */

    public boolean genInitialPath() {
        BlockPos startPos = this.entryPoint.position.add(entryPoint.getFacing().getDirectionVec());
        if (!network.isNodePresent(startPos))
            return false;

        List<PathNode> nodeStack = new ArrayList<>();
        List<BlockPos> iteratedPositions = new ArrayList<>();
        HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();

        nodeStack.add(new PathNode(null, network.getNodeFromPosition(startPos)));
        while (!nodeStack.isEmpty()) {
            PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
            for (EnumFacing direction : EnumFacing.VALUES) {
                BlockPos neighbourPos = pathNode.realNode.position.add(direction.getDirectionVec());
                if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(entryPoint.position) ||
                        iteratedPositions.contains(neighbourPos) ||
                        (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite()))) {
                    continue;
                }

                WorldNetworkNode neighbourNode = network.getNodeFromPosition(neighbourPos);
                if (neighbourNode.canAcceptTraveller(this, direction.getOpposite())) {
                    if (isValidEndpoint(this, pathNode.realNode.position, neighbourPos)) {
                        if (!endpoints.containsKey(neighbourPos)) {
                            endpoints.put(neighbourPos, new HashMap<>());
                        }
                        endpoints.get(neighbourPos).put(direction.getOpposite(), new EndpointData(new PathNode(pathNode, network.getNodeFromPosition(neighbourPos)), direction.getOpposite()));
                    } else {
                        nodeStack.add(new PathNode(pathNode, network.getNodeFromPosition(neighbourPos)));
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

        WorldNetworkPath path = null;
        if (sortedEndpointData.isEmpty()) {
            if (this.network.equals(entryPoint.network)) {
                return false;
            }
        } else {
            PathNode node = sortedEndpointData.get(0).node;
            while (node.from != null) {
                node = node.from;
            }

            node.from = new PathNode(null, entryPoint);
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
        return !traveller.triedEndpoints.contains(new Tuple<>(network.getNodeFromPosition(endPoint), getFacingFromVector(endPoint.subtract(from))))
                && network.isNodePresent(endPoint)
                && network.getNodeFromPosition(endPoint).isEndpoint()
                && network.getNodeFromPosition(endPoint).canAcceptTraveller(traveller, getFacingFromVector(from.subtract(endPoint)));
    }

    @Override
    public void update() {
        if (!network.isNodePresent(currentNode.position)) {
            // Unregister before dropping because drop actions will empty our nbt.
            this.network.unregisterTraveller(this, false, true);
            dropActions.values().forEach(action -> action.dropToWorld(WorldNetworkTraveller.this));
            return;
        }

        if (!currentNode.isLoaded())
            return;

        if (travelledDistance >= 0.5F) {
            if (!network.isNodePresent(nextNode.position) || (!nextNode.isEndpoint() && !nextNode.canAcceptTraveller(this, getFacingVector()))) {
                EnumFacing injectionFace = getFacingFromVector(activePath.getEnd().realNode.position.subtract(activePath.getEnd().from.realNode.position)).getOpposite();
                triedEndpoints.add(new Tuple<>(activePath.getEnd().realNode, injectionFace));
                previousNode.unregisterTraveller(this);
                currentNode.unregisterTraveller(this);
                genPath(true);
                currentNode.registerTraveller(this);
                new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.world, currentNode.position);
                travelledDistance = 0.5F;
                TravellerDataMessage message = new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.position, previousNode.position);
                message.travelledDistance = travelledDistance;
                message.sendToAllWatching(this.network.world, this.currentNode.position);
            } else if (travelledDistance >= 1F) {
                if (nextNode.isEndpoint()) {
                    if (travelledDistance >= 1.25F) {
                        travelledDistance = 0F;
                        EnumFacing injectionFace = getFacingFromVector(nextNode.position.subtract(currentNode.position)).getOpposite();
                        boolean didInject = ((WorldNetworkEndpoint) nextNode).inject(this, injectionFace);

                        if (!didInject) {
                            new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.world, this.currentNode.position);
                            triedEndpoints.add(new Tuple<>(nextNode, injectionFace));
                            previousNode.unregisterTraveller(this);
                            currentNode.unregisterTraveller(this);
                            genPath(true);
                            currentNode.registerTraveller(this);
                            travelledDistance = -1.15F;
                            TravellerDataMessage message = new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.position, previousNode.position);
                            message.travelledDistance = travelledDistance;
                            message.sendToAllWatching(this.network.world, this.currentNode.position);
                        } else {
                            network.unregisterTraveller(this, false, true);
                        }
                    }
                } else if (nextNode.position.equals(activePath.getEnd().realNode.position)) {
                    if (travelledDistance >= 1.25F) {
                        previousNode.unregisterTraveller(this);
                        currentNode.unregisterTraveller(this);
                        genPath(true);
                        new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.world, currentNode.position);
                        travelledDistance = -1.1F;
                        TravellerDataMessage message = new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.position, previousNode.position);
                        message.travelledDistance = travelledDistance;
                        message.sendToAllWatching(this.network.world, this.currentNode.position);
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

        travelledDistance += (1F / 10F);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setFloat("travelled", travelledDistance);
        tagCompound.setTag("data", data);
        tagCompound.setLong("entrypoint", entryPoint.position.toLong());
        tagCompound.setLong("curnode", currentNode.position.toLong());
        tagCompound.setLong("prevnode", previousNode.position.toLong());
        tagCompound.setLong("nextnode", nextNode.position.toLong());

        tagCompound.setInteger("tried", triedEndpoints.size());
        for (int i = 0; i < triedEndpoints.size(); i++) {
            tagCompound.setLong("triedp" + i, triedEndpoints.get(i).getFirst().position.toLong());
            tagCompound.setInteger("triedf" + i, triedEndpoints.get(i).getSecond().getIndex());
        }

        tagCompound.setInteger("actions", dropActions.size());
        for (int i = 0; i < dropActions.size(); i++) {
            tagCompound.setString("action" + i, (String) dropActions.keySet().toArray()[i]);
        }

        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        travelledDistance = nbt.getFloat("travelled");
        data = nbt.getCompoundTag("data");
        entryPoint = (WorldNetworkEntryPoint) network.getNodeFromPosition(BlockPos.fromLong(nbt.getLong("entrypoint")));
        currentNode = network.getNodeFromPosition(BlockPos.fromLong(nbt.getLong("curnode")));
        previousNode = network.getNodeFromPosition(BlockPos.fromLong(nbt.getLong("prevnode")));
        nextNode = network.getNodeFromPosition(BlockPos.fromLong(nbt.getLong("nextnode")));

        for (int i = 0; i < nbt.getInteger("tried"); i++) {
            triedEndpoints.add(new Tuple<>(network.getNodeFromPosition(
                    BlockPos.fromLong(nbt.getLong("tried" + i))),
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
        WorldNetworkTraveller traveller = new WorldNetworkTraveller(this.data);
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

    public void moveTo(WorldNetwork newNetwork) {
        WorldNetwork prevNetwork = this.network;

        prevNetwork.unregisterTraveller(this, true, false);
        this.network = newNetwork;
        this.network.travellers.put(data, this);

        if (!network.isNodePresent(entryPoint.position)) {
            //TODO: Handle purgatory.
        }

        if (!network.isNodePresent(currentNode.position)) {
            dropActions.values().forEach(action -> action.dropToWorld(WorldNetworkTraveller.this));
            prevNetwork.unregisterTraveller(this, true, false);
            this.network.travellers.remove(data);
            return;
        } else {
        }
        if (!network.isNodePresent(nextNode.position)) {
            genPath(true);
            return;
        }

        TravellerMoveMessage message = new TravellerMoveMessage(this);
        message.sendToAllWatching(network.world, currentNode.position);
    }
}
