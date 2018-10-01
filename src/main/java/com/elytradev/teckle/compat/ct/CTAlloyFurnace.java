package com.elytradev.teckle.compat.ct;

import com.elytradev.teckle.common.crafting.AlloyRecipe;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Iterator;
import java.util.List;

@ZenClass("mods.teckle.alloy_furnace")
public class CTAlloyFurnace {

    @ZenMethod
    @SuppressWarnings("unchecked")
    public static void addRecipe(IItemStack output, IIngredient[] inputs) {
        ItemStack outputStack = CraftTweakerMC.getItemStack(output);
        Tuple<Object, Integer>[] inputTuples = (Tuple<Object, Integer>[]) new Tuple[inputs.length];

        if(inputs.length > 9) {
            throw new RuntimeException("Alloy Furnace cannot take more than 9 inputs; got "+inputs.length+" inputs instead");
        }

        for(int i = 0; i < inputs.length; i++) {
            inputTuples[i] = TeckleCTPlugin.convertIngredient(inputs[i]);
        }

        CraftTweakerAPI.apply(new Add(new AlloyRecipe(outputStack, inputTuples)));
    }

    public static class Add implements IAction {
        private final AlloyRecipe recipe;

        public Add(AlloyRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public void apply() {
            AlloyRecipes.getInstance().getMasterRecipeList().add(recipe);
        }

        @Override
        public String describe() {
            return "Adding "+recipe.getCraftingResult().toString()+" recipe for Alloy Furnace";
        }
    }

    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        CraftTweakerAPI.apply(new Remove(CraftTweakerMC.getItemStack(output)));
    }

    public static class Remove implements IAction {
        private final ItemStack output;

        public Remove(ItemStack output) {
            this.output = output;
        }

        @Override
        public void apply() {
            AlloyRecipes
                .getInstance()
                .getMasterRecipeList()
                .removeIf(
                    (recipe) -> TeckleCTPlugin.stacksEqual(output, recipe.getCraftingResult(), output.hasTagCompound())
            );
        }

        @Override
        public String describe() {
            return "Adding Alloy Furnace recipe for "+output.toString();
        }
    }

    @ZenMethod
    @SuppressWarnings("unchecked")
    public static void removeInputRecipe(IIngredient[] inputs) {
        Tuple<Object, Integer>[] inputTuples = (Tuple<Object, Integer>[]) new Tuple[inputs.length];

        if(inputs.length > 9) {
            throw new RuntimeException("Alloy Furnace cannot take more than 9 inputs; got "+inputs.length+" inputs instead");
        }

        for(int i = 0; i < inputs.length; i++) {
            inputTuples[i] = TeckleCTPlugin.convertIngredient(inputs[i]);
        }

        NonNullList<Object> referenceInputs = (new AlloyRecipe(ItemStack.EMPTY, inputTuples)).getInputs();

        CraftTweakerAPI.apply(new RemoveInput(referenceInputs));
    }

    public static class RemoveInput implements IAction {
        private NonNullList<Object> inputs;

        public RemoveInput(NonNullList<Object> inputs) {
            this.inputs = inputs;
        }

        @Override
        public void apply() {

            Iterator<AlloyRecipe> recipeIterator = AlloyRecipes.getInstance().getMasterRecipeList().iterator();

            while(recipeIterator.hasNext()) {
                AlloyRecipe recipe = recipeIterator.next();

                if(recipe.getInputs().size() != inputs.size()) {
                    continue;
                }

                boolean matches = true;
                for(int i = 0; i < inputs.size(); i++) {
                    Object a = recipe.getInputs().get(i);
                    Object b = inputs.get(i);

                    if(a instanceof List && b instanceof List) {
                        List<ItemStack> listA = (List<ItemStack>)a;
                        List<ItemStack> listB = (List<ItemStack>)b;

                        if(listA.size() != listB.size()) {
                            matches = false;
                            break;
                        }

                        boolean matchesNested = true;

                        for(int j = 0; j < listA.size(); j++) {
                            if(!TeckleCTPlugin.stacksEqual(listA.get(j), listB.get(j), listB.get(j).hasTagCompound())) {
                                matchesNested = false;
                                break;
                            }
                        }

                        if(!matchesNested) {
                            matches = false;
                            break;
                        }

                        // pass
                    } else if(a instanceof ItemStack && b instanceof ItemStack && TeckleCTPlugin.stacksEqual((ItemStack)a, (ItemStack)b, ((ItemStack)b).hasTagCompound())) {
                        // pass
                    } else {
                        matches = false;
                        break;
                    }
                }

                if(matches) {
                    recipeIterator.remove();
                }
            }
        }

        @Override
        public String describe() {
            return "Removing matching Alloy Furnace recipes for given inputs (not stated here)";
        }
    }

    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new RemoveAll());
    }

    public static class RemoveAll implements IAction {
        @Override
        public void apply() {
            AlloyRecipes.getInstance().getMasterRecipeList().clear();
        }

        @Override
        public String describe() {
            return "Removing all recipes from Alloy Furnace";
        }
    }
}
