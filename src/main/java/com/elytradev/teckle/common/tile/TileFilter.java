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
import com.elytradev.teckle.api.capabilities.NetworkTileTransporter;
import com.elytradev.teckle.client.gui.GuiFilter;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.container.ContainerFilter;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class TileFilter extends TileNetworkMember implements ITickable, IElementProvider {

    public EnumDyeColor colour = null;
    public AdvancedItemStackHandler filterData = new AdvancedItemStackHandler(9).withSlotLimit(slot -> 16);
    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(9);
    private NetworkTileTransporter networkTile = new NetworkTileTransporter() {
        @Override
        public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
            return new WorldNetworkEntryPoint(network, pos, getFacing());
        }

        @Override
        public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
            return side.equals(getFacing());
        }

        @Override
        public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
            if (traveller.getEntryPoint().position.equals(TileFilter.this.pos))
                return true;

            if (from.equals(getFacing().getOpposite())) {
                // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
                ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));
                boolean foundNonEmptySlot = false;
                boolean colourMatches = !traveller.data.hasKey("colour");
                if (!colourMatches) {
                    if (TileFilter.this.colour == null) {
                        colourMatches = true;
                    } else {
                        colourMatches = TileFilter.this.colour.equals(EnumDyeColor.byMetadata(traveller.data.getInteger("colour")));
                    }
                }

                if (!colourMatches)
                    return false;

                for (int i = 0; i < filterData.getSlots(); i++) {
                    if (!filterData.getStackInSlot(i).isEmpty()) {
                        foundNonEmptySlot = true;

                        if (filterData.getStackInSlot(i).isItemEqualIgnoreDurability(travellerStack)) {
                            return true;
                        }
                    }
                }

                return !foundNonEmptySlot;
            }
            return false;
        }

        @Override
        public boolean canConnectTo(EnumFacing side) {
            return side.equals(getFacing()) || side.getOpposite().equals(getFacing());
        }

        @Override
        public EnumFacing getFacing() {
            if (world != null) {
                IBlockState thisState = world.getBlockState(pos);
                if (thisState.getBlock().equals(TeckleObjects.blockFilter)) {
                    return thisState.getValue(BlockFilter.FACING);
                }
            }

            return EnumFacing.DOWN;
        }

        @Override
        public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
            if (!traveller.data.hasKey("stack"))
                return; // wtf am I supposed to do with this???

            ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
            EnumFacing facing = getFacing();
            BlockPos sourcePos = pos.offset(facing);

            // Try and put it back where we found it.
            if (side.equals(getFacing())) {
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

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tagCompound = super.getUpdateTag();
        if (colour != null) {
            tagCompound.setInteger("colour", colour.getMetadata());
        } else {
            tagCompound.removeTag("colour");
        }

        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);

        this.colour = !pkt.getNbtCompound().hasKey("colour") ? null : EnumDyeColor.byMetadata(pkt.getNbtCompound().getInteger("colour"));
    }


    /**
     * Attempt to push to our network, by pulling from our input position.
     *
     * @return true if a push occurred, false otherwise.
     */
    public boolean tryPush() {
        boolean result = false;

        if (cooldown > 0)
            return result;

        TileEntity potentialInsertionTile = world.getTileEntity(pos.offset(networkTile.getFacing()));
        boolean destinationIsAir = world.isAirBlock(pos.offset(networkTile.getFacing()));
        boolean hasInsertionDestination = potentialInsertionTile != null && ((networkTile.getNode() != null && networkTile.getNode().network != null)
                || (potentialInsertionTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, networkTile.getFacing().getOpposite())));

        if (!world.isRemote && (hasInsertionDestination || destinationIsAir)) {
            WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) networkTile.getNode().network.getNodeFromPosition(pos);
            EnumFacing facing = networkTile.getFacing();

            ItemStack extractionData = getExtractionData(facing);

            if (!extractionData.isEmpty()) {
                if (hasInsertionDestination) {
                    result = attemptInsertion(potentialInsertionTile, thisNode, extractionData);
                } else {
                    result = ejectExtractionData(facing, extractionData);
                }
            }
        }

        if (result) {
            this.world.playEvent(1000, pos, 0);
        }

        cooldown = 10;
        return result;
    }

    private boolean ejectExtractionData(EnumFacing facing, ItemStack extractionData) {
        EnumFacing enumfacing = networkTile.getFacing();
        double x = pos.getX() + 0.7D * (double) enumfacing.getFrontOffsetX();
        double y = pos.getY() + 0.7D * (double) enumfacing.getFrontOffsetY();
        double z = pos.getZ() + 0.7D * (double) enumfacing.getFrontOffsetZ();

        if (facing.getAxis() == EnumFacing.Axis.Y) {
            y = y - 0.125D;
        } else {
            y = y - 0.15625D;
        }

        EntityItem entityitem = new EntityItem(world, x, y, z, extractionData);
        double d3 = world.rand.nextDouble() * 0.1D + 0.2D;
        entityitem.motionX = (double) facing.getFrontOffsetX() * d3;
        entityitem.motionY = 0.20000000298023224D;
        entityitem.motionZ = (double) facing.getFrontOffsetZ() * d3;
        entityitem.motionX += world.rand.nextGaussian() * 0.007499999832361937D * (double) 2.5D;
        entityitem.motionY += world.rand.nextGaussian() * 0.007499999832361937D * (double) 2.5D;
        entityitem.motionZ += world.rand.nextGaussian() * 0.007499999832361937D * (double) 2.5D;
        world.spawnEntity(entityitem);
        return true;
    }

    private boolean attemptInsertion(TileEntity potentialInsertionTile, WorldNetworkEntryPoint thisNode, ItemStack extractionData) {
        boolean result = false;
        if (networkTile.getNode() != null && networkTile.getNode().network != null && potentialInsertionTile instanceof TileNetworkMember) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("stack", extractionData.writeToNBT(new NBTTagCompound()));
            if (this.colour != null)
                tagCompound.setInteger("colour", this.colour.getMetadata());
            WorldNetworkTraveller traveller = thisNode.addTraveller(tagCompound);
            if (!Objects.equals(traveller, WorldNetworkTraveller.NONE)) {
                traveller.dropActions.put(DropActions.ITEMSTACK.getFirst(), DropActions.ITEMSTACK.getSecond());
                result = true;
            } else {
                ItemStack remaining = extractionData;
                for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                    remaining = buffer.insertItem(i, remaining, false);
                }

                if (!remaining.isEmpty()) {
                    WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                    remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                    DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
                }
            }
        } else {
            IItemHandler insertHandler = world.getTileEntity(pos.offset(networkTile.getFacing())).getCapability
                    (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, networkTile.getFacing().getOpposite());

            ItemStack remaining = extractionData;
            for (int i = 0; i < insertHandler.getSlots() && !remaining.isEmpty(); i++) {
                remaining = insertHandler.insertItem(i, remaining, false);
            }

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
        }
        return result;
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
            extractionData = buffer.extractItem(bufferSlot, 8, false);
        } else {
            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null && world.getTileEntity(pos.offset(facing.getOpposite()))
                    .hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                IItemHandler itemHandler = world.getTileEntity(pos.offset(facing.getOpposite()))
                        .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);


                if (filterData.stream().anyMatch(itemStack -> !itemStack.isEmpty())) {
                    for (ItemStack stack : filterData.getStacks()) {
                        if (!extractionData.isEmpty())
                            break;
                        if (stack.isEmpty())
                            continue;

                        for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                            ItemStack extractTest = itemHandler.extractItem(slot, stack.getCount(), true);
                            if (Objects.equals(extractTest.getItem(), stack.getItem()) && extractTest.getMetadata() == stack.getMetadata()) {
                                extractionData = itemHandler.extractItem(slot, stack.getCount(), false);
                            }
                        }
                    }
                } else {
                    for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                        extractionData = itemHandler.extractItem(slot, 8, false);
                    }
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

        boolean canFitItems = world.isAirBlock(pos.add(networkTile.getFacing().getOpposite().getDirectionVec())) && canFitItemsInBuffer();
        if (canFitItems) {
            List<EntityItem> itemsToPickup = getItemsInBlockPos(pos.add(networkTile.getFacing().getOpposite().getDirectionVec()));
            if (world.getBlockState(pos).getValue(BlockFilter.TRIGGERED) && world.isAirBlock(pos.add(networkTile.getFacing().getOpposite().getDirectionVec())
                    .add(networkTile.getFacing().getOpposite().getDirectionVec())))
                itemsToPickup.addAll(getItemsInBlockPos(pos.add(networkTile.getFacing().getOpposite().getDirectionVec())
                        .add(networkTile.getFacing().getOpposite().getDirectionVec())));

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

        this.colour = !compound.hasKey("colour") ? null : EnumDyeColor.byMetadata(compound.getInteger("colour"));
        filterData.deserializeNBT(compound.getCompoundTag("filterData"));
        buffer.deserializeNBT(compound.getCompoundTag("buffer"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (colour != null) {
            compound.setInteger("colour", colour.getMetadata());
        } else {
            compound.removeTag("colour");
        }
        compound.setTag("filterData", filterData.serializeNBT());
        compound.setTag("buffer", buffer.serializeNBT());

        return super.writeToNBT(compound);
    }

    public List<EntityItem> getItemsInBlockPos(BlockPos pos) {
        return world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new TileFilter.ProbeCapability();
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

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerFilter(this, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientElement(EntityPlayer player) {
        return new GuiFilter(this, player);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            if (networkTile.node == null)
                return;

            if (TeckleMod.INDEV)
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network", networkTile.getNode().network.getNetworkID().toString().toUpperCase().replaceAll("-", ""))));

            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < buffer.getSlots(); i++) {
                stacks.add(buffer.getStackInSlot(i));
            }

            ProbeData probeData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(probeData);
        }
    }
}
