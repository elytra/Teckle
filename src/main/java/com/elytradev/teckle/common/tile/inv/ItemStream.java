package com.elytradev.teckle.common.tile.inv;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by darkevilmac on 5/4/17.
 */
public class ItemStream {

    public static Stream<ItemStack> createItemStream(IItemHandler handler) {
        AtomicInteger i = new AtomicInteger(0);
        return Stream.generate(() -> handler.getStackInSlot(i.getAndIncrement())).limit(handler.getSlots());
    }

    public static Stream<ItemStack> createItemStream(IInventory inv) {
        AtomicInteger i = new AtomicInteger(0);
        return Stream.generate(() -> inv.getStackInSlot(i.getAndIncrement())).limit(inv.getSizeInventory());
    }

    public static NonNullList<ItemStack> convertCollectedListToNonNull(List<ItemStack> stacks) {
        NonNullList<ItemStack> result = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) {
            result.set(i, stacks.get(i));
        }

        return result;
    }
}
