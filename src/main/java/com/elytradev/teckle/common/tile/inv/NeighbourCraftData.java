package com.elytradev.teckle.common.tile.inv;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class NeighbourCraftData {

    public static final NeighbourCraftData EMPTY = new NeighbourCraftData(-1, ItemStack.EMPTY, null);
    public int slot;
    public ItemStack stack;
    public IItemHandler handler;

    public NeighbourCraftData(int slot, ItemStack stack, IItemHandler handler) {
        this.slot = slot;
        this.stack = stack;
        this.handler = handler;
    }
}
