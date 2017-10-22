package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.SERVER)
public class RetrieverSelectorModeChangeMessage extends Message {

    public BlockPos pos;
    public boolean useSelector;

    public RetrieverSelectorModeChangeMessage(NetworkContext ctx) {
        super(ctx);
    }

    public RetrieverSelectorModeChangeMessage(BlockPos pos, boolean useSelector) {
        super(TeckleNetworking.NETWORK);
        this.pos = pos;
        this.useSelector = useSelector;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileRetriever retriever = (TileRetriever) sender.world.getTileEntity(pos);
            if (!retriever.isUsableByPlayer(sender))
                return;
            retriever.setUseSelector(useSelector);
        }
    }
}
