package com.elytradev.teckle.common.block;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.TileTransposer;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
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

        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        List<IWorldNetwork> neighbourNetworks = TeckleObjects.blockItemTube.getNeighbourNetworks(worldIn, pos);
        IWorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getTileNetworked(tileAtPos);
        if (!neighbourNetworks.isEmpty()) {
            // Found neighbour networks, join the network or merge.
            IWorldNetwork network = neighbourNetworks.remove(0);
            thisNetworkTile.setNode(thisNetworkTile.createNode(network, pos));
            network.registerNode(thisNetworkTile.getNode());

            while (!neighbourNetworks.isEmpty()) {
                network = network.merge(neighbourNetworks.remove(0));
            }
        } else {
            // No neighbours, make a new network.
            WorldNetwork network = new WorldNetwork(worldIn, null);
            WorldNetworkDatabase.registerWorldNetwork(network);
            WorldNetworkNode node = thisNetworkTile.createNode(network, pos);
            network.registerNode(node);
            if (worldIn.getTileEntity(pos) != null) {
                thisNetworkTile.setNode(node);
            }
        }

        //Check for possible neighbour nodes...
        List<TileEntity> neighbourNodes = TeckleObjects.blockItemTube.getPotentialNeighbourNodes(worldIn, pos, thisNetworkTile.getNode().network, false);
        for (TileEntity neighbourTile : neighbourNodes) {
            if (CapabilityWorldNetworkTile.isPositionNetworkTile(worldIn, neighbourTile.getPos())) {
                IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(worldIn, neighbourTile.getPos());
                if (!thisNetworkTile.getNode().network.isNodePresent(neighbourTile.getPos())) {
                    thisNetworkTile.getNode().network.registerNode(neighbourNetworkTile.createNode(thisNetworkTile.getNode().network, neighbourTile.getPos()));
                    neighbourNetworkTile.setNode(thisNetworkTile.getNode().network.getNodeFromPosition(neighbourTile.getPos()));
                }
            } else {
                if (!thisNetworkTile.getNode().network.isNodePresent(neighbourTile.getPos())) {
                    thisNetworkTile.getNode().network.registerNode(new ItemNetworkEndpoint(thisNetworkTile.getNode().network, neighbourTile.getPos()));
                }
            }
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(blockAccess, pos, neighbor);
        // Handles cleanup of endpoint nodes, or nodes that should have been removed but weren't.
        TileTransposer transposer = (TileTransposer) blockAccess.getTileEntity(pos);
        if (transposer.getWorld().isRemote)
            return;

        IWorldNetworkTile thisNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(blockAccess, pos);
        if (thisNetworkTile.getNode() == null || thisNetworkTile.getNode().network == null) {
            World world = transposer.getWorld();
            List<IWorldNetwork> neighbourNetworks = TeckleObjects.blockItemTube.getNeighbourNetworks(world, pos);
            if (!neighbourNetworks.isEmpty()) {
                // Found neighbour networks, join the network or merge.
                IWorldNetwork network = neighbourNetworks.remove(0);
                thisNetworkTile.setNode(thisNetworkTile.createNode(network, pos));
                network.registerNode(thisNetworkTile.getNode());

                while (!neighbourNetworks.isEmpty()) {
                    network = network.merge(neighbourNetworks.remove(0));
                }
            } else {
                // No neighbours, make a new network.
                WorldNetwork network = new WorldNetwork(world, null);
                WorldNetworkDatabase.registerWorldNetwork(network);
                WorldNetworkNode node = thisNetworkTile.createNode(network, pos);
                network.registerNode(node);
                if (world.getTileEntity(pos) != null) {
                    thisNetworkTile.setNode(node);
                }
            }
        }

        if (!thisNetworkTile.getNode().network.isNodePresent(neighbor)) {
            // Node not already present, check if we can add to network.
            if (blockAccess.getTileEntity(neighbor) != null) {
                TileEntity neighbourTile = blockAccess.getTileEntity(neighbor);
                if (neighbourTile != null) {
                    if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                            WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                        // Create endpoint and put it in the network.
                        ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(thisNetworkTile.getNode().network, neighbor);
                        thisNetworkTile.getNode().network.registerNode(nodeEndpoint);
                    } else if (CapabilityWorldNetworkTile.isPositionNetworkTile(blockAccess, neighbourTile.getPos())) {
                        IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(blockAccess, neighbourTile.getPos());
                        if (neighbourNetworkTile.isValidNetworkMember(thisNetworkTile.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                            thisNetworkTile.getNode().network.registerNode((neighbourNetworkTile.createNode(thisNetworkTile.getNode().network, neighbourTile.getPos())));
                        }
                    }
                }
            }
        } else {
            if (blockAccess.getTileEntity(neighbor) == null) {
                thisNetworkTile.getNode().network.unregisterNodeAtPosition(neighbor);
            } else {
                TileEntity neighbourTile = blockAccess.getTileEntity(neighbor);
                if (!neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                    if (CapabilityWorldNetworkTile.isPositionNetworkTile(blockAccess, neighbourTile.getPos())) {
                        IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(blockAccess, neighbourTile.getPos());
                        if (neighbourNetworkTile.isValidNetworkMember(thisNetworkTile.getNode().network, WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                            return;
                        }
                    }

                    thisNetworkTile.getNode().network.unregisterNodeAtPosition(neighbor);
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
        if (tileAtPos != null && CapabilityWorldNetworkTile.isPositionNetworkTile(worldIn, pos)) {
            IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(worldIn, pos);

            if (neighbourNetworkTile.getNode() == null)
                return;
            neighbourNetworkTile.getNode().network.unregisterNodeAtPosition(pos);
            neighbourNetworkTile.getNode().network.validateNetwork();
            neighbourNetworkTile.setNode(null);

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
