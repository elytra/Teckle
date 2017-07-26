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

import com.elytradev.teckle.common.network.messages.AlloyFurnaceMessage;
import com.elytradev.teckle.common.tile.TileAlloyFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * Created by darkevilmac on 4/20/2017.
 */
public class ContainerAlloyFurnace extends Container {
    public final TileAlloyFurnace alloyFurnace;
    public final EntityPlayer player;

    public int fuelBurnTime, cookTime, currentFuelWorth;

    public ContainerAlloyFurnace(TileAlloyFurnace tile, EntityPlayer player) {
        this.alloyFurnace = tile;
        this.player = player;

        // slots for recipe area.
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlotToContainer(new SlotItemHandler(tile.itemStackHandler, j + i * 3, 44 + j * 18, 15 + i * 18));
            }
        }
        this.addSlotToContainer(new SlotFuel(tile.itemStackHandler, 9, 18, 51));
        this.addSlotToContainer(new SlotItemHandler(tile.itemStackHandler, 10, 146, 33));

        bindPlayerInventory(player.inventory);
    }

    protected void bindPlayerInventory(InventoryPlayer inventoryplayer) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryplayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryplayer, i, 8 + i * 18, 142));
        }
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (this.cookTime != this.alloyFurnace.cookTime || this.fuelBurnTime != this.alloyFurnace.fuelBurnTime || this.alloyFurnace.currentFuelWorth != this.alloyFurnace.currentFuelWorth) {
            this.cookTime = this.alloyFurnace.cookTime;
            this.fuelBurnTime = this.alloyFurnace.fuelBurnTime;
            this.currentFuelWorth = this.alloyFurnace.currentFuelWorth;

            new AlloyFurnaceMessage(alloyFurnace).sendToAllWatching(alloyFurnace);
        }
    }


    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return alloyFurnace.isUsableByPlayer(player);
    }

    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 11) {
                if (!this.mergeItemStack(itemstack1, 11, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.mergeItemStack(itemstack1, 9, 10, false)) {
                    if (!this.mergeItemStack(itemstack1, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
                }
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


    public class SlotFuel extends SlotItemHandler {

        public SlotFuel(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            return super.isItemValid(stack) && TileEntityFurnace.isItemFuel(stack);
        }
    }
}
