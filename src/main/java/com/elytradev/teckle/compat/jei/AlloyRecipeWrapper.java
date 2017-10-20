package com.elytradev.teckle.compat.jei;

import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.crafting.AlloyRecipe;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IStackHelper;
import net.minecraft.item.ItemStack;

import java.util.List;

public class AlloyRecipeWrapper implements IRecipeWrapper {

    private final AlloyRecipe recipe;

    public AlloyRecipeWrapper(AlloyRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ItemStack recipeOutput = recipe.getCraftingResult();
        IStackHelper stackHelper = TeckleJEIPlugin.HELPERS.getStackHelper();

        try {
            List<List<ItemStack>> inputLists = stackHelper.expandRecipeItemStackInputs(recipe.getInputs());
            ingredients.setInputLists(ItemStack.class, inputLists);
            ingredients.setOutput(ItemStack.class, recipeOutput);
        } catch (RuntimeException e) {
            TeckleLog.error("Failed to get ingredients for recipe wrapper. {} {}", recipe, e);
        }
    }

}
