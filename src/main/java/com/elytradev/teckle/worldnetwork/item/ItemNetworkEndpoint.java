package com.elytradev.teckle.worldnetwork.item;

import com.elytradev.teckle.worldnetwork.WorldNetwork;
import com.elytradev.teckle.worldnetwork.WorldNetworkEndpoint;
import com.elytradev.teckle.worldnetwork.WorldNetworkTraveller;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Created by darkevilmac on 3/27/2017.
 */
public class ItemNetworkEndpoint extends WorldNetworkEndpoint {

    public ItemNetworkEndpoint(WorldNetwork network, BlockPos pos) {
        super(network, pos);
    }

    @Override
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        TileEntity endPointTile = network.getWorld().getTileEntity(position);

        if (endPointTile instanceof ICapabilityProvider) {
            ICapabilityProvider endPointCapabilityProvider = endPointTile;
            if (endPointCapabilityProvider.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from)) {
                IItemHandler itemHandler = endPointCapabilityProvider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from);

                ItemStack dataStack = new ItemStack(traveller.data.getCompoundTag("stack"));
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    dataStack = itemHandler.insertItem(slot, dataStack, false);

                    if (dataStack.isEmpty())
                        return true;
                }

                traveller.data.setTag("stack", dataStack.getTagCompound());
            }
        }

        return super.inject(traveller, from);
    }

    private boolean canInject(WorldNetworkTraveller traveller, EnumFacing from) {
        TileEntity endPointTile = network.getWorld().getTileEntity(position);

        if (endPointTile instanceof ICapabilityProvider) {
            ICapabilityProvider endPointCapabilityProvider = endPointTile;
            if (endPointCapabilityProvider.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from)) {
                IItemHandler itemHandler = endPointCapabilityProvider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from);

                ItemStack dataStack = new ItemStack(traveller.data.getCompoundTag("stack"));
                ItemStack initialStack = dataStack.copy();
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    dataStack = itemHandler.insertItem(slot, dataStack, true);

                    if (dataStack.isEmpty() || dataStack.getCount() != initialStack.getCount())
                        return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return super.canAcceptTraveller(traveller, from) && canInject(traveller, from);
    }

}
