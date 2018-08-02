package com.elytradev.teckle.common.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.Optional;

public class BlueprintUtil {

    public static void setInventoryOfBlueprint(ItemStack stack, NonNullList<ItemStack> inventory, World world) {
        if (stack.isEmpty())
            return;
        if (!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();
        NBTTagCompound templateData = new NBTTagCompound();
        ItemStackHelper.saveAllItems(templateData, inventory);
        tag.setTag("templateData", templateData);

        Optional<String> s = getInventoryFromBlueprint(stack)
                .map(inv -> CraftingManager.findMatchingRecipe(inv, world))
                .map(IRecipe::getRegistryName)
                .map(ResourceLocation::toString);
        if (s.isPresent())
            tag.setString("recipeId", s.get());
        else
            tag.removeTag("recipeId");
    }

    public static NonNullList<ItemStack> getListFromBlueprint(ItemStack stack) {
        NonNullList<ItemStack> templates = NonNullList.withSize(9, ItemStack.EMPTY);
        if (!isBlueprintValid(stack))
            return templates;

        NBTTagCompound tag = stack.getTagCompound();

        if (tag.hasKey("templateData", Constants.NBT.TAG_COMPOUND)) {
            ItemStackHelper.loadAllItems(tag.getCompoundTag("templateData"), templates);
        }
        return templates;
    }
    public static Optional<InventoryCrafting> getInventoryFromBlueprint(ItemStack stack) {
        if (!isBlueprintValid(stack))
            return Optional.empty();

        NBTTagCompound tag = stack.getTagCompound();

        if (tag.hasKey("templateData", Constants.NBT.TAG_COMPOUND)) {
            NonNullList<ItemStack> templates = NonNullList.withSize(9, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(tag.getCompoundTag("templateData"), templates);

            InventoryCrafting templateGrid = new InventoryCrafting(new Container() {
                @Override
                public boolean canInteractWith(EntityPlayer playerIn) {
                    return false;
                }
            }, 3, 3);
            for (int i = 0; i < templates.size(); i++) {
                templateGrid.setInventorySlotContents(i, templates.get(i).copy());
            }

            return Optional.of(templateGrid);
        }
        return Optional.empty();
    }

    public static Optional<IRecipe> getRecipeFromBlueprint(ItemStack stack, World world) {
        if (!isBlueprintValid(stack))
            return Optional.empty();

        NBTTagCompound tag = stack.getTagCompound();

        IRecipe recipe = null;

        if (tag.hasKey("recipeId", Constants.NBT.TAG_STRING)) {
            recipe = CraftingManager.getRecipe(new ResourceLocation(tag.getString("recipeId")));
        }
        if (recipe == null) {
            recipe = getInventoryFromBlueprint(stack).map(inv -> CraftingManager.findMatchingRecipe(inv, world)).orElse(null);
        }
        return Optional.ofNullable(recipe);
    }

    public static boolean isBlueprintValid(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound();
    }
}