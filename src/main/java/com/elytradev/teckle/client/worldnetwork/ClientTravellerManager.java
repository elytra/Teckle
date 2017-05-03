package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Manages all travellers on this client, mostly for visuals.
 */
public class ClientTravellerManager {

    private static List<UUID> travellersToRemove = new ArrayList<>();
    private static HashMap<UUID, DummyNetworkTraveller> travellers = new HashMap<>();

    @SubscribeEvent
    public static void onTickEvent(TickEvent.ClientTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.END) || Minecraft.getMinecraft().world == null || Minecraft.getMinecraft().isGamePaused())
            return;

        for (DummyNetworkTraveller traveller : travellers.values()) {
            if (traveller.travelledDistance >= 1) {
                if (traveller.nextNode.isEndpoint() || traveller.nextNode == WorldNetworkNode.NONE) {
                    if (traveller.travelledDistance >= 1.25F)
                        travellersToRemove.add(traveller.data.getUniqueId("id"));
                } else {
                    traveller.travelledDistance = 0;
                    traveller.previousNode = traveller.currentNode;
                    traveller.currentNode = traveller.nextNode;
                    traveller.nextNode = traveller.activePath.next();

                    World clientWorld = Minecraft.getMinecraft().world;
                    TileEntity tileAtPrev = clientWorld.getTileEntity(traveller.previousNode.position);
                    TileEntity tileAtCur = clientWorld.getTileEntity(traveller.currentNode.position);

                    if (tileAtPrev != null && tileAtPrev instanceof TileNetworkMember)
                        ((TileNetworkMember) tileAtPrev).removeTraveller(traveller.data);
                    if (tileAtCur != null && tileAtCur instanceof TileNetworkMember)
                        ((TileNetworkMember) tileAtCur).addTraveller(traveller);
                }
            }

            traveller.travelledDistance += (1F / 10F);
        }

        for (UUID id : travellersToRemove) {
            DummyNetworkTraveller traveller = travellers.get(id);
            if (traveller == null)
                continue;
            World clientWorld = Minecraft.getMinecraft().world;
            TileEntity tileAtPrev = traveller.previousNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.previousNode.position) : null;
            TileEntity tileAtCur = traveller.currentNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.currentNode.position) : null;

            if (tileAtPrev instanceof TileNetworkMember)
                ((TileNetworkMember) tileAtPrev).removeTraveller(traveller.data);
            if (tileAtCur instanceof TileNetworkMember)
                ((TileNetworkMember) tileAtCur).removeTraveller(traveller.data);
        }
        travellersToRemove.forEach(tagCompound -> travellers.remove(tagCompound));
        travellersToRemove.clear();
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote) {
            travellers.clear();
            travellersToRemove.clear();
        }
    }

    public static void removeTraveller(NBTTagCompound data, boolean immediate) {
        if (!immediate) {
            travellersToRemove.add(data.getUniqueId("id"));
        } else {
            DummyNetworkTraveller traveller = travellers.remove(data.getUniqueId("id"));
            if (traveller == null)
                return;
            World clientWorld = Minecraft.getMinecraft().world;
            TileEntity tileAtPrev = traveller.previousNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.previousNode.position) : null;
            TileEntity tileAtCur = traveller.currentNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.currentNode.position) : null;

            if (tileAtPrev instanceof TileNetworkMember)
                ((TileNetworkMember) tileAtPrev).removeTraveller(traveller.data);
            if (tileAtCur instanceof TileNetworkMember)
                ((TileNetworkMember) tileAtCur).removeTraveller(traveller.data);
        }
    }

    public static DummyNetworkTraveller put(NBTTagCompound key, DummyNetworkTraveller value) {
        World clientWorld = Minecraft.getMinecraft().world;
        TileEntity tileAtCur = clientWorld.getTileEntity(value.currentNode.position);

        if (tileAtCur != null && tileAtCur instanceof TileNetworkMember)
            ((TileNetworkMember) tileAtCur).addTraveller(value);

        return travellers.put(key.getUniqueId("id"), value);
    }

    public static DummyNetworkTraveller get(UUID key) {
        return travellers.get(key);
    }
}
