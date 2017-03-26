package com.elytradev.teckle.worldnetwork;

import net.minecraft.util.ITickable;

/**
 * A piece of data travelling to a node in the network.
 */
public class WorldNetworkTraveller implements ITickable {

    public WorldNetworkNode previousNode, currentNode, nextNode, destinationNode;
    public float travelledDistance = 0F;

    //TODO: static method to find possible nodes to travel to.

    @Override
    public void update() {
    }
}
