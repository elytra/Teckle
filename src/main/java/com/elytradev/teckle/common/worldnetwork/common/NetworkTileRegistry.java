package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInterModComms;

import java.util.Objects;

/**
 * Handles all code related to the registration of Network Tiles, IMCs are forwarded here to be handled.
 */
public class NetworkTileRegistry {

    // Kept private, use delegate methods.
    private static RegistryNamespaced<ResourceLocation, Class<? extends WorldNetworkTile>> REGISTRY;

    public static void handleIMCEvent(FMLInterModComms.IMCEvent e) {
        for (FMLInterModComms.IMCMessage msg : e.getMessages()) {
            if (msg.isNBTMessage() && Objects.equals(msg.getNBTValue().getString("type"), "RegisterTile"))
                parseIMCMessage(msg);
        }
    }

    private static void parseIMCMessage(FMLInterModComms.IMCMessage msg) {
        NBTTagCompound receivedData = msg.getNBTValue();
        ResourceLocation tileKey = new ResourceLocation(msg.getSender(), receivedData.getString("id"));
        if (REGISTRY.containsKey(tileKey)) {
            TeckleMod.LOG.warn("A mod attempted to register a network tile with the same id twice, it will be skipped.");
            return;
        }
        Class<?> tileClass;
        try {
            tileClass = Class.forName(receivedData.getString("class"));
        } catch (ClassNotFoundException exception) {
            TeckleMod.LOG.error("Attempted to register custom network tile from a mod with the id {}", msg.getSender());
            TeckleMod.LOG.error("Failed to get class for network tile with name {}", receivedData.getString("class"));
            exception.printStackTrace();
            return;
        }
        try {
            tileClass.getConstructor(World.class, BlockPos.class, EnumFacing.class);
        } catch (NoSuchMethodException e) {
            TeckleMod.LOG.error("Tried to register a world network tile via IMC but received a class that does not have a constructor matching (World, BlockPos, EnumFacing), it will be skipped. Mod: {}, Class: {}", msg.getSender(), tileClass.getName());
        }
        if (!WorldNetworkTile.class.isAssignableFrom(tileClass)) {
            TeckleMod.LOG.error("Tried to register a world network tile via IMC but received a class that does not extend WorldNetworkTile, it will be skipped. Mod: {}, Class: {}", msg.getSender(), tileClass.getName());
            return;
        }

        registerNetworkTile(tileKey, (Class<? extends WorldNetworkTile>) tileClass);
    }

    public static void registerNetworkTile(ResourceLocation id, Class<? extends WorldNetworkTile> clazz) {
        TeckleMod.LOG.info("Registering a network tile with the following key {}", id);
        REGISTRY.putObject(id, clazz);
    }

    public static void registerNetworkTile(String modID, String tileID, Class<? extends WorldNetworkTile> clazz) {
        registerNetworkTile(new ResourceLocation(modID, tileID), clazz);
    }

    public static Class<? extends WorldNetworkTile> getNetworkTile(ResourceLocation id) {
        return REGISTRY.getObject(id);
    }

    public static ResourceLocation getNetworkTileName(Class<? extends WorldNetworkTile> entry) {
        return REGISTRY.getNameForObject(entry);
    }

    public static void setup() {
        REGISTRY = new RegistryNamespaced<>();
    }
}
