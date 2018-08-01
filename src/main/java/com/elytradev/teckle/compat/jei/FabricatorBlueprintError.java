package com.elytradev.teckle.compat.jei;

import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class FabricatorBlueprintError implements IRecipeTransferError {
    @Override
    public Type getType() {
        return Type.USER_FACING;
    }

    @Override
    public void showError(Minecraft mc, int mouseX, int mouseY, IRecipeLayout recipeLayout, int recipeX, int recipeY) {
        mc.fontRenderer.drawStringWithShadow(TextFormatting.RED + I18n.format("tooltip.teckle.fabricator.missing"), recipeX + 60, recipeY, 0);
    }
}