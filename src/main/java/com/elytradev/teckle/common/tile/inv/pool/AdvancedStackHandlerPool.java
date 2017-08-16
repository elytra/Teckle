package com.elytradev.teckle.common.tile.inv.pool;

import com.elytradev.teckle.common.worldnetwork.common.node.PositionData;
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.*;

public class AdvancedStackHandlerPool extends WorldSavedData {

    private static final String DATA_NAME = "tecklestackpool";
    private static final HashMap<Integer, AdvancedStackHandlerPool> DIMENSION_POOLS = Maps.newHashMap();

    private Map<UUID, AdvancedStackHandlerEntry> registeredHandlers = Maps.newHashMap();

    public AdvancedStackHandlerPool(String name) {
        super(name);
    }

    public AdvancedStackHandlerPool() {
        super(DATA_NAME);
    }

    @SubscribeEvent
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
            getSavedPool(DimensionManager.getWorld(dim));
        }
        return DIMENSION_POOLS.get(dim);
    }

    private static AdvancedStackHandlerPool getSavedPool(World world) {
        MapStorage storage = world.getPerWorldStorage();
        AdvancedStackHandlerPool instance = (AdvancedStackHandlerPool) storage.getOrLoadData(AdvancedStackHandlerPool.class, DATA_NAME);

        if (instance == null) {
            instance = new AdvancedStackHandlerPool();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
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
        NBTTagList entries = tag.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entryCompound = entries.getCompoundTagAt(i);
            AdvancedStackHandlerEntry advancedStackHandlerEntry = AdvancedStackHandlerEntry.create(entryCompound);
            registeredHandlers.put(advancedStackHandlerEntry.getId(), advancedStackHandlerEntry);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList entries = new NBTTagList();
        for (AdvancedStackHandlerEntry entry : registeredHandlers.values()) {
            PositionData positionData = PositionData.getPositionData(entry.getDimension(), entry.getPos());
            if (positionData.allNodeContainers().isEmpty())
                continue;

            entries.appendTag(entry.serialize());
        }
        tag.setTag("entries", entries);
        return tag;
    }
}
