package com.elytradev.teckle.common.container;

import com.elytradev.teckle.common.network.messages.clientbound.RetrieverSelectorMessage;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerRetriever extends Container {

    public TileRetriever retriever;
    public EntityPlayer player;
    private int selectorPos = -1;

    public ContainerRetriever(TileRetriever retriever, EntityPlayer player) {
        this.retriever = retriever;
        this.player = player;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlotToContainer(new SlotItemHandler(retriever.filterData.getHandler(), j + i * 3, 62 + j * 18, 17 + i * 18));
            }
        }
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

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return retriever.isUsableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack stackSlotCopy = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            stackSlotCopy = stackInSlot.copy();

            if (index < 9) {
                if (!this.mergeItemStack(stackInSlot, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(stackInSlot, 0, 9, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (stackInSlot.getCount() == stackSlotCopy.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stackInSlot);
        }

        return stackSlotCopy;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (selectorPos != retriever.getSelectorPosition()) {
            this.selectorPos = retriever.getSelectorPosition();
            new RetrieverSelectorMessage(selectorPos, retriever.getPos()).sendToAllWatching(retriever);
        }
    }
}
