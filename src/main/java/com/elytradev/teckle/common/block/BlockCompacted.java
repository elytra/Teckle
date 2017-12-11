package com.elytradev.teckle.common.block;

import com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.item.ItemIngot;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.Objects;

public class BlockCompacted extends Block implements IResourceHolder {

    public static final PropertyEnum<ItemIngot.IngotType> TYPE_ENUM = PropertyEnum.create("ingot", ItemIngot.IngotType.class,
            Objects::nonNull);

    public BlockCompacted(Material blockMaterialIn) {
        super(blockMaterialIn);
        this.setHarvestLevel("pickaxe", 2);
        this.setHardness(4F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(TYPE_ENUM, ItemIngot.IngotType.RED_ALLOY));
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE_ENUM);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(TYPE_ENUM).getMetadata();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(TYPE_ENUM, ItemIngot.IngotType.byMetadata(meta));
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab == TeckleObjects.creativeTab) {
            for (ItemIngot.IngotType ingotType : ItemIngot.IngotType.values()) {
                if (ingotType != ItemIngot.IngotType.BRASS) {
                    ItemStack stack = new ItemStack(this);
                    stack.setItemDamage(ingotType.getMetadata());
                    items.add(stack);
                }
            }
        }
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(TYPE_ENUM).getMetadata();
    }

    @Nullable
    @Override
    public ResourceLocation getResource(EnumResourceType resourceType, int meta) {
        if (resourceType == EnumResourceType.TEXTURE) {
            ItemIngot.IngotType ingotType = ItemIngot.IngotType.byMetadata(meta);
            return new ResourceLocation(TeckleMod.MOD_ID, "blocks/" + ingotType.getName() + "_block");
        }
        return null;
    }
}
