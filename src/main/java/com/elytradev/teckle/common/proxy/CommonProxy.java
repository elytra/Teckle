package com.elytradev.teckle.common.proxy;

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.handlers.TeckleGuiHandler;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkDatabase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public class CommonProxy {

    public void registerRenderers(LoaderState.ModState state) {
    }

    public void registerHandlers() {
        NetworkRegistry.INSTANCE.registerGuiHandler(TeckleMod.INSTANCE, new TeckleGuiHandler());

        MinecraftForge.EVENT_BUS.register(WorldNetworkDatabase.class);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
