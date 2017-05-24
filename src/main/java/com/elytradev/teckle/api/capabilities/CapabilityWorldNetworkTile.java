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

package com.elytradev.teckle.api.capabilities;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.impl.NetworkTileTransporter;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityWorldNetworkTile {

    @CapabilityInject(IWorldNetworkTile.class)
    public static Capability<IWorldNetworkTile> NETWORK_TILE_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IWorldNetworkTile.class, new Capability.IStorage<IWorldNetworkTile>() {
                    @Override
                    public NBTBase writeNBT(Capability<IWorldNetworkTile> capability, IWorldNetworkTile instance, EnumFacing side) {
                        return new NBTTagCompound();
                    }

                    @Override
                    public void readNBT(Capability<IWorldNetworkTile> capability, IWorldNetworkTile instance, EnumFacing side, NBTBase base) {
                    }
                },
                () -> new NetworkTileTransporter() {
                    @Override
                    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
                        return true;
                    }

                    @Override
                    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
                        return true;
                    }

                    @Override
                    public boolean canConnectTo(EnumFacing side) {
                        return true;
                    }
                });
    }

    /**
     * Checks if the tile in the specified world at the specified position has a network tile cap.
     *
     * @param world the world to check in.
     * @param pos   the position of the tile.
     * @return true if present, false otherwise.
     */
    public static boolean isPositionNetworkTile(IBlockAccess world, BlockPos pos) {
        return !(world.getTileEntity(pos) == null || !world.getTileEntity(pos).hasCapability(NETWORK_TILE_CAPABILITY, null));
    }

    public static boolean isTileNetworked(TileEntity tileEntity) {
        return !(tileEntity == null || !tileEntity.hasCapability(NETWORK_TILE_CAPABILITY, null));
    }

    public static IWorldNetworkTile getTileNetworked(TileEntity tileEntity) {
        return tileEntity.getCapability(NETWORK_TILE_CAPABILITY, null);
    }

    public static IWorldNetworkTile getNetworkTileAtPosition(IBlockAccess world, BlockPos pos) {
        return world.getTileEntity(pos).getCapability(NETWORK_TILE_CAPABILITY, null);
    }

}
