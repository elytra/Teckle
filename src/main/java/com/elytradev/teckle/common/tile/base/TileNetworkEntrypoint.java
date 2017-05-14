package com.elytradev.teckle.common.tile.base;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.util.EnumFacing;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public abstract class TileNetworkEntrypoint extends TileNetworkMember {
    public abstract EnumFacing getFacing();

    @Override
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return side.equals(getFacing());
    }

    /**
     * Handles a traveller being returned to this filter after all destinations have been attempted.
     *
     * @param traveller
     * @param side
     */
    public abstract void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side);

}
