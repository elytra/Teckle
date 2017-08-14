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
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

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
    public static List<IWorldNetwork> getNeighbourNetworks(WorldNetworkTile networkTile, World world, BlockPos pos) {
        List<IWorldNetwork> neighbourNetworks = new ArrayList<>();
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());

            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourPos, facing.getOpposite()) && networkTile.canConnectTo(facing)) {
                WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourPos, facing.getOpposite());
                if (!neighbourNetworks.contains(neighbourNetworkTile.getNode().getNetwork()))
                    neighbourNetworks.add(neighbourNetworkTile.getNode().getNetwork());
            }
        }

        return neighbourNetworks;
    }

    /**
     * Get a list of potential tiles to connect to.
     *
     * @return a list of tiles with an item handler or network tile.
     */
    public static List<TileEntity> getPotentialNeighbourNodes(WorldNetworkTile networkTile, World world, BlockPos pos, boolean loadChunks) {
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
            WorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, facing);
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
                    if (!thisNetworkTile.getNode().getNetwork().isNodePresent(neighbourTile.getPos())) {
                        WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        thisNetworkTile.getNode().getNetwork().registerNode(neighbourNetworkTile.createNode(thisNetworkTile.getNode().getNetwork(), neighbourTile.getPos()));
                        neighbourNetworkTile.setNode(thisNetworkTile.getNode().getNetwork().getNode(neighbourTile.getPos(), capabilityFace));
                    }
                } else {
                    if (!thisNetworkTile.getNode().getNetwork().isNodePresent(neighbourTile.getPos())) {
                        thisNetworkTile.getNode().getNetwork().registerNode(new ItemNetworkEndpoint(thisNetworkTile.getNode().getNetwork(), neighbourTile.getPos(), capabilityFace));
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
        WorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, capabilityFace.getOpposite());

        if (thisNetworkTile == null || thisNetworkTile.getNode() == null) {
            onNodePlaced(world, pos);
            return;
        }

        if (thisNetworkTile.getNode() != null && thisNetworkTile.getNode().getNetwork() != null && !thisNetworkTile.getNode().getNetwork().isNodePresent(neighbourPos)) {
            // Node not already present, check if we can add to network.
            if (world.getTileEntity(neighbourPos) != null) {
                TileEntity neighbourTile = world.getTileEntity(neighbourPos);
                if (neighbourTile != null) {
                    if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                            WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                        // Create endpoint and put it in the network.
                        ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(thisNetworkTile.getNode().getNetwork(), neighbourPos, capabilityFace);
                        thisNetworkTile.getNode().getNetwork().registerNode(nodeEndpoint);
                    } else if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                        WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        if (neighbourNetworkTile.isValidNetworkMember(thisNetworkTile.getNode().getNetwork(), WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                            thisNetworkTile.getNode().getNetwork().registerNode(neighbourNetworkTile.createNode(thisNetworkTile.getNode().getNetwork(), neighbourTile.getPos()));
                        }
                    }
                }
            }
        } else {
            if (world.getTileEntity(neighbourPos) == null) {
                thisNetworkTile.getNode().getNetwork().unregisterNodeAtPosition(neighbourPos, capabilityFace);
            } else {
                TileEntity neighbourTile = world.getTileEntity(neighbourPos);
                if (!neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                    if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                        WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        if (neighbourNetworkTile.isValidNetworkMember(thisNetworkTile.getNode().getNetwork(), WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                            return;
                        }
                    }

                    thisNetworkTile.getNode().getNetwork().unregisterNodeAtPosition(neighbourPos, capabilityFace);
                }
            }
        }
    }

    @Override
    public void onNodeBroken(World world, BlockPos pos) {
        if (world.isRemote)
            return;

        for (int i = -1; i < EnumFacing.values().length; i++) {
            EnumFacing facing = i > -1 ? EnumFacing.values()[i] : null;
            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos, facing)) {
                WorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, facing);
                if (thisNetworkTile.getNode() != null && !(i > -1 && thisNetworkTile.getCapabilityFace() == null)) {
                    thisNetworkTile.getNode().getNetwork().unregisterNodeAtPosition(pos, facing);
                    thisNetworkTile.getNode().getNetwork().validateNetwork();
                    thisNetworkTile.setNode(null);
                }
            }
        }
    }

    @Nonnull
    @Override
    public ItemStack insertData(WorldNetworkEntryPoint entryPoint, BlockPos insertInto, ItemStack insertData, ImmutableMap<String, NBTBase> additionalData, BiPredicate<WorldNetworkNode, EnumFacing> endpointPredicate, boolean networksInsertionOnly, boolean simulate) {
        ItemStack remaining = insertData.copy();
        WorldNetworkTile networkTile = entryPoint.getNetworkTile();
        World world = entryPoint.getNetwork().getWorld();
        if (networkTile.getNode() != null && networkTile.getNode().getNetwork() != null
                && CapabilityWorldNetworkTile.isPositionNetworkTile(world, insertInto)) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("stack", insertData.serializeNBT());
            additionalData.forEach(tagCompound::setTag);
            WorldNetworkTraveller traveller = entryPoint.addTraveller(tagCompound, !simulate);
            traveller.setEndpointPredicate(endpointPredicate);
            if (simulate)
                entryPoint.getNetwork().unregisterTraveller(traveller, true, false);
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