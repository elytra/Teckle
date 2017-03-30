package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.block.property.UnlistedBool;
import com.elytradev.teckle.common.tile.TileItemEntrypoint;
import com.elytradev.teckle.common.tile.TileItemNetworkMember;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.worldnetwork.*;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
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

public class BlockItemTube extends BlockContainer {

    public static UnlistedBool NORTH = new UnlistedBool("north");
    public static UnlistedBool EAST = new UnlistedBool("east");
    public static UnlistedBool SOUTH = new UnlistedBool("south");
    public static UnlistedBool WEST = new UnlistedBool("west");
    public static UnlistedBool UP = new UnlistedBool("up");
    public static UnlistedBool DOWN = new UnlistedBool("down");
    public static UnlistedBool NODE = new UnlistedBool("node");

    public BlockItemTube(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setLightOpacity(0);

        this.setDefaultState(blockState.getBaseState()
        );
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return getBoundingBox(state, source, pos);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
        List<EnumFacing> connections = getConnections(world, pos);
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

        return extendedBlockState.withProperty(NORTH, connections.contains(EnumFacing.NORTH))
                .withProperty(EAST, connections.contains(EnumFacing.EAST))
                .withProperty(SOUTH, connections.contains(EnumFacing.SOUTH))
                .withProperty(WEST, connections.contains(EnumFacing.WEST))
                .withProperty(DOWN, connections.contains(EnumFacing.DOWN))
                .withProperty(UP, connections.contains(EnumFacing.UP))
                .withProperty(NODE, node);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        IExtendedBlockState extendedState = (IExtendedBlockState) getExtendedState(state, world, pos);

        float pixel = 1f / 16f;
        float min = pixel * 5;
        float max = 1 - min;

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
        return new ExtendedBlockState(this, new IProperty[]{}, new IUnlistedProperty[]{NORTH, EAST, SOUTH, WEST, UP, DOWN, NODE});
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
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (worldIn.isRemote)
            return;

        List<WorldNetwork> neighbourNetworks = getNeighbourNetworks(worldIn, pos);
        TileItemTube tube = ((TileItemTube) worldIn.getTileEntity(pos));
        if (!neighbourNetworks.isEmpty()) {
            // Found neighbour networks, join the network or merge.
            WorldNetwork network = neighbourNetworks.remove(0);

            network.registerNode(new WorldNetworkNode(network, pos));
            tube.network = network;

            while (!neighbourNetworks.isEmpty()) {
                network = network.merge(neighbourNetworks.remove(0));
            }
        } else {
            // No neighbours, make a new network.
            WorldNetwork network = new WorldNetwork(worldIn);
            network.registerNode(new WorldNetworkNode(network, pos));
            if (worldIn.getTileEntity(pos) != null) {
                tube.network = network;
            }
        }

        //Check for possible neighbour nodes...
        List<WorldNetworkNode> neighbourNodes = getPotentialNeighbourNodes(worldIn, pos, tube.network);
        for (WorldNetworkNode neighbourNode : neighbourNodes) {
            if (!tube.network.isNodePresent(neighbourNode.position)) {
                tube.network.registerNode(neighbourNode);
            }
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
        // Handles cleanup of endpoint nodes, or nodes that should have been removed but weren't.
        TileItemTube tube = (TileItemTube) world.getTileEntity(pos);
        if (tube.getWorld().isRemote)
            return;

        if (!tube.network.isNodePresent(neighbor)) {
            // Node not already present, check if we can add to network.
            if (world.getTileEntity(neighbor) != null) {
                TileEntity neighbourTile = world.getTileEntity(neighbor);
                if (neighbourTile != null) {
                    if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                            WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbor)))) {
                        // Create endpoint and put it in the network.
                        ItemNetworkEndpoint nodeEndpoint = new ItemNetworkEndpoint(tube.network, neighbor);
                        tube.network.registerNode(nodeEndpoint);
                    }
                }
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        if (tileAtPos != null) {
            TileItemTube tube = (TileItemTube) tileAtPos;
            tube.network.unregisterNodeAtPosition(pos);
            tube.network.validateNetwork();
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileItemTube();
    }

    /**
     * Check if the block has any potential connections to tubes around it.
     *
     * @param world the world to check within
     * @param pos   the position of the tube to check around
     * @return a list of neighbouring networks.
     */
    private List<WorldNetwork> getNeighbourNetworks(IBlockAccess world, BlockPos pos) {
        List<WorldNetwork> neighbourNetworks = new ArrayList<>();
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());

            if (world.getBlockState(neighbourPos).getBlock() instanceof BlockItemTube) {
                TileItemTube neighbourTube = (TileItemTube) world.getTileEntity(neighbourPos);
                if (!neighbourNetworks.contains(neighbourTube.network))
                    neighbourNetworks.add(neighbourTube.network);
            }
        }

        return neighbourNetworks;
    }

    private List<WorldNetworkNode> getPotentialNeighbourNodes(IBlockAccess world, BlockPos pos, WorldNetwork network) {
        List<WorldNetworkNode> neighbourNodes = new ArrayList<>();

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());
            TileEntity neighbourTile = world.getTileEntity(neighbourPos);

            if (neighbourTile != null) {
                if (neighbourTile instanceof TileItemEntrypoint
                        && ((TileItemEntrypoint) neighbourTile).getFacing().equals
                        (WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)).getOpposite())) {
                    neighbourNodes.add(new WorldNetworkEntryPoint(network, neighbourPos, facing));
                } else if (neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                        WorldNetworkTraveller.getFacingFromVector(pos.subtract(neighbourPos)))) {
                    neighbourNodes.add(new WorldNetworkEndpoint(network, neighbourPos));
                }
            }
        }

        return neighbourNodes;
    }

    public List<EnumFacing> getConnections(IBlockAccess world, BlockPos pos) {
        List<EnumFacing> connections = new ArrayList<>();

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());

            if (canConnectTo(world, neighbourPos, facing.getOpposite())) {
                connections.add(facing);
            }
        }

        return connections;
    }

    private boolean canConnectTo(IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntity tileAtPos = world.getTileEntity(pos);

        boolean canConnect = false;

        if (tileAtPos != null) {
            if (tileAtPos.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
                canConnect = true;
            } else if (tileAtPos instanceof TileItemNetworkMember) {
                canConnect = true;
            }
        }

        return canConnect;
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
