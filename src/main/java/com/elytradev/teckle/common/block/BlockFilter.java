/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.block;

import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkAssistantHolder;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.handlers.TeckleGuiHandler;
import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class BlockFilter extends BlockContainer {

    public static PropertyDirection FACING = PropertyDirection.create("facing");
    public static PropertyBool TRIGGERED = PropertyBool.create("triggered");

    public BlockFilter(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setDefaultState(blockState.getBaseState());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, BlockPistonBase.getFacing(meta)).withProperty(TRIGGERED, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();
        if (state.getValue(TRIGGERED)) {
            i |= 8;
        }

        return i;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFilter();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        getNetworkHelper(worldIn).onNodePlaced(worldIn, pos);
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(blockAccess, pos, neighbor);
        TileEntity tile = blockAccess.getTileEntity(pos);

        getNetworkHelper(tile.getWorld()).onNodeNeighbourChange(tile.getWorld(), pos, neighbor);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = EnumFacing.getDirectionFromEntityLiving(pos, placer);
        if (placer != null && placer.isSneaking()) direction = direction.getOpposite();
        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, direction).withProperty(TRIGGERED, false);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote)
            return;

        boolean powered = worldIn.isBlockPowered(pos);
        boolean hadPower = state.getValue(TRIGGERED);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileFilter) {
            if (powered) {
                worldIn.setBlockState(pos, state.withProperty(TRIGGERED, true));
                if (!hadPower) {
                    ((TileFilter) tileentity).tryPush();
                }
            } else {
                worldIn.setBlockState(pos, state.withProperty(TRIGGERED, false));
            }
        }
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, TRIGGERED);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        if (tileAtPos != null && CapabilityWorldNetworkTile.isPositionNetworkTile(worldIn, pos, null)) {
            getNetworkHelper(worldIn).onNodeBroken(worldIn, pos);

            if (tileAtPos instanceof TileFilter) {
                TileFilter filter = (TileFilter) worldIn.getTileEntity(pos);

                // Vomit the buffer.
                filter.bufferData.getHandler().stream().filter(stack -> !stack.isEmpty()).forEach(stack -> InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack));
                // Vomit the filter data.
                filter.filterData.getHandler().stream().filter(stack -> !stack.isEmpty()).forEach(stack -> InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack));
                AdvancedStackHandlerPool.getPool(worldIn).remove(filter.filterData.getId());
                AdvancedStackHandlerPool.getPool(worldIn).remove(filter.bufferData.getId());
            }
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!playerIn.isSneaking()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity != null) {
                playerIn.openGui(TeckleMod.INSTANCE, TeckleGuiHandler.ElementType.ELEMENT_PROVIDER.caseNumber, worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }

        return false;
    }

    public IWorldNetworkAssistant<ItemStack> getNetworkHelper(World world) {
        return world.getCapability(CapabilityWorldNetworkAssistantHolder.NETWORK_ASSISTANT_HOLDER_CAPABILITY, null).getAssistant(ItemStack.class);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}
