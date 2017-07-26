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

import com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkAssistantHolder;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.block.property.UnlistedBool;
import com.elytradev.teckle.common.block.property.UnlistedEnum;
import com.elytradev.teckle.common.tile.TileItemTube;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class BlockItemTube extends BlockContainer implements IResourceHolder {

    public static UnlistedBool NORTH = new UnlistedBool("north");
    public static UnlistedBool EAST = new UnlistedBool("east");
    public static UnlistedBool SOUTH = new UnlistedBool("south");
    public static UnlistedBool WEST = new UnlistedBool("west");
    public static UnlistedBool UP = new UnlistedBool("up");
    public static UnlistedBool DOWN = new UnlistedBool("down");
    public static UnlistedBool NODE = new UnlistedBool("node");

    public static UnlistedEnum<EnumDyeColor> COLOUR = new UnlistedEnum("colour", EnumDyeColor.class);

    public BlockItemTube(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(0.25F);
        this.setResistance(10.0F);
        this.setLightOpacity(0);
        this.setDefaultState(blockState.getBaseState());
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getBoundingBox(state, source, pos);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
        List<EnumFacing> connections = getConnections(world, pos);
        EnumDyeColor colour = null;
        boolean node = connections.isEmpty() || connections.size() > 2 || connections.size() == 1;

        for (EnumFacing facing : connections) {
            if (node)
                break;

            for (EnumFacing otherFacing : EnumFacing.VALUES) {
                if (otherFacing.equals(facing.getOpposite()) || otherFacing.equals(facing))
                    continue;

                if (connections.contains(otherFacing)) {
                    node = true;
                    break;
                }
            }
        }

        if (world.getTileEntity(pos) instanceof TileItemTube) {
            colour = ((TileItemTube) world.getTileEntity(pos)).getColour();
        }

        return extendedBlockState.withProperty(NORTH, connections.contains(EnumFacing.NORTH))
                .withProperty(EAST, connections.contains(EnumFacing.EAST))
                .withProperty(SOUTH, connections.contains(EnumFacing.SOUTH))
                .withProperty(WEST, connections.contains(EnumFacing.WEST))
                .withProperty(DOWN, connections.contains(EnumFacing.DOWN))
                .withProperty(UP, connections.contains(EnumFacing.UP))
                .withProperty(NODE, node)
                .withProperty(COLOUR, colour);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedState = (IExtendedBlockState) getExtendedState(state, world, pos);

        float pixel = 1f / 16f;
        float min = pixel * 4;
        float max = 1 - min;

        if (extendedState.getValue(NODE)) {
            min -= pixel;
            max += pixel;
        }

        float x1 = min;
        float y1 = min;
        float z1 = min;
        float x2 = max;
        float y2 = max;
        float z2 = max;

        if (extendedState.getValue(NORTH)) z1 = 0;
        if (extendedState.getValue(WEST)) x1 = 0;
        if (extendedState.getValue(DOWN)) y1 = 0;
        if (extendedState.getValue(EAST)) x2 = 1;
        if (extendedState.getValue(SOUTH)) z2 = 1;
        if (extendedState.getValue(UP)) y2 = 1;

        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return getBoundingBox(state, world, pos).offset(pos);
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty[]{}, new IUnlistedProperty[]{NORTH, EAST, SOUTH, WEST, UP, DOWN, NODE, COLOUR});
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            if (Objects.equals(playerIn.getHeldItem(hand).getItem(), Items.MILK_BUCKET)) {
                if (worldIn.getTileEntity(pos) instanceof TileItemTube) {
                    ((TileItemTube) worldIn.getTileEntity(pos)).setColour(null);
                }
            }
        }

        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        getNetworkHelper(worldIn).onNodePlaced(worldIn, pos);
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(blockAccess, pos, neighbor);
        TileItemTube tube = (TileItemTube) blockAccess.getTileEntity(pos);
        if (tube.getWorld().isRemote)
            return;

        getNetworkHelper(tube.getWorld()).onNodeNeighbourChange(tube.getWorld(), pos, neighbor);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        getNetworkHelper(worldIn).onNodeBroken(worldIn, pos);

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileItemTube();
    }

    public List<EnumFacing> getConnections(IBlockAccess world, BlockPos pos) {
        List<EnumFacing> connections = new ArrayList<>();

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (canConnectTo(world, pos, facing.getOpposite())) {
                connections.add(facing);
            }
        }

        return connections;
    }

    private boolean canConnectTo(IBlockAccess world, BlockPos pos, EnumFacing side) {
        BlockPos connectionPos = pos.add(side.getOpposite().getDirectionVec());
        TileEntity tileAtPos = world.getTileEntity(connectionPos);

        boolean canConnect = false;
        if (tileAtPos != null) {
            if (tileAtPos.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
                canConnect = true;
            }
            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, connectionPos, side)
                    && CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, connectionPos, side).canConnectTo(side)) {
                canConnect = true;
            }
        }

        if (canConnect && CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos, side)) {
            canConnect = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, side).canConnectTo(side.getOpposite());
        }

        return canConnect;
    }

    public IWorldNetworkAssistant<ItemStack> getNetworkHelper(World world) {
        return world.getCapability(CapabilityWorldNetworkAssistantHolder.NETWORK_ASSISTANT_HOLDER_CAPABILITY, null).getAssistant(ItemStack.class);
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

    @Override
    @SideOnly(Side.CLIENT)
    public ResourceLocation getResource(EnumResourceType resourceType, int meta) {
        return new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "blocks/itemtube.full");
    }
}
