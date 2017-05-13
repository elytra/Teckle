package com.elytradev.teckle.jei;

import mezz.jei.api.BlankModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

/**
 * Created by darkevilmac on 5/13/17.
 */
@JEIPlugin
public class TeckleJEIPlugin extends BlankModPlugin {

    @Override
    public void register(IModRegistry registry) {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(new FabricatorRecipeTransferHandler(), VanillaRecipeCategoryUid.CRAFTING);
    }
}
