package com.elytradev.teckle.worldnetwork;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

import java.util.ArrayList;
import java.util.List;

/**
 * A piece of data travelling to a node in the network.
 */
public class WorldNetworkTraveller implements ITickable {

    private final WorldNetworkEntryPoint entryPoint;
    public WorldNetwork network;
    public WorldNetworkNode previousNode, currentNode, nextNode;
    public WorldNetworkPath activePath;
    // The current distance travelled between our previous node, and the next node.

    public float travelledDistance = 0F;
    public NBTTagCompound data;
    public List<WorldNetworkNode> triedEndpoints = new ArrayList<>();

    protected WorldNetworkTraveller(WorldNetworkEntryPoint entryPoint, NBTTagCompound data) {
        this.network = entryPoint.network;
        this.entryPoint = entryPoint;

        this.data = data;
    }

    @Override
    public void update() {
        if (travelledDistance >= 1) {
            if (nextNode.isEndpoint()) {
                boolean didInject = ((WorldNetworkEndpoint) nextNode).inject(this);

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
            }
        }

        travelledDistance += (1 / 20);
    }
}
