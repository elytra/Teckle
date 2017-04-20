package com.elytradev.teckle.common.tile.inv;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class AdvancedItemStackHandler extends ItemStackHandler {

    public BiPredicate<Integer, ItemStack> insertCheck = (integer, stack) -> true;
    public IContentChangeListener changeListener = slot -> {/*NOOP*/};
    public ISlotLimit slotLimit = slot -> 64;

    public AdvancedItemStackHandler(int i) {
        super(i);
    }

    public AdvancedItemStackHandler withChangeListener(IContentChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public AdvancedItemStackHandler withInsertCheck(BiPredicate<Integer, ItemStack> canPutInSlot) {
        this.insertCheck = canPutInSlot;
        return this;
    }

    public AdvancedItemStackHandler withSlotLimit(ISlotLimit slotLimit) {
        this.slotLimit = slotLimit;
        return this;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (insertCheck.test(slot, stack))
            return super.insertItem(slot, stack, simulate);
        else
            return stack;
    }

    @Override
    protected void onContentsChanged(int slot) {
        changeListener.onContentChange(slot);
        super.onContentsChanged(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotLimit.slotLimit(slot);
    }

    public IItemHandler subHandler(int startSlot, int size) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return size;
            }

            @Nonnull
            @Override
            public ItemStack getStackInSlot(int slot) {
                checkSlot(slot);
                return AdvancedItemStackHandler.this.getStackInSlot(slot + startSlot);
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                checkSlot(slot);
                return AdvancedItemStackHandler.this.insertItem(slot + startSlot, stack, simulate);
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                checkSlot(slot);
                return AdvancedItemStackHandler.this.extractItem(slot + startSlot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                checkSlot(slot);
                return AdvancedItemStackHandler.this.getSlotLimit(slot + startSlot);
            }

            private void checkSlot(int slot) {
                if (slot < 0 || slot >= size)
                    throw new RuntimeException("Slot " + slot + " not in valid range - [0," + size + ")");
            }
        };
    }

    public Stream<ItemStack> stream() {
        return this.stacks.stream();
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    public interface IContentChangeListener {
        void onContentChange(int slot);
    }

    public interface ISlotLimit {
        int slotLimit(int slot);
    }

}