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

package com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode;

import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.google.common.collect.ImmutableMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public class SortModeAnyStack extends SortMode {
    public SortModeAnyStack(int id, String unlocalizedName, SortModeType type) {
        super(id, unlocalizedName, type);
    }

    public SortModeAnyStack() {
        super(3, "sortmode.anystack", SortModeType.SLOT);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        if (sortingMachine.getSource() == null)
            return false;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush();
        if (stacksToPush.isEmpty())
            return false;

        IItemHandler pushStackHandler = sortingMachine.getStacksToPush().get(0).itemHandler;
        for (int i = 0; i < stacksToPush.size(); i++) {
            ItemStack stackFromSource = stacksToPush.get(i).getStack();
            if (stackFromSource.isEmpty())
                continue;

            for (int compartmentNumber = 0; compartmentNumber < sortingMachine.getCompartmentHandlers().size(); compartmentNumber++) {
                IItemHandler compartment = sortingMachine.getCompartmentHandlers().get(compartmentNumber);
                EnumDyeColor compartmentColour = sortingMachine.colours[compartmentNumber];

                for (int slot = 0; slot < compartment.getSlots(); slot++) {
                    ItemStack compartmentStack = compartment.getStackInSlot(slot);
                    if (compartmentStack.isEmpty() || !compartmentStack.isItemEqual(stackFromSource))
                        continue;

                    ItemStack result = sortingMachine.addToNetwork(pushStackHandler, i, compartmentStack.getCount(), compartmentColour == null ? ImmutableMap.of()
                            : ImmutableMap.of("colour", new NBTTagInt(compartmentColour.getMetadata())));

                    if (result.isEmpty())
                        return true;
                }
            }

        }

        if (!sortingMachine.defaultRoute.isBlocked()) {
            for (int sourceSlot = 0; sourceSlot < pushStackHandler.getSlots(); sourceSlot++) {
                ItemStack sourceStack = pushStackHandler.getStackInSlot(sourceSlot);
                if (sourceStack.isEmpty())
                    continue;

                ItemStack result = sortingMachine.addToNetwork(pushStackHandler, sourceSlot, sourceStack.getCount(), !sortingMachine.defaultRoute.isColoured() ? ImmutableMap.of()
                        : ImmutableMap.of("colour", new NBTTagInt(sortingMachine.defaultRoute.getMetadata())));
                if (result.isEmpty())
                    return true;
            }
        }

        return false;
    }

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller
     * @param from
     * @return
     */
    @Override
    public boolean canAcceptTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller, EnumFacing from) {
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

        return false;
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

    @Override
    public int selectorPosition(TileSortingMachine sortingMachine) {
        return -1;
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {

    }

    /**
     * Accept the given traveller if the machine is set to inline mode.
     *
     * @param traveller the traveller to accept.
     * @param from      the side the traveller is to be injected into.
     * @return true if the entire traveller is accepted, false otherwise.
     */
    @Override
    public boolean acceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return false;
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
