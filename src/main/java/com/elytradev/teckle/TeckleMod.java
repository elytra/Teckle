package com.elytradev.teckle;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

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

    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent e) {

    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent e) {

    }

}
