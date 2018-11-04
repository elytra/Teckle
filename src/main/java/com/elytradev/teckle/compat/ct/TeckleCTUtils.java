package com.elytradev.teckle.compat.ct;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.item.IngredientStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * Utilities and helper methods for CTAlloyFurnace.
 * Used for comparing and converting between the various ways of representing ingredients.
 */
public class TeckleCTUtils {
    /**
     * Converts from CraftTweaker ingredients to AlloyRecipe ingredients
     *
     * @param ingredient The CraftTweaker ingredient to be converted
     * @return The converted ingredient, or null for unsupported ingredients
     */
    protected static Tuple<Object, Integer> convertIngredient(IIngredient ingredient) {
        // OreDict entry
        if (ingredient instanceof IOreDictEntry) {
            String ore = ((IOreDictEntry) ingredient).getName();

            return new Tuple<>(ore, ingredient.getAmount());
        }

        // Literal ItemStack
        if (ingredient instanceof IItemStack) {
            ItemStack stack = CraftTweakerMC.getItemStack((IItemStack) ingredient);

            return new Tuple<>(stack, stack.getCount());
        }

        // Wrapped OreDict entry with a stack size
        if (ingredient instanceof IngredientStack) {
            IngredientStack stack = (IngredientStack) ingredient;

            IIngredient internal = (IIngredient) stack.getInternal();

            if(internal==null) {
                CraftTweakerAPI.logWarning("Got null from IngredientStack#getInternal, you most likely have an outdated version of CraftTweaker! Please update to version 4.1.11 or later.");
                return new Tuple<>(ItemStack.EMPTY, 0);
            }

            if (internal instanceof IOreDictEntry) {
                String ore = ((IOreDictEntry) internal).getName();

                return new Tuple<>(ore, ingredient.getAmount());
            } else {
                // This will probably fall through to an invalid ingredient error, since there's no reason to wrap an ItemStack.
                return convertIngredient(ingredient);
            }
        }

        CraftTweakerAPI.logError("Bad ingredient: " + ingredient);
        return new Tuple<>(ItemStack.EMPTY, 0);
    }

    /**
     * Checks if the provided ItemStacks are equal.
     * Optionally compares NBT values.
     *
     * @param stackA   The first ItemStack
     * @param stackB   The second ItemStack
     * @param matchNbt Whether NBT data should be checked as well
     * @return A boolean indicating whether the stacks are equal.
     */
    protected static boolean stacksEqual(ItemStack stackA, ItemStack stackB, boolean matchNbt) {
        if (stackA.isEmpty() || stackB.isEmpty() || stackA.getItem() != stackB.getItem()) {
            return false;
        }

        if (matchNbt && !ItemStack.areItemStackTagsEqual(stackA, stackB)) {
            return false;
        }

        if (stackA.getHasSubtypes()) {
            boolean aWildcard = stackA.getItemDamage() == -1 || stackA.getItemDamage() == OreDictionary.WILDCARD_VALUE;
            boolean bWildcard = stackB.getItemDamage() == -1 || stackB.getItemDamage() == OreDictionary.WILDCARD_VALUE;

            if (!(aWildcard || bWildcard)) {
                if (stackA.getItemDamage() != stackB.getItemDamage()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Tests if the specified sets of ingredients are equivalent.
     * Note: Remember that Alloy recipes are based on Lists, not Sets.
     * It will not return true if one list is shuffled relative to the other.
     * <p>
     * Also note the distinction between the first and second inputs.
     * The second inputs alone determine whether NBT will be compared based
     * on whether they have NBT data.
     *
     * @param inputs The list of inputs representing the recipe being tested
     * @param filter The other list of inputs, generally used for filtering varying inputs in the first slot
     * @return Whether the input stack is equivalent to the filter
     */
    protected static boolean recipeIngredientsMatch(NonNullList<Object> inputs, NonNullList<Object> filter) {
        if (inputs.size() != filter.size()) {
            return false;
        }

        for (int i = 0; i < inputs.size(); i++) {
            Object inputOriginal = inputs.get(i);
            Object inputFilter = filter.get(i);

            // OreDict entry
            if (inputOriginal instanceof List && inputFilter instanceof List) {
                List listOriginal = (List) inputOriginal;
                List listFilter = (List) inputFilter;

                // Make sure the lists are equivalent.
                // We can't use List::equals because ItemStack does not have an implementation of equals.
                if (listOriginal.size() != listFilter.size()) {
                    return false;
                }

                for (int j = 0; j < listOriginal.size(); j++) {
                    if (listOriginal.get(j) instanceof ItemStack && listFilter.get(j) instanceof ItemStack) {

                        ItemStack itemOriginal = (ItemStack) listOriginal.get(j);
                        ItemStack itemFilter = (ItemStack) listFilter.get(j);

                        if (!stacksEqual(itemOriginal, itemFilter, itemFilter.hasTagCompound())) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                // pass
                continue;
            }

            // Literal ItemStack
            if (inputOriginal instanceof ItemStack && inputFilter instanceof ItemStack) {
                if (stacksEqual((ItemStack) inputOriginal, (ItemStack) inputFilter, ((ItemStack) inputFilter).hasTagCompound())) {
                    // pass
                    continue;
                }
            }

            return false;
        }

        return true;
    }
}
