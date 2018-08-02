package com.elytradev.teckle.common.item;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.util.BlueprintUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlueprint extends Item {
    public ItemBlueprint() {
        setMaxStackSize(1);
        setCreativeTab(TeckleObjects.creativeTab);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        BlueprintUtil.getRecipeFromBlueprint(stack, worldIn).ifPresent(recipe -> tooltip.add(recipe.getRegistryName().toString()));
    }

}
