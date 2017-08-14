package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by darkevilmac on 8/14/2017.
 */
public class NetworkTileFilter extends WorldNetworkTile {

    public NetworkTileFilter(World world) {
        super(world);
    }

    @Override
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return false;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return null;
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return false;
    }

    @Override
    public NBTBase serializeNBT() {
        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {

    }
}
