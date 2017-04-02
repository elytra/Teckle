package com.elytradev.teckle.common.tile.base;

import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import net.minecraft.util.EnumFacing;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public abstract class TileItemEntrypoint extends TileItemNetworkMember {
    public abstract EnumFacing getFacing();

    @Override
    public boolean isValidNetworkMember(WorldNetwork network, EnumFacing side) {
        if (side.equals(getFacing()) && getNode() == null)
            return true;

        return false;
    }
}
