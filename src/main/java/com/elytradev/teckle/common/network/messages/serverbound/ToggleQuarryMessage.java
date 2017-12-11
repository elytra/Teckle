package com.elytradev.teckle.common.network.messages.serverbound;

import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.block.BlockBeamQuarry;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.TileBeamQuarry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.SERVER)
public class ToggleQuarryMessage extends TeckleMessage {

    public BlockPos quarryPos;
    public boolean activateQuarry;

    public ToggleQuarryMessage() {
    }

    public ToggleQuarryMessage(BlockPos quarryPos, boolean activateQuarry) {
        this.quarryPos = quarryPos;
        this.activateQuarry = activateQuarry;
    }

    @Override
    protected void handle(EntityPlayer player) {
        if (player == null)
            return;
        World world = player.world;
        if (world.getTileEntity(quarryPos) instanceof TileBeamQuarry) {
            TileBeamQuarry beamQuarry = (TileBeamQuarry) world.getTileEntity(quarryPos);
            if (beamQuarry.isUsableByPlayer(player)) {
                world.setBlockState(quarryPos, world.getBlockState(quarryPos).withProperty(BlockBeamQuarry.ACTIVE, activateQuarry));
                ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(TeckleMod.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
                for (ChunkPos chunkPos : beamQuarry.chunksInBounds()) {
                    ForgeChunkManager.forceChunk(ticket, chunkPos);
                }
            }
        }
    }
}
