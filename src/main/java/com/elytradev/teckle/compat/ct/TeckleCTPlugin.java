package com.elytradev.teckle.compat.ct;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.item.IngredientStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraftforge.oredict.OreDictionary;

import java.lang.reflect.Field;

public class TeckleCTPlugin {
    private static Field internalIngredient;

    static {
        try {
            internalIngredient = IngredientStack.class.getDeclaredField("ingredient");

            internalIngredient.setAccessible(true);
        } catch(NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void registerClasses() {
        CraftTweakerAPI.registerClass(CTAlloyFurnace.class);
    }

    static Tuple<Object, Integer> convertIngredient(IIngredient ingredient) {
        if(ingredient instanceof IOreDictEntry) {
            String ore = ((IOreDictEntry)ingredient).getName();

            return new Tuple<>(ore, ingredient.getAmount());
        } else if(ingredient instanceof IItemStack) {
            ItemStack stack = CraftTweakerMC.getItemStack((IItemStack) ingredient);

            return new Tuple<>(stack, stack.getCount());
        } else if(ingredient instanceof IngredientStack) {
            IngredientStack stack = (IngredientStack)ingredient;
            IIngredient internal;

            try {
                internal = (IIngredient) internalIngredient.get(stack);
            } catch (IllegalAccessException e) {
                // not possible
                internal = null;
            }

            if(internal instanceof IOreDictEntry) {
                String ore = ((IOreDictEntry)internal).getName();

                return new Tuple<>(ore, ingredient.getAmount());
            } else {
                return convertIngredient(ingredient);
            }
        } else {
            CraftTweakerAPI.logError("Bad ingredient: "+ingredient);
            return new Tuple<>(ItemStack.EMPTY, 0);
        }
    }

    static boolean stacksEqual(ItemStack a, ItemStack b, boolean matchNbt) {
        if(a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) {
            return false;
        }

        if(matchNbt && !ItemStack.areItemStackTagsEqual(a, b)) {
            return false;
        }

        if(a.getHasSubtypes()) {
            boolean aWildcard = a.getItemDamage() == -1 || a.getItemDamage() == OreDictionary.WILDCARD_VALUE;
            boolean bWildcard = b.getItemDamage() == -1 || b.getItemDamage() == OreDictionary.WILDCARD_VALUE;

            if(!(aWildcard || bWildcard)) {
                if(a.getItemDamage() != b.getItemDamage()) {
                    return false;
                }
            }
        }

        return true;
    }
}
