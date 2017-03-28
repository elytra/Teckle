package com.elytradev.teckle.block;

import com.elytradev.teckle.TeckleMod;
import com.elytradev.teckle.tile.TileItemTube;
import com.elytradev.teckle.worldnetwork.WorldNetwork;
import com.elytradev.teckle.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.worldnetwork.WorldNetworkTraveller;
import com.elytradev.teckle.worldnetwork.item.ItemNetworkEndpoint;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by darkevilmac on 3/27/2017.
 */
public class BlockItemTube extends BlockContainer {
    public BlockItemTube(Material materialIn) {
        super(materialIn);

        this.setRegistryName(new ResourceLocation(TeckleMod.MOD_ID, "itemTube"));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (worldIn.isRemote)
            return;

        List<WorldNetwork> neighbourNetworks = getNeighbourNetworks(worldIn, pos);
        if (!neighbourNetworks.isEmpty()) {
            // Found neighbour networks, join the network or merge.
            WorldNetwork network = neighbourNetworks.remove(0);

            TileItemTube tube = (TileItemTube) worldIn.getTileEntity(pos);
            network.registerNode(new WorldNetworkNode(network, pos));
            tube.network = network;

            while (!neighbourNetworks.isEmpty()) {
                network = network.merge(neighbourNetworks.remove(0));
            }
        } else {
            // No neighbours, make a new network.
            System.out.println("Generating new network.");
            WorldNetwork network = new WorldNetwork(worldIn);
            network.registerNode(new WorldNetworkNode(network, pos));
            if (worldIn.getTileEntity(pos) != null) {
                ((TileItemTube) worldIn.getTileEntity(pos)).network = network;
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

        if (tube.network.isNodePresent(neighbor)) {
            //Node is already present, run validation on network.
            tube.network.validateNetwork();
        } else {
            // Node not already present, check if we can add to network.
            if (world.getTileEntity(neighbor) != null) {
                TileEntity neighbourTile = world.getTileEntity(neighbor);
                if (neighbourTile instanceof ICapabilityProvider) {
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
                neighbourNetworks.add(neighbourTube.network);
            }
        }

        return neighbourNetworks;
    }


}
