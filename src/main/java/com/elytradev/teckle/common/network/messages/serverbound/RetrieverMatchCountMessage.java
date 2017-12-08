package com.elytradev.teckle.common.network.messages.serverbound;

import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.SERVER)
public class RetrieverMatchCountMessage extends TeckleMessage {

    public BlockPos pos;
    public boolean matchCount;

    public RetrieverMatchCountMessage() {
    }

    public RetrieverMatchCountMessage(NetworkContext ctx) {
    }

    public RetrieverMatchCountMessage(BlockPos pos, boolean matchCount) {
        this.pos = pos;
        this.matchCount = matchCount;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileRetriever retriever = (TileRetriever) sender.world.getTileEntity(pos);
            if (!retriever.isUsableByPlayer(sender))
                return;
            retriever.setMatchCount(matchCount);
        }
    }
}
