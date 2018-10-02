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
 * ZenScript class for accessing Alloy Furnace recipes. Exposes functions to add recipes, remove recipes by output, remove recipes by inputs, and remove all recipes.
 */
@ZenClass("mods.teckle.alloy_furnace")
@ZenRegister
@SuppressWarnings("unused") // javac doesn't know that @ZenRegister allows CT to find this class.
public class CTAlloyFurnace {

    /**
     * Creates a new AlloyRecipe and packages it into an Add action.
     */
    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient[] inputs) {
        @SuppressWarnings("unchecked")
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
            AlloyRecipes.getInstance().add(recipe);
        }

        @Override
        public String describe() {
            return "Adding "+recipe.getCraftingResult().toString()+" recipe for Alloy Furnace";
        }
    }

    /**
     * Creates a Remove action that will remove all recipes with the matching output.
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
                .removeMatching(
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
     */
    @ZenMethod
    public static void removeInputRecipe(IIngredient[] inputs) {
        @SuppressWarnings("unchecked")
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
                .removeMatching(
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
            AlloyRecipes.getInstance().clear();
        }

        @Override
        public String describe() {
            return "Removing all recipes from Alloy Furnace";
        }
    }
}
