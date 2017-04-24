package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.TileTransposer;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
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
import net.minecraft.inventory.InventoryHelper;
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
public class BlockTransposer extends BlockContainer {

    public static PropertyDirection FACING = PropertyDirection.create("facing");
    public static PropertyBool TRIGGERED = PropertyBool.create("triggered");

    public BlockTransposer(Material materialIn) {
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
        return new TileTransposer();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isRemote)
            return;

        List<WorldNetwork> neighbourNetworks = TeckleObjects.blockItemTube.getNeighbourNetworks(worldIn, pos);
        TileTransposer transposer = ((TileTransposer) worldIn.getTileEntity(pos));
        if (!neighbourNetworks.isEmpty()) {
            // Found neighbour networks, join the network or merge.
            WorldNetwork network = neighbourNetworks.remove(0);
            transposer.setNode(transposer.getNode(network));
            network.registerNode(transposer.getNode());

            while (!neighbourNetworks.isEmpty()) {
                network = network.merge(neighbourNetworks.remove(0));
            }
        } else {
            // No neighbours, make a new network.
            WorldNetwork network = new WorldNetwork(worldIn, null);
            WorldNetworkDatabase.registerWorldNetwork(network);
            WorldNetworkNode node = transposer.getNode(network);
            network.registerNode(node);
            if (worldIn.getTileEntity(pos) != null) {
                transposer.setNode(node);
            }
        }

        //Check for possible neighbour nodes...
        List<TileEntity> neighbourNodes = TeckleObjects.blockItemTube.getPotentialNeighbourNodes(worldIn, pos, transposer.getNode().network, false);
        for (TileEntity neighbourNode : neighbourNodes) {
            if (neighbourNode instanceof TileNetworkMember) {
                if (!transposer.getNode().network.isNodePresent(neighbourNode.getPos())) {
                    transposer.getNode().network.registerNode(((TileNetworkMember) neighbourNode).getNode(transposer.getNode().network));
                    ((TileNetworkMember) neighbourNode).setNode(transposer.getNode().network.getNodeFromPosition(neighbourNode.getPos()));
                }
            } else {
                if (!transposer.getNode().network.isNodePresent(neighbourNode.getPos())) {
                    transposer.getNode().network.registerNode(new ItemNetworkEndpoint(transposer.getNode().network, neighbourNode.getPos()));
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
        TileTransposer transposer = (TileTransposer) world.getTileEntity(pos);
        if (transposer.getWorld().isRemote)
            return;

        if (!transposer.getNode().network.isNodePresent(neighbor)) {
            // Node not already present, check if we can add to network.
            if (world.getTileEntity(neighbor) != null) {
                TileEntity neighbourTile = world.getTileEntity(neighbor);
                if (neighbourTile != null) {
                    if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                            WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                        // Create endpoint and put it in the network.
                        ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(transposer.getNode().network, neighbor);
                        transposer.getNode().network.registerNode(nodeEndpoint);
                    } else if (neighbourTile instanceof TileNetworkMember) {
                        if (((TileNetworkMember) neighbourTile).isValidNetworkMember(transposer.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                            transposer.getNode().network.registerNode(((TileNetworkMember) neighbourTile).getNode(transposer.getNode().network));
                        }
                    }
                }
            }
        } else {
            if (world.getTileEntity(neighbor) == null) {
                transposer.getNode().network.unregisterNodeAtPosition(neighbor);
            } else {
                TileEntity neighbourTile = world.getTileEntity(neighbor);
                if (!neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                    if (neighbourTile instanceof TileNetworkMember) {
                        if (((TileNetworkMember) neighbourTile).isValidNetworkMember(transposer.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                            return;
                        }
                    }

                    transposer.getNode().network.unregisterNodeAtPosition(neighbor);
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
        if (tileentity instanceof TileTransposer) {
            if (powered) {
                worldIn.setBlockState(pos, state.withProperty(TRIGGERED, true));
                if (!hadPower)
                    ((TileTransposer) tileentity).tryPush();
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

            if (tileAtPos instanceof TileTransposer) {
                TileTransposer transposer = (TileTransposer) worldIn.getTileEntity(pos);

                // Vomit the buffer.
                transposer.buffer.stream().filter(stack -> !stack.isEmpty()).forEach(stack -> InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack));
            }
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
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
