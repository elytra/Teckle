package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.tile.TileBeamQuarry;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockBeamQuarry extends BlockContainer {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    protected BlockBeamQuarry(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setDefaultState(blockState.getBaseState());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);

        if (!worldIn.isRemote) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity != null && tileEntity instanceof TileBeamQuarry) {
                // set the default bounds of the quarry.
                EnumFacing facing = placer.getHorizontalFacing();
                EnumFacing relativeLeft = facing.rotateYCCW();
                EnumFacing relativeRight = facing.rotateY();
                BlockPos basePos = pos.offset(facing);
                BlockPos min = basePos.add(relativeLeft.getDirectionVec().crossProduct(new Vec3i(4, 4, 4)));
                BlockPos max = basePos.add(relativeRight.getDirectionVec().crossProduct(new Vec3i(4, 4, 4)));
                max = max.add(facing.getDirectionVec().crossProduct(new Vec3i(8, 8, 8)));
                ((TileBeamQuarry) tileEntity).setBounds(min, max);
            }
        }
    }

    @Override
    public BlockStateContainer getBlockState() {
        return new BlockStateContainer(this, ACTIVE, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing front = EnumFacing.getFront(meta & 7);
        if (front.getAxis() == EnumFacing.Axis.Y) {
            front = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, front).withProperty(ACTIVE, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();

        if (state.getValue(ACTIVE).booleanValue()) {
            i |= 8;
        }

        return i;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileBeamQuarry();
    }

}
