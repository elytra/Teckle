package com.elytradev.teckle.common.tile.base;

import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public abstract class TileNetworkEntrypoint extends TileNetworkMember {
    public abstract EnumFacing getFacing();

    @Override
    public boolean isValidNetworkMember(WorldNetwork network, EnumFacing side) {
        if (side.equals(getFacing()) && getNode() == null)
            return true;

        return false;
    }

    /**
     * Handles a traveller being returned to this filter after all destinations have been attempted.
     *
     * @param traveller
     * @param side
     */
    public abstract void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side);

}
