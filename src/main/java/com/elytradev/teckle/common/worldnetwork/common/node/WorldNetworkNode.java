package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode {

    // Empty node, used instead of null because fuck NPEs.
    public static final WorldNetworkNode NONE = new WorldNetworkNode();
    public BlockPos position;
    public IWorldNetwork network;
    private HashMap<UUID, WorldNetworkTraveller> travellers = new HashMap<>();

    public WorldNetworkNode() {
        this.position = new BlockPos(0, -1, 0);
        this.network = null;
    }

    public WorldNetworkNode(IWorldNetwork network, BlockPos position) {
        this.position = position;
        this.network = network;
    }

    public boolean isLoaded() {
        if (network == null || network.getWorld() == null)
            return false;
        return network.getWorld().isBlockLoaded(position);
    }

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (isLoaded()) {
            if (getNetworkTile() != null)
                return getNetworkTile().canAcceptTraveller(traveller, from);
        }
        return isLoaded();
    }

    public IWorldNetworkTile getNetworkTile() {
        if (CapabilityWorldNetworkTile.isPositionNetworkTile(network.getWorld(), position))
            return CapabilityWorldNetworkTile.getNetworkTileAtPosition(network.getWorld(), position);
        else return null;
    }

    public void registerTraveller(WorldNetworkTraveller traveller) {
        travellers.put(traveller.data.getUniqueId("id"), traveller);
    }

    public void unregisterTraveller(WorldNetworkTraveller traveller) {
        travellers.remove(traveller.data.getUniqueId("id"));
    }

    public boolean isEndpoint() {
        return this instanceof WorldNetworkEndpoint;
    }

    public boolean isEntrypoint() {
        return this instanceof WorldNetworkEntryPoint;
    }

    public Collection<WorldNetworkTraveller> getTravellers() {
        return (travellers.values());
    }
}
