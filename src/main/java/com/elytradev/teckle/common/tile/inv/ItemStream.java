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
