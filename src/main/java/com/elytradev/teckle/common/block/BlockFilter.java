package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.*;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
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
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class BlockFilter extends BlockContainer {

    public static PropertyDirection FACING = PropertyDirection.create("facing");
    public static PropertyBool TRIGGERED = PropertyBool.create("triggered");

    public BlockFilter(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(blockState.getBaseState());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, BlockPistonBase.getFacing(meta)).withProperty(TRIGGERED, Boolean.valueOf((meta & 8) > 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();
        if (state.getValue(TRIGGERED).booleanValue()) {
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
        if (worldIn.isRemote)
            return;
        EnumFacing facing = state.getValue(FACING);
        TileFilter filter = (TileFilter) worldIn.getTileEntity(pos);
        TileEntity neighbour = worldIn.getTileEntity(pos.offset(facing));
        if (neighbour != null && neighbour instanceof TileItemTube) {
            TileItemTube tube = (TileItemTube) neighbour;
            filter.setNode(new WorldNetworkEntryPoint(tube.getNode().network, pos, facing));
            tube.getNode().network.registerNode(filter.getNode());
        } else {
            WorldNetwork network = new WorldNetwork(worldIn, null);
            WorldNetworkDatabase.registerWorldNetwork(network);
            WorldNetworkNode node = filter.getNode(network);
            network.registerNode(node);
            if (worldIn.getTileEntity(pos) != null) {
                filter.setNode(node);
            }
        }


        //Check for possible neighbour nodes...
        List<TileEntity> neighbourNodes = TeckleObjects.blockItemTube.getPotentialNeighbourNodes(worldIn, pos, filter.getNode().network, false);
        for (TileEntity neighbourNode : neighbourNodes) {
            if (neighbourNode instanceof TileNetworkMember) {
                if (!filter.getNode().network.isNodePresent(neighbourNode.getPos())) {
                    filter.getNode().network.registerNode(((TileNetworkMember) neighbourNode).getNode(filter.getNode().network));
                    ((TileNetworkMember) neighbourNode).setNode(filter.getNode().network.getNodeFromPosition(neighbourNode.getPos()));
                }
            } else {
                if (!filter.getNode().network.isNodePresent(neighbourNode.getPos())) {
                    filter.getNode().network.registerNode(new ItemNetworkEndpoint(filter.getNode().network, neighbourNode.getPos()));
                }
            }
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = EnumFacing.getDirectionFromEntityLiving(pos, placer);

        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, direction).withProperty(TRIGGERED, false);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
        // Handles cleanup of endpoint nodes, or nodes that should have been removed but weren't.
        TileFilter filter = (TileFilter) world.getTileEntity(pos);
        if (filter.getWorld().isRemote)
            return;

        if (!filter.getNode().network.isNodePresent(neighbor)) {
            // Node not already present, check if we can add to network.
            if (world.getTileEntity(neighbor) != null) {
                TileEntity neighbourTile = world.getTileEntity(neighbor);
                if (neighbourTile != null) {
                    if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                            WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                        // Create endpoint and put it in the network.
                        ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(filter.getNode().network, neighbor);
                        filter.getNode().network.registerNode(nodeEndpoint);
                    } else if (neighbourTile instanceof TileNetworkMember) {
                        if (((TileNetworkMember) neighbourTile).isValidNetworkMember(filter.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                            filter.getNode().network.registerNode(((TileNetworkMember) neighbourTile).getNode(filter.getNode().network));
                        }
                    }
                }
            }
        } else {
            if (world.getTileEntity(neighbor) == null) {
                filter.getNode().network.unregisterNodeAtPosition(neighbor);
            } else {
                TileEntity neighbourTile = world.getTileEntity(neighbor);
                if (!neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                    if (neighbourTile instanceof TileNetworkMember) {
                        if (((TileNetworkMember) neighbourTile).isValidNetworkMember(filter.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                            return;
                        }
                    }

                    filter.getNode().network.unregisterNodeAtPosition(neighbor);
                }
            }
        }
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
                if (!hadPower)
                    ((TileFilter) tileentity).tryPush();
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
        if (tileAtPos != null) {
            TileNetworkMember networkMember = (TileNetworkMember) tileAtPos;
            if (networkMember.getNode() == null)
                return;
            networkMember.getNode().network.unregisterNodeAtPosition(pos);
            networkMember.getNode().network.validateNetwork();
            networkMember.setNode(null);
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!playerIn.isSneaking()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity != null) {
                playerIn.openGui(TeckleMod.INSTANCE, 1, worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
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
        return BlockRenderLayer.SOLID;
    }
}
