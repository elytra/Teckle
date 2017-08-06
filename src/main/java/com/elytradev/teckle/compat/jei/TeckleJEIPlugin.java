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

package com.elytradev.teckle.compat.jei;

import com.elytradev.teckle.client.gui.GuiAlloyFurnace;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.crafting.AlloyRecipe;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.item.ItemStack;


@JEIPlugin
public class TeckleJEIPlugin implements IModPlugin {

    public static final String ALLOYRECIPE_UID = "com.elytradev.teckle.alloyrecipes";
    public static IJeiHelpers HELPERS;

    @Override
    public void register(IModRegistry registry) {
        HELPERS = registry.getJeiHelpers();
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(new FabricatorRecipeTransferHandler(), VanillaRecipeCategoryUid.CRAFTING);

        registry.addRecipes(AlloyRecipes.getInstance().getRecipes(), ALLOYRECIPE_UID);
        registry.handleRecipes(AlloyRecipe.class, AlloyRecipeWrapper::new, ALLOYRECIPE_UID);
        registry.addRecipeCatalyst(new ItemStack(TeckleObjects.blockAlloyFurnace), ALLOYRECIPE_UID);

        registry.addRecipeClickArea(GuiAlloyFurnace.class, 107, 32, 26, 23, ALLOYRECIPE_UID);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        HELPERS = registry.getJeiHelpers();
        registry.addRecipeCategories(new AlloyFurnaceCategory(HELPERS.getGuiHelper()));
    }
}
