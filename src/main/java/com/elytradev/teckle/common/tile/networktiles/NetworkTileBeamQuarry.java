package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NetworkTileBeamQuarry extends WorldNetworkTile {

    /**
     * WorldNetworkTile default constructor, you need to have a constructor with a world, position, and enumfacing to properly load.
     *
     * @param world the world the tile resides in.
     * @param pos   the position of the tile.
     * @param face  the face of the tile.
     */
    public NetworkTileBeamQuarry(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return new WorldNetworkEntryPoint(network, pos, EnumFacing.UP, EnumFacing.UP);
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side == EnumFacing.UP;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {

    }
}
