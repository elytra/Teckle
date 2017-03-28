package com.elytradev.teckle.worldnetwork.item;

import com.elytradev.teckle.worldnetwork.WorldNetworkEndpoint;
import com.elytradev.teckle.worldnetwork.WorldNetworkTraveller;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Created by darkevilmac on 3/27/2017.
 */
public class ItemNetworkEndpoint extends WorldNetworkEndpoint {

    @Override
    public boolean inject(WorldNetworkTraveller traveller) {
        EnumFacing injectionFace = getInjectionFace(traveller);
        TileEntity endPointTile = network.getWorld().getTileEntity(position);

        if (endPointTile instanceof ICapabilityProvider) {
            ICapabilityProvider endPointCapabilityProvider = endPointTile;
            if (endPointCapabilityProvider.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, injectionFace)) {
                IItemHandler itemHandler = endPointCapabilityProvider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, injectionFace);

                ItemStack dataStack = new ItemStack(traveller.data.getCompoundTag("stack"));
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    dataStack = itemHandler.insertItem(slot, dataStack, false);

                    if (dataStack.isEmpty())
                        return true;
                }

                traveller.data.setTag("stack", dataStack.getTagCompound());
            }
        }

        return super.inject(traveller);
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller) {
        return super.canAcceptTraveller(traveller);
    }

    private EnumFacing getInjectionFace(WorldNetworkTraveller traveller) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (traveller.previousNode.position.subtract(traveller.currentNode.position).equals(facing.getDirectionVec())) {
                return facing.getOpposite();
            }
        }

        return EnumFacing.DOWN;
    }

}
