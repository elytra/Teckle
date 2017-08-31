package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleMod;
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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NetworkTileSortingMachineOutput extends NetworkTileSortingMachineBase {

    public List<WorldNetworkTraveller> returnedTravellers = Lists.newArrayList();
    private PullMode pullMode = new PullModeSingleStep();
    private SortMode sortMode = new SortModeAnyStack();
    private EnumFacing cachedFace = EnumFacing.DOWN;

    public NetworkTileSortingMachineOutput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileSortingMachineOutput(TileSortingMachine sortingMachine) {
        super(sortingMachine.getWorld(), sortingMachine.getPos(), sortingMachine.getFacing());

        this.filterData = sortingMachine.filterData;
        this.bufferData = sortingMachine.bufferData;
        this.filterID = sortingMachine.filterID;
        this.bufferID = sortingMachine.bufferID;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return new WorldNetworkEntryPoint(network, pos, getCapabilityFace(), getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (Objects.equals(traveller.getEntryPoint().getPosition(), getPos()))
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
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return Objects.equals(side, getOutputFace());
    }

    @Override
    public EnumFacing getOutputFace() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockSortingMachine)) {
                cachedFace = thisState.getValue(BlockSortingMachine.FACING);
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
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockSortingMachine)) {
                setCapabilityFace(thisState.getValue(BlockSortingMachine.FACING));
            }
        }

        return super.getCapabilityFace();
    }

    @Override
    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }

    @Override
    public PullMode getPullMode() {
        return pullMode;
    }

    public void setPullMode(PullMode pullMode) {
        this.pullMode = pullMode;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = super.serializeNBT();
        tag.setInteger("cachedFace", getOutputFace().getIndex());
        tag.setInteger("pullmodeid", pullMode.getID());
        tag.setInteger("sortmodeid", sortMode.getID());
        tag.setTag("pullmode", getPullMode().serializeNBT());
        tag.setTag("sortmode", getSortMode().serializeNBT());

        NBTTagList returnedTravellerList = new NBTTagList();
        this.returnedTravellers.stream().map(WorldNetworkTraveller::serializeNBT).forEach(returnedTravellerList::appendTag);
        tag.setTag("returnedTravellers", returnedTravellerList);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        super.deserializeNBT(tag);
        this.cachedFace = EnumFacing.VALUES[tag.getInteger("cachedFace")];
        try {
            this.sortMode = SortMode.SORT_MODES.get(tag.getInteger("sortmodeid")).newInstance();
            this.pullMode = PullMode.PULL_MODES.get(tag.getInteger("pullmodeid")).newInstance();

            this.sortMode.deserializeNBT(tag.getCompoundTag("sortmode"));
            this.pullMode.deserializeNBT(tag.getCompoundTag("pullmode"));
        } catch (Exception e) {
            TeckleMod.LOG.warn("Failed to deserialize pull mode and sort mode, they will be reset to default. Caught: {}", e);
            this.sortMode = new SortModeAnyStack();
            this.pullMode = new PullModeSingleStep();
        }

        this.returnedTravellers = new ArrayList<>();
        NBTTagList returnedTravellersList = tag.getTagList("returnedTravellers", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < returnedTravellersList.tagCount(); i++) {
            NBTTagCompound travellerTag = returnedTravellersList.getCompoundTagAt(i);
            this.returnedTravellers.add(new WorldNetworkTraveller(travellerTag));
        }
    }
}
