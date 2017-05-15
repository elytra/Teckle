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

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.handlers.TeckleGuiHandler;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
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
