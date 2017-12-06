package com.elytradev.teckle.common.network.messages.clientbound;

import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.CLIENT)
public class TileUpdateMessage extends TeckleMessage {

    public BlockPos tilePos;
    public NBTTagCompound tag;

    public TileUpdateMessage() {
    }

    public TileUpdateMessage(World world, BlockPos tilePos) {
        this.tilePos = tilePos;
        if (world.getTileEntity(tilePos) != null) {
            this.tag = world.getTileEntity(tilePos).getUpdateTag();
        }
    }

    @Override
    protected void handle(EntityPlayer player) {
        if (player != null && player.world.getTileEntity(tilePos) != null) {
            player.world.getTileEntity(tilePos).handleUpdateTag(tag);
        }
    }
}
