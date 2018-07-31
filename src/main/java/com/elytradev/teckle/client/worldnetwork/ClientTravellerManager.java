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

package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
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
        if (e.phase == TickEvent.Phase.END || Minecraft.getMinecraft().world == null || Minecraft.getMinecraft().isGamePaused())
            return;

        // Move the travellers between tiles and increase their travelled distance.
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
                    TileEntity tileAtPrev = clientWorld.getTileEntity(traveller.previousNode.getPosition());
                    TileEntity tileAtCur = clientWorld.getTileEntity(traveller.currentNode.getPosition());

                    if (CapabilityWorldNetworkTile.isTileNetworked(tileAtPrev))
                        CapabilityWorldNetworkTile.getTileNetworked(tileAtPrev).removeClientTraveller(traveller.data);
                    if (CapabilityWorldNetworkTile.isTileNetworked(tileAtCur))
                        CapabilityWorldNetworkTile.getTileNetworked(tileAtCur).addClientTraveller(traveller);
                }
            }

            traveller.travelledDistance += (1F / 10F);
        }

        // Remove any queued travellers.
        for (UUID id : travellersToRemove) {
            DummyNetworkTraveller traveller = travellers.get(id);
            if (traveller == null)
                continue;
            World clientWorld = Minecraft.getMinecraft().world;
            TileEntity tileAtPrev = traveller.previousNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.previousNode.getPosition()) : null;
            TileEntity tileAtCur = traveller.currentNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.currentNode.getPosition()) : null;

            if (CapabilityWorldNetworkTile.isTileNetworked(tileAtPrev))
                CapabilityWorldNetworkTile.getTileNetworked(tileAtPrev).removeClientTraveller(traveller.data);
            if (CapabilityWorldNetworkTile.isTileNetworked(tileAtCur))
                CapabilityWorldNetworkTile.getTileNetworked(tileAtCur).removeClientTraveller(traveller.data);
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

    /**
     * Remove a traveller from the manager.
     *
     * @param data      the traveller data
     * @param immediate whether the traveller should be immediately destroyed,
     *                  or if it should enter the queue for the next tick.
     */
    public static void removeTraveller(NBTTagCompound data, boolean immediate) {
        if (!immediate) {
            travellersToRemove.add(data.getUniqueId("id"));
        } else {
            DummyNetworkTraveller traveller = travellers.remove(data.getUniqueId("id"));
            if (traveller == null)
                return;
            World clientWorld = Minecraft.getMinecraft().world;
            TileEntity tileAtPrev = traveller.previousNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.previousNode.getPosition()) : null;
            TileEntity tileAtCur = traveller.currentNode != WorldNetworkNode.NONE ? clientWorld.getTileEntity(traveller.currentNode.getPosition()) : null;

            if (CapabilityWorldNetworkTile.isTileNetworked(tileAtPrev))
                CapabilityWorldNetworkTile.getTileNetworked(tileAtPrev).removeClientTraveller(traveller.data);
            if (CapabilityWorldNetworkTile.isTileNetworked(tileAtCur))
                CapabilityWorldNetworkTile.getTileNetworked(tileAtCur).removeClientTraveller(traveller.data);
        }
    }

    /**
     * Add a traveller to the manager.
     *
     * @param key   the tag for the traveller.
     * @param value the traveller itself.
     * @return the traveller.
     */
    public static DummyNetworkTraveller put(NBTTagCompound key, DummyNetworkTraveller value) {
        World clientWorld = Minecraft.getMinecraft().world;
        TileEntity tileAtCur = clientWorld.getTileEntity(value.currentNode.getPosition());

        if (CapabilityWorldNetworkTile.isTileNetworked(tileAtCur))
            CapabilityWorldNetworkTile.getTileNetworked(tileAtCur).addClientTraveller(value);

        return travellers.put(key.getUniqueId("id"), value);
    }

    /**
     * Get a traveller based on its UUID.
     *
     * @param key the uuid of the traveller
     * @return the traveller if present.
     */
    public static DummyNetworkTraveller get(UUID key) {
        if (!travellers.containsKey(key))
            return null;

        return travellers.get(key);
    }
}
