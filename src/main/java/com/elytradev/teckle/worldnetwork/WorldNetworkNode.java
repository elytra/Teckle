package com.elytradev.teckle.worldnetwork;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode {

    // Empty node, used instead of null because fuck NPEs.
    public static final WorldNetworkNode NONE = new WorldNetworkNode();

    public BlockPos position;
    public WorldNetwork network;
    private List<WorldNetworkTraveller> travellers;

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }

    public boolean isEndpoint() {
        return this instanceof WorldNetworkEndpoint;
    }

    public boolean isEntrypoint() {
        return this instanceof WorldNetworkEntryPoint;
    }
}
