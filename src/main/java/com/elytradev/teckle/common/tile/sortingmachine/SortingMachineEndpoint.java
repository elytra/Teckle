package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class SortingMachineEndpoint extends WorldNetworkEndpoint {
    public SortingMachineEndpoint(IWorldNetwork network, BlockPos position, EnumFacing capabilityFace) {
        super(network, position, Lists.newArrayList(capabilityFace));
    }

    @Override
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        TileSortingMachine sortingMachine = (TileSortingMachine) this.getNetworkTile(capabilityFaces.get(0)).getTileEntity();
        if (sortingMachine == null)
            return false;
        ItemStack remaining = sortingMachine.getSortMode().acceptTraveller(sortingMachine, traveller, from);

        if (remaining == null)
            return false;

        if (remaining.isEmpty())
            return true;

        traveller.data.setTag("stack", remaining.serializeNBT());
        return false;
    }

}
