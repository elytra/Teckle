package com.elytradev.teckle.common.tile.inv.pool;

import com.elytradev.teckle.common.TeckleMod;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.*;

public class AdvancedStackHandlerPool extends WorldSavedData {

    private static final String DATA_NAME = "tecklestackpool";
    private static final HashMap<Integer, AdvancedStackHandlerPool> DIMENSION_POOLS = Maps.newHashMap();
    private static final HashMap<Integer, AdvancedStackHandlerPool> FALLBACK_POOLS = Maps.newHashMap();

    private Map<UUID, AdvancedStackHandlerEntry> registeredHandlers = Maps.newHashMap();

    public AdvancedStackHandlerPool(String name) {
        super(name);
    }

    public AdvancedStackHandlerPool() {
        super(DATA_NAME);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().isRemote)
            return;

        if (!DIMENSION_POOLS.containsKey(e.getWorld().provider.getDimension())) {
            DIMENSION_POOLS.put(e.getWorld().provider.getDimension(), new AdvancedStackHandlerPool());
            getSavedPool(e.getWorld());
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote)
            return;

        if (DIMENSION_POOLS.containsKey(e.getWorld().provider.getDimension())) {
            DIMENSION_POOLS.remove(e.getWorld().provider.getDimension());
        }
    }

    public static AdvancedStackHandlerPool getPool(@Nonnull World world) {
        return getPool(world.provider.getDimension());
    }

    public static AdvancedStackHandlerPool getPool(Integer dim) {
        if (!DIMENSION_POOLS.containsKey(dim)) {
            DIMENSION_POOLS.put(dim, new AdvancedStackHandlerPool());
            if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                getSavedPool(DimensionManager.getWorld(dim));
            }
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            FALLBACK_POOLS.put(dim, new AdvancedStackHandlerPool());
            return FALLBACK_POOLS.get(dim);
        }
        return DIMENSION_POOLS.get(dim);
    }

    private static AdvancedStackHandlerPool getSavedPool(World world) {
        AdvancedStackHandlerPool data = (AdvancedStackHandlerPool) world.getPerWorldStorage().getOrLoadData(AdvancedStackHandlerPool.class, DATA_NAME);
        if (data == null) {
            data = AdvancedStackHandlerPool.getPool(world);
            world.getPerWorldStorage().setData(DATA_NAME, data);
            data.markDirty();
        }

        return data;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public boolean containsKey(Object key) {
        return registeredHandlers.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return registeredHandlers.containsValue(value);
    }

    public AdvancedStackHandlerEntry get(Object key) {
        return registeredHandlers.get(key);
    }

    public AdvancedStackHandlerEntry put(UUID key, AdvancedStackHandlerEntry value) {
        TeckleMod.LOG.debug("Added handler to pool {}", value);
        return registeredHandlers.put(key, value);
    }

    public AdvancedStackHandlerEntry remove(Object key) {
        return registeredHandlers.remove(key);
    }

    public void putAll(Map<? extends UUID, ? extends AdvancedStackHandlerEntry> m) {
        registeredHandlers.putAll(m);
    }

    public Set<UUID> keySet() {
        return registeredHandlers.keySet();
    }

    public Collection<AdvancedStackHandlerEntry> values() {
        return registeredHandlers.values();
    }

    public Set<Map.Entry<UUID, AdvancedStackHandlerEntry>> entrySet() {
        return registeredHandlers.entrySet();
    }

    public AdvancedStackHandlerEntry putIfAbsent(UUID key, AdvancedStackHandlerEntry value) {
        return registeredHandlers.putIfAbsent(key, value);
    }

    public boolean remove(Object key, Object value) {
        return registeredHandlers.remove(key, value);
    }

    public boolean replace(UUID key, AdvancedStackHandlerEntry oldValue, AdvancedStackHandlerEntry newValue) {
        return registeredHandlers.replace(key, oldValue, newValue);
    }

    public AdvancedStackHandlerEntry replace(UUID key, AdvancedStackHandlerEntry value) {
        return registeredHandlers.replace(key, value);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        for (int i = 0; i < tag.getInteger("tags"); i++) {
            NBTTagCompound entryCompound = tag.getCompoundTag("e" + i);
            AdvancedStackHandlerEntry advancedStackHandlerEntry = AdvancedStackHandlerEntry.create(entryCompound);
            registeredHandlers.put(advancedStackHandlerEntry.getId(), advancedStackHandlerEntry);
        }
        TeckleMod.LOG.debug("Deserialized {} stack handlers.", tag.getInteger("tags"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if (FMLCommonHandler.instance().getEffectiveSide().isClient())
            return tag;

        int skipped = 0;
        List<NBTTagCompound> entries = Lists.newArrayList();
        for (AdvancedStackHandlerEntry entry : registeredHandlers.values()) {
            World world = DimensionManager.getWorld(entry.getDimension());
            // sanity check, makes sure we don't save stuff if there's nothing at the position this handler is at.
            // not perfect but it gets the job done.
            if (entry.getPos() != null && world.isBlockLoaded(entry.getPos()) && world.getTileEntity(entry.getPos()) == null) {
                skipped++;
                continue;
            }
            entries.add(entry.serialize());
        }
        tag.setInteger("tags", entries.size());
        for (int i = 0; i < entries.size(); i++) {
            tag.setTag("e" + i, entries.get(i));
        }
        TeckleMod.LOG.debug("Serialized {} stack handlers, skipped {}", entries.size(), skipped);
        return tag;
    }
}
