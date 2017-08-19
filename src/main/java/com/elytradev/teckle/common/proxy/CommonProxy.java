/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.proxy;

import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.exception.MissingOreException;
import com.elytradev.teckle.common.handlers.TeckleGuiHandler;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Objects;


public class CommonProxy {

    public void registerRenderers(LoaderState.ModState state) {
    }

    public void registerHandlers() {
        NetworkRegistry.INSTANCE.registerGuiHandler(TeckleMod.INSTANCE, new TeckleGuiHandler());

        MinecraftForge.EVENT_BUS.register(WorldNetworkDatabase.class);
        MinecraftForge.EVENT_BUS.register(AdvancedStackHandlerPool.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerConnect(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player != null && Objects.equals(e.player.getGameProfile().getName(), "darkevilmac")) {
            TeckleMod.INDEV = true;
            TeckleLog.developerPlayer = e.player;
        }
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.player != null && Objects.equals(e.player.getGameProfile().getName(), "darkevilmac")) {
            TeckleMod.INDEV = false;
            TeckleLog.developerPlayer = null;
        }
    }

    public void handleMissingOres(String m) {
        throw new MissingOreException(m);
    }

}
