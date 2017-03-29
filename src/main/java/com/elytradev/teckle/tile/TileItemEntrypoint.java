package com.elytradev.teckle.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public class TileItemEntrypoint extends TileEntity{

    private EnumFacing facing = EnumFacing.DOWN;

    public EnumFacing getFacing() {
        return facing;
    }
}
