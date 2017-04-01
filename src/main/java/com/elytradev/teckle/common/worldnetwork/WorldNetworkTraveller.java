package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

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

    protected WorldNetworkTraveller(WorldNetworkEntryPoint entryPoint, NBTTagCompound data) {
        this.network = entryPoint.network;
        this.entryPoint = entryPoint;

        this.data = data;
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
                boolean didInject = ((WorldNetworkEndpoint) nextNode).inject(this,
                        getFacingFromVector(nextNode.position.subtract(currentNode.position)).getOpposite());

                if (!didInject) {
                    entryPoint.findNodeForTraveller(this);
                } else {
                    network.unregisterTraveller(this);
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
