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

package com.elytradev.teckle.common.crafting;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.item.ItemIngot;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;


public class AlloyRecipes {

    private static final AlloyRecipes INSTANCE = new AlloyRecipes();
    private List<AlloyRecipe> recipes = new ArrayList<>();

    public AlloyRecipes() {
    }

    public static AlloyRecipes getInstance() {
        return INSTANCE;
    }

    public List<AlloyRecipe> getRecipes() {
        return Lists.newArrayList(recipes);
    }

    public void init() {
        recipes.clear();
        AlloyRecipe siliconBouleRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconBoule),
                new Tuple<>("sand", 8),
                new Tuple<>("coal", 8));
        recipes.add(siliconBouleRecipe);

        AlloyRecipe redDopedWaferRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconWafer, 1, ItemSiliconWafer.WaferType.RED.getMetadata()),
                new Tuple<>("dustRedstone", 4),
                new Tuple<>(new ItemStack(TeckleObjects.itemSiliconWafer, 1, 0), null));
        recipes.add(redDopedWaferRecipe);

        AlloyRecipe blueDopedWaferRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconWafer, 1, ItemSiliconWafer.WaferType.BLUE.getMetadata()),
                new Tuple<>("dustNikolite", 4),
                new Tuple<>(new ItemStack(TeckleObjects.itemSiliconWafer, 1, 0), null));
        recipes.add(blueDopedWaferRecipe);

        AlloyRecipe brassIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.BRASS.getMetadata()),
                new Tuple<>("ingotTin", 1),
                new Tuple<>("ingotCopper", 3)
        );
        recipes.add(brassIngotRecipe);

        AlloyRecipe redAlloyIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.RED_ALLOY.getMetadata()),
                new Tuple<>("ingotCopper", 1),
                new Tuple<>("dustRedstone", 4)
        );
        recipes.add(redAlloyIngotRecipe);

        AlloyRecipe redAlloyIngotRecipeAlt = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.RED_ALLOY.getMetadata()),
                new Tuple<>("ingotIron", 1),
                new Tuple<>("dustRedstone", 4)
        );
        recipes.add(redAlloyIngotRecipeAlt);

        AlloyRecipe blueAlloyIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.BLUE_ALLOY.getMetadata()),
                new Tuple<>("ingotSilver", 1),
                new Tuple<>("dustNikolite", 4)
        );
        recipes.add(blueAlloyIngotRecipe);
    }


    public void clear() {
        recipes.clear();
    }
}
