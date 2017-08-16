package com.elytradev.teckle.common.tile.inv.pool;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Entry in the advancedhandler pool, used for serialization and deserialization. Stores the handler, the dimension,
 * the id, and the position in the world (if applicable)
 */
public class AdvancedStackHandlerEntry {

    @Nullable
    private BlockPos pos;

    private UUID id;
    private int dimension;
    private AdvancedItemStackHandler handler;

    public AdvancedStackHandlerEntry(UUID id, int dimension, BlockPos pos, AdvancedItemStackHandler handler) {
        this.setPos(pos);
        this.setId(id);
        this.setDimension(dimension);
        this.setHandler(handler);
        AdvancedStackHandlerPool.getPool(dimension).put(id, this);
    }

    public static AdvancedStackHandlerEntry create(NBTTagCompound tag) {
        BlockPos pos = BlockPos.fromLong(tag.getLong("pos"));
        AdvancedItemStackHandler handler = new AdvancedItemStackHandler(tag.getInteger("handlerSize"));
        handler.deserializeNBT(tag.getCompoundTag("handler"));
        return new AdvancedStackHandlerEntry(tag.getUniqueId("id"), tag.getInteger("dimension"), pos, handler);
    }

    public NBTTagCompound serialize() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setUniqueId("id", getId());
        tag.setInteger("handlerSize", getHandler().getSlots());
        tag.setTag("handler", getHandler().serializeNBT());
        tag.setLong("pos", getPos().toLong());
        tag.setInteger("dimension", getDimension());

        return tag;
    }

    @Override
    public String toString() {
        return "AdvancedStackHandlerEntry{" +
                "pos=" + getPos() +
                ", id=" + getId() +
                ", dimension=" + getDimension() +
                ", handler=" + getHandler() +
                '}';
    }

    @Nullable
    public BlockPos getPos() {
        return pos;
    }

    public void setPos(@Nullable BlockPos pos) {
        this.pos = pos;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public AdvancedItemStackHandler getHandler() {
        return handler;
    }

    public void setHandler(AdvancedItemStackHandler handler) {
        this.handler = handler;
    }
}
