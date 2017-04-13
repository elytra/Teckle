package com.elytradev.teckle.common.container;

import com.elytradev.teckle.common.tile.TileFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/**
 * Created by darkevilmac on 4/12/2017.
 */
public class ContainerFilter extends Container {
    public final TileFilter filter;
    public final EntityPlayer player;

    public ContainerFilter(TileFilter tile, EntityPlayer player) {
        this.filter = tile;
        this.player = player;
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
        return filter.isUsableByPlayer(player);
    }

    public static class SlotBuffer extends Slot {
        public SlotBuffer(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }
    }

    public static class SlotFilterData extends Slot {
        public SlotFilterData(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }
    }
}
