package com.elytradev.teckle.compat.ct;

import com.elytradev.teckle.common.crafting.AlloyRecipe;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * ZenScript class for accessing Alloy Furnace recipes. Exposes functions to register recipes, remove recipes by output, remove recipes by inputs, and remove all recipes.
 */
@ZenClass("mods.teckle.alloy_furnace")
@ZenRegister
public class CTAlloyFurnace {

    /**
     * Creates and registers a recipe for the alloy furnace based on the data provided.
     *
     * @param output The ItemStack resulting from the recipe.
     * @param inputs An array of ingredients required to make the recipe.
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient[] inputs) {
        Tuple<Object, Integer>[] inputTuples = (Tuple<Object, Integer>[]) new Tuple[inputs.length];

        if(inputs.length > 9) {
            throw new RuntimeException("Alloy Furnace cannot take more than 9 inputs; got "+inputs.length+" inputs instead");
        }

        for(int i = 0; i < inputs.length; i++) {
            inputTuples[i] = TeckleCTUtils.convertIngredient(inputs[i]);
        }

        ItemStack outputStack = CraftTweakerMC.getItemStack(output);
        CraftTweakerAPI.apply(new Add(new AlloyRecipe(outputStack, inputTuples)));
    }

    /**
     * Created by addRecipe. Registers the internal recipe with the AlloyRecipes registry.
     */
    public static class Add implements IAction {
        private final AlloyRecipe recipe;

        public Add(AlloyRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public void apply() {
            AlloyRecipes.getInstance().registerRecipe(recipe);
        }

        @Override
        public String describe() {
            return "Adding "+recipe.getCraftingResult().toString()+" recipe for Alloy Furnace";
        }
    }

    /**
     * Creates a Remove action that will remove all recipes with the matching output.
     *
     * @param output The output to be matched against.
     *               Optionally compares NBT if it is specified.
     */
    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        CraftTweakerAPI.apply(new Remove(CraftTweakerMC.getItemStack(output)));
    }

    /**
     * Removes every recipe from the AlloyRecipes registry that has the specified output.
     */
    public static class Remove implements IAction {
        private final ItemStack output;

        public Remove(ItemStack output) {
            this.output = output;
        }

        @Override
        public void apply() {
            AlloyRecipes
                .getInstance()
                .unregisterMatching(
                    recipe -> TeckleCTUtils.stacksEqual(output, recipe.getCraftingResult(), output.hasTagCompound())
            );
        }

        @Override
        public String describe() {
            return "Removing Alloy Furnace recipe for "+output.toString();
        }
    }

    /**
     * Creates a RemoveInput action that will remove all recipes with the matching inputs.
     *
     * @param inputs The inputs that will be matched against existing recipes.
     *               NBT will be individually compared if it is specified.
     */
    @ZenMethod
    public static void removeInputRecipe(IIngredient[] inputs) {
        Tuple<Object, Integer>[] inputTuples = (Tuple<Object, Integer>[]) new Tuple[inputs.length];

        if(inputs.length > 9) {
            throw new RuntimeException("Alloy Furnace cannot take more than 9 inputs; got "+inputs.length+" inputs instead");
        }

        for(int i = 0; i < inputs.length; i++) {
            inputTuples[i] = TeckleCTUtils.convertIngredient(inputs[i]);
        }

        NonNullList<Object> referenceInputs = (new AlloyRecipe(ItemStack.EMPTY, inputTuples)).getInputs();

        CraftTweakerAPI.apply(new RemoveInput(referenceInputs));
    }

    /**
     * Removes every recipe from the AlloyRecipes registry that has the specified inputs.
     */
    public static class RemoveInput implements IAction {
        private NonNullList<Object> inputs;

        public RemoveInput(NonNullList<Object> inputs) {
            this.inputs = inputs;
        }

        @Override
        public void apply() {
            AlloyRecipes
                .getInstance()
                .unregisterMatching(
                    recipe -> TeckleCTUtils.recipeIngredientsMatch(recipe.getInputs(), inputs)
            );
        }

        @Override
        public String describe() {
            return "Removing matching Alloy Furnace recipes for given inputs: "+inputs;
        }
    }

    /**
     * Creates a RemoveAll action that will remove every recipe from the Alloy Furnace.
     * Useful if you want to disable the Alloy Furnace entirely, or want to rewrite all of its recipes.
     */
    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new RemoveAll());
    }

    /**
     * Removes all recipes from AlloyRecipes.
     */
    public static class RemoveAll implements IAction {
        @Override
        public void apply() {
            AlloyRecipes.getInstance().unregisterAll();
        }

        @Override
        public String describe() {
            return "Removing all recipes from Alloy Furnace";
        }
    }
}
