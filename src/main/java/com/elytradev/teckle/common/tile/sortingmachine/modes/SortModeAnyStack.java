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

package com.elytradev.teckle.common.tile.sortingmachine.modes;

import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Created by darkevilmac on 5/22/17.
 */
public class SortModeAnyStack extends SortMode {
    public SortModeAnyStack() {
        super(3, "sortmode.anystack", SortModeType.SLOT);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        if (sortingMachine.getSource() == null)
            return false;

        IItemHandler sourceItemHandler = sortingMachine.getSource().getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, sortingMachine.getFacing().getOpposite());

        for (int compartmentNumber = 0; compartmentNumber < sortingMachine.getCompartmentHandlers().size(); compartmentNumber++) {
            IItemHandler compartment = sortingMachine.getCompartmentHandlers().get(compartmentNumber);
            EnumDyeColor compartmentColour = sortingMachine.colours[compartmentNumber];

            for (int slot = 0; slot < compartment.getSlots(); slot++) {
                ItemStack compartmentStack = compartment.getStackInSlot(slot);

                if (compartmentStack.isEmpty())
                    continue;

                // Found an item, scan the pull place for it.
                for (int sourceSlot = 0; sourceSlot < sourceItemHandler.getSlots(); sourceSlot++) {

                }
            }
        }

        return false;
    }

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller
     * @return
     */
    @Override
    public boolean canAcceptTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller) {
        if (traveller.data.hasKey("stack")) {
            ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

            for (int compartmentNumber = 0; compartmentNumber < sortingMachine.getCompartmentHandlers().size(); compartmentNumber++) {
                IItemHandler compartment = sortingMachine.getCompartmentHandlers().get(compartmentNumber);
                for (int slot = 0; slot < compartment.getSlots(); slot++) {
                    ItemStack stackInSlot = compartment.getStackInSlot(slot);
                    if (ItemStack.areItemsEqual(travellerStack, stackInSlot)) {
                        return true;
                    }
                }
            }
        }

        return !sortingMachine.defaultRoute.isBlocked();
    }

    /**
     * Sort a traveller going through the network and change it if needed.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller      the traveller entering the machine.
     * @return the modified traveller.
     */
    @Override
    public WorldNetworkTraveller processExistingTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller) {
        if (traveller.data.hasKey("stack")) {
            ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

            for (int compartmentNumber = 0; compartmentNumber < sortingMachine.getCompartmentHandlers().size(); compartmentNumber++) {
                IItemHandler compartment = sortingMachine.getCompartmentHandlers().get(compartmentNumber);
                EnumDyeColor compartmentColour = sortingMachine.colours[compartmentNumber];

                for (int slot = 0; slot < compartment.getSlots(); slot++) {
                    ItemStack stackInSlot = compartment.getStackInSlot(slot);
                    if (ItemStack.areItemsEqual(travellerStack, stackInSlot)) {
                        traveller.data.setInteger("colour", compartmentColour.getMetadata());
                        return traveller;
                    }
                }
            }

            if (!sortingMachine.defaultRoute.isBlocked()) {
                if (sortingMachine.defaultRoute.isColoured()) {
                    traveller.data.setInteger("colour", sortingMachine.defaultRoute.getColour().getMetadata());
                } else {
                    traveller.data.removeTag("colour");
                }
            }
        }

        return traveller;
    }

    /**
     * Get the position of the selector, if no selector is used in this mode return -1.
     *
     * @param sortingMachine the sorting machine.
     * @return the selector position, a value between -1 and 7 (inclusive)
     */
    @Override
    public int selectorPosition(TileSortingMachine sortingMachine) {
        return -1;
    }

    @Override
    public NBTBase serializeNBT() {
        // We don't store anything of interest.
        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        // We don't store anything of interest.
    }
}
