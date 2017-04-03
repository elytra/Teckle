package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.common.tile.base.TileItemNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
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

/**
 * Created by darkevilmac on 4/2/2017.
 */
public class ClientTravellerManager {

    public static HashMap<NBTTagCompound, DumbNetworkTraveller> travellers = new HashMap<>();

    @SubscribeEvent
    public static void onTickEvent(TickEvent.ClientTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.END) || Minecraft.getMinecraft().world == null)
            return;


        List<NBTTagCompound> travellersToRemove = new ArrayList<>();
        for (DumbNetworkTraveller traveller : travellers.values()) {
            if (traveller.travelledDistance >= 1) {
                if (traveller.nextNode.isEndpoint() || traveller.nextNode == WorldNetworkNode.NONE) {
                    World clientWorld = Minecraft.getMinecraft().world;
                    TileEntity tileAtPrev = clientWorld.getTileEntity(traveller.previousNode.position);

                    if (tileAtPrev != null && tileAtPrev instanceof TileItemNetworkMember)
                        ((TileItemNetworkMember) tileAtPrev).removeTraveller(traveller.data);
                    travellersToRemove.add(traveller.data);
                } else {
                    traveller.travelledDistance = 0;
                    traveller.previousNode = traveller.currentNode;
                    traveller.currentNode = traveller.nextNode;
                    traveller.nextNode = traveller.activePath.next();

                    World clientWorld = Minecraft.getMinecraft().world;
                    TileEntity tileAtPrev = clientWorld.getTileEntity(traveller.previousNode.position);
                    TileEntity tileAtCur = clientWorld.getTileEntity(traveller.currentNode.position);

                    if (tileAtPrev != null && tileAtPrev instanceof TileItemNetworkMember)
                        ((TileItemNetworkMember) tileAtPrev).removeTraveller(traveller.data);
                    if (tileAtCur != null && tileAtCur instanceof TileItemNetworkMember)
                        ((TileItemNetworkMember) tileAtCur).addTraveller(traveller);
                }
            }

            traveller.travelledDistance += (1F / 20F);
        }

        travellersToRemove.forEach(tagCompound -> travellers.remove(tagCompound));
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote) {
            travellers.clear();
        }
    }

}
