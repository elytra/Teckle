package com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.ItemStream;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.tile.sortingmachine.NetworkTileSortingMachineBase;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class SortModeFullMatchBase extends SortMode {

    public int selectorPosition = 0;
    public int compartmentSlot = 0;
    public List<ItemStack> stacksLeftToSatisfy = Lists.newArrayList();

    public SortModeFullMatchBase(int id, String unlocalizedName, SortModeType type) {
        super(id, unlocalizedName, type);
    }

    protected ItemStack handleAcceptedTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, ItemStack travellerStack, Optional<ItemStack> matchingStack) {
        if (matchingStack.isPresent()) {
            ItemStack toInsert = travellerStack.copy();
            toInsert.setCount(matchingStack.get().getCount());
            ItemStack remainder = travellerStack.copy();
            remainder.setCount(travellerStack.getCount() - matchingStack.get().getCount());
            ItemStack leftover = sortingMachine.getBuffer().insertItem(toInsert, false);
            remainder.grow(leftover.getCount());

            matchingStack.get().shrink(toInsert.getCount() - leftover.getCount());
            stacksLeftToSatisfy.removeIf(ItemStack::isEmpty);
            if (stacksLeftToSatisfy.isEmpty())
                sortingMachine.getPullMode().pause();

            return remainder;
        } else if (!sortingMachine.getDefaultRoute().isBlocked()) {
            WorldNetworkTraveller travellerCopy = traveller.clone();
            if (sortingMachine.getDefaultRoute().isColoured()) {
                travellerCopy.data.setInteger("colour", sortingMachine.getDefaultRoute().getColour().getMetadata());
            } else {
                travellerCopy.data.removeTag("colour");
            }
            BlockPos insertInto = sortingMachine.getPos().offset(sortingMachine.getOutputTile().getOutputFace());
            ImmutableMap<String, NBTBase> collect = ImmutableMap.copyOf(travellerCopy.data.getKeySet().stream().collect(Collectors.toMap(o -> o, o -> travellerCopy.data.getTag(o))));
            ItemStack result = (ItemStack) sortingMachine.getNetworkAssistant(ItemStack.class)
                    .insertData((WorldNetworkEntryPoint) sortingMachine.getOutputTile().getNode(), insertInto,
                            travellerStack, collect, false, false);

            if (result.isEmpty() || result.getCount() != travellerStack.getCount()) {
                sortingMachine.setTriggered();
            }

            return result;
        }
        return null;
    }

    protected void genStacksToSatisfy(AdvancedItemStackHandler buffer, List<IItemHandler> compartmentHandlers) {
        if (stacksLeftToSatisfy.isEmpty()) {
            stacksLeftToSatisfy = ItemStream.createItemStream(compartmentHandlers.get(selectorPosition)).filter(s -> !s.isEmpty()).map(ItemStack::copy).collect(Collectors.toList());

            // Don't satisfy things in the buffer. Duh.
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

            if (matchingSlotData.isPresent() && sortingMachine.bufferData.getHandler().canInsertItem(matchingSlotData.get().getStack().copy())) {
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
            ItemStack remaining = sortingMachine.bufferData.getHandler().insertItem(extracted, false);

            slotCountEntry.getValue().shrink(countToExtract - remaining.getCount());
            if (!remaining.isEmpty()) {
                slotData.itemHandler.getStackInSlot(slotData.slot).grow(remaining.getCount());
            }
        }
        stacksLeftToSatisfy.removeIf(ItemStack::isEmpty);
        return false;
    }

    public Triple<ItemStack, SlotData, ItemStack> selectCompartmentSlot(TileSortingMachine sortingMachine, IItemHandler compartmentHandler, IItemHandler pushStackHandler) {
        ItemStack selectedStack = ItemStack.EMPTY;
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
                return new ImmutableTriple<>(selectedStack, selectedStackInSlot, selectedCompartmentStack);
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

        return new ImmutableTriple<>(selectedStack, selectedStackInSlot, selectedCompartmentStack);
    }
}
