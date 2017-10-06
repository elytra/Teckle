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
import com.elytradev.teckle.common.tile.sortingmachine.NetworkTileSortingMachineBase;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The partial match selector mode. Sort when any item set in the current compartment is available.
 */
public class SortModePartialMatchSelector extends SortMode {

    public int selectorPosition = 0;
    public int compartmentSlot = 0;
    public int coolDown = 5;

    public SortModePartialMatchSelector() {
        super(1, "sortmode.partialmatchselector", SortModeType.COMPARTMENT);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        if (sortingMachine.getSource() == null)
            return false;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush(false);
        if (stacksToPush.isEmpty())
            return false;

        IItemHandler pushStackHandler = sortingMachine.getStacksToPush(false).get(0).itemHandler;
        if (pushStackHandler == sortingMachine.bufferData.getHandler()) {
            return false;
        }

        IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition);
        Map<SlotData, Integer> slotsToExtract = Maps.newHashMap();

        // Gather information to shove in the buffer, as well as confirm that it will all fit.
        for (int i = 0; i < compartmentHandler.getSlots(); i++) {
            ItemStack compartmentStackInSlot = compartmentHandler.getStackInSlot(i);
            if (compartmentStackInSlot.isEmpty())
                continue;

            Optional<SlotData> matchingSlotData = stacksToPush.stream().filter(slotData -> compartmentStackInSlot.isItemEqual(slotData.getStack())
                    && compartmentStackInSlot.getCount() <= slotData.getStack().getCount()).findFirst();
            if (matchingSlotData.isPresent() && matchingSlotData.get().canExtractCount(compartmentStackInSlot.getCount())
                    && sortingMachine.bufferData.getHandler().canInsertItem(compartmentStackInSlot.copy())) {
                slotsToExtract.put(matchingSlotData.get(), compartmentStackInSlot.getCount());
            }
        }
        AdvancedItemStackHandler bufferClone = sortingMachine.bufferData.getHandler().copy();
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

            sortingMachine.bufferData.getHandler().insertItem(slotData.extract(count, false), false);
        }

        sortingMachine.getPullMode().pause();

        return true;
    }

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine one of the sorting machine network tiles.
     * @param traveller      the traveller to check.
     * @param from           the face the traveller is from.
     * @return true if the traveller can be accepted and false otherwise.
     */
    @Override
    public boolean canAcceptTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, EnumFacing from) {
        if (!traveller.data.hasKey("stack")) {
            return false;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));
        List<ItemStack> stacksToSatisfy = ItemStream.createItemStream(sortingMachine.getCompartmentHandlers().get(selectorPosition)).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        Optional<ItemStack> matchingStack = stacksToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)
                && travellerStack.getCount() >= stack.getCount()).findFirst();
        if (matchingStack.isPresent() && !sortingMachine.getPullMode().isPaused()) {
            return sortingMachine.getBuffer().canInsertItem(travellerStack);
        } else if (!sortingMachine.getDefaultRoute().isBlocked()) {
            WorldNetworkTraveller travellerCopy = traveller.clone();

            if (!sortingMachine.getDefaultRoute().isBlocked()) {
                if (sortingMachine.getDefaultRoute().isColoured()) {
                    travellerCopy.data.setInteger("colour", sortingMachine.getDefaultRoute().getColour().getMetadata());
                } else {
                    travellerCopy.data.removeTag("colour");
                }
            }

            BlockPos insertInto = sortingMachine.getPos().offset(sortingMachine.getOutputTile().getOutputFace());
            ImmutableMap<String, NBTBase> collect = ImmutableMap.copyOf(travellerCopy.data.getKeySet().stream().collect(Collectors.toMap(o -> o, o -> travellerCopy.data.getTag(o))));
            ItemStack result = sortingMachine.getNetworkAssistant(ItemStack.class).insertData((WorldNetworkEntryPoint) sortingMachine.getOutputTile().getNode(),
                    insertInto, travellerStack, collect, false, true);

            return result.isEmpty();
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
    public WorldNetworkTraveller processExistingTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller) {
        if (!traveller.data.hasKey("stack")) {
            return traveller;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));
        List<ItemStack> stacksToSatisfy = ItemStream.createItemStream(sortingMachine.getCompartmentHandlers().get(selectorPosition)).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        Optional<ItemStack> matchingStack = stacksToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)
                && travellerStack.getCount() >= stack.getCount()).findFirst();
        if (matchingStack.isPresent()) {
            EnumDyeColor compartmentColour = sortingMachine.colours[selectorPosition];
            if (compartmentColour != null) {
                traveller.data.setInteger("colour", compartmentColour.getMetadata());
            } else {
                traveller.data.removeTag("colour");
            }
            if (selectorPosition == 8) {
                selectorPosition = 0;
            } else {
                selectorPosition++;
            }
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

    /**
     * Called once a tick (20x/second)
     *
     * @param sortingMachine the sorting machine.
     */
    @Override
    public void onTick(TileSortingMachine sortingMachine) {
        if (coolDown > 0) {
            coolDown--;
            return;
        }

        coolDown = 5;

        // Do some checks to see if this tick should actually perform anything.
        if (!sortingMachine.getPullMode().isPaused())
            return;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush(false);
        if (stacksToPush.isEmpty()) {
            sortingMachine.getPullMode().unpause();
            return;
        }

        IItemHandler bufferHandler = sortingMachine.getStacksToPush(false).get(0).itemHandler;
        if (bufferHandler != sortingMachine.bufferData.getHandler()) {
            sortingMachine.getPullMode().unpause();
            return;
        }

        IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition);
        EnumDyeColor compartmentColour = sortingMachine.colours[selectorPosition];
        ItemStack selectedStack = ItemStack.EMPTY;
        SlotData selectedSlotData = null;
        ItemStack selectedCompartmentStack = compartmentHandler.getStackInSlot(compartmentSlot);

        ItemStack selectedCompartmentStackClone = selectedCompartmentStack;
        if (selectedCompartmentStack.isEmpty()
                || sortingMachine.getStacksToPush(false).stream()
                .noneMatch(slotData -> slotData.getStack().isItemEqual(selectedCompartmentStackClone)
                        && slotData.getStack().getCount() >= selectedCompartmentStackClone.getCount())) {

            // Choose a new compartment slot that will work.
            boolean foundMatch = false;
            for (int i = 0; i < compartmentHandler.getSlots(); i++) {
                ItemStack compartmentStackInSlot = compartmentHandler.getStackInSlot(i);

                if (compartmentStackInSlot.isEmpty())
                    continue;

                Optional<SlotData> matchingSlotData = sortingMachine.getStacksToPush(false).stream().filter(slotData -> !slotData.getStack().isEmpty())
                        .filter(slotData -> slotData.getStack().isItemEqual(compartmentStackInSlot)
                                && slotData.getStack().getCount() >= compartmentStackInSlot.getCount()).findFirst();

                if (matchingSlotData.isPresent()) {
                    compartmentSlot = i;
                    selectedCompartmentStack = compartmentStackInSlot;
                    selectedStack = selectedCompartmentStack.copy();
                    selectedSlotData = matchingSlotData.get();
                    foundMatch = true;
                    break;
                }
            }

            // If nothing was found then reset the compartment slot, unpause the pull mode, and choose a new selector position.
            if (!foundMatch) {
                compartmentSlot = 0;
                sortingMachine.getPullMode().unpause();
                chooseSelectorPosition(sortingMachine);
                return;
            }
        }

        // Create a stream from the items in the buffer.
        Stream<ItemStack> bufferStream = ItemStream.createItemStream(sortingMachine.bufferData.getHandler());
        if (selectedCompartmentStack.isEmpty() && bufferStream.allMatch(stack -> stack.isEmpty() ||
                ItemStream.createItemStream(compartmentHandler).noneMatch(tStack -> tStack.isItemEqual(stack)))) {
            compartmentSlot = 0;
            sortingMachine.getPullMode().unpause();
            return;
        }

        if (selectedSlotData == null) {
            // Find the first slot matching the selected compartment.
            Optional<SlotData> matchingSlotData = sortingMachine.getStacksToPush(false).stream().filter
                    (slotData -> {
                        ItemStack slotStack = slotData.getStack();
                        ItemStack compartmentStack = compartmentHandler.getStackInSlot(compartmentSlot);
                        return !slotStack.isEmpty()
                                && slotStack.isItemEqual(compartmentStack)
                                && slotStack.getCount() >= compartmentStack.getCount();
                    }).findFirst();

            if (matchingSlotData.isPresent()) {
                selectedSlotData = matchingSlotData.get();
                selectedStack = selectedCompartmentStack.copy();
            } else {
                compartmentSlot = 0;
                sortingMachine.getPullMode().unpause();
                return;
            }
        }

        // Add what we found to the network.
        ImmutableMap<String, NBTBase> additionalData = compartmentColour == null ? ImmutableMap.of()
                : ImmutableMap.of("colour", new NBTTagInt(compartmentColour.getMetadata()));
        sortingMachine.addToNetwork(selectedSlotData.itemHandler, selectedSlotData.slot,
                selectedStack.getCount(), additionalData);

        // If the buffer has been emptied move the selector.
        bufferStream = ItemStream.createItemStream(sortingMachine.bufferData.getHandler());
        if (bufferStream.allMatch(stack -> stack.isEmpty() ||
                ItemStream.createItemStream(compartmentHandler).noneMatch(tStack -> tStack.isItemEqual(stack)))) {
            chooseSelectorPosition(sortingMachine);
            sortingMachine.getPullMode().unpause();
        }
    }

    /**
     * Determines the selector position for the compartments.
     *
     * @param sortingMachine the sorting machine.
     */
    private void chooseSelectorPosition(TileSortingMachine sortingMachine) {
        for (int i = selectorPosition + 1; i < 8; i++) {
            IItemHandler selectedCompartmentHandler = sortingMachine.getCompartmentHandlers().get(i);

            if (ItemStream.createItemStream(selectedCompartmentHandler).anyMatch(stack -> !stack.isEmpty())) {
                selectorPosition = i;
                return;
            }
        }

        // Try again from 0...
        for (int i = 0; i < 8; i++) {
            IItemHandler selectedCompartmentHandler = sortingMachine.getCompartmentHandlers().get(i);

            if (ItemStream.createItemStream(selectedCompartmentHandler).anyMatch(stack -> !stack.isEmpty())) {
                selectorPosition = i;
                return;
            }
        }

        // Nothing was found, resort to 0...
        selectorPosition = 0;
    }

    /**
     * Accept the given traveller if the machine is set to inline mode.
     *
     * @param sortingMachine one of the network tiles for the sorting machine.
     * @param traveller      the traveller to accept.
     * @param from           the side the traveller is to be injected into.
     * @return the remaining traveller stack
     */
    @Override
    public ItemStack acceptTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, EnumFacing from) {
        if (!traveller.data.hasKey("stack")) {
            return null;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));
        IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition);
        // Gather any stacks that aren't empty.
        List<ItemStack> stacksThatSatisfy = ItemStream.createItemStream(compartmentHandler).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Optional<ItemStack> matchingStack = stacksThatSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)
                && travellerStack.getCount() >= stack.getCount()).findFirst();
        if (matchingStack.isPresent() && !sortingMachine.getPullMode().isPaused()) {
            //
            ItemStack toInsert = travellerStack.copy();
            toInsert.setCount(matchingStack.get().getCount());
            ItemStack remainder = travellerStack.copy();
            remainder.setCount(travellerStack.getCount() - matchingStack.get().getCount());
            ItemStack leftover = sortingMachine.getBuffer().insertItem(toInsert, false);
            remainder.grow(leftover.getCount());
            sortingMachine.getPullMode().pause();

            return remainder;
        } else if (!sortingMachine.getDefaultRoute().isBlocked()) {
            return defaultRouteStack(sortingMachine, traveller, travellerStack);
        }

        return null;
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();

        tagCompound.setInteger("selectorPosition", selectorPosition);
        tagCompound.setInteger("compartmentSlot", compartmentSlot);
        tagCompound.setInteger("cooldown", coolDown);

        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        if (!(nbt instanceof NBTTagCompound))
            return;

        NBTTagCompound tagCompound = (NBTTagCompound) nbt;
        selectorPosition = tagCompound.getInteger("selectorPosition");
        compartmentSlot = tagCompound.getInteger("compartmentSlot");
        coolDown = tagCompound.getInteger("cooldown");
    }
}
