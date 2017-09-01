/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Stores and serializes worldnetworks and their data.
 */
public class WorldNetworkDatabase extends WorldSavedData {

    private static final String NAME = "tecklenetworks";
    private static HashMap<Integer, WorldNetworkDatabase> DIMENSION_DATABASES = Maps.newHashMap();

    private HashMap<UUID, IWorldNetwork> networks = Maps.newHashMap();
    private HashMap<Pair<BlockPos, EnumFacing>, UUID> remappedNodes = Maps.newHashMap();

    private World world;
    private int cooldownTime = TeckleMod.CONFIG.databaseCleaningCooldown;

    public WorldNetworkDatabase(World world) {
        super(NAME);
        this.world = world;
    }

    public WorldNetworkDatabase() {
        super(NAME);
    }

    public WorldNetworkDatabase(String name) {
        super(name);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().isRemote)
            return;

        if (!DIMENSION_DATABASES.containsKey(e.getWorld().provider.getDimension())) {
            DIMENSION_DATABASES.put(e.getWorld().provider.getDimension(), new WorldNetworkDatabase(e.getWorld()));
            getSavedDatabase(e.getWorld());
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e) {
        if (DIMENSION_DATABASES.containsKey(e.getWorld().provider.getDimension())) {
            DIMENSION_DATABASES.remove(e.getWorld().provider.getDimension());
        }
    }

    @SubscribeEvent
    public static void onTickEvent(TickEvent.WorldTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.START) || e.side.isClient())
            return;

        getNetworkDB(e.world).onTick(e);
    }

    /**
     * Register a worldnetwork with the appropriate database.
     *
     * @param network the network to register.
     */
    public static void registerWorldNetwork(IWorldNetwork network) {
        if (!DIMENSION_DATABASES.containsKey(network.getWorld().provider.getDimension())) {
            DIMENSION_DATABASES.put(network.getWorld().provider.getDimension(), new WorldNetworkDatabase(network.getWorld()));
            getSavedDatabase(network.getWorld());
        }
        DIMENSION_DATABASES.get(network.getWorld().provider.getDimension()).networks.put(network.getNetworkID(), network);

        TeckleMod.LOG.debug("Registered new network, total is now {}", DIMENSION_DATABASES.get(network.getWorld().provider.getDimension()).networks.size());
    }

    /**
     * Get the worldnetworkdatabase for the given dimension id.
     *
     * @param world the world to get for.
     * @return the database retrieved.
     */
    public static WorldNetworkDatabase getNetworkDB(@Nonnull World world) {
        return getNetworkDB(world.provider.getDimension());
    }

    /**
     * Get the worldnetworkdatabase for the given dimension id.
     *
     * @param dim the dimension id of the database to retrieve.
     * @return the database retrieved.
     */
    public static WorldNetworkDatabase getNetworkDB(Integer dim) {
        if (!DIMENSION_DATABASES.containsKey(dim)) {
            DIMENSION_DATABASES.put(dim, new WorldNetworkDatabase(DimensionManager.getWorld(dim)));
            getSavedDatabase(DimensionManager.getWorld(dim));
        }
        return DIMENSION_DATABASES.get(dim);
    }

    /**
     * Generate or load a worldnetworkdatabase for the given world.
     *
     * @param world the world to get for.
     * @return the database retrieved.
     */
    private static WorldNetworkDatabase getSavedDatabase(@Nonnull World world) {
        WorldNetworkDatabase data = (WorldNetworkDatabase) world.getPerWorldStorage().getOrLoadData(WorldNetworkDatabase.class, "tecklenetworks");
        if (data == null) {
            data = WorldNetworkDatabase.getNetworkDB(world);
            world.getPerWorldStorage().setData("tecklenetworks", data);
            data.markDirty();
        }

        return data;
    }

    private NBTTagCompound saveDatabase(NBTTagCompound databaseCompound) {
        cleanAndUpdate(getWorld(), true, false);
        databaseCompound.setInteger("world", world.provider.getDimension());
        databaseCompound.setInteger("nCount", networks.size());
        List<IWorldNetwork> iWorldNetworks = networks.values().stream().collect(Collectors.toList());
        for (int i = 0; i < iWorldNetworks.size(); i++) {
            databaseCompound.setTag("n" + i, iWorldNetworks.get(i).serializeNBT());
        }

        databaseCompound.setInteger("rnCount", remappedNodes.size());
        List<Map.Entry<Pair<BlockPos, EnumFacing>, UUID>> remappedNodes = this.remappedNodes.entrySet().stream().collect(Collectors.toList());
        for (int i = 0; i < remappedNodes.size(); i++) {
            EnumFacing nodeFace = remappedNodes.get(i).getKey().getRight();
            databaseCompound.setLong("rNBP" + i, remappedNodes.get(i).getKey().getLeft().toLong());
            databaseCompound.setInteger("rNF" + i, nodeFace != null ? nodeFace.getIndex() : -1);
            databaseCompound.setUniqueId("rNN" + i, remappedNodes.get(i).getValue());
        }

        TeckleMod.LOG.debug("Serialized networks in {}, total is {}", world.provider.getDimension(), networks.size());
        return databaseCompound;
    }


    private void loadDatabase(NBTTagCompound compound) {
        if (world == null) {
            world = DimensionManager.getWorld(compound.getInteger("world"));
        }

        WorldNetworkDatabase.DIMENSION_DATABASES.put(world.provider.getDimension(), this);

        for (int i = 0; i < compound.getInteger("nCount"); i++) {
            WorldNetwork network = new WorldNetwork(world, null, true);
            network.deserializeNBT(compound.getCompoundTag("n" + i));
        }
        TeckleMod.LOG.debug("Deserialized networks in {}, total is {}", world.provider.getDimension(), networks.size());

        if (!compound.hasKey("rnCount"))
            return;

        for (int i = 0; i < compound.getInteger("rnCount"); i++) {
            BlockPos nodePos = BlockPos.fromLong(compound.getLong("rNBP" + i));
            int nodeFaceIndex = compound.getInteger("rNF" + i);
            UUID newNetworkID = compound.getUniqueId("rNN" + i);
            EnumFacing nodeFace = nodeFaceIndex < 0 ? null : EnumFacing.values()[nodeFaceIndex];
            remappedNodes.put(new ImmutablePair<>(nodePos, nodeFace), newNetworkID);
        }
    }

    /**
     * Get a worldnetwork with the given id.
     *
     * @return the associated worldnetwork, or a new network if one was not already present.
     */
    public IWorldNetwork get(UUID id) {
        if (!networks.containsKey(id))
            networks.put(id, new WorldNetwork(world, id));

        return networks.get(id);
    }

    /**
     * Check if a network with the given id is present in this database.
     *
     * @param id the network id.
     * @return true if present, false otherwise.
     */
    public boolean isNetworkPresent(UUID id) {
        return networks.containsKey(id);
    }

    private void onTick(TickEvent.WorldTickEvent e) {
        if (networks.isEmpty() || !Objects.equals(world, e.world))
            return;
        boolean doSearch = this.cooldownTime <= 0;
        cleanAndUpdate(e.world, doSearch, true);

        if (this.cooldownTime <= 0) {
            this.cooldownTime = TeckleMod.CONFIG.databaseCleaningCooldown;
        } else {
            this.cooldownTime--;
        }
    }

    private void cleanAndUpdate(World world, boolean doSearch, boolean update) {
        Predicate<IWorldNetwork> isStray = n -> n.getNodePositions().isEmpty()
                || n.nodeStream().noneMatch(NodeContainer::hasNetworkTile);
        if (doSearch) {
            int sizePre = networks.size();
            networks.values().stream().filter(isStray).forEach(n -> TeckleMod.LOG.debug("Removing {}", n));
            networks.values().removeIf(isStray);
            if (sizePre != networks.size())
                TeckleMod.LOG.debug("Cleaned networks, initial size: {}, post size: {}, diff: {}", sizePre,
                        networks.size(), sizePre - networks.size());
        }
        if (update)
            Lists.newArrayList(networks.values()).stream().filter(n -> Objects.equals(n.getWorld(), world)).forEach(ITickable::update);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public World getWorld() {
        return world;
    }

    public HashMap<Pair<BlockPos, EnumFacing>, UUID> getRemappedNodes() {
        return remappedNodes;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        loadDatabase(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return saveDatabase(compound);
    }

}
