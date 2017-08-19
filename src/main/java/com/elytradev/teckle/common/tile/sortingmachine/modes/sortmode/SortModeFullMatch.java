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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Full match sort mode without a slider, will accept only if all items in a given compartment are available.
 */
public class SortModeFullMatch extends SortModeFullMatchBase {

    public int compartmentSlot = 0;
    public int coolDown = 5;

    public SortModeFullMatch() {
        super(2, "sortmode.fullmatch", SortModeType.COMPARTMENT);
        selectorPosition = -1;
    }

    public SortModeFullMatch(int id, String unlocalizedName, SortModeType type) {
        super(id, unlocalizedName, type);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        if (sortingMachine.getSource() == null)
            return false;

        if (sortingMachine.getStacksToPush(false).isEmpty())
            return false;

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush(true);
        AdvancedItemStackHandler buffer = sortingMachine.buffer;
        if (selectorPosition != -1) {
            genStacksToSatisfy(sortingMachine);
        }

        if (stacksLeftToSatisfy.isEmpty()) {
            int selectedCompartment = -1;
            Map<ItemStack, List<SlotData>> matchingDataBySlot = new HashMap<>();
            for (int compartment = 0; compartment < 8; compartment++) {
                IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(compartment);
                if (ItemStream.createItemStream(compartmentHandler).allMatch(ItemStack::isEmpty))
                    continue;
                boolean failedChecks = false;
                for (int slot = 0; slot < compartmentHandler.getSlots(); slot++) {
                    ItemStack stackInSlot = compartmentHandler.getStackInSlot(slot);
                    if (stackInSlot.isEmpty())
                        continue;

                    List<SlotData> matchingData = stacksToPush.stream().filter(slotData -> slotData.isItemEqual(stackInSlot)).collect(Collectors.toList());
                    int totalMatchingCount = matchingData.stream().mapToInt(SlotData::getCount).sum();
                    if (totalMatchingCount < stackInSlot.getCount()) {
                        failedChecks = true;
                        matchingDataBySlot = new HashMap<>();
                        break;
                    } else {
                        matchingDataBySlot.put(stackInSlot.copy(), matchingData);
                    }
                }
                if (!failedChecks) {
                    selectedCompartment = compartment;
                    break;
                }
            }

            if (selectedCompartment != -1) {
                selectorPosition = selectedCompartment;
                AdvancedItemStackHandler bufferCopy = sortingMachine.buffer.copy();
                boolean canFit = matchingDataBySlot.keySet().stream().allMatch(s -> bufferCopy.insertItem(s, false).isEmpty());
                // Odds are this won't ever happen but this is just in case we can't fit all the items into the buffer to be pushed.
                if (!canFit) {
                    selectorPosition = -1;
                    return false;
                }
                // Inserts all the matching items into the buffer.
                for (Map.Entry<ItemStack, List<SlotData>> itemStackListEntry : matchingDataBySlot.entrySet()) {
                    ItemStack stack = itemStackListEntry.getKey();
                    List<SlotData> matchingSlots = itemStackListEntry.getValue();

                    while (stack.getCount() > 0) {
                        for (SlotData matchingSlot : matchingSlots) {
                            ItemStack extractedStack = matchingSlot.extract(stack.getCount(), false);
                            stack.shrink(extractedStack.getCount());
                            buffer.insertItem(extractedStack, false);
                            if (stack.isEmpty())
                                break;
                        }
                    }
                }
            }
            selectorPosition = selectedCompartment;
        } else {
            // Gather information to shove in the buffer, as well as confirm that it will all fit.
            Map<SlotData, ItemStack> slotsToExtract = Maps.newHashMap();
            if (gatherRequiredStacks(sortingMachine, stacksToPush, slotsToExtract)) return false;
        }
        if (selectorPosition != -1) {
            genStacksToSatisfy(sortingMachine);
        }
        if (stacksLeftToSatisfy.isEmpty()) {
            sortingMachine.getPullMode().pause();
            return true;
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
                return !sortingMachine.defaultRoute.isBlocked() && sortingMachine.buffer.canInsertItem(travellerStack);
        }

        List<ItemStack> stacksToSatisfy = stacksLeftToSatisfy.stream().map(ItemStack::copy).collect(Collectors.toList());
        if (stacksToSatisfy.isEmpty()) {
            for (int i = 0; i < sortingMachine.getCompartmentHandlers().size(); i++) {
                stacksToSatisfy = ItemStream.createItemStream(sortingMachine.getCompartmentHandlers().get(i)).filter(s -> !s.isEmpty()).map(ItemStack::copy).collect(Collectors.toList());
                Optional<ItemStack> matchingStack = stacksToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)).findFirst();

                if (matchingStack.isPresent()) {
                    break;
                }
            }

            AdvancedItemStackHandler buffer = sortingMachine.buffer;
            if (!buffer.stream().allMatch(ItemStack::isEmpty)) {
                for (int i = 0; i < buffer.getStacks().size(); i++) {
                    if (buffer.getStackInSlot(i).isEmpty())
                        continue;

                    ItemStack clonedBufferStack = buffer.getStackInSlot(i).copy();
                    for (ItemStack stackToSatisfy : stacksToSatisfy) {
                        if (stackToSatisfy.isEmpty())
                            continue;

                        if (stackToSatisfy.isItemEqual(clonedBufferStack)) {
                            if (stackToSatisfy.getCount() >= clonedBufferStack.getCount()) {
                                stackToSatisfy.shrink(clonedBufferStack.getCount());
                                clonedBufferStack.shrink(clonedBufferStack.getCount());
                            } else {
                                clonedBufferStack.shrink(stackToSatisfy.getCount());
                                stackToSatisfy.shrink(stackToSatisfy.getCount());
                            }
                        }

                        if (clonedBufferStack.isEmpty())
                            break;
                    }
                }
                stacksToSatisfy.removeIf(ItemStack::isEmpty);
            }
        }
        Optional<ItemStack> matchingStack = stacksToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)).findFirst();

        if (matchingStack.isPresent()) {
            return sortingMachine.buffer.canInsertItem(travellerStack);
        } else if (!sortingMachine.defaultRoute.isBlocked()) {
            WorldNetworkTraveller travellerCopy = traveller.clone();

            if (!sortingMachine.defaultRoute.isBlocked()) {
                if (sortingMachine.defaultRoute.isColoured()) {
                    travellerCopy.data.setInteger("colour", sortingMachine.defaultRoute.getColour().getMetadata());
                } else {
                    travellerCopy.data.removeTag("colour");
                }
            }

            BlockPos insertInto = sortingMachine.getPos().offset(sortingMachine.getEjectionTile().getOutputFace());
            ImmutableMap<String, NBTBase> collect = ImmutableMap.copyOf(travellerCopy.data.getKeySet().stream().collect(Collectors.toMap(o -> o, o -> travellerCopy.data.getTag(o))));
            ItemStack result = (ItemStack) sortingMachine.getNetworkAssistant(ItemStack.class).insertData((WorldNetworkEntryPoint) sortingMachine.getEjectionTile().getNode(),
                    insertInto, travellerStack, collect, false, true);

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
    public ItemStack acceptTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller, EnumFacing from) {
        if (!traveller.data.hasKey("stack")) {
            return null;
        }

        ItemStack travellerStack = new ItemStack(traveller.data.getCompoundTag("stack"));

        if (stacksLeftToSatisfy.isEmpty()) {
            sortingMachine.getPullMode().pause();

            for (int i = 0; i < sortingMachine.getCompartmentHandlers().size(); i++) {
                stacksLeftToSatisfy = ItemStream.createItemStream(sortingMachine.getCompartmentHandlers().get(i)).filter(s -> !s.isEmpty()).map(ItemStack::copy).collect(Collectors.toList());
                Optional<ItemStack> matchingStack = stacksLeftToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)).findFirst();

                if (matchingStack.isPresent()) {
                    selectorPosition = i;
                    break;
                }
            }

            AdvancedItemStackHandler buffer = sortingMachine.buffer;
            if (!buffer.stream().allMatch(ItemStack::isEmpty)) {
                for (int i = 0; i < buffer.getStacks().size(); i++) {
                    if (buffer.getStackInSlot(i).isEmpty())
                        continue;

                    ItemStack clonedBufferStack = buffer.getStackInSlot(i).copy();
                    for (ItemStack stackToSatisfy : stacksLeftToSatisfy) {
                        if (stackToSatisfy.isEmpty())
                            continue;

                        if (stackToSatisfy.isItemEqual(clonedBufferStack)) {
                            if (stackToSatisfy.getCount() >= clonedBufferStack.getCount()) {
                                stackToSatisfy.shrink(clonedBufferStack.getCount());
                                clonedBufferStack.shrink(clonedBufferStack.getCount());
                            } else {
                                clonedBufferStack.shrink(stackToSatisfy.getCount());
                                stackToSatisfy.shrink(stackToSatisfy.getCount());
                            }
                        }

                        if (clonedBufferStack.isEmpty())
                            break;
                    }
                }
                stacksLeftToSatisfy.removeIf(ItemStack::isEmpty);
            }
        }
        Optional<ItemStack> matchingStack = stacksLeftToSatisfy.stream().filter(stack -> stack.isItemEqual(travellerStack)).findFirst();
        ItemStack remainder = handleAcceptedTraveller(sortingMachine, traveller, travellerStack, matchingStack);
        if (remainder != null) return remainder;

        return null;
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
        if (!sortingMachine.getPullMode().isPaused()) {
            return;
        } else if (selectorPosition < 0) {
            sortingMachine.getPullMode().unpause();
        }

        List<SlotData> stacksToPush = sortingMachine.getStacksToPush(false);
        if (stacksToPush.isEmpty()) {
            sortingMachine.getPullMode().unpause();
            return;
        }

        IItemHandler pushStackHandler = sortingMachine.getStacksToPush(false).get(0).itemHandler;
        if (pushStackHandler != sortingMachine.buffer) {
            return;
        }

        IItemHandler compartmentHandler = sortingMachine.getCompartmentHandlers().get(selectorPosition);
        EnumDyeColor compartmentColour = sortingMachine.colours[selectorPosition];
        ItemStack selectedStack = null;
        SlotData selectedStackInSlot = null;
        ItemStack selectedCompartmentStack = compartmentHandler.getStackInSlot(compartmentSlot);

        if (selectedCompartmentStack.isEmpty()) {
            if (compartmentSlot < 6) {
                for (int currentCompartmentItem = compartmentSlot; currentCompartmentItem < compartmentHandler.getSlots(); currentCompartmentItem++) {
                    if (!compartmentHandler.getStackInSlot(currentCompartmentItem).isEmpty()) {
                        selectedCompartmentStack = compartmentHandler.getStackInSlot(currentCompartmentItem);
                        compartmentSlot = currentCompartmentItem;
                        break;
                    }
                }
            } else {
                sortingMachine.getPullMode().unpause();
                return;
            }
        }

        if (selectedCompartmentStack.isEmpty()) {
            compartmentSlot = 0;
            sortingMachine.getPullMode().unpause();
        }

        for (int slot = 0; slot < pushStackHandler.getSlots(); slot++) {
            ItemStack stackInSlot = pushStackHandler.getStackInSlot(slot);

            if (stackInSlot.isEmpty())
                continue;

            if (selectedCompartmentStack.isItemEqual(stackInSlot) && selectedCompartmentStack.getCount() <= stackInSlot.getCount()) {
                selectedStack = selectedCompartmentStack.copy();
                selectedStackInSlot = new SlotData(pushStackHandler, slot);
                compartmentSlot++;
                break;
            } else {
                continue;
            }
        }

        if (selectedStack == null)
            return;

        sortingMachine.addToNetwork(selectedStackInSlot.itemHandler, selectedStackInSlot.slot, selectedStack.getCount(),
                compartmentColour != null ? ImmutableMap.of("colour", new NBTTagInt(compartmentColour.getMetadata())) : ImmutableMap.of());

        Stream<ItemStack> bufferStream = ItemStream.createItemStream(pushStackHandler);
        // If the buffer has been emptied move the selector.
        if (bufferStream.allMatch(ItemStack::isEmpty)) {
            compartmentSlot = 0;
            selectorPosition = -1;
            sortingMachine.getPullMode().unpause();
        }
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
