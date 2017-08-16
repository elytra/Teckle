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
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
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

    @CapabilityInject(WorldNetworkTile.class)
    public static Capability<WorldNetworkTile> NETWORK_TILE_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(WorldNetworkTile.class, new Capability.IStorage<WorldNetworkTile>() {
                    @Override
                    public NBTBase writeNBT(Capability<WorldNetworkTile> capability, WorldNetworkTile instance, EnumFacing side) {
                        return new NBTTagCompound();
                    }

                    @Override
                    public void readNBT(Capability<WorldNetworkTile> capability, WorldNetworkTile instance, EnumFacing side, NBTBase base) {
                    }
                },
                () -> new WorldNetworkTile(null) {
                    @Override
                    public NBTBase serializeNBT() {
                        return null;
                    }

                    @Override
                    public void deserializeNBT(NBTBase nbt) {

                    }

                    @Override
                    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
                        return false;
                    }

                    @Override
                    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
                        return null;
                    }

                    @Override
                    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
                        return false;
                    }

                    @Override
                    public boolean canConnectTo(EnumFacing side) {
                        return false;
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
        return isPositionNetworkTile(world, pos, null);
    }

    public static boolean isPositionNetworkTile(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return world.getTileEntity(pos) != null && isTileNetworked(world.getTileEntity(pos), face);
    }

    public static boolean isTileNetworked(TileEntity tileEntity, EnumFacing face) {
        return tileEntity != null && tileEntity.hasCapability(NETWORK_TILE_CAPABILITY, face);
    }

    public static boolean isTileNetworked(TileEntity tileEntity) {
        return isTileNetworked(tileEntity, null);
    }

    public static WorldNetworkTile getTileNetworked(TileEntity tileEntity) {
        return getTileNetworked(tileEntity, null);
    }

    public static WorldNetworkTile getTileNetworked(TileEntity tileEntity, EnumFacing face) {
        return tileEntity.getCapability(NETWORK_TILE_CAPABILITY, face);
    }

    public static WorldNetworkTile getNetworkTileAtPosition(IBlockAccess world, BlockPos pos) {
        return getNetworkTileAtPosition(world, pos, null);
    }

    public static WorldNetworkTile getNetworkTileAtPosition(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return world.getTileEntity(pos).getCapability(NETWORK_TILE_CAPABILITY, face);
    }

}
