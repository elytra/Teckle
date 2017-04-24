package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.teckle.common.tile.base.TileNetworkMember;
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
    public WorldNetwork network;
    private HashMap<UUID, WorldNetworkTraveller> travellers = new HashMap<>();

    protected WorldNetworkNode() {
        this.position = new BlockPos(0, -1, 0);
        this.network = null;
    }

    public WorldNetworkNode(WorldNetwork network, BlockPos position) {
        this.position = position;
        this.network = network;
    }

    public boolean isLoaded() {
        if (network == null || network.world == null)
            return false;
        return network.world.isBlockLoaded(position);
    }

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (isLoaded()) {
            return getTile().canAcceptTraveller(traveller, from);
        }
        return isLoaded();
    }

    public TileNetworkMember getTile() {
        if (network.world.getTileEntity(position) instanceof TileNetworkMember)
            return (TileNetworkMember) network.world.getTileEntity(position);
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
