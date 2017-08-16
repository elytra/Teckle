package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;
import java.util.UUID;


public class NetworkTileFilter extends WorldNetworkTile {

    public EnumFacing cachedFace = EnumFacing.DOWN;
    public EnumDyeColor colour = null;
    public UUID filterID, bufferID;
    public AdvancedStackHandlerEntry filterData, bufferData;

    public NetworkTileFilter(World world) {
        super(world);
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        this.pos = pos;
        return new WorldNetworkEntryPoint(network, pos, getOutputFace(), getCapabilityFace());
    }

    @Override
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return Objects.equals(side, getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (Objects.equals(traveller.getEntryPoint().position, this.pos))
            return true;

        if (Objects.equals(from, getOutputFace().getOpposite()) && !this.isPowered()) {
            // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
            ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));
            boolean foundNonEmptySlot = false;
            boolean colourMatches = !traveller.data.hasKey("colour");
            if (!colourMatches) {
                if (this.colour == null) {
                    colourMatches = true;
                } else {
                    colourMatches = Objects.equals(this.colour, EnumDyeColor.byMetadata(traveller.data.getInteger("colour")));
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
        if (world != null && world.isBlockLoaded(pos) && Objects.equals(world.getBlockState(pos).getBlock(), TeckleObjects.blockFilter)) {
            return world.getBlockState(pos).getValue(BlockFilter.TRIGGERED).booleanValue();
        }
        return false;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side.equals(getOutputFace()) || side.getOpposite().equals(getOutputFace());
    }

    @Override
    public EnumFacing getOutputFace() {
        if (world != null && world.isBlockLoaded(pos)) {
            IBlockState thisState = world.getBlockState(pos);
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockFilter)) {
                return thisState.getValue(BlockFilter.FACING);
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
        BlockPos sourcePos = pos.offset(facing);

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
            ItemStack remaining = stack.copy();
            for (int i = 0; i < bufferData.getHandler().getSlots() && !remaining.isEmpty(); i++) {
                remaining = bufferData.getHandler().insertItem(i, remaining, false);
            }

            // Spawn into the world I guess...
            if (!remaining.isEmpty()) {
                WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
            }
        }
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (colour != null) {
            tag.setInteger("colour", colour.getMetadata());
        } else {
            tag.removeTag("colour");
        }
        tag.setInteger("cachedFace", getOutputFace().getIndex());
        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("filter", filterID);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        NBTTagCompound tag = (NBTTagCompound) nbt;
        this.colour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
        this.cachedFace = EnumFacing.values()[tag.getInteger("cachedFace")];
        this.bufferID = tag.getUniqueId("buffer");
        this.filterID = tag.getUniqueId("filter");
        this.bufferData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(bufferID);
        this.filterData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(filterID);
    }
}
