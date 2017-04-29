package com.elytradev.teckle.common.item;

import com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.TileItemTube;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public class ItemPaintbrush extends Item implements IResourceHolder {

    public ItemPaintbrush() {
        setMaxStackSize(1);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + EnumDyeColor.byDyeDamage(stack.getMetadata()).getUnlocalizedName();
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, NonNullList<ItemStack> subItems) {
        for (EnumDyeColor color : EnumDyeColor.values()) {
            subItems.add(new ItemStack(itemIn, 1, color.getDyeDamage()));
        }
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getHeldItem(hand);

        if (!player.canPlayerEdit(pos, facing, itemstack)) {
            return EnumActionResult.FAIL;
        }

        if (Objects.equals(worldIn.getBlockState(pos).getBlock(), TeckleObjects.blockItemTube)) {
            TileItemTube tubeItem = (TileItemTube) worldIn.getTileEntity(pos);

            if (tubeItem != null) {
                tubeItem.setColour(EnumDyeColor.byDyeDamage(getMetadata(itemstack)));
                worldIn.notifyBlockUpdate(pos, worldIn.getBlockState(pos), worldIn.getBlockState(pos), 4);
            }
        }

        return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public ResourceLocation getResource(EnumResourceType resourceType, int meta) {
        EnumDyeColor type = EnumDyeColor.byDyeDamage(meta);

        return new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "items/paintbrush_" + type.getName());
    }
}
