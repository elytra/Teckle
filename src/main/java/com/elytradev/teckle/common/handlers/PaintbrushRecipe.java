package com.elytradev.teckle.common.handlers;

import com.elytradev.teckle.common.item.ItemPaintbrush;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;

/**
 * Created by darkevilmac on 4/13/2017.
 */
public class PaintbrushRecipe implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        boolean hasPaintbrush = false;
        boolean hasDye = false;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stackInSlot = inv.getStackInSlot(i);

            if (!stackInSlot.isEmpty()) {
                if (stackInSlot.getItem() instanceof ItemPaintbrush) {
                    if (hasPaintbrush)
                        return false;
                    hasPaintbrush = true;
                } else if (Arrays.asList(OreDictionary.getOreIDs(stackInSlot)).contains(OreDictionary.getOreID("dye"))) {
                    if (hasDye)
                        return false;
                    hasDye = true;
                }
            }
        }

        return hasPaintbrush && hasDye;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = ItemStack.EMPTY;

        ItemStack paintbrush = ItemStack.EMPTY;
        ItemStack dye = ItemStack.EMPTY;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stackInSlot = inv.getStackInSlot(i);

            if (!stackInSlot.isEmpty()) {
                if (stackInSlot.getItem() instanceof ItemPaintbrush) {
                    if (paintbrush.isEmpty())
                        return result;
                    paintbrush = stackInSlot;
                } else if (Arrays.asList(OreDictionary.getOreIDs(stackInSlot)).contains(OreDictionary.getOreID("dye"))) {
                    if (dye.isEmpty())
                        return result;
                    dye = stackInSlot;
                }
            }
        }
        if (paintbrush.isEmpty() || dye.isEmpty())
            return result;

        result = paintbrush.copy();
        result.setItemDamage(dye.getItemDamage());

        return result;
    }

    @Override
    public int getRecipeSize() {
        return 4;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
    }
}
