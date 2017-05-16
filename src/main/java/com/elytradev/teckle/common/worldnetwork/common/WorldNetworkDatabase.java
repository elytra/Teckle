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
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stores and serializes worldnetworks and their data.
 */
public class WorldNetworkDatabase extends WorldSavedData {

    private static final String NAME = "tecklenetworks";
    private static HashMap<Integer, WorldNetworkDatabase> DIMENSION_DATABASES = Maps.newHashMap();

    private HashMap<UUID, IWorldNetwork> networks = Maps.newHashMap();
    private World world;

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

    @SubscribeEvent
    private static void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().isRemote)
            return;

        if (!DIMENSION_DATABASES.containsKey(e.getWorld().provider.getDimension())) {
            DIMENSION_DATABASES.put(e.getWorld().provider.getDimension(), new WorldNetworkDatabase(e.getWorld()));
            getSavedDatabase(e.getWorld());
        }
    }

    @SubscribeEvent
    private static void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote)
            return;

        if (DIMENSION_DATABASES.containsKey(e.getWorld().provider.getDimension())) {
            DIMENSION_DATABASES.remove(e.getWorld().provider.getDimension());
        }
    }

    @SubscribeEvent
    private static void onTickEvent(TickEvent.WorldTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.START) || e.side.isClient())
            return;

        DIMENSION_DATABASES.get(e.world.provider.getDimension()).onTick(e);
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
        databaseCompound.setInteger("world", world.provider.getDimension());
        databaseCompound.setInteger("nCount", networks.size());
        List<IWorldNetwork> iWorldNetworks = networks.values().stream().collect(Collectors.toList());
        for (int i = 0; i < iWorldNetworks.size(); i++) {
            databaseCompound.setTag("n" + i, iWorldNetworks.get(i).serializeNBT());
        }

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

    private void onTick(TickEvent.WorldTickEvent e) {
        if (networks.isEmpty() || !world.equals(e.world))
            return;

        List<IWorldNetwork> emptyNetworks = new ArrayList<>();
        for (IWorldNetwork network : networks.values()) {
            if (network.getNodes().isEmpty()) {
                if (!emptyNetworks.contains(network))
                    emptyNetworks.add(network);

                TeckleMod.LOG.debug("Found empty network " + network);
                continue;
            }
            if (e.world.equals(network.getWorld()))
                network.update();
        }

        for (IWorldNetwork emptyNetwork : emptyNetworks) {
            TeckleMod.LOG.debug("Removing empty network " + emptyNetwork);
            networks.remove(emptyNetwork.getNetworkID());
        }

        if (!this.isDirty())
            this.markDirty();
    }

    public World getWorld() {
        return world;
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
