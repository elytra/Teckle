package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.tile.base.TileNetworkEntrypoint;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class TileFilter extends TileNetworkEntrypoint implements ITickable {

    public EnumDyeColor colour = null;
    public NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
    public ItemStackHandler buffer = new ItemStackHandler(4);

    public IInventory inventory = new IInventory() {
        @Override
        public int getSizeInventory() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            boolean empty = true;

            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    empty = false;
                    break;
                }
            }

            return empty;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return stacks.get(index);
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            return ItemStackHelper.getAndSplit(stacks, index, count);
        }

        @Override
        public ItemStack removeStackFromSlot(int index) {
            return ItemStackHelper.getAndRemove(stacks, index);
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            stacks.set(index, stack);
        }

        @Override
        public int getInventoryStackLimit() {
            return 8;
        }

        @Override
        public void markDirty() {
            TileFilter.this.markDirty();
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer player) {
            return TileFilter.this.isUsableByPlayer(player);
        }

        @Override
        public void openInventory(EntityPlayer player) {
        }

        @Override
        public void closeInventory(EntityPlayer player) {
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return true;
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int value) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
            stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public ITextComponent getDisplayName() {
            return null;
        }
    };
    private int cooldown = 0;

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tagCompound = super.getUpdateTag();
        if (colour != null) {
            tagCompound.setInteger("colour", colour.getMetadata());
        } else {
            tagCompound.removeTag("colour");
        }

        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);

        this.colour = !pkt.getNbtCompound().hasKey("colour") ? null : EnumDyeColor.byMetadata(pkt.getNbtCompound().getInteger("colour"));
    }

    @Override
    public WorldNetworkNode getNode(WorldNetwork network) {
        return new WorldNetworkEntryPoint(network, pos, getFacing());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return true;
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
    public boolean pushToNetwork() {
        boolean result = false;

        if (cooldown > 0)
            return result;

        if (getNode() != null && getNode().network != null && !world.isRemote && world.getTileEntity(pos.offset(getFacing())) instanceof TileNetworkMember) {
            WorldNetworkEntryPoint thisNode = (WorldNetworkEntryPoint) getNode().network.getNodeFromPosition(pos);
            EnumFacing facing = getFacing();

            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null) {
                TileEntity pullFrom = world.getTileEntity(pos.offset(facing.getOpposite()));
                if (pullFrom.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                    IItemHandler itemHandler = pullFrom.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
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
                        extractionData = buffer.extractItem(bufferSlot, 8, false);
                    } else {
                        if (stacks.stream().anyMatch(itemStack -> !itemStack.isEmpty())) {
                            for (ItemStack stack : stacks) {
                                if (!extractionData.isEmpty())
                                    break;
                                if (stack.isEmpty())
                                    continue;
                                for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                                    ItemStack extractTest = itemHandler.extractItem(slot, stack.getCount(), true);
                                    if (Objects.equals(extractTest.getItem(), stack.getItem()) && extractTest.getMetadata() == stack.getMetadata()) {
                                        extractionData = itemHandler.extractItem(slot, stack.getCount(), false);
                                    }
                                }
                            }
                        } else {
                            for (int slot = 0; slot < itemHandler.getSlots() && extractionData.isEmpty(); slot++) {
                                extractionData = itemHandler.extractItem(slot, 8, false);
                            }
                        }
                    }

                    if (!extractionData.isEmpty()) {
                        NBTTagCompound tagCompound = new NBTTagCompound();
                        tagCompound.setTag("stack", extractionData.writeToNBT(new NBTTagCompound()));
                        if (this.colour != null)
                            tagCompound.setInteger("colour", this.colour.getMetadata());
                        WorldNetworkTraveller traveller = thisNode.addTraveller(tagCompound);
                        if (!Objects.equals(traveller, WorldNetworkTraveller.NONE)) {
                            traveller.dropActions.put(DropActions.ITEMSTACK.getFirst(), DropActions.ITEMSTACK.getSecond());
                            result = true;
                        } else {
                            ItemStack remaining = extractionData;
                            for (int i = 0; i < buffer.getSlots(); i++) {
                                remaining = buffer.insertItem(i, remaining, false);
                            }

                            if (!remaining.isEmpty()) {
                                WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                                remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                                DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
                            }
                        }
                    }
                }
            }
        }

        if (result) {
            this.world.playEvent(1000, pos, 0);
        }

        cooldown = 10;
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

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.colour = !compound.hasKey("colour") ? null : EnumDyeColor.byMetadata(compound.getInteger("colour"));
        ItemStackHelper.loadAllItems(compound, stacks);
        buffer.deserializeNBT(compound.getCompoundTag("buffer"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (colour != null) {
            compound.setInteger("colour", colour.getMetadata());
        } else {
            compound.removeTag("colour");
        }
        ItemStackHelper.saveAllItems(compound, this.stacks);
        compound.setTag("buffer", buffer.serializeNBT());

        return super.writeToNBT(compound);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
