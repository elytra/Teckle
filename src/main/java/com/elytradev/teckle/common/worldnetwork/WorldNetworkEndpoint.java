package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Created by darkevilmac on 3/26/2017.
 */
public abstract class WorldNetworkEndpoint extends WorldNetworkNode {

    public WorldNetworkEndpoint(WorldNetwork network, BlockPos position) {
        super(network, position);
    }

    /**
     * Inject the traveller into the endpoint.
     *
     * @param traveller
     * @param from
     * @return if injection was successful, and no product is remaining.
     */
    public abstract boolean inject(WorldNetworkTraveller traveller, EnumFacing from);

}
