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
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.common.network.TravellerDataMessage;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A node used to add travellers to a network, handles initial endpoint finding, as well as finding new endpoints when one fails.
 */
public class WorldNetworkEntryPoint extends WorldNetworkNode {

    public WorldNetworkEndpoint endpoint = new WorldNetworkEndpoint(network, position) {
        @Override
        public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
            World world = network.getWorld();
            if (world.getTileEntity(position) != null) {
                TileEntity tileAtPos = world.getTileEntity(position);
                if (tileAtPos.hasCapability(CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY, null)
                        && tileAtPos.getCapability(CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY, null).getNode().isEntrypoint())
                    tileAtPos.getCapability(CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY, null).acceptReturn(traveller, from);
            }
            return true;
        }
    };
    private EnumFacing facing = EnumFacing.DOWN;

    public WorldNetworkEntryPoint(IWorldNetwork network, BlockPos position, EnumFacing facing) {
        this.network = network;
        this.position = position;
        this.facing = facing;

        this.endpoint.position = this.position;
        this.endpoint.network = this.network;
    }

    public WorldNetworkTraveller addTraveller(NBTTagCompound data) {
        WorldNetworkTraveller traveller = new WorldNetworkTraveller(this, data);
        if (traveller.genInitialPath()) {
            network.registerTraveller(traveller, false);
            new TravellerDataMessage(TravellerDataMessage.Action.REGISTER, traveller, traveller.currentNode.position,
                    traveller.previousNode.position).sendToAllWatching(network.getWorld(), position);
            return traveller;
        }

        return WorldNetworkTraveller.NONE;
    }

    public EnumFacing getFacing() {
        return facing;
    }

}