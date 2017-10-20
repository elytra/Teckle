package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockRetriever;
import com.elytradev.teckle.common.tile.TileLitNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TileRetriever extends TileLitNetworkMember {

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
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            if (Objects.equals(facing, getFacing()))
                return (T) outputTile;
            else if (Objects.equals(facing, getFacing().getOpposite()))
                return (T) inputTile;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
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

}
