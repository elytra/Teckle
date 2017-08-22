package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public class NetworkTileSortingMachineInput extends NetworkTileSortingMachineBase {

    public NetworkTileSortingMachineInput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileSortingMachineInput(TileSortingMachine sortingMachine) {
        super(sortingMachine.getWorld(), sortingMachine.getPos(), sortingMachine.getFacing().getOpposite());

        this.filterData = sortingMachine.filterData;
        this.bufferData = sortingMachine.bufferData;
        this.filterID = sortingMachine.filterID;
        this.bufferID = sortingMachine.bufferID;
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
        return getOutputTile().getSortMode();
    }

    @Override
    public PullMode getPullMode() {
        return getOutputTile().getPullMode();
    }

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
        return getSortMode().canAcceptTraveller(this, traveller, from);
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return Objects.equals(side, getCapabilityFace());
    }

}
