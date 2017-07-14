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

package com.elytradev.teckle.common.tile;

import com.elytradev.probe.api.IProbeData;
import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.probe.api.impl.ProbeData;
import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.impl.NetworkTileTransporter;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockTransposer;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.PositionImpl;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by darkevilmac on 4/21/2017.
 */
public class TileTransposer extends TileNetworkMember implements ITickable {

    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(9);
    private NetworkTileTransporter networkTile = new NetworkTileTransporter() {

        @Override
        public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
            return side.equals(getOutputFace());
        }


        @Override
        public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
            return new WorldNetworkEntryPoint(network, pos, getOutputFace(), getCapabilityFace());
        }

        @Override
        public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
            if (traveller.getEntryPoint().position.equals(TileTransposer.this.pos))
                return true;

            return from.equals(getOutputFace().getOpposite()) && !TileTransposer.this.isPowered();
        }

        @Override
        public boolean canConnectTo(EnumFacing side) {
            return side.equals(getOutputFace()) || side.getOpposite().equals(getOutputFace());
        }


        @Override
        public EnumFacing getOutputFace() {
            if (world != null) {
                IBlockState thisState = world.getBlockState(pos);
                if (thisState.getBlock().equals(TeckleObjects.blockTransposer)) {
                    return thisState.getValue(BlockTransposer.FACING);
                }
            }

            return EnumFacing.DOWN;
        }

        @Override
        public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
            if (!traveller.data.hasKey("stack"))
                return; // wtf am I supposed to do with this???

            ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
            EnumFacing facing = getOutputFace();

            // Try and put it back where we found it.
            if (side.equals(getOutputFace())) {
                if (world.getTileEntity(pos.offset(facing.getOpposite())) != null) {
                    TileEntity pushTo = world.getTileEntity(pos.offset(facing.getOpposite()));
                    if (pushTo.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                        IItemHandler itemHandler = pushTo.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                        for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
                            stack = itemHandler.insertItem(slot, stack, false);
                        }
                    }
                }
            }
            if (!stack.isEmpty()) {
                // Spawn into the world I guess.
                ItemStack remaining = stack.copy();
                for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                    remaining = buffer.insertItem(i, remaining, false);
                }

                if (!remaining.isEmpty()) {
                    WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                    remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                    DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
                }
            }
        }
    };

    private int cooldown = 0;


    /**
     * Attempt to push to our network, by pulling from our input position.
     *
     * @return true if a push occurred, false otherwise.
     */
    public boolean tryPush() {
        boolean result = false;
        if (cooldown > 0)
            return result;
        try {
            TileEntity potentialInsertionTile = world.getTileEntity(pos.offset(networkTile.getOutputFace()));
            boolean destinationIsAir = world.isAirBlock(pos.offset(networkTile.getOutputFace()));
            boolean hasInsertionDestination = potentialInsertionTile != null && ((networkTile.getNode() != null && networkTile.getNode().network != null)
                    || (potentialInsertionTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, networkTile.getOutputFace().getOpposite())));

            if (!world.isRemote && (hasInsertionDestination || destinationIsAir)) {
                WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) networkTile.getNode().network.getNodeFromPosition(pos);
                EnumFacing facing = networkTile.getOutputFace();

                ItemStack extractionData = getExtractionData(facing);

                if (!extractionData.isEmpty()) {
                    if (hasInsertionDestination) {
                        result = attemptInsertion(potentialInsertionTile, thisNode, extractionData);
                    } else {
                        result = ejectExtractionData(facing, extractionData);
                    }
                }

            }

        } catch (NullPointerException e) {
            boolean bool = networkTile == null;
            String debugInfo = "NTile " + (bool ? "null" : networkTile.toString());
            bool = bool || networkTile.getNode() == null;
            debugInfo += " node " + (bool ? "null" : networkTile.getNode().toString());
            bool = bool || networkTile.getNode().network == null;
            debugInfo += " network " + (bool ? "null" : networkTile.getNode().network.toString());
            TeckleMod.LOG.error("****************OH SHIT TECKLE BROKE*******************");
            TeckleMod.LOG.error("Caught NPE in tryPush!, {}", this);
            TeckleMod.LOG.error("Exception follows, {}", e);
            TeckleMod.LOG.error("Here's some useful debug info, {}", debugInfo);
            TeckleMod.LOG.error("****************OH SHIT TECKLE BROKE*******************");
        }
        cooldown = 10;
        return result;
    }

    private boolean ejectExtractionData(EnumFacing facing, ItemStack extractionData) {
        BlockSourceImpl coords = new BlockSourceImpl(world, pos);
        double d0 = coords.getX() + 0.7D * (double) networkTile.getOutputFace().getFrontOffsetX();
        double d1 = coords.getY() + 0.7D * (double) networkTile.getOutputFace().getFrontOffsetY();
        double d2 = coords.getZ() + 0.7D * (double) networkTile.getOutputFace().getFrontOffsetZ();
        BehaviorDefaultDispenseItem.doDispense(world, extractionData, 6, facing, new PositionImpl(d0, d1, d2));

        return true;
    }

    private boolean attemptInsertion(TileEntity potentialInsertionTile, WorldNetworkEntryPoint thisNode, ItemStack extractionData) {
        IWorldNetworkAssistant<ItemStack> networkAssistant = getNetworkAssistant(ItemStack.class);
        ItemStack remaining = networkAssistant.insertData(thisNode, potentialInsertionTile.getPos(), extractionData, ImmutableMap.of(), false, false).copy();

        if (!remaining.isEmpty()) {
            for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                remaining = buffer.insertItem(i, remaining, false);
            }

            if (!remaining.isEmpty()) {
                WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
            }
        }

        return remaining.isEmpty();
    }

    private ItemStack getExtractionData(EnumFacing facing) {
        ItemStack extractionData = ItemStack.EMPTY;

        // Check if the buffer is empty first...
        int bufferSlot = -1;
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                bufferSlot = i;
                break;
            }
        }

        if (bufferSlot != -1) {
            extractionData = buffer.extractItem(bufferSlot, 1, false);
        } else {
            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null && world.getTileEntity(pos.offset(facing.getOpposite()))
                    .hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                IItemHandler itemHandler = world.getTileEntity(pos.offset(facing.getOpposite()))
                        .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                    ItemStack extractTest = itemHandler.extractItem(slot, 1, true);
                    if (!extractTest.isEmpty())
                        extractionData = itemHandler.extractItem(slot, 1, false);
                }
            }
        }

        return extractionData;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        if (oldState.getBlock() == newSate.getBlock()) {
            return false;
        }

        return super.shouldRefresh(world, pos, oldState, newSate);
    }

    @Override
    public void update() {
        if (world.isRemote || networkTile.getNode() == null || networkTile.getNode().network == null)
            return;

        if (cooldown > 0) {
            cooldown--;
        }

        boolean canFitItems = world.isAirBlock(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec())) && canFitItemsInBuffer();
        if (canFitItems) {
            List<EntityItem> itemsToPickup = getItemsInBlockPos(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec()));
            if (world.getBlockState(pos).getValue(BlockTransposer.TRIGGERED) && world.isAirBlock(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec())
                    .add(networkTile.getOutputFace().getOpposite().getDirectionVec())))
                itemsToPickup.addAll(getItemsInBlockPos(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec())
                        .add(networkTile.getOutputFace().getOpposite().getDirectionVec())));

            for (EntityItem entityItem : itemsToPickup) {
                ItemStack entityStack = entityItem.getEntityItem().copy();

                for (int i = 0; i < buffer.getSlots() && !entityStack.isEmpty(); i++) {
                    entityStack = buffer.insertItem(i, entityStack, false);
                }

                entityItem.setEntityItemStack(entityStack);
                if (entityStack.isEmpty()) {
                    world.removeEntity(entityItem);
                }

                canFitItems = canFitItemsInBuffer();
                if (!canFitItems)
                    break;
            }
        }
    }

    public boolean canFitItemsInBuffer() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (buffer.getStackInSlot(i).isEmpty() || buffer.getStackInSlot(i).getCount() < buffer.getSlotLimit(i)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        buffer.deserializeNBT(compound.getCompoundTag("buffer"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("buffer", buffer.serializeNBT());

        return super.writeToNBT(compound);
    }

    public List<EntityItem> getItemsInBlockPos(BlockPos pos) {
        return world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new TileTransposer.ProbeCapability();
            return (T) probeCapability;
        }
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY)
            return (T) networkTile;
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
        if (capability == TeckleMod.PROBE_CAPABILITY) return true;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    public boolean isPowered() {
        return world.getBlockState(pos).getValue(BlockTransposer.TRIGGERED);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            if (networkTile.node == null)
                return;

            if (TeckleMod.INDEV)
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network", networkTile.node.network.getNetworkID().toString().toUpperCase().replaceAll("-", ""))));

            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < buffer.getSlots(); i++) {
                stacks.add(buffer.getStackInSlot(i));
            }

            ProbeData probeData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(probeData);
        }
    }
}
