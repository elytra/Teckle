package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.block.BlockOre;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Created by darkevilmac on 4/20/2017.
 */
public class BlockNikoliteOre extends BlockOre {
    public BlockNikoliteOre() {
        super();
        this.setHardness(3.0F);
        this.setResistance(5.0F);
        this.setSoundType(SoundType.STONE);
        this.setHarvestLevel("pickaxe", 2);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return TeckleObjects.itemNikolite;
    }

    @Override
    public int quantityDropped(Random random) {
        return 4;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(TeckleObjects.blockNikoliteOre);
    }

    @Override
    public int getExpDrop(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, int fortune) {
        if (this.getItemDropped(state, RANDOM, fortune) != Item.getItemFromBlock(this)) {
            return 1 + RANDOM.nextInt(5);
        }
        return 0;
    }

}
