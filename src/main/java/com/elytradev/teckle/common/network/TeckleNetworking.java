package com.elytradev.teckle.common.network;

import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.teckle.common.TeckleMod;

/**
 * Created by darkevilmac on 3/31/2017.
 */
public class TeckleNetworking {

    public static final NetworkContext NETWORK = NetworkContext.forChannel(TeckleMod.MOD_ID);

    public static void setupNetwork() {
        NETWORK.register(TravellerDataMessage.class);
        NETWORK.register(TravellerMoveMessage.class);
        NETWORK.register(AlloyFurnaceMessage.class);

        NETWORK.register(FilterColourChangeMessage.class);
    }

}
