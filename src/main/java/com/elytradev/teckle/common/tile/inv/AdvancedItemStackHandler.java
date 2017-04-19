package com.elytradev.teckle.common.tile.inv;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;

public class AdvancedItemStackHandler extends ItemStackHandler {

    public BiPredicate<Integer, ItemStack> slotCheck = (integer, stack) -> true;
    public IContentChangeListener changeListener = slot -> {/*NOOP*/};
    public ISlotLimit slotLimit = slot -> 64;

    public AdvancedItemStackHandler(int i) {
        super(i);
    }

    public AdvancedItemStackHandler withChangeListener(IContentChangeListener changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    public AdvancedItemStackHandler withSlotCheck(BiPredicate<Integer, ItemStack> canPutInSlot) {
        this.slotCheck = canPutInSlot;
        return this;
    }

    public AdvancedItemStackHandler withSlotLimit(ISlotLimit slotLimit) {
        this.slotLimit = slotLimit;
        return this;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slotCheck.test(slot, stack))
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

    public interface IContentChangeListener {
        void onContentChange(int slot);
    }

    public interface ISlotLimit {
        int slotLimit(int slot);
    }

}