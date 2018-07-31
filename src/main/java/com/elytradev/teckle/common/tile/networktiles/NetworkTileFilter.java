package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.UUID;


public class NetworkTileFilter extends NetworkTileTransposer {

    public EnumDyeColor cachedColour = null;
    public UUID filterID;
    public AdvancedStackHandlerEntry filterData;

    public NetworkTileFilter(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileFilter(TileFilter filter) {
        super(filter.getWorld(), filter.getPos(), null);
        this.filterData = filter.filterData;
        this.bufferData = filter.bufferData;
        this.filterID = filter.filterID;
        this.bufferID = filter.bufferID;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        this.setPos(pos);
        return new WorldNetworkEntryPoint(network, pos, getCapabilityFace(), getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (Objects.equals(traveller.getEntryPoint().getPosition(), this.getPos()))
            return true;

        if (Objects.equals(from, getOutputFace().getOpposite()) && !this.isPowered()) {
            // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
            ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));
            boolean foundNonEmptySlot = false;
            boolean colourMatches = !traveller.data.hasKey("colour");
            if (!colourMatches) {
                if (this.getColour() == null) {
                    colourMatches = true;
                } else {
                    colourMatches = Objects.equals(this.getColour(), EnumDyeColor.byMetadata(traveller.data.getInteger("colour")));
                }
            }

            if (!colourMatches)
                return false;

            for (int i = 0; i < filterData.getHandler().getSlots(); i++) {
                if (!filterData.getHandler().getStackInSlot(i).isEmpty()) {
                    foundNonEmptySlot = true;

                    if (filterData.getHandler().getStackInSlot(i).isItemEqualIgnoreDurability(travellerStack)) {
                        return true;
                    }
                }
            }

            return !foundNonEmptySlot;
        }
        return false;
    }

    private boolean isPowered() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos()) && Objects.equals(getWorld().getBlockState(getPos()).getBlock(), TeckleObjects.blockFilter)) {
            return getWorld().getBlockState(getPos()).getValue(BlockFilter.TRIGGERED);
        }
        return false;
    }

    public EnumDyeColor getColour() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos()) && getWorld().getTileEntity(getPos()) instanceof TileFilter) {
            this.cachedColour = ((TileFilter) getWorld().getTileEntity(getPos())).colour;
        }

        return this.cachedColour;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side.equals(getOutputFace()) || side.getOpposite().equals(getOutputFace());
    }

    @Override
    public EnumFacing getOutputFace() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (thisState.getBlock() instanceof BlockFilter) {
                cachedFace = thisState.getValue(BlockFilter.FACING);
                if (getNode() instanceof WorldNetworkEntryPoint)
                    ((WorldNetworkEntryPoint) getNode()).setOutputFace(cachedFace);
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
        handleReturnStack(side, stack, facing);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (getColour() != null) {
            tag.setInteger("colour", getColour().getMetadata());
        } else {
            tag.removeTag("colour");
        }
        tag.setInteger("cachedFace", getOutputFace().getIndex());
        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("filter", filterID);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        this.cachedColour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
        this.cachedFace = EnumFacing.values()[tag.getInteger("cachedFace")];
        this.bufferID = tag.getUniqueId("buffer");
        this.filterID = tag.getUniqueId("filter");
        this.bufferData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(bufferID);
        this.filterData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(filterID);
    }
}
