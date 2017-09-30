package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.network.messages.TileLitMessage;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TileLitNetworkMember extends TileNetworkMember implements ITickable {

    private boolean isLit;

    @Override
    public void update() {
        if (world.isRemote)
            return;

        if (isLit()) {
            if (!world.isBlockPowered(pos)) {
                setLit(false);
                new TileLitMessage(this).sendToAllWatching(this);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        setLit(compound.getBoolean("isLit"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("isLit", isLit());
        return super.writeToNBT(compound);
    }

    public void setTriggered() {
        if (!isLit()) {
            setLit(true);
            new TileLitMessage(this).sendToAllWatching(this);
        }
    }

    @Override
    public void onLoad() {
        if (world.isRemote) new TileLitMessage(this).sendToAllWatching(this);
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock() && super.shouldRefresh(world, pos, oldState, newSate);
    }

    public boolean isLit() {
        return isLit;
    }

    public void setLit(boolean lit) {
        isLit = lit;
    }
}
