package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.block.BlockBeamQuarry;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.ItemStream;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TileBeamQuarry extends TileEntity implements ITickable {

    public BlockPos min = pos, max = pos, cursor = pos;
    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(16);
    public AdvancedItemStackHandler junkSupply = new AdvancedItemStackHandler(12);
    public AdvancedItemStackHandler junkTypes = new AdvancedItemStackHandler(6);

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.min = BlockPos.fromLong(tag.getLong("min"));
        this.max = BlockPos.fromLong(tag.getLong("max"));
        this.cursor = BlockPos.fromLong(tag.getLong("cursor"));

        this.buffer.deserializeNBT(tag.getCompoundTag("buffer"));
        this.junkSupply.deserializeNBT(tag.getCompoundTag("junkSupply"));
        this.junkTypes.deserializeNBT(tag.getCompoundTag("junkTypes"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setLong("min", this.min.toLong());
        tag.setLong("max", this.max.toLong());
        tag.setLong("cursor", this.cursor.toLong());

        tag.setTag("buffer", this.buffer.serializeNBT());
        tag.setTag("junkSupply", this.junkSupply.serializeNBT());
        tag.setTag("junkTypes", this.junkTypes.serializeNBT());

        return super.writeToNBT(tag);
    }

    @Override
    public void update() {
        //TODO: Power consumption and cooldown.
        if (isActive()) {
            // TODO: Remove, just for testing.
            if (world.getTotalWorldTime() % 5 != 0)
                return;

            if (cursor == pos) {
                cursor = min;
            }

            // Check the current cursor position for validity.
            IBlockState cursorState = world.getBlockState(cursor);
            if (!isStateValid(cursorState)) {
                int xRange = Math.abs(min.getX() - max.getX()) + 1;
                int zRange = Math.abs(min.getZ() - max.getZ()) + 1;
                for (int y = cursor.getY(); y > 0 && !isStateValid(cursorState); y++) {
                    for (int x = 0; x < xRange && !isStateValid(cursorState); x++) {
                        for (int z = 0; z < zRange && !isStateValid(cursorState); z++) {
                            cursor = new BlockPos(min.getX() + x, y, min.getZ() + z);
                            if (isStateValid(world.getBlockState(cursor))) {
                                cursorState = world.getBlockState(cursor);
                                break;
                            }
                        }
                    }
                }
            }
            // Mine the current cursor position.
            if (isStateValid(cursorState)) {
                AxisAlignedBB dropBox = new AxisAlignedBB(cursor.getX(), cursor.getY(), cursor.getZ(),
                        cursor.getX(), cursor.getY(), cursor.getZ());
                dropBox = dropBox.expand(1.5, 1.5, 1.5);
                world.destroyBlock(pos, true);
                List<EntityItem> entityItems = world.getEntitiesWithinAABB(EntityItem.class, dropBox);
                List<ItemStack> items = entityItems.stream().map(EntityItem::getItem).collect(Collectors.toList());
                entityItems.forEach(Entity::setDead);
                for (ItemStack item : items) {
                    Stream<ItemStack> junkStream = ItemStream.createItemStream(junkTypes);
                    ItemStack finalItem = item;
                    if (junkStream.anyMatch(j -> ItemHandlerHelper.canItemStacksStack(finalItem, j))) {
                        item = junkSupply.insertItem(item, false);
                    }
                    item = buffer.insertItem(item, false);
                    world.spawnEntity(new EntityItem(world, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, item));
                }
            }
        } else {
            // Show the border for the quarry area.
            for (int x = min.getX(); x < max.getX(); x++) {
                for (int z = min.getZ(); z < max.getZ(); z++) {
                    world.spawnParticle(EnumParticleTypes.REDSTONE, true, x, pos.getY(), z, 0, 0, 0);
                }
            }
        }
    }

    /**
     * Check if the state given is valid for mining or if the cursor needs to move.
     *
     * @param state the state to check.
     * @return true if the state can be mined, false otherwise.
     */
    public boolean isStateValid(IBlockState state) {
        return state.getBlock() != Blocks.AIR && state.getBlock() != Blocks.BEDROCK;
    }

    /**
     * Checks if the quarry is currently active with power.
     *
     * @return true if the quarry can run, false otherwise.
     */
    public boolean isActive() {
        return world.getBlockState(pos).getValue(BlockBeamQuarry.ACTIVE);
    }

    /**
     * Set the bounds to mine within, also updates the cursor.
     *
     * @param min the minimum position mining will be restricted in.
     * @param max the maximum position mining will be restricted in.
     */
    public void setBounds(BlockPos min, BlockPos max) {
        this.min = min;
        this.max = max;
        this.cursor = min;
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;

    }
}
