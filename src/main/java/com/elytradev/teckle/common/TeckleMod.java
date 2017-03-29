package com.elytradev.teckle.common;

import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.Logger;

import static com.elytradev.teckle.common.TeckleMod.MOD_ID;
import static com.elytradev.teckle.common.TeckleMod.MOD_NAME;

/**
 * Created by darkevilmac on 3/25/2017.
 */
@Mod(modid = MOD_ID, name = MOD_NAME)
public class TeckleMod {

    public static final String MOD_ID = "teckle";
    public static final String MOD_NAME = "Teckle";
    public static final String MOD_VER = "";
    public static final String RESOURCE_DOMAIN = "teckle:";
    public static final TeckleObjects OBJECTS = new TeckleObjects();


    public static Logger LOG;

    @SidedProxy(serverSide = "com.elytradev.teckle.common.CommonProxy", clientSide = "com.elytradev.teckle.client.ClientProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        LOG = e.getModLog();
        OBJECTS.preInit(e);

        MinecraftForge.EVENT_BUS.register(WorldNetwork.class);
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
