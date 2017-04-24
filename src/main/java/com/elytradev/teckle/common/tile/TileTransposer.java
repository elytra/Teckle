package com.elytradev.teckle.common.tile;

import com.elytradev.probe.api.IProbeData;
import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.probe.api.impl.ProbeData;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockTransposer;
import com.elytradev.teckle.common.tile.base.TileNetworkEntrypoint;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.worldnetwork.*;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by darkevilmac on 4/21/2017.
 */
public class TileTransposer extends TileNetworkEntrypoint implements ITickable {

    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(9);
    private int cooldown = 0;

    @Override
    public WorldNetworkNode getNode(WorldNetwork network) {
        return new WorldNetworkEntryPoint(network, pos, getFacing());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return true;
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side.equals(getFacing()) || side.getOpposite().equals(getFacing());
    }

    @Override
    public WorldNetworkNode getNode() {
        return super.getNode();
    }

    @Override
    public void setNode(WorldNetworkNode node) {
        super.setNode(node);
    }

    /**
     * Attempt to push to our network, by pulling from our input position.
     *
     * @return true if a push occurred, false otherwise.
     */
    public boolean tryPush() {
        boolean result = false;

        if (cooldown > 0)
            return result;

        TileEntity potentialInsertionTile = world.getTileEntity(pos.offset(getFacing()));
        boolean destinationIsAir = world.isAirBlock(pos.offset(getFacing()));
        boolean hasInsertionDestination = potentialInsertionTile != null && ((getNode() != null && getNode().network != null)
                || (potentialInsertionTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing().getOpposite())));

        if (!world.isRemote && (hasInsertionDestination || destinationIsAir)) {
            WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) getNode().network.getNodeFromPosition(pos);
            EnumFacing facing = getFacing();

            ItemStack extractionData = getExtractionData(facing);

            if (!extractionData.isEmpty()) {
                if (hasInsertionDestination) {
                    result = attemptInsertion(potentialInsertionTile, thisNode, extractionData);
                } else {
                    result = ejectExtractionData(facing, extractionData);
                }
            }
        }

        if (result) {
            this.world.playEvent(1000, pos, 0);
        }

        cooldown = 10;
        return result;
    }

    private boolean ejectExtractionData(EnumFacing facing, ItemStack extractionData) {
        EnumFacing enumfacing = getFacing();
        double x = pos.getX() + 0.7D * (double) enumfacing.getFrontOffsetX();
        double y = pos.getY() + 0.7D * (double) enumfacing.getFrontOffsetY();
        double z = pos.getZ() + 0.7D * (double) enumfacing.getFrontOffsetZ();

        if (facing.getAxis() == EnumFacing.Axis.Y) {
            y = y - 0.125D;
        } else {
            y = y - 0.15625D;
        }

        EntityItem entityitem = new EntityItem(world, x, y, z, extractionData);
        double d3 = world.rand.nextDouble() * 0.1D + 0.2D;
        entityitem.motionX = (double) facing.getFrontOffsetX() * d3;
        entityitem.motionY = 0.20000000298023224D;
        entityitem.motionZ = (double) facing.getFrontOffsetZ() * d3;
        entityitem.motionX += world.rand.nextGaussian() * 0.007499999832361937D * (double) 2.5D;
        entityitem.motionY += world.rand.nextGaussian() * 0.007499999832361937D * (double) 2.5D;
        entityitem.motionZ += world.rand.nextGaussian() * 0.007499999832361937D * (double) 2.5D;
        world.spawnEntity(entityitem);
        return true;
    }

    private boolean attemptInsertion(TileEntity potentialInsertionTile, WorldNetworkEntryPoint thisNode, ItemStack extractionData) {
        boolean result = false;
        if (getNode() != null && getNode().network != null && potentialInsertionTile instanceof TileNetworkMember) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            tagCompound.setTag("stack", extractionData.writeToNBT(new NBTTagCompound()));
            WorldNetworkTraveller traveller = thisNode.addTraveller(tagCompound);
            if (!Objects.equals(traveller, WorldNetworkTraveller.NONE)) {
                traveller.dropActions.put(DropActions.ITEMSTACK.getFirst(), DropActions.ITEMSTACK.getSecond());
                result = true;
            } else {
                ItemStack remaining = extractionData;
                for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                    remaining = buffer.insertItem(i, remaining, false);
                }

                if (!remaining.isEmpty()) {
                    WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                    remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                    DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
                }
            }
        } else {
            IItemHandler insertHandler = world.getTileEntity(pos.offset(getFacing())).getCapability
                    (CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing().getOpposite());

            ItemStack remaining = extractionData;
            for (int i = 0; i < insertHandler.getSlots() && !remaining.isEmpty(); i++) {
                remaining = insertHandler.insertItem(i, remaining, false);
            }

            if (!remaining.isEmpty()) {
                for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                    remaining = buffer.insertItem(i, remaining, false);
                }

                if (!remaining.isEmpty()) {
                    WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                    remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                    DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
                }
            }
        }
        return result;
    }

    private ItemStack getExtractionData(EnumFacing facing) {
        ItemStack extractionData = ItemStack.EMPTY;

        // Check if the buffer is empty first...
        int bufferSlot = -1;
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (!buffer.getStackInSlot(i).isEmpty()) {
                bufferSlot = i;
                break;
            }
        }

        if (bufferSlot != -1) {
            extractionData = buffer.extractItem(bufferSlot, 1, false);
        } else {
            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null && world.getTileEntity(pos.offset(facing.getOpposite()))
                    .hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                IItemHandler itemHandler = world.getTileEntity(pos.offset(facing.getOpposite()))
                        .getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                    ItemStack extractTest = itemHandler.extractItem(slot, 1, true);
                    if (!extractTest.isEmpty())
                        extractionData = itemHandler.extractItem(slot, 1, false);
                }
            }
        }

        return extractionData;
    }

    @Override
    public EnumFacing getFacing() {
        if (world != null) {
            IBlockState thisState = world.getBlockState(pos);
            if (thisState.getBlock().equals(TeckleObjects.blockTransposer)) {
                return thisState.getValue(BlockTransposer.FACING);
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
            ItemStack remaining = stack.copy();
            for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
                remaining = buffer.insertItem(i, remaining, false);
            }

            if (!remaining.isEmpty()) {
                WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
            }
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

        boolean canFitItems = world.isAirBlock(pos.add(getFacing().getOpposite().getDirectionVec())) && canFitItemsInBuffer();
        if (canFitItems) {
            List<EntityItem> itemsToPickup = getItemsInBlockPos(pos.add(getFacing().getOpposite().getDirectionVec()));
            if (world.getBlockState(pos).getValue(BlockTransposer.TRIGGERED) && world.isAirBlock(pos.add(getFacing().getOpposite().getDirectionVec())
                    .add(getFacing().getOpposite().getDirectionVec())))
                itemsToPickup.addAll(getItemsInBlockPos(pos.add(getFacing().getOpposite().getDirectionVec())
                        .add(getFacing().getOpposite().getDirectionVec())));

            for (EntityItem entityItem : itemsToPickup) {
                ItemStack entityStack = entityItem.getEntityItem().copy();

                for (int i = 0; i < buffer.getSlots() && !entityStack.isEmpty(); i++) {
                    entityStack = buffer.insertItem(i, entityStack, false);
                }

                entityItem.setEntityItemStack(entityStack);
                if (entityStack.isEmpty()) {
                    world.removeEntity(entityItem);
                }

                canFitItems = canFitItemsInBuffer();
                if (!canFitItems)
                    break;
            }
        }
    }

    public boolean canFitItemsInBuffer() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            if (buffer.getStackInSlot(i).isEmpty() || buffer.getStackInSlot(i).getCount() < buffer.getSlotLimit(i)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        buffer.deserializeNBT(compound.getCompoundTag("buffer"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("buffer", buffer.serializeNBT());

        return super.writeToNBT(compound);
    }

    public List<EntityItem> getItemsInBlockPos(BlockPos pos) {
        return world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos.getX() - 0.5, pos.getY() - 0.5, pos.getZ() - 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new TileTransposer.ProbeCapability();
            return (T) probeCapability;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            if (node == null)
                return;

            if (TeckleMod.INDEV)
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network", node.network.id.toString().toUpperCase().replaceAll("-", ""))));

            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < buffer.getSlots(); i++) {
                stacks.add(buffer.getStackInSlot(i));
            }

            ProbeData probeData = new ProbeData(new TextComponentTranslation("tooltip.teckle.filter.buffer")).withInventory(ImmutableList.copyOf(stacks));
            data.add(probeData);
        }
    }
}
