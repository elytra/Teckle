package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockBreaker;
import com.elytradev.teckle.common.tile.TileBreaker;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;
import java.util.UUID;

public class NetworkTileBreaker extends WorldNetworkTile {
    public EnumFacing cachedFace = EnumFacing.DOWN;
    public UUID bufferID;
    public AdvancedStackHandlerEntry bufferData;

    public NetworkTileBreaker(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileBreaker(TileBreaker tileBreaker) {
        super(tileBreaker.getWorld(), tileBreaker.getPos(), null);
        this.bufferID = tileBreaker.bufferID;
        this.bufferData = tileBreaker.bufferData;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        this.setPos(pos);
        return new WorldNetworkEntryPoint(network, pos, getCapabilityFace(), getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return Objects.equals(traveller.getEntryPoint().getPosition(), this.getPos())
                || Objects.equals(from, getOutputFace().getOpposite()) && !this.isPowered();

    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side.equals(getOutputFace()) || side.getOpposite().equals(getOutputFace());
    }

    @Override
    public EnumFacing getOutputFace() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (thisState.getBlock() instanceof BlockBreaker) {
                cachedFace = thisState.getValue(BlockBreaker.FACING);
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

    private boolean isPowered() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())
                && Objects.equals(getWorld().getBlockState(getPos()).getBlock(), TeckleObjects.blockBreaker)) {
            return getWorld().getBlockState(getPos()).getValue(BlockBreaker.TRIGGERED);
        }
        return false;
    }

    protected void handleReturnStack(EnumFacing from, ItemStack stack, EnumFacing neighbourFace) {
        // Try and put it back where we found it.
        if (Objects.equals(from, getOutputFace())) {
            if (getWorld().getTileEntity(getPos().offset(neighbourFace.getOpposite())) != null) {
                TileEntity pushTo = getWorld().getTileEntity(getPos().offset(neighbourFace.getOpposite()));
                if (pushTo.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, neighbourFace)) {
                    IItemHandler itemHandler = pushTo.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, neighbourFace);
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
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("cachedFace", getOutputFace().getIndex());
        tag.setUniqueId("buffer", bufferID);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        this.cachedFace = EnumFacing.values()[tag.getInteger("cachedFace")];
        this.bufferID = tag.getUniqueId("buffer");
        this.bufferData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(bufferID);
    }
}
