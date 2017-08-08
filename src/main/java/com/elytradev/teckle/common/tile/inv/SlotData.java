package com.elytradev.teckle.common.tile.inv;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * A wrapper for items in handlers, reduces the need for use of tuples in a lot of places.
 */
public class SlotData {

    public IItemHandler itemHandler;
    public int slot;

    public SlotData(IItemHandler itemHandler, int slot) {
        this.itemHandler = itemHandler;
        this.slot = slot;
    }

    public ItemStack getStack() {
        return itemHandler.getStackInSlot(slot);
    }

    public int getCount() {
        return getStack().getCount();
    }

    public boolean isEmpty() {
        return getStack().isEmpty();
    }

    public boolean isItemEqual(ItemStack other) {
        return getStack().isItemEqual(other);
    }

    public Item getItem() {
        return getStack().getItem();
    }

    public boolean canExtractCount(int count) {
        return itemHandler.extractItem(slot, count, true).getCount() == count;
    }

    public ItemStack extract(Integer count, boolean simulate) {
        return itemHandler.extractItem(slot, count, simulate);
    }
}
