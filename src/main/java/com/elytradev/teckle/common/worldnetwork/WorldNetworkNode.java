package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode {

    // Empty node, used instead of null because fuck NPEs.
    public static final WorldNetworkNode NONE = new WorldNetworkNode();

    public BlockPos position;
    public WorldNetwork network;
    private List<WorldNetworkTraveller> travellers = new ArrayList<>();

    protected WorldNetworkNode() {
    }

    public WorldNetworkNode(WorldNetwork network, BlockPos position) {
        this.position = position;
        this.network = network;
    }

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return true;
    }

    public void registerTraveller(WorldNetworkTraveller traveller) {
        travellers.add(traveller);
    }

    public void unregisterTraveller(WorldNetworkTraveller traveller) {
        travellers.remove(traveller);
    }

    public boolean isEndpoint() {
        return this instanceof WorldNetworkEndpoint;
    }

    public boolean isEntrypoint() {
        return this instanceof WorldNetworkEntryPoint;
    }

    public List<WorldNetworkTraveller> getTravellers() {
        return travellers;
    }
}
