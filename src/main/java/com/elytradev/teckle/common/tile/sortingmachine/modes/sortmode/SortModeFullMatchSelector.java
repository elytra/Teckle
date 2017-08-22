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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortModeFullMatchSelector extends SortModeFullMatchBase {

    public int coolDown = 5;

    public SortModeFullMatchSelector() {
        super(0, "sortmode.fullmatchselector", SortModeType.COMPARTMENT);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        if (sortingMachine.getSource() == null)
            return false;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush(false);
        if (stacksToPush.isEmpty())
            return false;

        if (stacksToPush.get(0).itemHandler == sortingMachine.bufferData.getHandler()) {
            genStacksToSatisfy(sortingMachine.bufferData.getHandler(), sortingMachine.getCompartmentHandlers());
            if (stacksLeftToSatisfy.isEmpty()) {
                sortingMachine.getPullMode().pause();
                return false;
            }
        }

        AdvancedItemStackHandler pushStackHandler = sortingMachine.bufferData.getHandler();
        Map<SlotData, ItemStack> slotsToExtract = Maps.newHashMap();

        changeCompartment(sortingMachine);
        genStacksToSatisfy(sortingMachine.bufferData.getHandler(), sortingMachine.getCompartmentHandlers());
        if (stacksLeftToSatisfy.isEmpty()) {
            sortingMachine.getPullMode().pause();
            return true;
        } else {
            stacksToPush = sortingMachine.getStacksToPush(true);
        }

        if (gatherRequiredStacks(sortingMachine, stacksToPush, slotsToExtract)) return false;

        if (stacksLeftToSatisfy.isEmpty()) {
            sortingMachine.getPullMode().pause();
        }
        return true;
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
        if (coolDown > 0) {
            coolDown--;
            return;
        }

        coolDown = 5;

        if (!sortingMachine.getPullMode().isPaused())
            return;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush(false);
        if (stacksToPush.isEmpty() || stacksToPush.stream().allMatch(slotData -> slotData.getStack().isEmpty())) {
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
        Triple<ItemStack, SlotData, ItemStack> selectedData = selectCompartmentSlot(sortingMachine, compartmentHandler, bufferHandler);
        ItemStack selectedStack = selectedData.getLeft();
        SlotData selectedSlotData = selectedData.getMiddle();
        ItemStack selectedCompartmentStack = selectedData.getRight();

        if (selectedStack.isEmpty())
            return;

        sortingMachine.addToNetwork(selectedSlotData.itemHandler, selectedSlotData.slot, selectedStack.getCount(),
                compartmentColour != null ? ImmutableMap.of("colour", new NBTTagInt(compartmentColour.getMetadata())) : ImmutableMap.of());

        Stream<ItemStack> bufferStream = ItemStream.createItemStream(sortingMachine.bufferData.getHandler());
        // If the buffer has been emptied move the selector.
        if (bufferStream.allMatch(stack -> stack.isEmpty() ||
                ItemStream.createItemStream(compartmentHandler).noneMatch(tStack -> tStack.isItemEqual(stack)))) {
            sortingMachine.getPullMode().unpause();

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
    }

    private void changeCompartment(TileSortingMachine sortingMachine) {
        IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition);

        if (ItemStream.createItemStream(compartmentHandler).allMatch(ItemStack::isEmpty)) {
            boolean changedSelector = false;
            for (int i = selectorPosition + 1; i < 8; i++) {
                IItemHandler selectedCompartmentHandler = sortingMachine.getCompartmentHandlers().get(i);

                if (ItemStream.createItemStream(selectedCompartmentHandler).anyMatch(stack -> !stack.isEmpty())) {
                    selectorPosition = i;
                    changedSelector = true;
                    break;
                }
            }

            if (!changedSelector)
                selectorPosition = 0;
        }
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
    public boolean canAcceptTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, EnumFacing from) {
        if (!traveller.data.hasKey("stack")) {
            return false;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

        genStacksToSatisfy(sortingMachine.getBuffer(), sortingMachine.getCompartmentHandlers());

        Optional<ItemStack> matchingStack = stacksLeftToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)).findFirst();
        if (matchingStack.isPresent()) {
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
                    insertInto, travellerStack, collect, !sortingMachine.isLoaded(), true);

            return result.isEmpty();
        }

        return false;
    }

    /**
     * Accept the given traveller if the machine is set to inline mode.
     *
     * @param sortingMachine
     * @param traveller      the traveller to accept.
     * @param from           the side the traveller is to be injected into.
     * @return true if the entire traveller is accepted, false otherwise.
     */
    @Override
    public ItemStack acceptTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, EnumFacing from) {
        if (!traveller.data.hasKey("stack")) {
            return null;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

        genStacksToSatisfy(sortingMachine.getBuffer(), sortingMachine.getCompartmentHandlers());
        Optional<ItemStack> matchingStack = stacksLeftToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)).findFirst();
        ItemStack handledStack = handleAcceptedTraveller(sortingMachine, traveller, travellerStack, matchingStack);
        if (handledStack != null) return handledStack;

        if (stacksLeftToSatisfy.isEmpty()) {
            sortingMachine.getPullMode().pause();
        }

        return null;
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();

        tagCompound.setInteger("selectorPosition", selectorPosition);
        tagCompound.setInteger("compartmentSlot", compartmentSlot);
        tagCompound.setInteger("cooldown", coolDown);
        tagCompound.setInteger("stacksLeftToSatisfy", stacksLeftToSatisfy.size());
        for (int i = 0; i < stacksLeftToSatisfy.size(); i++) {
            tagCompound.setTag("stacksLeftToSatisfy" + i, stacksLeftToSatisfy.get(i).serializeNBT());
        }

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

        int stacksLeftToSatisfySize = tagCompound.getInteger("stacksLeftToSatisfy");
        stacksLeftToSatisfy = Lists.newArrayListWithExpectedSize(stacksLeftToSatisfySize);
        for (int i = 0; i < stacksLeftToSatisfySize; i++) {
            stacksLeftToSatisfy.set(i, new ItemStack(tagCompound.getCompoundTag("stacksLeftToSatisfy" + i)));
        }
    }
}
