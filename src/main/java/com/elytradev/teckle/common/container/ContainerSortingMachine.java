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

package com.elytradev.teckle.common.container;

import com.elytradev.teckle.common.network.messages.SortingMachineSelectorMessage;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Created by darkevilmac on 5/17/17.
 */
public class ContainerSortingMachine extends Container {

    public EntityPlayer player;
    public TileSortingMachine sortingMachine;

    private int selectorPos = -1;

    public ContainerSortingMachine(TileSortingMachine tileSortingMachine, EntityPlayer player) {
        this.player = player;
        this.sortingMachine = tileSortingMachine;

        for (int box = 0; box < 8; box++) {
            int xS = 11 + ((box & 3) * 40);
            int yS = 8 + (box > 3 ? 62 : 0);
            for (int slot = 0; slot < 6; slot++) {
                int slotNumber = (box * 6) + slot;
                int xPos = xS + ((slot & 1) * 18);
                int yPos = yS + ((slot >= 2 ? (slot >= 4 ? 2 : 1) : 0) * 18);
                this.addSlotToContainer(new SlotItemHandler(tileSortingMachine.filterData.getHandler(), slotNumber, xPos, yPos));
            }
        }

        bindPlayerInventory(player.inventory);
    }

    protected void bindPlayerInventory(InventoryPlayer inventoryplayer) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryplayer, j + i * 9 + 9, 8 + j * 18, 76 + 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryplayer, i, 8 + i * 18, 76 + 142));
        }
    }


    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 48) {
                if (!this.mergeItemStack(itemstack1, 48, 84, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, 48, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    /**
     * Determines whether supplied player can use this container
     *
     * @param playerIn
     */
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return sortingMachine.isUsableByPlayer(playerIn);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (selectorPos != sortingMachine.getSortMode().selectorPosition(sortingMachine)) {
            this.selectorPos = sortingMachine.getSortMode().selectorPosition(sortingMachine);
            new SortingMachineSelectorMessage(selectorPos, sortingMachine.getPos()).sendToAllWatching(sortingMachine);
        }
    }
}
