package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.teckle.common.tile.base.TileNetworkEntrypoint;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller.getFacingFromVector;

/**
 * A node used to add travellers to a network, handles initial endpoint finding, as well as finding new endpoints when one fails.
 */
public class WorldNetworkEntryPoint extends WorldNetworkNode {

    public WorldNetworkEndpoint endpoint = new WorldNetworkEndpoint(network, position) {
        @Override
        public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
            if (network.world.getTileEntity(position) != null && network.world.getTileEntity(position) instanceof TileNetworkEntrypoint) {
                ((TileNetworkEntrypoint) network.world.getTileEntity(position)).acceptReturn(traveller, from);
            }
            return true;
        }
    };
    private EnumFacing facing = EnumFacing.DOWN;

    public WorldNetworkEntryPoint(WorldNetwork network, BlockPos position, EnumFacing facing) {
        this.network = network;
        this.position = position;
        this.facing = facing;

        this.endpoint.position = this.position;
        this.endpoint.network = this.network;
    }

    public void addTraveller(NBTTagCompound data) {
        WorldNetworkTraveller traveller = new WorldNetworkTraveller(this, data);
        traveller.genInitialPath();
        network.registerTraveller(traveller);
    }

    public EnumFacing getFacing() {
        return facing;
    }

}
