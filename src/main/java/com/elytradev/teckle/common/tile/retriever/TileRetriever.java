package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.probe.api.IProbeData;
import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.probe.api.UnitDictionary;
import com.elytradev.probe.api.impl.ProbeData;
import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.client.gui.GuiRetriever;
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockRetriever;
import com.elytradev.teckle.common.container.ContainerRetriever;
import com.elytradev.teckle.common.tile.TileLitNetworkMember;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileRetriever extends TileLitNetworkMember implements IElementProvider {

    public NetworkTileRetrieverOutput outputTile;
    public NetworkTileRetrieverInput inputTile;
    public EnumFacing cachedFace = EnumFacing.DOWN;

    public AdvancedStackHandlerEntry bufferData, filterData;
    public UUID bufferID, filterID;

    @Override
    public void validate() {
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
            this.inputTile = new NetworkTileRetrieverInput(this);
        if (this.outputTile == null)
            this.outputTile = new NetworkTileRetrieverOutput(this);

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
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = new NBTTagCompound();
        return this.writeToNBT(tag);
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
        return oldState.getBlock() != newSate.getBlock() && super.shouldRefresh(world, pos, oldState, newSate);

    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new ProbeCapability();
            return (T) probeCapability;
        }
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            if (Objects.equals(facing, getFacing()))
                return (T) outputTile;
            else if (Objects.equals(facing, getFacing().getOpposite()))
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

    public EnumFacing getFacing() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockRetriever)) {
                cachedFace = thisState.getValue(BlockRetriever.FACING);
            }
        }

        return cachedFace;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.cachedFace = EnumFacing.values()[tag.getInteger("cachedFace")];

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            int dimID = tag.getInteger("databaseID");
            this.bufferID = tag.getUniqueId("buffer");
            this.filterID = tag.getUniqueId("filter");
            this.bufferData = AdvancedStackHandlerPool.getPool(dimID).get(bufferID);
            this.filterData = AdvancedStackHandlerPool.getPool(dimID).get(filterID);

            if (loadNetworkTile(tag, "inputTileID", getFacing().getOpposite(), NetworkTileRetrieverInput.class)) {
                loadNetworkTile(tag, "outputTileID", getFacing(), NetworkTileRetrieverOutput.class);
            }
        }

        if (this.outputTile != null)
            this.outputTile.setColour(tag.getInteger("colour") < 0 ? null
                    : EnumDyeColor.byMetadata(tag.getInteger("colour")));
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
                    .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), inputTile.getCapabilityFace())).findAny();
            if (any.isPresent()) {
                networkID = networkDB.getRemappedNodes().remove(any.get());
                TeckleLog.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
            }

            IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
            for (NodeContainer container : network.getNodeContainersAtPosition(pos)) {
                if (Objects.equals(container.getFacing(), tileFace) && container.getNetworkTile() != null && tileType.isInstance(container.getNetworkTile())) {
                    if (tileType == NetworkTileRetrieverOutput.class) {
                        outputTile = (NetworkTileRetrieverOutput) container.getNetworkTile();
                    } else {
                        inputTile = (NetworkTileRetrieverInput) container.getNetworkTile();
                    }
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("cachedFace", this.outputTile.getOutputFace().getIndex());
        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("filter", filterID);
        tag.setInteger("colour", outputTile.getColour() == null ? -1 : outputTile.getColour().getMetadata());

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
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
    public void update() {
        super.update();
    }

    public void onPulse() {
        outputTile.onPulse();
    }

    public EnumDyeColor getColour() {
        return outputTile.getColour();
    }

    public void setColour(EnumDyeColor colour) {
        outputTile.setColour(colour);
    }

    public boolean useSelector() {
        return outputTile.useSelector();
    }

    public void setUseSelector(boolean useSelector) {
        outputTile.useSelector = useSelector;
    }

    public boolean matchCount() {
        return outputTile.matchCount;
    }

    public void setMatchCount(boolean matchCount) {
        outputTile.matchCount = matchCount;
    }

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerRetriever(this, player);
    }

    @Override
    public Object getClientElement(EntityPlayer player) {
        return new GuiRetriever(this, player);
    }

    public int getSelectorPosition() {
        return outputTile.selectorPosition;
    }

    public void setSelectorPos(int selectorPos) {
        this.outputTile.selectorPosition = selectorPos;
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
        }
    }
}
