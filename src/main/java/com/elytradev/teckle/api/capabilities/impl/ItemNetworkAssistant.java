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

package com.elytradev.teckle.api.capabilities.impl;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import com.google.common.collect.ImmutableMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The network assistant for the teckle item worldnetwork.
 */
public class ItemNetworkAssistant implements IWorldNetworkAssistant<ItemStack> {

    /**
     * Check if the block has any potential connections to tubes around it.
     *
     * @param pos the position of the tube to check around
     * @return a list of neighbouring networks.
     */
    public static List<IWorldNetwork> getNeighbourNetworks(IWorldNetworkTile networkTile, World world, BlockPos pos) {
        List<IWorldNetwork> neighbourNetworks = new ArrayList<>();
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());

            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourPos, facing.getOpposite()) && networkTile.canConnectTo(facing)) {
                IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourPos, facing.getOpposite());
                if (!neighbourNetworks.contains(neighbourNetworkTile.getNode().network))
                    neighbourNetworks.add(neighbourNetworkTile.getNode().network);
            }
        }

        return neighbourNetworks;
    }

    /**
     * Get a list of potential tiles to connect to.
     *
     * @return a list of tiles with an item handler or network tile.
     */
    public static List<TileEntity> getPotentialNeighbourNodes(IWorldNetworkTile networkTile, World world, BlockPos pos, boolean loadChunks) {
        List<TileEntity> neighbourNodes = new ArrayList<>();

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());
            if (!loadChunks && !world.isBlockLoaded(neighbourPos))
                continue;
            TileEntity neighbourTile = world.getTileEntity(neighbourPos);

            if (neighbourTile != null && networkTile.canConnectTo(facing)) {
                if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourPos, facing.getOpposite())) {
                    neighbourNodes.add(neighbourTile);
                } else if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                    neighbourNodes.add(neighbourTile);
                }
            }
        }

        return neighbourNodes;
    }

    @Override
    public void onNodePlaced(World world, BlockPos pos) {
        if (world.isRemote)
            return;

        for (EnumFacing facing : EnumFacing.VALUES) {
            IWorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, facing);
            if (thisNetworkTile == null)
                continue;

            List<IWorldNetwork> neighbourNetworks = getNeighbourNetworks(thisNetworkTile, world, pos);
            if (!neighbourNetworks.isEmpty()) {
                // Found neighbour networks, join the network or merge.
                IWorldNetwork network = neighbourNetworks.remove(0);
                thisNetworkTile.setNode(thisNetworkTile.createNode(network, pos));
                network.registerNode(thisNetworkTile.getNode());

                while (!neighbourNetworks.isEmpty()) {
                    network = network.merge(neighbourNetworks.remove(0));
                }
            } else {
                // No neighbours, make a new network.
                WorldNetwork network = new WorldNetwork(world, null);
                WorldNetworkDatabase.registerWorldNetwork(network);
                WorldNetworkNode node = thisNetworkTile.createNode(network, pos);
                network.registerNode(node);
                if (world.getTileEntity(pos) != null) {
                    thisNetworkTile.setNode(node);
                }
            }

            //Check for possible neighbour nodes...
            List<TileEntity> neighbourNodes = getPotentialNeighbourNodes(thisNetworkTile, world, pos, false);
            for (TileEntity neighbourTile : neighbourNodes) {
                BlockPos posDiff = pos.subtract(neighbourTile.getPos());
                EnumFacing capabilityFace = EnumFacing.getFacingFromVector(posDiff.getX(), posDiff.getY(), posDiff.getZ());

                if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                    if (!thisNetworkTile.getNode().network.isNodePresent(neighbourTile.getPos())) {
                        IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        thisNetworkTile.getNode().network.registerNode(neighbourNetworkTile.createNode(thisNetworkTile.getNode().network, neighbourTile.getPos()));
                        neighbourNetworkTile.setNode(thisNetworkTile.getNode().network.getNodeFromPosition(neighbourTile.getPos()));
                    }
                } else {
                    if (!thisNetworkTile.getNode().network.isNodePresent(neighbourTile.getPos())) {
                        thisNetworkTile.getNode().network.registerNode(new ItemNetworkEndpoint(thisNetworkTile.getNode().network, neighbourTile.getPos(), capabilityFace));
                    }
                }
            }

            if (thisNetworkTile.getCapabilityFace() == null)
                break;
        }
    }

    @Override
    public void onNodeNeighbourChange(World world, BlockPos pos, BlockPos neighbourPos) {
        if (world.isRemote)
            return;

        BlockPos posDiff = pos.subtract(neighbourPos);
        EnumFacing capabilityFace = EnumFacing.getFacingFromVector(posDiff.getX(), posDiff.getY(), posDiff.getZ());
        IWorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, capabilityFace.getOpposite());

        if (thisNetworkTile == null || thisNetworkTile.getNode().network == null)
            return;

        if (thisNetworkTile != null && thisNetworkTile.getNode() != null && thisNetworkTile.getNode().network != null
                && !thisNetworkTile.getNode().network.isNodePresent(neighbourPos)) {
            // Node not already present, check if we can add to network.
            if (world.getTileEntity(neighbourPos) != null) {
                TileEntity neighbourTile = world.getTileEntity(neighbourPos);
                if (neighbourTile != null) {
                    if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                            WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                        // Create endpoint and put it in the network.
                        ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(thisNetworkTile.getNode().network, neighbourPos, capabilityFace);
                        thisNetworkTile.getNode().network.registerNode(nodeEndpoint);
                    } else if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                        IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        if (neighbourNetworkTile.isValidNetworkMember(thisNetworkTile.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                            thisNetworkTile.getNode().network.registerNode(neighbourNetworkTile.createNode(thisNetworkTile.getNode().network, neighbourTile.getPos()));
                        }
                    }
                }
            }
        } else {
            if (world.getTileEntity(neighbourPos) == null && thisNetworkTile.getNode().network.isNodePresent(neighbourPos)) {
                thisNetworkTile.getNode().network.unregisterNodeAtPosition(neighbourPos);
            } else {
                TileEntity neighbourTile = world.getTileEntity(neighbourPos);
                if (!neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                    if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                        IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        if (neighbourNetworkTile.isValidNetworkMember(thisNetworkTile.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                            return;
                        }
                    }

                    thisNetworkTile.getNode().network.unregisterNodeAtPosition(neighbourPos);
                }
            }
        }
    }

    @Override
    public void onNodeBroken(World world, BlockPos pos) {
        if (world.isRemote)
            return;

        if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos)) {
            IWorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos);
            if (thisNetworkTile.getNode() != null) {
                thisNetworkTile.getNode().network.unregisterNodeAtPosition(pos);
                thisNetworkTile.getNode().network.validateNetwork();
                thisNetworkTile.setNode(null);
            }
        }
    }

    @Override
    public ItemStack insertData(WorldNetworkEntryPoint entryPoint, BlockPos insertInto, ItemStack insertData,
                                ImmutableMap<String, NBTBase> additionalData, boolean networksInsertionOnly, boolean simulate) {
        ItemStack remaining = insertData.copy();
        IWorldNetworkTile networkTile = entryPoint.getNetworkTile();
        World world = entryPoint.network.getWorld();
        if (networkTile.getNode() != null && networkTile.getNode().network != null
                && CapabilityWorldNetworkTile.isPositionNetworkTile(world, insertInto)) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("stack", insertData.serializeNBT());
            additionalData.forEach(tagCompound::setTag);
            WorldNetworkTraveller traveller = entryPoint.addTraveller(tagCompound, !simulate);
            if (simulate)
                entryPoint.network.unregisterTraveller(traveller, true, false);
            if (Objects.equals(traveller, WorldNetworkTraveller.NONE) || traveller == null) {
                return remaining.copy();
            } else {
                traveller.dropActions.put(DropActions.ITEMSTACK.getFirst(), DropActions.ITEMSTACK.getSecond());
                remaining = ItemStack.EMPTY;
            }
        }
        if (!remaining.isEmpty() && !networksInsertionOnly) {
            if (world.getTileEntity(insertInto) != null) {
                TileEntity insertionTile = world.getTileEntity(insertInto);
                EnumFacing insertFace = entryPoint.getNetworkTile().getOutputFace().getOpposite();
                if (insertionTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, insertFace)) {
                    IItemHandler insertionHandler = insertionTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, insertFace);

                    ItemStack remainingInserted = remaining.copy();
                    for (int i = 0; i < insertionHandler.getSlots(); i++) {
                        if (remainingInserted.isEmpty()) {
                            return remainingInserted;
                        }
                        remainingInserted = insertionHandler.insertItem(i, remainingInserted, simulate).copy();
                    }
                    remaining = remainingInserted;
                }
            }
        }

        return remaining;
    }
}
