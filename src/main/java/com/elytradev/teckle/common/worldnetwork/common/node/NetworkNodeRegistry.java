package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraftforge.fml.common.event.FMLInterModComms;

import java.util.Objects;

/**
 * Handles all code related to the registration of Network Nodes, IMCs are forwarded here to be handled.
 */
public class NetworkNodeRegistry {

    // Kept private, use delegate methods.
    private static RegistryNamespaced<ResourceLocation, Class<? extends WorldNetworkNode>> REGISTRY;

    public static void handleIMCEvent(FMLInterModComms.IMCEvent e) {
        for (FMLInterModComms.IMCMessage msg : e.getMessages()) {
            if (msg.isNBTMessage() && Objects.equals(msg.getNBTValue().getString("type"), "RegisterNode"))
                parseIMCMessage(msg);
        }
    }

    private static void parseIMCMessage(FMLInterModComms.IMCMessage msg) {
        NBTTagCompound receivedData = msg.getNBTValue();
        ResourceLocation nodeKey = new ResourceLocation(msg.getSender(), receivedData.getString("id"));
        if (REGISTRY.containsKey(nodeKey)) {
            TeckleMod.LOG.warn("A mod attempted to register a network node with the same id twice, it will be skipped.");
            return;
        }
        Class<?> nodeClazz;
        try {
            nodeClazz = Class.forName(receivedData.getString("class"));
        } catch (ClassNotFoundException e) {
            TeckleMod.LOG.error("Attempted to register custom network node from a mod with the id {}", msg.getSender());
            TeckleMod.LOG.error("Failed to get class for network node with name {}", receivedData.getString("class"));
            e.printStackTrace();
            return;
        }
        try {
            nodeClazz.getConstructor(IWorldNetwork.class, BlockPos.class, EnumFacing.class);
        } catch (NoSuchMethodException e) {
            TeckleMod.LOG.error("Tried to register a world network node via IMC but received a class that does not have a constructor matching (IWorldNetwork, BlockPos, EnumFacing), it will be skipped. Mod: {}, Class: {}", msg.getSender(), nodeClazz.getName());
            return;
        }
        if (!WorldNetworkNode.class.isAssignableFrom(nodeClazz)) {
            TeckleMod.LOG.error("Tried to register a world network node via IMC but received a class that does not extend WorldNetworkNode, it will be skipped. Mod: {}, Class: {}", msg.getSender(), nodeClazz.getName());
            return;
        }


        registerNetworkNode(nodeKey, (Class<? extends WorldNetworkNode>) nodeClazz);
    }

    public static void registerNetworkNode(ResourceLocation id, Class<? extends WorldNetworkNode> clazz) {
        TeckleMod.LOG.info("Registering a network node with the following key {}", id);
        REGISTRY.putObject(id, clazz);
    }

    public static void registerNetworkNode(String modID, String nodeID, Class<? extends WorldNetworkNode> clazz) {
        registerNetworkNode(new ResourceLocation(modID, nodeID), clazz);
    }

    public static Class<? extends WorldNetworkNode> getNetworkNode(ResourceLocation id) {
        return REGISTRY.getObject(id);
    }

    public static ResourceLocation getNetworkNodeName(Class<? extends WorldNetworkNode> entry) {
        return REGISTRY.getNameForObject(entry);
    }
}
