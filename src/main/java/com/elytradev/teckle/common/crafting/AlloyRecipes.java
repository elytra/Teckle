package com.elytradev.teckle.common.crafting;

import com.elytradev.teckle.common.TeckleObjects;
import com.google.common.collect.Lists;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.List;

/**
 * Created by darkevilmac on 4/19/2017.
 */
public class AlloyRecipes {

    private static final AlloyRecipes INSTANCE = new AlloyRecipes();
    private HashMap<ItemStack, AlloyRecipe> recipes = new HashMap<>();

    public AlloyRecipes() {
    }

    public static AlloyRecipes getInstance() {
        return INSTANCE;
    }

    public List<AlloyRecipe> getRecipes() {
        return Lists.newArrayList(recipes.values());
    }

    public AlloyRecipe getRecipeByResult(ItemStack result) {
        return recipes.get(result);
    }

    public void init() {
        OreDictionary.registerOre("coal", Items.COAL); // Nothing to see here, move along.

        AlloyRecipe siliconBouleRecipe = new AlloyRecipe(new ItemStack(TeckleObjects.itemSiliconBoule), new Tuple<>("sand", 8), new Tuple<>("coal", 8));
        recipes.put(siliconBouleRecipe.getCraftingResult(), siliconBouleRecipe);
    }


}
