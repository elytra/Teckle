package com.elytradev.teckle.jei;

import com.elytradev.teckle.common.container.ContainerFabricator;
import com.elytradev.teckle.common.network.FabricatorTemplateMessage;
import com.elytradev.teckle.common.tile.TileFabricator;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Handles setting templates in the Fabricator.
 */
public class FabricatorRecipeTransferHandler implements IRecipeTransferHandler<ContainerFabricator> {
    @Override
    public Class getContainerClass() {
        return ContainerFabricator.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(ContainerFabricator container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            Map<Integer, ? extends IGuiIngredient<ItemStack>> guiIngredients = recipeLayout.getItemStacks().getGuiIngredients();
            TileFabricator fabricator = container.fabricator;
            for (int i = 1; i < 10; i++) {
                ItemStack templateIngredient = ItemStack.EMPTY;
                if (guiIngredients.containsKey(i)) {
                    templateIngredient = guiIngredients.get(i).getDisplayedIngredient();
                    if (templateIngredient == null) {
                        templateIngredient = ItemStack.EMPTY;
                    } else {
                        templateIngredient.setCount(1);
                    }
                }
                fabricator.setTemplateSlot(i - 1, templateIngredient);
                new FabricatorTemplateMessage(fabricator.getPos(), templateIngredient, i - 1).sendToServer();
            }
        }
        return null;
    }

}
