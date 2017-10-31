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
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.helper.CapabilityHelper;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
            if (!networkTile.canConnectTo(facing))
                continue;

            BlockPos neighbourPos = pos.add(facing.getDirectionVec());
            EnumFacing capFace = facing.getOpposite();

            TileEntity tile = world.getTileEntity(neighbourPos);
            if (CapabilityWorldNetworkTile.isTileNetworked(tile, capFace)) {
                WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getTileNetworked(tile, capFace);
                if (neighbourNetworkTile.getNode() == null || neighbourNetworkTile.getNode().getNetwork() == null)
                    continue;
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
    public static List<TileEntity> getPotentialNeighbourNodes(WorldNetworkTile networkTile, boolean loadChunks) {
        List<TileEntity> neighbourNodes = new ArrayList<>();
        BlockPos pos = networkTile.getPos();
        World world = networkTile.getWorld();

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());
            if (!loadChunks && !world.isBlockLoaded(neighbourPos))
                continue;
            TileEntity neighbourTile = world.getTileEntity(neighbourPos);

            if (neighbourTile != null && networkTile.canConnectTo(facing)) {
                EnumFacing capabilityFace = WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos));
                if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourPos, facing.getOpposite())) {
                    neighbourNodes.add(neighbourTile);
                } else if (CapabilityHelper.isItemHandler(neighbourTile, capabilityFace)) {
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

        List<WorldNetworkTile> handledTiles = Lists.newArrayList();
        for (EnumFacing facing : EnumFacing.VALUES) {
            WorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, facing);
            if (thisNetworkTile == null || handledTiles.contains(thisNetworkTile))
                continue;

            handledTiles.add(thisNetworkTile);
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
            List<TileEntity> neighbourNodes = getPotentialNeighbourNodes(thisNetworkTile, false);
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
                    if (!thisNetworkTile.getNode().getNetwork().isNodePresent(neighbourTile.getPos(), capabilityFace)) {
                        thisNetworkTile.getNode().getNetwork().registerNode(new ItemNetworkEndpoint(thisNetworkTile.getNode().getNetwork(), neighbourTile.getPos(), capabilityFace));
                    }
                }
            }

            if (thisNetworkTile.getCapabilityFace() == null)
                break;
        }
    }

    @Override
    public void onNodeNeighbourChange(World world, BlockPos pos, BlockPos neighbour) {
        if (world.isRemote)
            return;

        BlockPos posDiff = pos.subtract(neighbour);
        EnumFacing face = EnumFacing.getFacingFromVector(posDiff.getX(), posDiff.getY(), posDiff.getZ());
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!CapabilityWorldNetworkTile.isTileNetworked(tileEntity, face.getOpposite()))
            return;
        WorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getTileNetworked(tileEntity, face.getOpposite());

        if (thisNetworkTile.getNode() == null) {
            onNodePlaced(world, pos);
            return;
        }

        WorldNetworkNode node = thisNetworkTile.getNode();
        IWorldNetwork network = node != null ? node.getNetwork() : null;

        if (node == null || network == null) return;

        if (!network.isNodePresent(neighbour)) {
            // Node not already present, check if we can add to network.
            addNode(world, neighbour, face, thisNetworkTile);
        } else {
            // A node is present, check if it's still valid in this network.
            checkNeighbourValidity(world, neighbour, face, thisNetworkTile);
        }
    }

    private void checkNeighbourValidity(World world, BlockPos neighbour, EnumFacing face, WorldNetworkTile networkTile) {
        if (world.getTileEntity(neighbour) == null) {
            networkTile.getNode().getNetwork().unregisterNodeAtPosition(neighbour, face);
        } else {
            TileEntity neighbourTile = world.getTileEntity(neighbour);
            if (!CapabilityHelper.isItemHandler(neighbourTile, face)) {
                if (CapabilityWorldNetworkTile.isTileNetworked(neighbourTile, face)) {
                    WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getTileNetworked(neighbourTile, face);
                    if (neighbourNetworkTile.isValidNetworkMember(networkTile.getNode().getNetwork(), face)) {
                        return;
                    }
                }

                networkTile.getNode().getNetwork().unregisterNodeAtPosition(neighbour, face);
            }
        }
    }

    private void addNode(World world, BlockPos neighbourPos, EnumFacing face, WorldNetworkTile networkTile) {
        IWorldNetwork network = networkTile.getNode().getNetwork();
        TileEntity neighbourTile = world.getTileEntity(neighbourPos);
        if (neighbourTile != null) {
            if (CapabilityHelper.isItemHandler(neighbourTile, face)) {
                // Create endpoint and put it in the network.
                ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(network, neighbourPos, face);
                network.registerNode(nodeEndpoint);
            } else if (CapabilityWorldNetworkTile.isTileNetworked(neighbourTile, face)) {
                // Create node from network tile and put it in the network.
                WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getTileNetworked(neighbourTile, face);
                if (neighbourNetworkTile.isValidNetworkMember(networkTile.getNode().getNetwork(), face)) {
                    WorldNetworkNode neighbourNode = neighbourNetworkTile.createNode(network, neighbourTile.getPos());
                    networkTile.getNode().getNetwork().registerNode(neighbourNode);
                }
            }
        }
    }

    @Override
    public void onNodeBroken(World world, BlockPos pos) {
        if (world.isRemote)
            return;

        // Check all faces including null to ensure that all nodes are removed properly.
        for (int i = -1; i < EnumFacing.values().length; i++) {
            EnumFacing facing = i > -1 ? EnumFacing.values()[i] : null;
            TileEntity tile = world.getTileEntity(pos);
            if (CapabilityWorldNetworkTile.isTileNetworked(tile, facing)) {
                WorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getTileNetworked(tile, facing);
                if (thisNetworkTile.getNode() != null && !(i > -1 && thisNetworkTile.getCapabilityFace() == null)) {
                    IWorldNetwork network = thisNetworkTile.getNode().getNetwork();
                    TeckleLog.debug("Found networktile on {} it is being removed.", network);
                    thisNetworkTile.getNode().getNetwork().unregisterNodeAtPosition(pos, facing);
                    thisNetworkTile.getNode().getNetwork().validateNetwork();
                    thisNetworkTile.setNode(null);
                }
            }
        }
    }

    @Nonnull
    @Override
    public ItemStack insertData(WorldNetworkEntryPoint entryPoint, BlockPos insertInto, ItemStack insertData,
                                ImmutableMap<String, NBTBase> additionalData,
                                BiPredicate<WorldNetworkNode, EnumFacing> endpointPredicate,
                                boolean networksInsertionOnly, boolean simulate) {
        if (entryPoint == null)
            return insertData.copy();

        ItemStack remaining = insertData.copy();
        IWorldNetwork network = entryPoint.getNetwork();
        World world = entryPoint.getNetwork().getWorld();
        EnumFacing insertFace = entryPoint.getOutputFace().getOpposite();
        if (entryPoint != null && network != null
                && network.isNodePresent(insertInto, insertFace)) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("stack", insertData.serializeNBT());
            additionalData.forEach(tagCompound::setTag);
            WorldNetworkTraveller traveller = entryPoint.addTraveller(tagCompound, !simulate);
            traveller.setEndpointPredicate(endpointPredicate);
            if (simulate)
                entryPoint.getNetwork().unregisterTraveller(traveller, true, false);
            if (Objects.equals(traveller, WorldNetworkTraveller.NONE) || traveller == null) {
                if (networksInsertionOnly)
                    return remaining.copy();
            } else {
                traveller.dropActions.put(DropActions.ITEMSTACK.getFirst(), DropActions.ITEMSTACK.getSecond());
                remaining = ItemStack.EMPTY;
            }
        }
        if (!remaining.isEmpty() && !networksInsertionOnly) {
            if (world.getTileEntity(insertInto) != null) {
                TileEntity insertionTile = world.getTileEntity(insertInto);
                IItemHandler insertionHandler = CapabilityHelper.getItemHandler(insertionTile, insertFace);
                if (insertionHandler != null) {
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