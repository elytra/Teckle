package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.tile.base.TileNetworkEntrypoint;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class TileFilter extends TileNetworkEntrypoint implements ITickable {

    private int cooldown = 0;

    @Override
    public WorldNetworkNode getNode(WorldNetwork network) {
        return new WorldNetworkEntryPoint(network, pos, getFacing());
    }

    @Override
    public WorldNetworkNode getNode() {
        if (super.getNode() == null) {
            System.out.println("Getnode is null on filter.");
        }

        return super.getNode();
    }

    @Override
    public void setNode(WorldNetworkNode node) {
        System.out.println("SetNode called on filter.");

        super.setNode(node);
    }

    /**
     * Attempt to push to our network, by pulling from our input position.
     *
     * @return true if a push occurred, false otherwise.
     */
    public boolean pushToNetwork() {
        boolean result = false;

        if (getNode() != null && getNode().network != null && !world.isRemote && cooldown == 0) {
            WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) getNode().network.getNodeFromPosition(pos);
            EnumFacing facing = getFacing();

            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null) {
                TileEntity pullFrom = world.getTileEntity(pos.offset(facing.getOpposite()));
                if (pullFrom.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                    IItemHandler itemHandler = pullFrom.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                    ItemStack extractionData = ItemStack.EMPTY;
                    for (int slot = 0; slot < itemHandler.getSlots() && extractionData == ItemStack.EMPTY; slot++) {
                        extractionData = itemHandler.extractItem(slot, 8, false);
                    }

                    if (!extractionData.isEmpty()) {
                        NBTTagCompound tagCompound = new NBTTagCompound();
                        tagCompound.setTag("stack", extractionData.writeToNBT(new NBTTagCompound()));
                        thisNode.addTraveller(tagCompound);
                        result = true;
                    }
                }
            }
        }

        if (result) {
            this.world.playEvent(1000, pos, 0);
        }

        cooldown = 5;
        return result;
    }

    @Override
    public EnumFacing getFacing() {
        if (world != null) {
            IBlockState thisState = world.getBlockState(pos);
            if (thisState.getBlock().equals(TeckleObjects.blockFilter)) {
                return thisState.getValue(BlockFilter.FACING);
            }
        }

        return EnumFacing.DOWN;
    }

    @Override
    public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
        if (!traveller.data.hasKey("stack"))
            return; // wtf am I supposed to do with this???

        ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
        EnumFacing facing = getFacing();
        BlockPos sourcePos = pos.offset(facing);

        // Try and put it back where we found it.
        if (side.equals(getFacing())) {
            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null) {
                TileEntity pushTo = world.getTileEntity(pos.offset(facing.getOpposite()));
                if (pushTo.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                    IItemHandler itemHandler = pushTo.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                    for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
                        stack = itemHandler.insertItem(slot, stack, false);
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            // Spawn into the world I guess.
            world.spawnEntity(new EntityItem(world, sourcePos.getX(), sourcePos.getY(), sourcePos.getZ(), stack));
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        if (oldState.getBlock() == newSate.getBlock()) {
            return false;
        }

        return super.shouldRefresh(world, pos, oldState, newSate);
    }

    @Override
    public void update() {
        if (world.isRemote || getNode() == null || getNode().network == null)
            return;

        if (cooldown > 0) {
            cooldown--;
        }
    }
}
