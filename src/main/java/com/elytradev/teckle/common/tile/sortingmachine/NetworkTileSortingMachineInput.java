package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public class NetworkTileSortingMachineInput extends NetworkTileSortingMachineBase {

    public NetworkTileSortingMachineInput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    @Override
    public EnumFacing getCapabilityFace() {
        return getOutputTile().getOutputFace().getOpposite();
    }

    @Override
    public SortMode getSortMode() {
        return getOutputTile().getSortMode();
    }

    @Override
    public PullMode getPullMode() {
        return getOutputTile().getPullMode();
    }

    public NetworkTileSortingMachineInput(TileSortingMachine sortingMachine) {
        super(sortingMachine.getWorld(), sortingMachine.getPos(), sortingMachine.getFacing().getOpposite());
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

    @Override
    public NBTTagCompound serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {

    }
}
