package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.teckle.common.network.TravellerDataMessage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A piece of tagCompound travelling to a node in the network.
 */
public class WorldNetworkTraveller implements ITickable, INBTSerializable<NBTTagCompound> {

    public WorldNetwork network;
    public WorldNetworkNode previousNode, currentNode, nextNode;
    public WorldNetworkPath activePath;
    public float travelledDistance = 0F;
    // The current distance travelled between our previous node, and the increment node.
    public NBTTagCompound data;
    public List<Tuple<WorldNetworkEndpoint, EnumFacing>> triedEndpoints = new ArrayList<>();
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

    public EnumFacing getFacingVector() {
        return getFacingFromVector(nextNode.position.subtract(currentNode.position)).getOpposite();
    }

    @Override
    public void update() {
        if (travelledDistance >= 0.5F) {
            if (!network.isNodePresent(nextNode.position) || (!nextNode.isEndpoint() && !nextNode.canAcceptTraveller(this, getFacingVector()))) {
                entryPoint.findNodeForTraveller(this);
                new TravellerDataMessage(TravellerDataMessage.Action.UNREGISTER, this).sendToAllWatching(network.world, currentNode.position);
                new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.position, previousNode.position)
                        .sendToAllWatching(this.network.world, this.currentNode.position);
            } else if (travelledDistance >= 1F) {
                if (nextNode.isEndpoint()) {
                    if (travelledDistance >= 1.25F) {
                        travelledDistance = 0F;
                        EnumFacing injectionFace = getFacingFromVector(nextNode.position.subtract(currentNode.position)).getOpposite();
                        boolean didInject = ((WorldNetworkEndpoint) nextNode).inject(this, injectionFace);

                        if (!didInject) {
                            triedEndpoints.add(new Tuple<>((WorldNetworkEndpoint) nextNode, injectionFace));
                            entryPoint.findNodeForTraveller(this);
                            new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, this, currentNode.position, previousNode.position).sendToAllWatching(this.network.world, this.currentNode.position);
                        } else {
                            network.unregisterTraveller(this);
                        }
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

        travelledDistance += (1F / 20F);
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
            triedEndpoints.add(new Tuple<>((WorldNetworkEndpoint) network.getNodeFromPosition(
                    BlockPos.fromLong(nbt.getLong("tried" + i))),
                    EnumFacing.values()[data.getInteger("triedf")]));
        }
    }
}
