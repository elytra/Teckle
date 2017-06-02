package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class SortingMachineEndpoint extends WorldNetworkEndpoint {
    public SortingMachineEndpoint(IWorldNetwork network, BlockPos position, EnumFacing capabilityFace) {
        super(network, position, capabilityFace);
    }

    @Override
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        return ((TileSortingMachine) (getNetworkTile().getTileEntity())).getSortMode().acceptTraveller(traveller, from);
    }

}
