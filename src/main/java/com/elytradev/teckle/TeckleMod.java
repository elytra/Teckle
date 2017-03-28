package com.elytradev.teckle;

import com.elytradev.teckle.block.BlockItemTube;
import com.elytradev.teckle.tile.TileItemTube;
import com.elytradev.teckle.worldnetwork.WorldNetwork;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static com.elytradev.teckle.TeckleMod.MOD_ID;
import static com.elytradev.teckle.TeckleMod.MOD_NAME;

/**
 * Created by darkevilmac on 3/25/2017.
 */
@Mod(modid = MOD_ID, name = MOD_NAME)
public class TeckleMod {

    public static final String MOD_ID = "teckle";
    public static final String MOD_NAME = "Teckle";
    public static final String MOD_VER = "";

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        //noinspection deprecation
        GameRegistry.registerWithItem(new BlockItemTube(Material.CIRCUITS));
        GameRegistry.registerTileEntity(TileItemTube.class, "teckle.itemTube");

        MinecraftForge.EVENT_BUS.register(WorldNetwork.class);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent e) {

    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent e) {

    }

}
