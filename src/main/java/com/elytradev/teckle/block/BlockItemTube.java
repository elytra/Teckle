package com.elytradev.teckle.block;

import com.elytradev.teckle.tile.TileItemTube;
import com.elytradev.teckle.worldnetwork.WorldNetwork;
import com.elytradev.teckle.worldnetwork.WorldNetworkNode;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by darkevilmac on 3/27/2017.
 */
public class BlockItemTube extends BlockContainer {
    protected BlockItemTube(Material materialIn) {
        super(materialIn);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        List<WorldNetwork> neighbourNetworks = getNeighbourNetworks(worldIn, pos);
        if (!neighbourNetworks.isEmpty()) {
            // Found neighbour networks, join the network or merge.
            WorldNetwork network = neighbourNetworks.get(0);
            network.registerNode(new WorldNetworkNode(pos, network));

            if (neighbourNetworks.size() > 1) {
                // Merge is needed.
                List<WorldNetwork> unmergedNetworks = new ArrayList<>();
                unmergedNetworks.addAll(neighbourNetworks);

                network = unmergedNetworks.remove(0);
                while (!unmergedNetworks.isEmpty()) {
                    network = network.merge(unmergedNetworks.remove(0));
                }
            }
        } else {
            // No neighbours, make a new network.
            WorldNetwork network = new WorldNetwork(worldIn);
            network.registerNode(new WorldNetworkNode(pos, network));
            if (worldIn.getTileEntity(pos) != null) {
                ((TileItemTube) worldIn.getTileEntity(pos)).network = network;
            }
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
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
    private List<WorldNetwork> getNeighbourNetworks(World world, BlockPos pos) {
        List<WorldNetwork> neighbourNetworks = new ArrayList<>();
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());

            if (world.getBlockState(neighbourPos).getBlock() instanceof BlockItemTube) {
                neighbourNetworks.add(((TileItemTube) world.getTileEntity(pos)).network);
            }
        }

        return neighbourNetworks;
    }

}
