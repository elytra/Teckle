package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.tile.base.TileItemNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkEntryPoint;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class BlockFilter extends BlockContainer {

    public static PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockFilter(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(blockState.getBaseState());
        this.setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.values()[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).ordinal();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFilter();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isRemote)
            return;
        EnumFacing facing = state.getValue(FACING);
        TileFilter tileEntityFilter = (TileFilter) worldIn.getTileEntity(pos);
        TileEntity neighbour = worldIn.getTileEntity(pos.offset(facing));
        if (neighbour != null && neighbour instanceof TileItemTube) {
            TileItemTube tube = (TileItemTube) neighbour;
            tileEntityFilter.node = new WorldNetworkEntryPoint(tube.node.network, pos, facing);
            tube.node.network.registerNode(tileEntityFilter.node);
            System.out.println(tileEntityFilter + " Setting network to " + tube.node.network);
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = EnumFacing.getDirectionFromEntityLiving(pos, placer);

        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, direction);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
        // Handles cleanup of endpoint nodes, or nodes that should have been removed but weren't.
        EnumFacing sideChanged = EnumFacing.DOWN;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing.getDirectionVec().equals(pos.subtract(neighbor))) {
                sideChanged = facing;
                break;
            }
        }

        TileFilter filter = (TileFilter) world.getTileEntity(pos);
        IBlockState state = world.getBlockState(pos);
        if (filter.getWorld().isRemote || !state.getValue(FACING).getOpposite().equals(sideChanged))
            return;

        TileEntity neighbourTile = world.getTileEntity(neighbor);
        if (filter.node == null || filter.node.network == null) {
            if (neighbourTile != null && neighbourTile instanceof TileItemTube) {
                filter.node = new WorldNetworkEntryPoint(((TileItemTube) neighbourTile).node.network, pos, state.getValue(FACING));
                ((TileItemTube) neighbourTile).node.network.registerNode(filter.node);
            }
        } else {
            if (neighbourTile == null || !(neighbourTile instanceof TileItemTube)) {
                filter.node.network.unregisterNodeAtPosition(pos);
                filter.node = null;
            }
        }
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        if (tileAtPos != null) {
            TileItemNetworkMember networkMember = (TileItemNetworkMember) tileAtPos;
            if (networkMember.node == null)
                return;
            networkMember.node.network.unregisterNodeAtPosition(pos);
            networkMember.node.network.validateNetwork();
            networkMember.node = null;
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote)
            return true;

        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile != null && tile instanceof TileFilter) {
            return ((TileFilter) tile).pushToNetwork();
        }

        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
