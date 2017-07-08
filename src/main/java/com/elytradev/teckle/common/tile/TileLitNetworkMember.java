package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.network.messages.TileLitMessage;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

public class TileLitNetworkMember extends TileNetworkMember implements ITickable {

    public boolean isLit;

    @Override
    public void update() {
        if (world.isRemote)
            return;

        if (isLit) {
            if (!world.isBlockPowered(pos)) {
                isLit = false;
                new TileLitMessage(this).sendToAllWatching(this);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        isLit = compound.getBoolean("isLit");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("isLit", isLit);
        return super.writeToNBT(compound);
    }

    public void setTriggered() {
        if (!isLit) {
            isLit = true;
            new TileLitMessage(this).sendToAllWatching(this);
        }
    }

}
