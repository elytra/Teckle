package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.teckle.common.network.TravellerDataMessage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A piece of tagCompound travelling to a node in the network.
 */
public class WorldNetworkTraveller implements ITickable {

    private final WorldNetworkEntryPoint entryPoint;
    public WorldNetwork network;
    public WorldNetworkNode previousNode, currentNode, nextNode;
    public WorldNetworkPath activePath;
    // The current distance travelled between our previous node, and the increment node.

    public float travelledDistance = 0F;
    public NBTTagCompound data;
    public List<EndpointData> triedEndpoints = new ArrayList<>();

    protected WorldNetworkTraveller(NBTTagCompound data) {
        this.entryPoint = null;

        this.data = data;
        this.data.setUniqueId("id", UUID.randomUUID());
    }

    protected WorldNetworkTraveller(WorldNetworkEntryPoint entryPoint, NBTTagCompound data) {
        this.network = entryPoint.network;
        this.entryPoint = entryPoint;

        this.data = data;
        this.data.setUniqueId("id", UUID.randomUUID());
    }

    public static EnumFacing getFacingFromVector(Vec3i vec) {
        for (EnumFacing facing : EnumFacing.VALUES)
            if (vec.equals(facing.getDirectionVec()))
                return facing;

        return EnumFacing.DOWN;
    }

    @Override
    public void update() {
        if (travelledDistance >= 1) {
            if (nextNode.isEndpoint()) {
                if (travelledDistance >= 1.25F) {
                    travelledDistance = 0F;
                    boolean didInject = ((WorldNetworkEndpoint) nextNode).inject(this,
                            getFacingFromVector(nextNode.position.subtract(currentNode.position)).getOpposite());

                    if (!didInject) {
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

        travelledDistance += (1F / 20F);
    }


}
