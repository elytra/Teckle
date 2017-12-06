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

package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.network.messages.clientbound.TravellerDataMessage;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * A node used to add travellers to a network, handles initial endpoint finding, as well as finding new endpoints when one fails.
 */
public class WorldNetworkEntryPoint extends WorldNetworkNode {

    private WorldNetworkEndpoint endpoint = new WorldNetworkEndpoint(getNetwork(), getPosition(), getCapabilityFace()) {
        @Override
        public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
            WorldNetworkTile networkTile = WorldNetworkEntryPoint.this.getNetworkTile();
            networkTile.acceptReturn(traveller, from);
            return true;
        }
    };

    private EnumFacing outputFace = EnumFacing.DOWN;

    public WorldNetworkEntryPoint(IWorldNetwork network, BlockPos position, EnumFacing capabilityFace, EnumFacing outputFace) {
        this.setNetwork(network);
        this.setPosition(position);
        this.setCapabilityFace(capabilityFace);
        this.setOutputFace(outputFace);

        this.getEndpoint().setPosition(this.getPosition());
        this.getEndpoint().setNetwork(this.getNetwork());
    }

    public WorldNetworkTraveller addTraveller(NBTTagCompound data, boolean send) {
        WorldNetworkTraveller traveller = new WorldNetworkTraveller(this, data);
        if (traveller.genInitialPath()) {
            getNetwork().registerTraveller(traveller, false);
            if (send) {
                new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, traveller, traveller.currentNode.getPosition(),
                        traveller.previousNode.getPosition()).sendToAllWatching(getNetwork().getWorld(), getPosition());
            }
            return traveller;
        }

        return WorldNetworkTraveller.NONE;
    }

    public EnumFacing getOutputFace() {
        return outputFace;
    }

    public void setOutputFace(EnumFacing outputFace) {
        this.outputFace = outputFace;
    }

    public WorldNetworkEndpoint getEndpoint() {
        return endpoint;
    }
}