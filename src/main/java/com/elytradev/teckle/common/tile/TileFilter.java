package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.tile.base.TileItemEntrypoint;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkEntryPoint;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class TileFilter extends TileItemEntrypoint implements ITickable {

    /**
     * Attempt to push to our network, by pulling from our input position.
     *
     * @return true if a push occurred, false otherwise.
     */
    public boolean pushToNetwork() {
        boolean result = false;

        IBlockState state = world.getBlockState(pos);

        if (node != null && node.network != null && !world.isRemote) {
            WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) node.network.getNodeFromPosition(pos);
            EnumFacing facing = state.getValue(BlockFilter.FACING);

            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null) {
                TileEntity pullFrom = world.getTileEntity(pos.offset(facing.getOpposite()));
                if (pullFrom.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                    IItemHandler itemHandler = pullFrom.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                    ItemStack extractionData = ItemStack.EMPTY;
                    for (int slot = 0; slot < itemHandler.getSlots() && extractionData == ItemStack.EMPTY; slot++) {
                        extractionData = itemHandler.extractItem(slot, 8, false);
                    }

                    if (extractionData != ItemStack.EMPTY) {
                        NBTTagCompound tagCompound = new NBTTagCompound();
                        tagCompound.setTag("stack", extractionData.writeToNBT(new NBTTagCompound()));
                        thisNode.addTraveller(tagCompound);
                        result = true;
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void update() {
        if (world.isRemote || node == null || node.network == null)
            return;

        if (!node.network.isNodePresent(pos))
            System.out.println(this + " Network " + node.network);
    }
}
