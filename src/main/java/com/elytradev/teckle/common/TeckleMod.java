package com.elytradev.teckle.common;

import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.proxy.CommonProxy;
import com.elytradev.teckle.common.worldgen.NikoliteOreGenerator;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
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
    public static final boolean INDEV = (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    @Mod.Instance()
    public static TeckleMod INSTANCE;

    @CapabilityInject(IProbeDataProvider.class)
    public static Capability<?> PROBE_CAPABILITY;
    public static Logger LOG;

    @SidedProxy(serverSide = "com.elytradev.teckle.common.proxy.CommonProxy", clientSide = "com.elytradev.teckle.client.proxy.ClientProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        PROXY.registerHandlers();
        LOG = e.getModLog();
        OBJECTS.preInit(e);
        CapabilityWorldNetworkTile.register();
        TeckleNetworking.setupNetwork();
        DropActions.init();
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

        AlloyRecipes.getInstance().init();
    }
}
