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

package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.pathing.WorldNetworkPath;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

/**
 * Created by darkevilmac on 4/9/2017.
 */
@ReceivedOn(Side.CLIENT)
public class TravellerMoveMessage extends Message {

    @MarshalledAs("int")
    public int pathIndex = -1;
    public NBTTagCompound data;
    public WorldNetworkPath path;


    public TravellerMoveMessage(NetworkContext ctx) {
        super(ctx);
    }

    public TravellerMoveMessage(WorldNetworkTraveller traveller) {
        super(TeckleNetworking.NETWORK);
        this.path = traveller.activePath;
        this.pathIndex = this.path.getIndex() - 1;
        this.data = traveller.data;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        UUID id = data.getUniqueId("id");
        if (ClientTravellerManager.get(id) == null)
            return;

        float travelledDistance = ClientTravellerManager.get(id).travelledDistance;
        path.setIndex(pathIndex);
        DummyNetworkTraveller traveller = new DummyNetworkTraveller(data, path);
        traveller.travelledDistance = travelledDistance;
        if (pathIndex < 0) {

        } else {
            traveller.previousNode = path.prev();
            traveller.currentNode = path.next();
            traveller.nextNode = path.next();
        }
        ClientTravellerManager.removeTraveller(data, true);
        ClientTravellerManager.put(data, traveller);
    }
}
