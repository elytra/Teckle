package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
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

public class NetworkTileRetrieverOutput extends NetworkTileRetrieverBase {

    public NetworkTileRetrieverOutput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return new WorldNetworkEntryPoint(network, pos, getOutputFace(), getCapabilityFace());
    }

    @Override
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return side.equals(getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return traveller.getEntryPoint().position.equals(getPos());
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side.equals(getOutputFace());
    }

    @Override
    public EnumFacing getOutputFace() {
        if (getWorld() != null) {
            IBlockState thisState = getWorld().getBlockState(getPos());
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
        EnumFacing facing = getOutputFace();

        // Try and put it back where we found it.
        if (side.equals(getOutputFace())) {
            if (getWorld().getTileEntity(getPos().offset(facing.getOpposite())) != null) {
                TileEntity pushTo = getWorld().getTileEntity(getPos().offset(facing.getOpposite()));
                if (pushTo.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                    IItemHandler itemHandler = pushTo.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                    for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
                        stack = itemHandler.insertItem(slot, stack, false);
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            IItemHandler itemHandler = bufferData.getHandler();
            for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
                stack = itemHandler.insertItem(slot, stack, false);
            }
            traveller.data.setTag("stack", stack.serializeNBT());
            if (!stack.isEmpty())
                DropActions.ITEMSTACK.getSecond().dropToWorld(traveller);
        }
    }

    @Override
    public EnumFacing getCapabilityFace() {
        return getOutputFace();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {

    }
}
