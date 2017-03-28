package com.elytradev.teckle.worldnetwork;

import net.minecraft.util.EnumFacing;

/**
 * Created by darkevilmac on 3/26/2017.
 */
public class WorldNetworkEndpoint extends WorldNetworkNode {

    /**
     * Inject the traveller into the endpoint.
     *
     * @param traveller
     * @param from
     * @return if injection was successful.
     */
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }

}
