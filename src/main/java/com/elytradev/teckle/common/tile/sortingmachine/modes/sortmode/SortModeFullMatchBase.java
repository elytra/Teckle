package com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.ItemStream;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class SortModeFullMatchBase extends SortMode {

    public int selectorPosition = 0;
    public List<ItemStack> stacksLeftToSatisfy = Lists.newArrayList();

    public SortModeFullMatchBase(int id, String unlocalizedName, SortModeType type) {
        super(id, unlocalizedName, type);
    }

    protected ItemStack handleAcceptedTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller, ItemStack travellerStack, Optional<ItemStack> matchingStack) {
        if (matchingStack.isPresent()) {
            ItemStack toInsert = travellerStack.copy();
            toInsert.setCount(matchingStack.get().getCount());
            ItemStack remainder = travellerStack.copy();
            remainder.setCount(travellerStack.getCount() - matchingStack.get().getCount());
            ItemStack leftover = sortingMachine.buffer.insertItem(toInsert, false);
            remainder.grow(leftover.getCount());

            matchingStack.get().shrink(toInsert.getCount() - leftover.getCount());
            stacksLeftToSatisfy.removeIf(ItemStack::isEmpty);
            if (stacksLeftToSatisfy.isEmpty())
                sortingMachine.getPullMode().pause();

            return remainder;
        } else if (!sortingMachine.defaultRoute.isBlocked()) {
            WorldNetworkTraveller travellerCopy = traveller.clone();
            if (sortingMachine.defaultRoute.isColoured()) {
                travellerCopy.data.setInteger("colour", sortingMachine.defaultRoute.getColour().getMetadata());
            } else {
                travellerCopy.data.removeTag("colour");
            }
            BlockPos insertInto = sortingMachine.getPos().offset(sortingMachine.getEjectionTile().getOutputFace());
            ImmutableMap<String, NBTBase> collect = ImmutableMap.copyOf(travellerCopy.data.getKeySet().stream().collect(Collectors.toMap(o -> o, o -> travellerCopy.data.getTag(o))));
            ItemStack result = (ItemStack) sortingMachine.getNetworkAssistant(ItemStack.class)
                    .insertData((WorldNetworkEntryPoint) sortingMachine.getEjectionTile().getNode(), insertInto,
                            travellerStack, collect, false, false);

            if (result.isEmpty() || result.getCount() != travellerStack.getCount()) {
                sortingMachine.setTriggered();
            }

            return result;
        }
        return null;
    }

    protected void genStacksToSatisfy(TileSortingMachine sortingMachine) {
        if (stacksLeftToSatisfy.isEmpty()) {
            stacksLeftToSatisfy = ItemStream.createItemStream(sortingMachine.getCompartmentHandlers().get(selectorPosition)).filter(s -> !s.isEmpty()).map(ItemStack::copy).collect(Collectors.toList());

            // Don't satisfy things in the buffer. Duh.
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
    }

    protected boolean gatherRequiredStacks(TileSortingMachine sortingMachine, List<SlotData> stacksToPush, Map<SlotData, ItemStack> slotsToExtract) {
        for (int i = 0; i < stacksLeftToSatisfy.size(); i++) {
            ItemStack stackToSatisfy = stacksLeftToSatisfy.get(i);

            Optional<SlotData> matchingSlotData = stacksToPush.stream().filter(slotData -> stackToSatisfy.isItemEqual(slotData.getStack())
                    && slotData.getStack().getCount() > 0).findFirst();

            if (matchingSlotData.isPresent() && sortingMachine.buffer.canInsertItem(matchingSlotData.get().getStack().copy())) {
                slotsToExtract.put(matchingSlotData.get(), stackToSatisfy);
                continue;
            }

            return true;
        }
        for (Map.Entry<SlotData, ItemStack> slotCountEntry : slotsToExtract.entrySet()) {
            SlotData slotData = slotCountEntry.getKey();
            ItemStack satisfyStack = slotCountEntry.getValue();

            Integer countToExtract = satisfyStack.getCount() < slotData.getStack().getCount() ? satisfyStack.getCount() : slotData.getStack().getCount();
            ItemStack extracted = slotData.extract(countToExtract, false);
            ItemStack remaining = sortingMachine.buffer.insertItem(extracted, false);

            slotCountEntry.getValue().shrink(countToExtract - remaining.getCount());
            if (!remaining.isEmpty()) {
                slotData.itemHandler.getStackInSlot(slotData.slot).grow(remaining.getCount());
            }
        }
        stacksLeftToSatisfy.removeIf(ItemStack::isEmpty);
        return false;
    }
}
