package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Created by darkevilmac on 4/4/2017.
 */
public class DummyWorldNetworkEndpoint extends WorldNetworkEndpoint {
    public DummyWorldNetworkEndpoint(WorldNetwork network, BlockPos position) {
        super(network, position);
    }

    @Override
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }
}
