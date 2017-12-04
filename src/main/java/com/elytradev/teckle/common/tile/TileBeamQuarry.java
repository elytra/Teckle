package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public class TileBeamQuarry extends TileNetworkMember {

    public BlockPos min = pos, max = pos;

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        min = BlockPos.fromLong(compound.getLong("min"));
        max = BlockPos.fromLong(compound.getLong("max"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setLong("min", min.toLong());
        tag.setLong("max", max.toLong());

        return super.writeToNBT(tag);
    }
}
