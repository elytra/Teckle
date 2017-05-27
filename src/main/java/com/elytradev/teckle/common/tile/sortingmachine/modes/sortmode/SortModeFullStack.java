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
import com.google.common.collect.ImmutableMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public class SortModeFullStack extends SortModeAnyStack {
    public SortModeFullStack() {
        super(4, "sortmode.fullstack", SortModeType.SLOT);
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
                    if (compartmentStack.isEmpty())
                        continue;

                    ItemStack result = sortingMachine.addToNetwork(pushStackHandler, slot, 64, compartmentColour == null ? ImmutableMap.of()
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

                ItemStack result = sortingMachine.addToNetwork(pushStackHandler, sourceSlot, 64, !sortingMachine.defaultRoute.isColoured() ? ImmutableMap.of()
                        : ImmutableMap.of("colour", new NBTTagInt(sortingMachine.defaultRoute.getMetadata())));
                if (result.isEmpty())
                    return true;
            }
        }

        return false;
    }
}