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

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.ItemStream;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullModeInline;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Full match sort mode without a slider, will accept only if all items in a given compartment are available.
 */
public class SortModeFullMatch extends SortMode {

    public int selectorPosition = -1;
    public int compartmentSlot = 0;
    public List<ItemStack> stacksLeftToSatisfy = Lists.newArrayList();

    public SortModeFullMatch() {
        super(2, "sortmode.fullmatch", SortModeType.COMPARTMENT);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        if (sortingMachine.getSource() == null)
            return false;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush();
        if (stacksToPush.isEmpty())
            return false;

        IItemHandler pushStackHandler = sortingMachine.getStacksToPush().get(0).itemHandler;
        if (pushStackHandler == sortingMachine.buffer || selectorPosition != -1) {
            return false;
        }

        // choose the compartment...
        for (int stackToPush = 0; stackToPush < stacksToPush.size(); stackToPush++) {
            SlotData stack = stacksToPush.get(stackToPush);
            if (stack.isEmpty())
                continue;

            for (int compartment = 0; compartment < sortingMachine.getCompartmentHandlers().size(); compartment++) {
                IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(compartment);
                Stream<ItemStack> compartmentStream = ItemStream.createItemStream(compartmentHandler);
                Optional<ItemStack> matchingStack = compartmentStream.filter(s -> s.isItemEqual(stack.getStack()) && stack.getStack().getCount() >= s.getCount()).findFirst();
                if (matchingStack.isPresent()) {
                    selectorPosition = compartment;
                    break;
                }
            }

            if (selectorPosition != -1)
                break;
        }

        if (selectorPosition == -1)
            return false;

        IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition);
        Map<SlotData, Integer> slotsToExtract = Maps.newHashMap();

        // Gather information to shove in the buffer, as well as confirm that it will all fit.
        for (int i = 0; i < compartmentHandler.getSlots(); i++) {
            ItemStack compartmentStackInSlot = compartmentHandler.getStackInSlot(i);
            if (compartmentStackInSlot.isEmpty())
                continue;

            Optional<SlotData> matchingSlotData = stacksToPush.stream().filter(slotData -> compartmentStackInSlot.isItemEqual(slotData.getStack())
                    && compartmentStackInSlot.getCount() >= slotData.getStack().getCount()).findFirst();
            if (matchingSlotData.isPresent() && matchingSlotData.get().canExtractCount(compartmentStackInSlot.getCount())
                    && sortingMachine.buffer.canInsertItem(compartmentStackInSlot.copy())) {
                slotsToExtract.put(matchingSlotData.get(), compartmentStackInSlot.getCount());
            } else {
                return false;
            }
        }
        AdvancedItemStackHandler bufferClone = sortingMachine.buffer.copy();
        // Confirm the buffer can fit everything before attempting actual insertion into the real thing.
        for (Map.Entry<SlotData, Integer> slotCountEntry : slotsToExtract.entrySet()) {
            SlotData slotData = slotCountEntry.getKey();
            Integer count = slotCountEntry.getValue();

            // If it didn't fit, return false.
            if (!bufferClone.insertItem(slotData.extract(count, true), false).isEmpty()) {
                return false;
            }
        }

        for (Map.Entry<SlotData, Integer> slotCountEntry : slotsToExtract.entrySet()) {
            SlotData slotData = slotCountEntry.getKey();
            Integer count = slotCountEntry.getValue();

            sortingMachine.buffer.insertItem(slotData.extract(count, false), false);
        }

        sortingMachine.getPullMode().pause();

        return true;
    }

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller
     * @return
     */
    @SuppressWarnings("OptionalIsPresent")
    @Override
    public boolean canAcceptTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller) {
        if (!traveller.data.hasKey("stack")) {
            return false;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

        // choose the compartment...
        if (selectorPosition == -1) {
            for (int compartment = 0; compartment < sortingMachine.getCompartmentHandlers().size(); compartment++) {
                IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(compartment);
                Stream<ItemStack> compartmentStream = ItemStream.createItemStream(compartmentHandler);
                Optional<ItemStack> matchingStack = compartmentStream.filter(s -> s.isItemEqual(travellerStack) && travellerStack.getCount() >= s.getCount()).findFirst();
                if (matchingStack.isPresent()) {
                    selectorPosition = compartment;
                    break;
                }
            }

            if (selectorPosition == -1)
                return !sortingMachine.defaultRoute.isBlocked();
        }

        if (stacksLeftToSatisfy.isEmpty())
            stacksLeftToSatisfy = ItemStream.createItemStream(sortingMachine.getCompartmentHandlers().get(selectorPosition)).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        Optional<ItemStack> matchingStack = stacksLeftToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)
                && travellerStack.getCount() >= stack.getCount()).findFirst();
        if (matchingStack.isPresent()) {
            return true;
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
        if (!traveller.data.hasKey("stack")) {
            return traveller;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

        Optional<ItemStack> matchingStack = stacksLeftToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)
                && travellerStack.getCount() >= stack.getCount()).findFirst();
        if (matchingStack.isPresent()) {
            EnumDyeColor compartmentColour = sortingMachine.colours[selectorPosition];
            if (compartmentColour != null) {
                traveller.data.setInteger("colour", compartmentColour.getMetadata());
            } else {
                traveller.data.removeTag("colour");
            }

            stacksLeftToSatisfy.remove(matchingStack.get());
            return traveller;
        }

        if (sortingMachine.defaultRoute.isColoured()) {
            traveller.data.setInteger("colour", sortingMachine.defaultRoute.getColour().getMetadata());
        } else {
            traveller.data.removeTag("colour");
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
        return selectorPosition;
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {
        if (!(sortingMachine.getPullMode() instanceof PullModeInline)) {
            if (!sortingMachine.getPullMode().isPaused())
                return;

            List<SlotData> stacksToPush = sortingMachine.getStacksToPush();
            if (stacksToPush.isEmpty()) {
                sortingMachine.getPullMode().unpause();
                return;
            }

            IItemHandler pushStackHandler = sortingMachine.getStacksToPush().get(0).itemHandler;
            if (pushStackHandler != sortingMachine.buffer) {
                return;
            }

            IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition - 1);
            EnumDyeColor compartmentColour = sortingMachine.colours[selectorPosition - 1];
            ItemStack selectedStack = null;
            SlotData selectedStackInSlot = null;
            ItemStack selectedCompartmentStack = compartmentHandler.getStackInSlot(compartmentSlot);

            if (selectedCompartmentStack.isEmpty() && compartmentSlot < 7) {
                for (int currentCompartmentItem = 0; currentCompartmentItem < compartmentHandler.getSlots(); currentCompartmentItem++) {
                    if (!compartmentHandler.getStackInSlot(currentCompartmentItem).isEmpty()) {
                        selectedCompartmentStack = compartmentHandler.getStackInSlot(currentCompartmentItem);
                        break;
                    }
                }
            } else {
                sortingMachine.getPullMode().unpause();
                return;
            }

            if (selectedCompartmentStack.isEmpty()) {
                compartmentSlot = 0;
                sortingMachine.getPullMode().unpause();
            }

            for (int slot = 0; slot < pushStackHandler.getSlots(); slot++) {
                ItemStack stackInSlot = pushStackHandler.getStackInSlot(slot);

                if (stackInSlot.isEmpty())
                    continue;

                if (selectedCompartmentStack.isItemEqual(stackInSlot) && selectedCompartmentStack.getCount() >= stackInSlot.getCount()) {
                    selectedStack = selectedCompartmentStack.copy();
                    selectedStackInSlot = new SlotData(pushStackHandler, slot);
                } else {
                    return;
                }
            }

            if (selectedStack == null)
                return;

            sortingMachine.addToNetwork(selectedStackInSlot.itemHandler, selectedStackInSlot.slot, selectedStack.getCount(),
                    compartmentColour != null ? ImmutableMap.of("colour", new NBTTagInt(compartmentColour.getMetadata())) : ImmutableMap.of());

            Stream<ItemStack> bufferStream = ItemStream.createItemStream(pushStackHandler);
            // If the buffer has been emptied move the selector.
            if (bufferStream.allMatch(ItemStack::isEmpty)) {
                sortingMachine.getPullMode().unpause();

                selectorPosition = -1;
            }
        }
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();

        tagCompound.setInteger("selectorPosition", selectorPosition);
        tagCompound.setInteger("compartmentSlot", compartmentSlot);

        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        if (!(nbt instanceof NBTTagCompound))
            return;

        NBTTagCompound tagCompound = (NBTTagCompound) nbt;
        selectorPosition = tagCompound.getInteger("selectorPosition");
        compartmentSlot = tagCompound.getInteger("compartmentSlot");
    }
}
