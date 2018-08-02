package com.elytradev.teckle.common.item;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.util.BlueprintUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlueprint extends Item {
    public ItemBlueprint() {
        setMaxStackSize(1);
        setCreativeTab(TeckleObjects.creativeTab);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack stack) {
        ItemStack out = BlueprintUtil.getRecipeFromBlueprint(stack, Minecraft.getMinecraft().world)
                .map(IRecipe::getRecipeOutput).orElse(ItemStack.EMPTY);
        if (out.isEmpty())
            return I18n.format("item.teckle.blueprint.empty.name");
        if (out.getItem() == TeckleObjects.itemBlueprint)
            return super.getItemStackDisplayName(stack) + " (" + super.getItemStackDisplayName(out) + ")";
        return super.getItemStackDisplayName(stack) + " (" + out.getDisplayName() + ")";
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
    }
}