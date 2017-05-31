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

package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.impl.NetworkTileTransporter;
import com.elytradev.teckle.client.gui.GuiSortingMachine;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullModeSingleStep;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModeAnyStack;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class TileSortingMachine extends TileNetworkMember implements ITickable, IElementProvider {

    public AdvancedItemStackHandler filterRows = new AdvancedItemStackHandler(48);
    public EnumDyeColor[] colours = new EnumDyeColor[8];
    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(18);
    public DefaultRoute defaultRoute = DefaultRoute.NONE;
    @SideOnly(Side.CLIENT)
    private int selectorPos;
    private PullMode pullMode = new PullModeSingleStep();
    private SortMode sortMode = new SortModeAnyStack();
    private List<IItemHandler> subHandlers;
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
            if (traveller.getEntryPoint().position.equals(TileSortingMachine.this.pos))
                return true;

            if (from.equals(getFacing().getOpposite())) {
                // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
                return getSortMode().canAcceptTraveller(TileSortingMachine.this, traveller);
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
                if (thisState.getBlock().equals(TeckleObjects.blockSortingMachine)) {
                    return thisState.getValue(BlockSortingMachine.FACING);
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
                ItemStack remaining = stack.copy();
                for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                    remaining = buffer.insertItem(i, remaining, false);
                }

                // Spawn into the world I guess.
                if (!remaining.isEmpty()) {
                    WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                    remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                    DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
                }
            }
        }

        /**
         * Called when a traveller is added to this tile, used for modifying the traveller.
         *
         * @param traveller the traveller added to this tile.
         */
        @Override
        public void onTravellerAdded(WorldNetworkTraveller traveller) {
            sortMode.processExistingTraveller(TileSortingMachine.this, traveller);
        }

        /**
         * Called when a traveller is removed from this tile, used for modifying the traveller.
         *
         * @param traveller the traveller removed from the tile.
         */
        @Override
        public void onTravellerRemoved(WorldNetworkTraveller traveller) {
            traveller.quickRepath();
        }
    };

    public List<IItemHandler> getCompartmentHandlers() {
        if (subHandlers == null || subHandlers.isEmpty()) {
            subHandlers = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                subHandlers.add(filterRows.subHandler(i * 6, 6));
            }
        }

        return subHandlers;
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
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
        if (world.isRemote)
            return;

        if (getSource() != null)
            getPullMode().onTick(this);

        getSortMode().onTick(this);
    }

    public TileEntity getSource() {
        if (world != null) {
            EnumFacing facing = networkTile.getFacing();
            BlockPos sourcePos = pos.offset(facing.getOpposite());

            TileEntity sourceTile = world.getTileEntity(sourcePos);
            if (sourceTile != null && sourceTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                return sourceTile;
            }
        }

        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        NBTTagList coloursTag = compound.getTagList("colours", 3);
        for (int i = 0; i < 8; i++) {
            if (coloursTag.getIntAt(i) > -1) {
                colours[i] = EnumDyeColor.byMetadata(coloursTag.getIntAt(i));
            } else {
                colours[i] = null;
            }
        }
        try {
            setPullMode(PullMode.PULL_MODES.get(compound.getInteger("pullModeID")).newInstance());
            getPullMode().deserializeNBT(compound.getCompoundTag("pullMode"));

            setSortMode(SortMode.SORT_MODES.get(compound.getInteger("sortModeID")).newInstance());
            getSortMode().deserializeNBT(compound.getCompoundTag("sortMode"));
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to read sorting machine modes from nbt.", e);
        }

        filterRows.deserializeNBT(compound.getCompoundTag("filterRows"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList coloursTag = new NBTTagList();
        for (int i = 0; i < colours.length; i++) {
            if (colours[i] != null) {
                coloursTag.appendTag(new NBTTagInt(colours[i].getMetadata()));
            } else {
                coloursTag.appendTag(new NBTTagInt(-1));
            }
        }
        compound.setTag("colours", coloursTag);
        compound.setTag("filterRows", filterRows.serializeNBT());
        compound.setTag("pullMode", getPullMode().serializeNBT());
        compound.setInteger("pullModeID", getPullMode().getID());

        compound.setTag("sortMode", getSortMode().serializeNBT());
        compound.setInteger("sortModeID", getSortMode().getID());

        return super.writeToNBT(compound);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY)
            return (T) networkTile;
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerSortingMachine(this, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientElement(EntityPlayer player) {
        return new GuiSortingMachine(this, player);
    }

    public EnumFacing getFacing() {
        return networkTile.getFacing();
    }

    public ItemStack addToNetwork(IItemHandler source, int slot, int quantity, ImmutableMap<String, NBTBase> additionalData) {
        TileEntity potentialInsertionTile = world.getTileEntity(pos.offset(getFacing()));
        ItemStack remaining = source.extractItem(slot, quantity, false).copy();

        if (CapabilityWorldNetworkTile.isTileNetworked(potentialInsertionTile)) {
            IWorldNetworkAssistant<ItemStack> networkAssistant = getNetworkAssistant(ItemStack.class);

            remaining = networkAssistant.insertData((WorldNetworkEntryPoint) getNetworkTile().getNode(), potentialInsertionTile.getPos(), remaining, additionalData).copy();

            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                // Couldn't put the stuff in the network, shove it into the buffer to try again later.
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

        }

        return remaining;
    }

    /**
     * Get a list of stacks that can be sorted from the source.
     *
     * @return the list of all itemstacks available for sorting.
     */
    public List<SlotData> getStacksToPush() {
        List<SlotData> stacks = Lists.newArrayList();

        if (!buffer.stream().allMatch(ItemStack::isEmpty)) {
            for (int i = 0; i < buffer.getStacks().size(); i++) {
                stacks.add(new SlotData(buffer, i));
            }
            return stacks;
        }

        if (getSource() != null && getSource().hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing())) {
            IItemHandler sourceItemHandler = getSource().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing());
            for (int i = 0; i < sourceItemHandler.getSlots(); i++) {
                stacks.add(new SlotData(sourceItemHandler, i));
            }
        }

        return stacks;
    }

    public IWorldNetworkTile getNetworkTile() {
        return networkTile;
    }

    public PullMode getPullMode() {
        return pullMode;
    }

    public void setPullMode(PullMode pullMode) {
        this.pullMode = pullMode;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;

        if (this.getPullMode().isPaused()) {
            this.getPullMode().unpause();
        }
    }

    @SideOnly(Side.CLIENT)
    public int getSelectorPos() {
        return selectorPos;
    }

    @SideOnly(Side.CLIENT)
    public void setSelectorPos(int selectorPos) {
        this.selectorPos = selectorPos;
    }

    public enum DefaultRoute implements IStringSerializable {
        WHITE(0, "white", EnumDyeColor.WHITE),
        ORANGE(1, "orange", EnumDyeColor.ORANGE),
        MAGENTA(2, "magenta", EnumDyeColor.MAGENTA),
        LIGHT_BLUE(3, "light_blue", EnumDyeColor.LIGHT_BLUE),
        YELLOW(4, "yellow", EnumDyeColor.YELLOW),
        LIME(5, "lime", EnumDyeColor.LIME),
        PINK(6, "pink", EnumDyeColor.PINK),
        GRAY(7, "gray", EnumDyeColor.GRAY),
        SILVER(8, "silver", EnumDyeColor.SILVER),
        CYAN(9, "cyan", EnumDyeColor.CYAN),
        PURPLE(10, "purple", EnumDyeColor.PURPLE),
        BLUE(11, "blue", EnumDyeColor.BLUE),
        BROWN(12, "brown", EnumDyeColor.BROWN),
        GREEN(13, "green", EnumDyeColor.GREEN),
        RED(14, "red", EnumDyeColor.RED),
        BLACK(15, "black", EnumDyeColor.BLACK),
        NONE(16, "none", null),
        BLOCKED(17, "blocked", null);

        private static final DefaultRoute[] META_LOOKUP = new DefaultRoute[values().length];

        static {
            for (DefaultRoute ingotType : values()) {
                META_LOOKUP[ingotType.getMetadata()] = ingotType;
            }
        }

        private final int meta;
        private final String name;
        private final EnumDyeColor colour;

        DefaultRoute(int meta, String name, EnumDyeColor colour) {
            this.meta = meta;
            this.name = name;
            this.colour = colour;
        }

        public static DefaultRoute byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public int getMetadata() {
            return this.meta;
        }

        public String getName() {
            return "defaultroute." + this.name;
        }

        public EnumDyeColor getColour() {
            return colour;
        }

        public boolean isBlocked() {
            return this == BLOCKED;
        }

        public boolean isColoured() {
            return this != BLOCKED && this != NONE;
        }
    }

}