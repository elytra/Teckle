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

package com.elytradev.teckle.common.handlers;

import com.elytradev.teckle.common.item.ItemPaintbrush;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;

/**
 * Created by darkevilmac on 4/13/2017.
 */
public class PaintbrushRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
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

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     *
     * @param width
     * @param height
     */
    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 4;
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
