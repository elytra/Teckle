package com.elytradev.teckle.common.container;

import com.elytradev.teckle.common.tile.TileBeamQuarry;
import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;

public class ContainerBeamQuarry extends Container {
    public final TileBeamQuarry beamQuarry;
    public final EntityPlayer player;

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
                this.addSlotToContainer(new SlotItemHandler(tile.junkSupply, j + i * 3, 116 + j * 18, 74 + i * 18));
            }
        }

        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                this.addSlotToContainer(new SlotItemHandler(tile.buffer, j + i * 5, 8 + j * 18, 56 + i * 18));
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

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return beamQuarry.isUsableByPlayer(player);
    }
}
