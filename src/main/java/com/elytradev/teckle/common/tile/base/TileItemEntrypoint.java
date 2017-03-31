package com.elytradev.teckle.common.tile.base;

import net.minecraft.util.EnumFacing;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public class TileItemEntrypoint extends TileItemNetworkMember {

    private EnumFacing facing = EnumFacing.DOWN;

    public EnumFacing getFacing() {
        return facing;
    }
}
