package com.elytradev.teckle.common.network.messages.serverbound;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.SERVER)
public class RetrieverColourChangeMessage extends TeckleMessage {

    public BlockPos pos;
    @MarshalledAs("i8")
    public int colour;

    public RetrieverColourChangeMessage(NetworkContext ctx) {
    }

    public RetrieverColourChangeMessage(BlockPos pos, EnumDyeColor colour) {
        this.pos = pos;
        this.colour = colour == null ? -1 : colour.getMetadata();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileRetriever retriever = (TileRetriever) sender.world.getTileEntity(pos);
            if (!retriever.isUsableByPlayer(sender))
                return;
            retriever.setColour(this.colour == -1 ? null : EnumDyeColor.byMetadata(this.colour));
        }
    }
}
