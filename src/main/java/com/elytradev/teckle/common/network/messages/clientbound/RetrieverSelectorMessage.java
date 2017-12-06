package com.elytradev.teckle.common.network.messages.clientbound;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.CLIENT)
public class RetrieverSelectorMessage extends TeckleMessage {

    @MarshalledAs("i8")
    public int selectorPos;
    public BlockPos retrieverPos;

    public RetrieverSelectorMessage(NetworkContext ctx) {
    }

    public RetrieverSelectorMessage(int selectorPos, BlockPos retrieverPos) {
        this.selectorPos = selectorPos;
        this.retrieverPos = retrieverPos;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileRetriever retriever = (TileRetriever) sender.world.getTileEntity(retrieverPos);
            if (!retriever.isUsableByPlayer(sender))
                return;

            retriever.setSelectorPos(selectorPos);
        }
    }
}