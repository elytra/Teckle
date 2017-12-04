package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.tile.TileBeamQuarry;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockBeamQuarry extends BlockContainer {

    public static PropertyEnum<QuarryState> STATE = PropertyEnum.create("active", QuarryState.class);

    protected BlockBeamQuarry(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setDefaultState(blockState.getBaseState());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(STATE, QuarryState.values()[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STATE).ordinal();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return meta < 2 ? new TileBeamQuarry() : null;
    }

    public enum QuarryState implements IStringSerializable {
        INACTIVE("inactive"), ACTIVE("active"), WALL("wall");

        private final String name;

        QuarryState(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
