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
import com.elytradev.teckle.client.gui.GuiSortingMachine;
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.network.messages.clientbound.TileLitMessage;
import com.elytradev.teckle.common.tile.TileLitNetworkMember;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullModeSingleStep;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModeAnyStack;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class TileSortingMachine extends TileLitNetworkMember implements IElementProvider {

    public EnumFacing cachedFace = EnumFacing.DOWN;
    public AdvancedStackHandlerEntry bufferData, filterData;
    public UUID bufferID, filterID;

    public DefaultRoute defaultRoute = DefaultRoute.NONE;
    public EnumDyeColor[] colours = new EnumDyeColor[8];

    private int selectorPos = -1;
    private List<IItemHandler> subHandlers;
    private NetworkTileSortingMachineOutput outputTile;
    private NetworkTileSortingMachineInput inputTile;

    @Override
    public void validate() {
        try {
            if (filterID == null) {
                if (filterData == null) {
                    filterData = new AdvancedStackHandlerEntry(UUID.randomUUID(), world.provider.getDimension(), pos, new AdvancedItemStackHandler(48));
                }
                filterID = filterData.getId();
            } else {
                filterData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(filterID);
            }
            if (bufferID == null) {
                if (bufferData == null) {
                    bufferData = new AdvancedStackHandlerEntry(UUID.randomUUID(), world.provider.getDimension(), pos, new AdvancedItemStackHandler(32));
                }
                bufferID = bufferData.getId();
            } else {
                bufferData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(bufferID);
            }
            if (this.inputTile == null)
                this.inputTile = new NetworkTileSortingMachineInput(this);
            if (this.outputTile == null)
                this.outputTile = new NetworkTileSortingMachineOutput(this);

            this.inputTile.filterData = this.filterData;
            this.inputTile.bufferData = this.bufferData;
            this.inputTile.filterID = this.filterID;
            this.inputTile.bufferID = this.bufferID;

            this.outputTile.filterData = this.filterData;
            this.outputTile.bufferData = this.bufferData;
            this.outputTile.filterID = this.filterID;
            this.outputTile.bufferID = this.bufferID;

            this.inputTile.setOtherTile(outputTile);
            this.outputTile.setOtherTile(inputTile);

            this.tileEntityInvalid = false;
        } catch (Exception e) {
            TeckleLog.error("Failed to validate sortingmachine. {}", e);
        }
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
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("sortmodeID", getSortMode().getID());
        tag.setInteger("pullmodeID", getPullMode().getID());
        return this.writeToNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);

        try {
            if (tag.hasKey("sortmodeID")) {
                getOutputTile().setSortMode(SortMode.SORT_MODES.get(tag.getInteger("sortmodeID")).newInstance());
            }
            if (tag.hasKey("pullmodeID")) {
                getOutputTile().setPullMode(PullMode.PULL_MODES.get(tag.getInteger("pullmodeID")).newInstance());
            }
        } catch (Exception e) {
            // just eat the exception it doesnt matter that much
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return oldState.getBlock() != newSate.getBlock() && super.shouldRefresh(world, pos, oldState, newSate);

    }

    @Override
    public void update() {
        super.update();

        if (world.isRemote)
            return;

        if (!getReturnedTravellers().isEmpty()) {
            WorldNetworkTraveller traveller = getReturnedTravellers().get(0);
            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos.offset(outputTile.getOutputFace()), outputTile.getOutputFace().getOpposite())) {
                BlockPos outputPos = pos.offset(outputTile.getOutputFace());

                ItemStack stackToInsert = new ItemStack(traveller.data.getCompoundTag("stack"));
                ImmutableMap<String, NBTBase> collect = ImmutableMap.copyOf(traveller.data.getKeySet().stream().collect(Collectors.toMap(o -> o, o -> traveller.data.getTag(o))));
                ItemStack result = (ItemStack) getNetworkAssistant(ItemStack.class).insertData((WorldNetworkEntryPoint) outputTile.getNode(),
                        outputPos, stackToInsert.copy(), collect, false, false);

                if (result.isEmpty()) {
                    getReturnedTravellers().remove(0);
                } else {
                    getReturnedTravellers().get(0).data.setTag("stack", result.serializeNBT());
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
            EnumFacing facing = outputTile.getOutputFace();
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
            EnumFacing facing = outputTile.getOutputFace();
            BlockPos sourcePos = pos.offset(facing);

            TileEntity sourceTile = world.getTileEntity(sourcePos);
            if (sourceTile != null && sourceTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
                return sourceTile;
            }
        }

        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.defaultRoute = DefaultRoute.byMetadata(tag.getInteger("defaultRoute"));
        this.cachedFace = EnumFacing.values()[tag.getInteger("cachedFace")];
        NBTTagList coloursTag = tag.getTagList("colours", 3);
        for (int i = 0; i < 8; i++) {
            if (coloursTag.getIntAt(i) > -1) {
                this.colours[i] = EnumDyeColor.byMetadata(coloursTag.getIntAt(i));
            } else {
                this.colours[i] = null;
            }
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (tag.hasKey("filterRows")) {
                validate();
                filterData.getHandler().deserializeNBT(tag.getCompoundTag("filterRows"));
                bufferData.getHandler().deserializeNBT(tag.getCompoundTag("buffer"));

                int returnedTravellersSize = tag.getInteger("returnedTravellers");
                getOutputTile().returnedTravellers = Lists.newArrayListWithExpectedSize(returnedTravellersSize);
                for (int i = 0; i < returnedTravellersSize; i++) {
                    WorldNetworkTraveller deserializedTraveller = new WorldNetworkTraveller(new NBTTagCompound());
                    deserializedTraveller.deserializeNBT(tag.getCompoundTag("returnedTravellers" + i));
                    tag.removeTag("returnedTravellers" + i);
                    getOutputTile().returnedTravellers.set(i, deserializedTraveller);
                }

                try {
                    getOutputTile().setSortMode(SortMode.SORT_MODES.get(tag.getInteger("sortmodeID")).newInstance());
                    getOutputTile().setPullMode(PullMode.PULL_MODES.get(tag.getInteger("pullmodeID")).newInstance());

                    getOutputTile().getSortMode().deserializeNBT(tag.getCompoundTag("sortmode"));
                    getOutputTile().getPullMode().deserializeNBT(tag.getCompoundTag("pullmode"));
                } catch (Exception e) {
                    TeckleLog.warn("Failed to deserialize pull mode and sort mode, they will be reset to default. Caught: {}", e);
                    getOutputTile().setSortMode(new SortModeAnyStack());
                    getOutputTile().setPullMode(new PullModeSingleStep());
                }

                tag.removeTag("sortmodeID");
                tag.removeTag("pullmodeID");
                tag.removeTag("sortmode");
                tag.removeTag("pullmode");
                tag.removeTag("returnedTravellers");
                tag.removeTag("filterRows");
                tag.removeTag("buffer");
            } else {
                int dimID = tag.getInteger("databaseID");
                if (tag.hasUniqueId("buffer")) {
                    this.bufferID = tag.getUniqueId("buffer");
                    this.bufferData = AdvancedStackHandlerPool.getPool(dimID).get(bufferID);
                }
                if (tag.hasUniqueId("filter")) {
                    this.filterID = tag.getUniqueId("filter");
                    this.filterData = AdvancedStackHandlerPool.getPool(dimID).get(filterID);
                }
            }

            if (loadNetworkTile(tag, "inputTileID", getFacing().getOpposite(), NetworkTileSortingMachineInput.class)) {
                loadNetworkTile(tag, "outputTileID", getFacing(), NetworkTileSortingMachineOutput.class);
            }
            validate();
        }
    }

    protected boolean loadNetworkTile(NBTTagCompound tag, String tileIDKey, EnumFacing tileFace, Class<? extends WorldNetworkTile> tileType) {
        UUID networkID = tag.hasUniqueId(tileIDKey) ? tag.getUniqueId(tileIDKey) : null;
        int dimID = tag.getInteger("databaseID");
        if (networkID == null) {
            getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            return false;
        } else {
            WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
            Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                    .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), tileFace)).findAny();
            if (any.isPresent()) {
                networkID = networkDB.getRemappedNodes().remove(any.get());
                TeckleLog.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
            }

            IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
            for (NodeContainer container : network.getNodeContainersAtPosition(pos)) {
                if (Objects.equals(container.getFacing(), tileFace) && container.getNetworkTile() != null && tileType.isInstance(container.getNetworkTile())) {
                    if (tileType == NetworkTileSortingMachineOutput.class) {
                        outputTile = (NetworkTileSortingMachineOutput) container.getNetworkTile();
                    } else {
                        inputTile = (NetworkTileSortingMachineInput) container.getNetworkTile();
                    }
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("defaultRoute", this.defaultRoute.getMetadata());
        tag.setInteger("cachedFace", this.outputTile.getOutputFace().getIndex());
        NBTTagList coloursTag = new NBTTagList();
        for (EnumDyeColor colour : this.colours) {
            if (colour != null) {
                coloursTag.appendTag(new NBTTagInt(colour.getMetadata()));
            } else {
                coloursTag.appendTag(new NBTTagInt(-1));
            }
        }
        tag.setTag("colours", coloursTag);
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (bufferData == null || filterData == null) {
                validate();
            }
            tag.setUniqueId("buffer", bufferData.getId());
            tag.setUniqueId("filter", filterData.getId());

            tag.setInteger("databaseID", getWorld().provider.getDimension());
            if (inputTile.getNode() == null || outputTile.getNode() == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            tag.setUniqueId("inputTileID", inputTile.getNode().getNetwork().getNetworkID());
            tag.setUniqueId("outputTileID", outputTile.getNode().getNetwork().getNetworkID());
        }
        return super.writeToNBT(tag);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new ProbeCapability();
            return (T) probeCapability;
        }
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            if (Objects.equals(facing, outputTile.getCapabilityFace()))
                return (T) outputTile;
            else if (Objects.equals(facing, inputTile.getCapabilityFace()))
                return (T) inputTile;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
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
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockSortingMachine)) {
                cachedFace = thisState.getValue(BlockSortingMachine.FACING);
            }
        }

        return cachedFace;
    }

    public ItemStack addToNetwork(IItemHandler source, int slot, int quantity, ImmutableMap<String, NBTBase> additionalData) {
        ItemStack remaining = source.extractItem(slot, quantity, false).copy();
        IWorldNetworkAssistant<ItemStack> networkAssistant = getNetworkAssistant(ItemStack.class);
        remaining = networkAssistant.insertData((WorldNetworkEntryPoint) getOutputTile().getNode(), pos.offset(getFacing()), remaining, additionalData, false, false).copy();

        if (!remaining.isEmpty()) {
            if (remaining.getCount() != quantity) {
                setTriggered();
            }

            for (int i = 0; i < bufferData.getHandler().getSlots() && !remaining.isEmpty(); i++) {
                remaining = bufferData.getHandler().insertItem(i, remaining, false);
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
        skipBuffer |= bufferData == null;
        List<SlotData> stacks = Lists.newArrayList();

        if (!skipBuffer && !bufferData.getHandler().stream().allMatch(ItemStack::isEmpty)) {
            for (int i = 0; i < bufferData.getHandler().getStacks().size(); i++) {
                stacks.add(new SlotData(bufferData.getHandler(), i));
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

    public NetworkTileSortingMachineOutput getOutputTile() {
        return outputTile;
    }

    public PullMode getPullMode() {
        return getOutputTile().getPullMode();
    }

    public void setPullMode(PullMode pullMode) {
        this.getOutputTile().setPullMode(pullMode);
    }

    public SortMode getSortMode() {
        return getOutputTile().getSortMode();
    }

    public void setSortMode(SortMode sortMode) {
        this.getOutputTile().setSortMode(sortMode);

        if (this.getPullMode().isPaused()) {
            this.getPullMode().unpause();
        }
    }

    public int getSelectorPos() {
        return selectorPos;
    }

    public void setSelectorPos(int selectorPos) {
        this.selectorPos = selectorPos;
    }

    public List<WorldNetworkTraveller> getReturnedTravellers() {
        return outputTile.returnedTravellers;
    }

    public List<IItemHandler> getCompartmentHandlers() {
        if (subHandlers == null || subHandlers.isEmpty()) {
            subHandlers = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                subHandlers.add(filterData.getHandler().subHandler(i * 6, 6));
            }
        }

        return subHandlers;
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

        @Override
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
                faceName = faceName.substring(0, 1).toUpperCase() + faceName.substring(1, faceName.length());
                if (node == null || nodes.contains(node))
                    continue;

                nodes.add(node);
                if (TeckleMod.INDEV)
                    data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network",
                            faceName,
                            node.getNetwork().getNetworkID().toString().toUpperCase().replaceAll("-", ""),
                            node.getNetwork().getNodePositions().size())));

                if (!node.getTravellers().isEmpty()) {
                    data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.traveller.data")));
                }

                for (WorldNetworkTraveller traveller : node.getTravellers()) {
                    float distance = (float) traveller.activePath.getIndex() / (float) traveller.activePath.pathPositions().size() * 10F;
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
            for (int i = 0; i < bufferData.getHandler().getSlots(); i++) {
                stacks.add(bufferData.getHandler().getStackInSlot(i));
            }

            ProbeData bufferData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(bufferData);

            if (!getReturnedTravellers().isEmpty()) {
                ProbeData returnedTravellerData = new ProbeData(new TextComponentTranslation("tooltip.teckle.sortingmachine.returns"))
                        .withInventory(ImmutableList.copyOf(getReturnedTravellers().stream().map
                                (traveller -> new ItemStack(traveller.data.getCompoundTag("stack"))).collect(Collectors.toList())));
                data.add(returnedTravellerData);
            }
        }
    }

}