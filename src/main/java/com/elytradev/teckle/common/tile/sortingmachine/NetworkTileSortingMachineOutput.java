package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullModeSingleStep;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModeAnyStack;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Objects;

public class NetworkTileSortingMachineOutput extends NetworkTileSortingMachineBase {

    private PullMode pullMode = new PullModeSingleStep();
    private SortMode sortMode = new SortModeAnyStack();
    public List<WorldNetworkTraveller> returnedTravellers = Lists.newArrayList();

    private EnumFacing cachedFace = EnumFacing.DOWN;

    public NetworkTileSortingMachineOutput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileSortingMachineOutput(TileSortingMachine sortingMachine) {
        super(sortingMachine.getWorld(), sortingMachine.getPos(), sortingMachine.getFacing());
    }

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
        if (Objects.equals(traveller.getEntryPoint().position, getPos()))
            return true;

        if (Objects.equals(from, getOutputFace().getOpposite())) {
            // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
            return getSortMode().canAcceptTraveller(this, traveller, from);
        }
        return false;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return Objects.equals(side, getOutputFace());
    }

    @Override
    public EnumFacing getOutputFace() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockSortingMachine)) {
                cachedFace = thisState.getValue(BlockSortingMachine.FACING);
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

    @Override
    public NBTTagCompound serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {

    }

    @Override
    public SortMode getSortMode() {
        return sortMode;
    }

    @Override
    public PullMode getPullMode() {
        return pullMode;
    }

    public void setPullMode(PullMode pullMode) {
        this.pullMode = pullMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }
}
