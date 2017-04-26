package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.base.TileNetworkEntrypoint;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.util.EnumFacing;

/**
 * Created by darkevilmac on 4/25/2017.
 */
public class TileSortingMachine extends TileNetworkEntrypoint {
    @Override
    public EnumFacing getFacing() {
        return null;
    }

    @Override
    public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {

    }

    @Override
    public WorldNetworkNode getNode(WorldNetwork network) {
        return null;
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return false;
    }
}
