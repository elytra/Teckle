package com.elytradev.teckle.common.tile.inv;

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

    public boolean isEmpty() {
        return itemHandler.getStackInSlot(slot).isEmpty();
    }

}
