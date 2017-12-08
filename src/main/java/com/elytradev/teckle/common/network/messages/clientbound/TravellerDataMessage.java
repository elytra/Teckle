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

package com.elytradev.teckle.common.network.messages.clientbound;

import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.WorldNetworkPath;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

/**
 * Adds or removes travellers and their paths to the client.
 */
@ReceivedOn(Side.CLIENT)
public class TravellerDataMessage extends TeckleMessage {

    private static final BlockPos IMPOSSIBLEPOS = new BlockPos(0, -1, 0);

    @MarshalledAs("float")
    public float travelledDistance = 0F;
    public NBTTagCompound data;
    public BlockPos prev, current;
    public WorldNetworkPath path;
    public Action action;

    public TravellerDataMessage(NetworkContext ctx) {
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller) {
        this.action = action;
        this.data = traveller.data;
        this.current = traveller.currentNode.getPosition();
        this.path = traveller.activePath;
        this.travelledDistance = traveller.travelledDistance;

        this.prev = IMPOSSIBLEPOS;

        // Prevents an NPE with concrete because it hates null :^)
        if (path == null)
            path = WorldNetworkPath.NOT_POSSIBLE;
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller, BlockPos current, BlockPos previous) {
        super();
        this.action = action;
        this.data = traveller.data;
        this.current = current;
        this.prev = previous;
        this.path = traveller.activePath;
        this.travelledDistance = traveller.travelledDistance;

        // Prevents an NPE with concrete because it hates null :^)
        if (path == null)
            path = WorldNetworkPath.NOT_POSSIBLE;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (Objects.equals(action, Action.REGISTER)) {
            DummyNetworkTraveller traveller = new DummyNetworkTraveller(data, path);
            traveller.activePath = path;
            if (!Objects.equals(prev, IMPOSSIBLEPOS)) {
                traveller.previousNode = path.next();
                traveller.currentNode = path.next();
                traveller.nextNode = path.next();
            } else {
                traveller.previousNode = new WorldNetworkNode(null, prev, null);
                traveller.currentNode = new WorldNetworkNode(null, current, null);
                traveller.nextNode = path.next();
            }

            traveller.travelledDistance = this.travelledDistance;

            ClientTravellerManager.put(data, traveller);
        } else if (Objects.equals(action, Action.UNREGISTER)) {
            ClientTravellerManager.removeTraveller(data, true);
        }
    }

    public enum Action {
        REGISTER,
        UNREGISTER,
        REREGISTER
    }
}
