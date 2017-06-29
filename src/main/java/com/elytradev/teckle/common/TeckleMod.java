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

package com.elytradev.teckle.common;

import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkAssistantHolder;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.proxy.CommonProxy;
import com.elytradev.teckle.common.worldgen.NikoliteOreGenerator;
import mcmultipart.api.multipart.IMultipartTile;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

import static com.elytradev.teckle.common.TeckleMod.MOD_ID;
import static com.elytradev.teckle.common.TeckleMod.MOD_NAME;
import static com.elytradev.teckle.common.TeckleMod.MOD_VER;

@Mod(modid = MOD_ID, name = MOD_NAME, version = MOD_VER)
public class TeckleMod {
    public static final String MOD_ID = "teckle";
    public static final String MOD_NAME = "Teckle";
    public static final String MOD_VER = "%TVER%";
    public static final String RESOURCE_DOMAIN = "teckle:";
    public static final TeckleObjects OBJECTS = new TeckleObjects();
    public static final boolean INDEV = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    @Mod.Instance()
    public static TeckleMod INSTANCE;

    @CapabilityInject(IProbeDataProvider.class)
    public static Capability<?> PROBE_CAPABILITY;

    @CapabilityInject(IMultipartTile.class)
    public static Capability<?> MULTIPART_CAPABILITY;

    public static Logger LOG;

    @SidedProxy(serverSide = "com.elytradev.teckle.common.proxy.CommonProxy", clientSide = "com.elytradev.teckle.client.proxy.ClientProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        PROXY.registerHandlers();
        LOG = e.getModLog();
        MinecraftForge.EVENT_BUS.register(OBJECTS);
        CapabilityWorldNetworkTile.register();
        CapabilityWorldNetworkAssistantHolder.register();
        TeckleNetworking.setupNetwork();
        GameRegistry.registerWorldGenerator(new NikoliteOreGenerator(), 2);

        PROXY.registerRenderers(e.getModState());
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent e) {
        OBJECTS.init(e);
        PROXY.registerRenderers(e.getModState());
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent e) {
        OBJECTS.postInit(e);
        PROXY.registerRenderers(e.getModState());
    }
}
