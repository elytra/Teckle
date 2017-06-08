/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.worldnetwork.item;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEndpoint;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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

    public ItemNetworkEndpoint(IWorldNetwork network, BlockPos pos, EnumFacing capabilityFace) {
        super(network, pos, capabilityFace);
    }

    @Override
    public boolean inject(WorldNetworkTraveller traveller, EnumFacing from) {
        TileEntity endPointTile = network.getWorld().getTileEntity(position);

        if (endPointTile != null) {
            ICapabilityProvider endPointCapabilityProvider = endPointTile;
            if (endPointCapabilityProvider.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from)) {
                IItemHandler itemHandler = endPointCapabilityProvider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, from);

                ItemStack dataStack = new ItemStack(traveller.data.getCompoundTag("stack")).copy();
                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    dataStack = itemHandler.insertItem(slot, dataStack, false);

                    if (dataStack.isEmpty())
                        break;
                }

                if (!dataStack.isEmpty())
                    traveller.data.setTag("stack", dataStack.writeToNBT(new NBTTagCompound()));

                return dataStack.isEmpty();
            }
        }

        return false;
    }

    private boolean canInject(WorldNetworkTraveller traveller, EnumFacing from) {
        TileEntity endPointTile = network.getWorld().getTileEntity(position);

        if (endPointTile != null) {
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
