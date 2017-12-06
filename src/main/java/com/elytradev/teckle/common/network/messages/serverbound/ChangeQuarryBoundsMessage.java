package com.elytradev.teckle.common.network.messages.serverbound;

import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.network.messages.clientbound.TileUpdateMessage;
import com.elytradev.teckle.common.tile.TileBeamQuarry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;

import java.util.stream.Stream;

@ReceivedOn(Side.SERVER)
public class ChangeQuarryBoundsMessage extends TeckleMessage {

    public BlockPos quarryPos;
    @MarshalledAs("int")
    public int left, right, forward;

    public ChangeQuarryBoundsMessage() {
    }

    public ChangeQuarryBoundsMessage(BlockPos quarryPos, int length, int width, int depth) {
        this.quarryPos = quarryPos;
        this.left = length;
        this.right = width;
        this.forward = depth;
    }

    @Override
    protected void handle(EntityPlayer player) {
        if (player == null)
            return;

        if (Stream.of(left, right, forward).anyMatch(i -> i > 64 || i < 1)) {
            if (player != null && player.getGameProfile() != null)
                TeckleLog.warn("{} tried to set a quarry size greater than 64.", player.getGameProfile().toString());
        }

        World world = player.world;
        if (world != null && world.getTileEntity(quarryPos) instanceof TileBeamQuarry) {
            TileBeamQuarry quarry = (TileBeamQuarry) world.getTileEntity(quarryPos);
            quarry.setDimensions(left, right, forward);
            new TileUpdateMessage(world, quarryPos).sendToAllWatching(quarry);
        }
    }
}
