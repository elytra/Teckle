package com.elytradev.teckle.common.container;

import com.elytradev.teckle.common.tile.TileBeamQuarry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerBeamQuarry extends Container {
    public final TileBeamQuarry beamQuarry;
    public final EntityPlayer player;

    public int power;

    public ContainerBeamQuarry(TileBeamQuarry tile, EntityPlayer player) {
        this.beamQuarry = tile;
        this.player = player;

        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlotToContainer(new SlotItemHandler(tile.junkTypes, j + i * 3, 116 + j * 18, 20 + i * 18));
            }
        }

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlotToContainer(new SlotItemHandler(tile.junkSupply.getHandler(), j + i * 3, 116 + j * 18, 74 + i * 18));
            }
        }

        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                this.addSlotToContainer(new SlotItemHandler(tile.bufferData.getHandler(), j + i * 5, 8 + j * 18, 56 + i * 18));
            }
        }

        bindPlayerInventory(player.inventory);
    }

    protected void bindPlayerInventory(InventoryPlayer inventoryplayer) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryplayer, j + i * 9 + 9, 8 + j * 18, 150 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryplayer, i, 8 + i * 18, 208));
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0)
            this.beamQuarry.energyStorage.setEnergyStored(data);
        super.updateProgressBar(id, data);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (power != beamQuarry.energyStorage.getEnergyStored()) {
            listeners.forEach(iContainerListener -> iContainerListener.sendWindowProperty(ContainerBeamQuarry.this, 0, power));
        }
        this.power = beamQuarry.energyStorage.getEnergyStored();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return beamQuarry.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < 43) {
                if (!this.mergeItemStack(itemstack1, 43, 79, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, 43, true)) {
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
}
