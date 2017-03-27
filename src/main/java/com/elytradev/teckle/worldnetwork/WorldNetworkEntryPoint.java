package com.elytradev.teckle.worldnetwork;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * A node used to add travellers to a network, handles initial endpoint finding, as well as finding new endpoints when one fails.
 */
public class WorldNetworkEntryPoint extends WorldNetworkNode {

    private EnumFacing facing;
    private int roundRobinTicker;

    public void createInitialPathForTraveller(WorldNetworkTraveller traveller) {
        BlockPos startPos = this.position.add(facing.getDirectionVec());

        if(!network.networkNodes.containsKey(startPos))
            return;


    }

    public void findNodeForTraveller(WorldNetworkTraveller traveller) {

    }

}
