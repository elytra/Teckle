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
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.container.ContainerFilter;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileFilter;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileTransposer;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TileFilter extends TileTransposer implements ITickable, IElementProvider {

    public EnumDyeColor colour = null;
    public UUID filterID;
    public AdvancedStackHandlerEntry filterData;
    private NetworkTileFilter networkTile;

    private int cooldown = 0;

    @Override
    public void validate() {
        try {
            AdvancedStackHandlerPool pool = AdvancedStackHandlerPool.getPool(world);
            this.bufferData = pool.getOrCreatePoolEntry(bufferID, getPos(), 9);
            this.bufferID = bufferData.getId();

            this.filterData = pool.getOrCreatePoolEntry(filterID, getPos(), 9);
            this.filterID = filterData.getId();

            if (networkTile == null)
                this.networkTile = new NetworkTileFilter(this);

            this.networkTile.filterData = this.filterData;
            this.networkTile.bufferData = this.bufferData;
            this.networkTile.filterID = this.filterID;
            this.networkTile.bufferID = this.bufferID;

            this.tileEntityInvalid = false;
        } catch (Exception e) {
            TeckleLog.error("Failed to validate transposer. {}", e);
        }
    }

    @Override
    protected NetworkTileTransposer getNetworkTile() {
        return networkTile;
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
        readFromNBT(tag);
    }

    /**
     * Attempt to push to our network, by pulling from our input position.
     *
     * @return true if a push occurred, false otherwise.
     */
    @Override
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
            boolean nullCheck = networkTile == null;
            String debugInfo = "NTile " + (nullCheck ? "null" : networkTile.toString());
            nullCheck = nullCheck || networkTile.getNode() == null;
            debugInfo += " node " + (nullCheck ? "null" : networkTile.getNode().toString());
            nullCheck = nullCheck || networkTile.getNode().getNetwork() == null;
            debugInfo += " network " + (nullCheck ? "null" : networkTile.getNode().getNetwork().toString());
            TeckleLog.error("****************Teckle Encountered An Error*******************");
            TeckleLog.error("Caught NPE in tryPush!, {}", this);
            TeckleLog.error("Here's some useful debug info, {}", debugInfo);
            e.printStackTrace();
            TeckleLog.error("**************************************************************");
        }
        cooldown = TeckleMod.CONFIG.filterCooldown;
        return result;
    }

    private boolean ejectExtractionData(EnumFacing facing, ItemStack extractionData) {
        BlockSourceImpl coords = new BlockSourceImpl(world, pos);
        double d0 = coords.getX() + 0.7D * (double) networkTile.getOutputFace().getXOffset();
        double d1 = coords.getY() + 0.7D * (double) networkTile.getOutputFace().getYOffset();
        double d2 = coords.getZ() + 0.7D * (double) networkTile.getOutputFace().getZOffset();
        BehaviorDefaultDispenseItem.doDispense(world, extractionData, 6, facing, new PositionImpl(d0, d1, d2));

        return true;
    }

    private boolean attemptInsertion(TileEntity potentialInsertionTile, WorldNetworkEntryPoint thisNode, ItemStack extractionData) {
        IWorldNetworkAssistant<ItemStack> networkAssistant = getNetworkAssistant(ItemStack.class);
        ImmutableMap<String, NBTBase> additionalData = colour == null ? ImmutableMap.of() : ImmutableMap.of("colour", new NBTTagInt(colour.getMetadata()));
        ItemStack remaining = networkAssistant.insertData(thisNode, potentialInsertionTile.getPos(), extractionData,
                additionalData, false, false).copy();

        remaining = reinsertOrDrop(remaining);

        return remaining.isEmpty();
    }

    private ItemStack getExtractionData(EnumFacing facing) {
        ItemStack extractionData = ItemStack.EMPTY;

        if (bufferData == null)
            return extractionData;

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
            BlockPos inputPos = pos.offset(facing.getOpposite());
            TileEntity tile = world.getTileEntity(inputPos);
            if (tile != null && tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);

                if (filterData.getHandler().stream().anyMatch(itemStack -> !itemStack.isEmpty())) {
                    for (ItemStack stack : filterData.getHandler().getStacks()) {
                        if (!extractionData.isEmpty())
                            break;
                        if (stack.isEmpty())
                            continue;

                        for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                            int countToExtract = stack.getCount() > 1 ? stack.getCount() : stack.getMaxStackSize();
                            ItemStack extractTest = itemHandler.extractItem(slot, countToExtract, true);
                            if (extractTest.isItemEqual(stack)) {
                                extractionData = itemHandler.extractItem(slot, countToExtract, false);
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
        return oldState.getBlock() != newSate.getBlock() && super.shouldRefresh(world, pos, oldState, newSate);
    }

    @Override
    public void update() {
        if (world.isRemote || networkTile.getNode() == null || networkTile.getNode().getNetwork() == null)
            return;

        if (cooldown > 0) {
            cooldown--;
        }

        pullItemEntities(isPowered());
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.colour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (tag.hasKey("filterData")) {
                // Handles legacy inventories that were stored directly on tiles.
                validate();
                filterData.getHandler().deserializeNBT(tag.getCompoundTag("filterData"));
                bufferData.getHandler().deserializeNBT(tag.getCompoundTag("buffer"));
                tag.removeTag("filterData");
                tag.removeTag("buffer");
            } else {
                AdvancedStackHandlerPool pool = AdvancedStackHandlerPool.getPool(world.provider.getDimension());
                if (tag.hasUniqueId("buffer")) {
                    this.bufferID = tag.getUniqueId("buffer");
                    this.bufferData = pool.get(bufferID);
                }
                if (tag.hasUniqueId("filter")) {
                    this.filterID = tag.getUniqueId("filter");
                    this.filterData = pool.get(filterID);
                }
            }

            UUID networkID = tag.hasKey("networkIDLeast") ? tag.getUniqueId("networkID") : null;
            int dimID = tag.getInteger("databaseID");
            if (networkID == null) {
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            } else {
                WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
                Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                        .filter(pair -> pair.getLeft() == getPos() && pair.getValue() == networkTile.getCapabilityFace())
                        .findAny();
                if (any.isPresent()) {
                    networkID = networkDB.getRemappedNodes().remove(any.get());
                    TeckleLog.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
                }

                IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
                for (NodeContainer container : network.getNodeContainersAtPosition(pos)) {
                    if (container.getFacing() == null && container.getNetworkTile() instanceof NetworkTileFilter) {
                        networkTile = (NetworkTileFilter) container.getNetworkTile();
                        break;
                    }
                }
            }
            validate();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if (colour != null) {
            tag.setInteger("colour", colour.getMetadata());
        } else {
            tag.removeTag("colour");
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (bufferData == null || filterData == null)
                validate();
            tag.setUniqueId("buffer", bufferData.getId());
            tag.setUniqueId("filter", filterData.getId());

            tag.setInteger("databaseID", getWorld().provider.getDimension());
            if (networkTile.getNode() == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            tag.setUniqueId("networkID", networkTile.getNode().getNetwork().getNetworkID());
        }

        return super.writeToNBT(tag);
    }

    @Override
    public List<EntityItem> getItemsInBlockPos(BlockPos pos) {
        return world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new TileFilter.ProbeCapability();
            return (T) probeCapability;
        }
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY)
            return (T) networkTile;
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public void setWorld(World worldIn) {
        super.setWorld(worldIn);
        if (networkTile != null)
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

    @Override
    public boolean isPowered() {
        return world.getBlockState(pos).getValue(BlockFilter.TRIGGERED);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            if (networkTile.getNode() == null)
                return;

            if (TeckleMod.INDEV)
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network",
                        "All",
                        networkTile.getNode().getNetwork().getNetworkID().toString().toUpperCase().replaceAll("-", ""),
                        networkTile.getNode().getNetwork().getNodes().size())));

            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < bufferData.getHandler().getSlots(); i++) {
                stacks.add(bufferData.getHandler().getStackInSlot(i));
            }

            ProbeData probeData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(probeData);
        }
    }
}
