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

import com.elytradev.probe.api.IProbeData;
import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.probe.api.UnitDictionary;
import com.elytradev.probe.api.impl.ProbeData;
import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.api.capabilities.impl.NetworkTileTransporter;
import com.elytradev.teckle.client.gui.GuiSortingMachine;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.network.messages.TileLitMessage;
import com.elytradev.teckle.common.tile.TileLitNetworkMember;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullModeSingleStep;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModeAnyStack;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableList;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
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
import java.util.stream.Collectors;


public class TileSortingMachine extends TileLitNetworkMember implements IElementProvider {

    public EnumFacing cachedFace = EnumFacing.DOWN;
    public AdvancedItemStackHandler filterRows = new AdvancedItemStackHandler(48);
    public EnumDyeColor[] colours = new EnumDyeColor[8];
    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(32);
    public List<WorldNetworkTraveller> returnedTravellers = Lists.newArrayList();
    public DefaultRoute defaultRoute = DefaultRoute.NONE;
    @SideOnly(Side.CLIENT)
    private int selectorPos = -1;
    private PullMode pullMode = new PullModeSingleStep();
    private SortMode sortMode = new SortModeAnyStack();
    private List<IItemHandler> subHandlers;
    private NetworkTileTransporter ejectionTile = new NetworkTileTransporter() {
        @Override
        public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
            return new WorldNetworkEntryPoint(network, pos, getOutputFace(), getCapabilityFace());
        }

        @Override
        public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
            return Objects.equals(side, getOutputFace());
        }

        @Override
        public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
            if (Objects.equals(traveller.getEntryPoint().position, TileSortingMachine.this.pos))
                return true;

            if (Objects.equals(from, getOutputFace().getOpposite())) {
                // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
                return getSortMode().canAcceptTraveller(TileSortingMachine.this, traveller, from);
            }
            return false;
        }

        @Override
        public boolean canConnectTo(EnumFacing side) {
            return Objects.equals(side, getOutputFace());
        }

        @Override
        public EnumFacing getOutputFace() {
            if (world != null) {
                IBlockState thisState = world.getBlockState(pos);
                if (Objects.equals(thisState.getBlock(), TeckleObjects.blockSortingMachine)) {
                    return thisState.getValue(BlockSortingMachine.FACING);
                }
            }

            return cachedFace;
        }

        @Override
        public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
            if (!traveller.data.hasKey("stack"))
                return; // wtf am I supposed to do with this???

            ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
            EnumFacing facing = getOutputFace();

            // Try and put it back where we found it.
            if (Objects.equals(side, getOutputFace())) {
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
                traveller.data.setTag("stack", stack.serializeNBT());
                if (returnedTravellers.size() <= 32) {
                    returnedTravellers.add(traveller);
                } else {
                    DropActions.ITEMSTACK.getSecond().dropToWorld(traveller);
                }
            }
        }

        @Override
        public EnumFacing getCapabilityFace() {
            return getOutputFace();
        }
    };
    private WorldNetworkTile insertionTile = new NetworkTileTransporter() {
        @Override
        public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
            return Objects.equals(side, getCapabilityFace());
        }

        @Override
        public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
            return new SortingMachineEndpoint(network, pos, getCapabilityFace());
        }

        @Override
        public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
            return sortMode.canAcceptTraveller(TileSortingMachine.this, traveller, from);
        }

        @Override
        public boolean canConnectTo(EnumFacing side) {
            return Objects.equals(side, getCapabilityFace());
        }

        @Override
        public EnumFacing getCapabilityFace() {
            return TileSortingMachine.this.ejectionTile.getOutputFace().getOpposite();
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

    @Override
    public void onLoad() {
        if (world.isRemote) new TileLitMessage(this).sendToAllWatching(this);
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
        this.handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);
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
        super.update();

        if (world.isRemote)
            return;

        if (!returnedTravellers.isEmpty()) {
            WorldNetworkTraveller traveller = returnedTravellers.get(0);
            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos.offset(ejectionTile.getOutputFace()), ejectionTile.getOutputFace().getOpposite())) {
                BlockPos outputPos = pos.offset(ejectionTile.getOutputFace());

                ItemStack stackToInsert = new ItemStack(traveller.data.getCompoundTag("stack"));
                ImmutableMap<String, NBTBase> collect = ImmutableMap.copyOf(traveller.data.getKeySet().stream().collect(Collectors.toMap(o -> o, o -> traveller.data.getTag(o))));
                ItemStack result = (ItemStack) getNetworkAssistant(ItemStack.class).insertData((WorldNetworkEntryPoint) ejectionTile.getNode(),
                        outputPos, stackToInsert.copy(), collect, false, false);

                if (result.isEmpty()) {
                    returnedTravellers.remove(0);
                } else {
                    returnedTravellers.get(0).data.setTag("stack", result.serializeNBT());
                }

                if (result.getCount() != stackToInsert.getCount()) {
                    setTriggered();
                }
            }
            return;
        }

        if (getSource() != null)
            getPullMode().onTick(this);

        getSortMode().onTick(this);
    }

    public TileEntity getSource() {
        if (world != null) {
            EnumFacing facing = ejectionTile.getOutputFace();
            BlockPos sourcePos = pos.offset(facing.getOpposite());

            TileEntity sourceTile = world.getTileEntity(sourcePos);
            if (sourceTile != null && sourceTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                return sourceTile;
            }
        }

        return null;
    }

    public TileEntity getOutput() {
        if (world != null) {
            EnumFacing facing = ejectionTile.getOutputFace();
            BlockPos sourcePos = pos.offset(facing);

            TileEntity sourceTile = world.getTileEntity(sourcePos);
            if (sourceTile != null && sourceTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
                return sourceTile;
            }
        }

        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        defaultRoute = DefaultRoute.byMetadata(compound.getInteger("defaultRoute"));
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

        cachedFace = EnumFacing.values()[compound.getInteger("cachedFace")];

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            filterRows.deserializeNBT(compound.getCompoundTag("filterRows"));
            buffer.deserializeNBT(compound.getCompoundTag("buffer"));
            int stacksLeftToSatisfySize = compound.getInteger("returnedTravellers");
            returnedTravellers = Lists.newArrayListWithExpectedSize(stacksLeftToSatisfySize);
            for (int i = 0; i < stacksLeftToSatisfySize; i++) {
                WorldNetworkTraveller deserializedTraveller = new WorldNetworkTraveller(new NBTTagCompound());
                deserializedTraveller.deserializeNBT(compound.getCompoundTag("returnedTravellers" + i));
                returnedTravellers.set(i, deserializedTraveller);
            }

            UUID networkID = compound.hasKey("networkIDEntryPoint") ? compound.getUniqueId("networkIDEntryPoint") : null;
            int dimID = compound.getInteger("databaseID");
            if (networkID == null) {
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            } else {
                WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
                Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                        .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), ejectionTile.getCapabilityFace())).findAny();
                if (any.isPresent()) {
                    networkID = networkDB.getRemappedNodes().remove(any.get());
                    TeckleMod.LOG.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
                }

                IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
                WorldNetworkNode node = ejectionTile.createNode(network, pos);
                network.registerNode(node);
                ejectionTile.setNode(node);

                networkID = compound.getUniqueId("networkIDEndPoint");
                any = networkDB.getRemappedNodes().keySet().stream()
                        .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), insertionTile.getCapabilityFace())).findAny();
                if (any.isPresent()) {
                    networkID = networkDB.getRemappedNodes().remove(any.get());
                    TeckleMod.LOG.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
                }
                network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
                node = insertionTile.createNode(network, pos);
                network.registerNode(node);
                insertionTile.setNode(node);
            }
        }
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


        compound.setTag("pullMode", getPullMode().serializeNBT());
        compound.setInteger("pullModeID", getPullMode().getID());
        compound.setTag("sortMode", getSortMode().serializeNBT());
        compound.setInteger("sortModeID", getSortMode().getID());

        compound.setInteger("defaultRoute", defaultRoute.getMetadata());
        compound.setInteger("cachedFace", ejectionTile.getOutputFace().getIndex());

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            compound.setTag("filterRows", filterRows.serializeNBT());
            compound.setTag("buffer", buffer.serializeNBT());
            compound.setInteger("returnedTravellers", returnedTravellers.size());
            for (int i = 0; i < returnedTravellers.size(); i++) {
                compound.setTag("returnedTravellers" + i, returnedTravellers.get(i).serializeNBT());
            }

            compound.setInteger("databaseID", getWorld().provider.getDimension());
            if (ejectionTile.getNode() == null || insertionTile.getNode() == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            compound.setUniqueId("networkIDEntryPoint", ejectionTile.getNode().getNetwork().getNetworkID());
            compound.setUniqueId("networkIDEndPoint", insertionTile.getNode().getNetwork().getNetworkID());
        }
        return super.writeToNBT(compound);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new TileSortingMachine.ProbeCapability();
            return (T) probeCapability;
        }
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            if (Objects.equals(facing, getFacing()))
                return (T) ejectionTile;
            else if (Objects.equals(facing, getFacing().getOpposite()))
                return (T) insertionTile;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
        if (capability == TeckleMod.PROBE_CAPABILITY) return true;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY
                && (Objects.equals(facing, getFacing()) || Objects.equals(facing, getFacing().getOpposite())))
            return true;
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
        return ejectionTile.getOutputFace();
    }

    public ItemStack addToNetwork(IItemHandler source, int slot, int quantity, ImmutableMap<String, NBTBase> additionalData) {
        ItemStack remaining = source.extractItem(slot, quantity, false).copy();
        IWorldNetworkAssistant<ItemStack> networkAssistant = getNetworkAssistant(ItemStack.class);
        remaining = networkAssistant.insertData((WorldNetworkEntryPoint) getEjectionTile().getNode(), pos.offset(getFacing()), remaining, additionalData, false, false).copy();

        if (!remaining.isEmpty()) {
            if (remaining.getCount() != quantity) {
                setTriggered();
            }

            for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                remaining = buffer.insertItem(i, remaining, false);
            }

            if (!remaining.isEmpty()) {
                WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
            }
        } else {
            setTriggered();
        }

        return remaining;
    }

    /**
     * Get a list of stacks that can be sorted from the source.
     *
     * @param skipBuffer
     * @return the list of all itemstacks available for sorting.
     */
    public List<SlotData> getStacksToPush(boolean skipBuffer) {
        List<SlotData> stacks = Lists.newArrayList();

        if (!skipBuffer && !buffer.stream().allMatch(ItemStack::isEmpty)) {
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

    public WorldNetworkTile getEjectionTile() {
        return ejectionTile;
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

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            List<WorldNetworkNode> nodes = Lists.newArrayList();
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (!CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos, facing))
                    continue;
                WorldNetworkTile networkTileAtPosition = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, facing);
                WorldNetworkNode node = networkTileAtPosition.getNode();
                String faceName = networkTileAtPosition.getCapabilityFace() == null ? "" : networkTileAtPosition.getCapabilityFace().getName();
                if (node == null || nodes.contains(node))
                    continue;

                nodes.add(node);
                if (TeckleMod.INDEV)
                    data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network",
                            faceName, node.getNetwork().getNetworkID().toString().toUpperCase().replaceAll("-", ""))));

                if (!node.getTravellers().isEmpty()) {
                    data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.traveller.data")));
                }

                for (WorldNetworkTraveller traveller : node.getTravellers()) {
                    float distance = Float.valueOf(traveller.activePath.getIndex()) / Float.valueOf(traveller.activePath.pathPositions().size()) * 10F;
                    distance += traveller.travelledDistance;
                    distance -= 0.1F;
                    distance = MathHelper.clamp(distance, 0F, 10F);
                    if (distance > 0) {
                        ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
                        data.add(new ProbeData(new TextComponentString(stack.getDisplayName()))
                                .withInventory(ImmutableList.of(stack))
                                .withBar(0, distance * 10, 100, UnitDictionary.PERCENT));
                    }
                }
            }

            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < buffer.getSlots(); i++) {
                stacks.add(buffer.getStackInSlot(i));
            }

            ProbeData bufferData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(bufferData);

            if (!returnedTravellers.isEmpty()) {
                ProbeData returnedTravellerData = new ProbeData(new TextComponentTranslation("tooltip.teckle.sortingmachine.returns"))
                        .withInventory(ImmutableList.copyOf(returnedTravellers.stream().map
                                (traveller -> new ItemStack(traveller.data.getCompoundTag("stack"))).collect(Collectors.toList())));
                data.add(returnedTravellerData);
            }
        }
    }

}