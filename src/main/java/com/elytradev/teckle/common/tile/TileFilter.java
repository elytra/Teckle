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
import com.elytradev.teckle.client.gui.GuiFilter;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.container.ContainerFilter;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileFilter;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
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
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;


public class TileFilter extends TileNetworkMember implements ITickable, IElementProvider {

    public EnumFacing cachedFace = EnumFacing.DOWN;
    public EnumDyeColor colour = null;
    public UUID filterID, bufferID;
    public AdvancedStackHandlerEntry filterData, bufferData;
    private NetworkTileFilter networkTile = new NetworkTileFilter(world);

    private int cooldown = 0;

    @Override
    public void validate() {
        if (filterID == null) {
            if (filterData == null) {
                filterData = new AdvancedStackHandlerEntry(UUID.randomUUID(), world.provider.getDimension(), pos, new AdvancedItemStackHandler(9));
            }
            filterID = filterData.getId();
        } else {
            filterData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(filterID);
        }
        if (bufferID == null) {
            if (bufferData == null) {
                bufferData = new AdvancedStackHandlerEntry(UUID.randomUUID(), world.provider.getDimension(), pos, new AdvancedItemStackHandler(9));
            }
            bufferID = bufferData.getId();
        } else {
            bufferData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(bufferID);
        }

        this.networkTile.filterData = this.filterData;
        this.networkTile.bufferData = this.bufferData;
        this.networkTile.filterID = this.filterID;
        this.networkTile.bufferID = this.bufferID;
    }

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
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.colour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
        super.readFromNBT(tag);
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
        try {
            TileEntity potentialInsertionTile = world.getTileEntity(pos.offset(networkTile.getOutputFace()));
            boolean destinationIsAir = world.isAirBlock(pos.offset(networkTile.getOutputFace()));
            boolean hasInsertionDestination = potentialInsertionTile != null && ((networkTile.getNode() != null && networkTile.getNode().getNetwork() != null)
                    || (potentialInsertionTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, networkTile.getOutputFace().getOpposite())));

            if (!world.isRemote && (hasInsertionDestination || destinationIsAir)) {
                WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) networkTile.getNode();
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
            bool = bool || networkTile.getNode().getNetwork() == null;
            debugInfo += " network " + (bool ? "null" : networkTile.getNode().getNetwork().toString());
            TeckleMod.LOG.error("****************OH SHIT TECKLE BROKE*******************");
            TeckleMod.LOG.error("Caught NPE in tryPush!, {}", this);
            e.printStackTrace();
            TeckleMod.LOG.error("Here's some useful debug info, {}", debugInfo);
            TeckleMod.LOG.error("****************OH SHIT TECKLE BROKE*******************");
        }
        cooldown = TeckleMod.CONFIG.filterCooldown;
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
        ImmutableMap<String, NBTBase> additionalData = colour == null ? ImmutableMap.of() : ImmutableMap.of("colour", new NBTTagInt(colour.getMetadata()));
        ItemStack remaining = networkAssistant.insertData(thisNode, potentialInsertionTile.getPos(), extractionData, additionalData, false, false).copy();

        if (!remaining.isEmpty()) {
            for (int i = 0; i < bufferData.getHandler().getSlots() && !remaining.isEmpty(); i++) {
                remaining = bufferData.getHandler().insertItem(i, remaining, false);
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
        for (int i = 0; i < bufferData.getHandler().getSlots(); i++) {
            if (!bufferData.getHandler().getStackInSlot(i).isEmpty()) {
                bufferSlot = i;
                break;
            }
        }

        if (bufferSlot != -1) {
            extractionData = bufferData.getHandler().extractItem(bufferSlot, 8, false);
        } else {
            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null && world.getTileEntity(pos.offset(facing.getOpposite()))
                    .hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                IItemHandler itemHandler = world.getTileEntity(pos.offset(facing.getOpposite()))
                        .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);


                if (filterData.getHandler().stream().anyMatch(itemStack -> !itemStack.isEmpty())) {
                    for (ItemStack stack : filterData.getHandler().getStacks()) {
                        if (!extractionData.isEmpty())
                            break;
                        if (stack.isEmpty())
                            continue;

                        for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                            ItemStack extractTest = itemHandler.extractItem(slot, stack.getCount() > 1 ? stack.getCount() : stack.getMaxStackSize(), true);
                            if (Objects.equals(extractTest.getItem(), stack.getItem()) && extractTest.getMetadata() == stack.getMetadata()) {
                                extractionData = itemHandler.extractItem(slot, stack.getCount() > 1 ? stack.getCount() : stack.getMaxStackSize(), false);
                            }
                        }
                    }
                } else {
                    for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                        extractionData = itemHandler.extractItem(slot, 64, false);
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
        if (world.isRemote || networkTile.getNode() == null || networkTile.getNode().getNetwork() == null)
            return;

        if (cooldown > 0) {
            cooldown--;
        }

        boolean canFitItems = world.isAirBlock(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec())) && canFitItemsInBuffer();
        if (canFitItems) {
            List<EntityItem> itemsToPickup = getItemsInBlockPos(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec()));
            if (world.getBlockState(pos).getValue(BlockFilter.TRIGGERED) && world.isAirBlock(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec())
                    .add(networkTile.getOutputFace().getOpposite().getDirectionVec())))
                itemsToPickup.addAll(getItemsInBlockPos(pos.add(networkTile.getOutputFace().getOpposite().getDirectionVec())
                        .add(networkTile.getOutputFace().getOpposite().getDirectionVec())));

            for (EntityItem entityItem : itemsToPickup) {
                ItemStack entityStack = entityItem.getItem().copy();

                for (int i = 0; i < bufferData.getHandler().getSlots() && !entityStack.isEmpty(); i++) {
                    entityStack = bufferData.getHandler().insertItem(i, entityStack, false);
                }

                entityItem.setItem(entityStack);
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
        for (int i = 0; i < bufferData.getHandler().getSlots(); i++) {
            if (bufferData.getHandler().getStackInSlot(i).isEmpty() || bufferData.getHandler().getStackInSlot(i).getCount() < bufferData.getHandler().getSlotLimit(i)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.colour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
        this.cachedFace = EnumFacing.values()[tag.getInteger("cachedFace")];

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (tag.hasKey("filterData")) {
                validate();
                filterData.getHandler().deserializeNBT(tag.getCompoundTag("filterData"));
                bufferData.getHandler().deserializeNBT(tag.getCompoundTag("buffer"));
                tag.removeTag("filterData");
                tag.removeTag("buffer");
            } else {
                this.bufferID = tag.getUniqueId("buffer");
                this.filterID = tag.getUniqueId("filter");
                this.bufferData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(bufferID);
                this.filterData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(filterID);
            }

            UUID networkID = tag.hasKey("networkID") ? tag.getUniqueId("networkID") : null;
            int dimID = tag.getInteger("databaseID");
            if (networkID == null) {
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            } else {
                WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
                Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                        .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), networkTile.getCapabilityFace())).findAny();
                if (any.isPresent()) {
                    networkID = networkDB.getRemappedNodes().remove(any.get());
                    TeckleMod.LOG.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
                }

                IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
                WorldNetworkNode node = null;
                node = networkTile.createNode(network, pos);
                network.registerNode(node);
                //if (network.getNode(pos, null) == null) {
                //    node = networkTile.createNode(network, pos);
                //    network.registerNode(node);
                //} else {
                //    node = network.getNode(pos, null);
                //}
                networkTile.setNode(node);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if (colour != null) {
            tag.setInteger("colour", colour.getMetadata());
        } else {
            tag.removeTag("colour");
        }
        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("filter", filterID);
        tag.setInteger("cachedFace", networkTile.getOutputFace().getIndex());

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            tag.setInteger("databaseID", getWorld().provider.getDimension());
            if (networkTile.getNode() == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            tag.setUniqueId("networkID", networkTile.getNode().getNetwork().getNetworkID());
        }
        return super.writeToNBT(tag);
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
    public void setWorld(World worldIn) {
        super.setWorld(worldIn);
        networkTile.setWorld(worldIn);
    }

    @Override
    protected void setWorldCreate(World worldIn) {
        super.setWorldCreate(worldIn);
        // Mojang's method doesnt do this because they were drunk or something idk.
        setWorld(worldIn);
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

    public boolean isPowered() {
        return world.getBlockState(pos).getValue(BlockFilter.TRIGGERED);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            if (networkTile.getNode() == null)
                return;

            if (TeckleMod.INDEV)
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network", networkTile.getNode().getNetwork().getNetworkID().toString().toUpperCase().replaceAll("-", ""))));

            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < bufferData.getHandler().getSlots(); i++) {
                stacks.add(bufferData.getHandler().getStackInSlot(i));
            }

            ProbeData probeData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(probeData);
        }
    }
}
