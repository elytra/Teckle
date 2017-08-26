package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class RetrieverEndpoint extends WorldNetworkEndpoint {
    public RetrieverEndpoint(IWorldNetwork network, BlockPos position, EnumFacing capabilityFace) {
        super(network, position, capabilityFace);
    }

    @Override
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        NetworkTileRetrieverInput retriever = (NetworkTileRetrieverInput) this.getNetworkTile();
        if (retriever == null)
            return false;
        ItemStack remaining = retriever.acceptTraveller(traveller);

        if (remaining == null)
            return false;

        if (remaining.isEmpty())
            return true;

        traveller.data.setTag("stack", remaining.serializeNBT());
        return false;
    }

}
