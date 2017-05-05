package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

/**
 * Created by darkevilmac on 5/3/17.
 */
public class TileFabricator extends TileEntity implements ITickable {

    public AdvancedItemStackHandler stackHandler;
    public InventoryCrafting craftingGrid = new InventoryCrafting(null, 3, 3);

    @Override
    public void update() {

    }

    public IRecipe getRecipe() {
        return null;
    }

    public void craft() {

    }

}
