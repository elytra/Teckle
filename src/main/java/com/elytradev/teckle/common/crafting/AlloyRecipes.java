package com.elytradev.teckle.common.crafting;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;

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
        AlloyRecipe siliconBouleRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconBoule),
                new Tuple<>("sand", 8),
                new Tuple<>("coal", 8));
        recipes.put(siliconBouleRecipe.getCraftingResult(), siliconBouleRecipe);

        AlloyRecipe redDopedWaferRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconWafer, 1, ItemSiliconWafer.WaferType.RED.getMetadata()),
                new Tuple<>("dustRedstone", 4),
                new Tuple<>(new ItemStack(TeckleObjects.itemSiliconWafer, 1, 0), null));
        recipes.put(redDopedWaferRecipe.getCraftingResult(), redDopedWaferRecipe);

        AlloyRecipe blueDopedWaferRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconWafer, 1, ItemSiliconWafer.WaferType.BLUE.getMetadata()),
                new Tuple<>("dustNikolite", 4),
                new Tuple<>(new ItemStack(TeckleObjects.itemSiliconWafer, 1, 0), null));
        recipes.put(blueDopedWaferRecipe.getCraftingResult(), blueDopedWaferRecipe);

        AlloyRecipe brassIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemBrassIngot),
                new Tuple<>("ingotTin", 1),
                new Tuple<>("ingotCopper", 3)
        );
        recipes.put(new ItemStack(TeckleObjects.itemBrassIngot), brassIngotRecipe);
    }


}
