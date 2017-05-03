package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.common.TeckleMod;
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
 * Created by darkevilmac on 4/3/2017.
 */
public class WorldNetworkDatabase extends WorldSavedData {

    protected static final String NAME = "tecklenetworks";
    public static HashMap<Integer, WorldNetworkDatabase> NETWORKDBS = new HashMap<>();
    public World world;
    protected HashMap<UUID, WorldNetwork> networks = new HashMap<>();

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
    public static void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().isRemote)
            return;

        if (!NETWORKDBS.containsKey(e.getWorld().provider.getDimension())) {
            NETWORKDBS.put(e.getWorld().provider.getDimension(), new WorldNetworkDatabase(e.getWorld()));
            getDatabase(e.getWorld());
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload e) {
        if (e.getWorld().isRemote)
            return;

        if (NETWORKDBS.containsKey(e.getWorld().provider.getDimension())) {
            NETWORKDBS.remove(e.getWorld().provider.getDimension());
        }
    }

    @SubscribeEvent
    public static void onTickEvent(TickEvent.WorldTickEvent e) {
        if (e.phase.equals(TickEvent.Phase.START) || e.side.isClient())
            return;

        NETWORKDBS.get(e.world.provider.getDimension()).onTick(e);
    }

    public static void registerWorldNetwork(WorldNetwork network) {
        if (!NETWORKDBS.containsKey(network.world.provider.getDimension())) {
            NETWORKDBS.put(network.world.provider.getDimension(), new WorldNetworkDatabase(network.world));
            getDatabase(network.world);
        }
        NETWORKDBS.get(network.world.provider.getDimension()).networks.put(network.id, network);
    }

    public static WorldNetworkDatabase getNetworkDB(@Nonnull World world) {
        return getNetworkDB(world.provider.getDimension());
    }

    public static WorldNetworkDatabase getNetworkDB(Integer dim) {
        if (!NETWORKDBS.containsKey(dim)) {
            NETWORKDBS.put(dim, new WorldNetworkDatabase(DimensionManager.getWorld(dim)));
            getDatabase(DimensionManager.getWorld(dim));
        }
        return NETWORKDBS.get(dim);
    }

    private static WorldNetworkDatabase getDatabase(World world) {
        WorldNetworkDatabase data = (WorldNetworkDatabase) world.getPerWorldStorage().getOrLoadData(WorldNetworkDatabase.class, "tecklenetworks");
        if (data == null) {
            data = WorldNetworkDatabase.getNetworkDB(world);
            world.getPerWorldStorage().setData("tecklenetworks", data);
            data.markDirty();
        }

        return data;
    }

    public NBTTagCompound saveDatabase(NBTTagCompound databaseCompound) {
        databaseCompound.setInteger("world", world.provider.getDimension());
        databaseCompound.setInteger("nCount", networks.size());
        List<WorldNetwork> worldNetworks = networks.values().stream().collect(Collectors.toList());
        for (int i = 0; i < worldNetworks.size(); i++) {
            databaseCompound.setTag("n" + i, worldNetworks.get(i).serializeNBT());
        }

        return databaseCompound;
    }

    public void loadDatabase(NBTTagCompound compound) {
        if (world == null) {
            world = DimensionManager.getWorld(compound.getInteger("world"));
        }

        WorldNetworkDatabase.NETWORKDBS.put(world.provider.getDimension(), this);

        for (int i = 0; i < compound.getInteger("nCount"); i++) {
            WorldNetwork network = new WorldNetwork(world, null, true);
            network.deserializeNBT(compound.getCompoundTag("n" + i));
        }
    }

    public WorldNetwork get(UUID id) {
        if (!networks.containsKey(id))
            networks.put(id, new WorldNetwork(world, id));

        return networks.get(id);
    }

    public void onTick(TickEvent.WorldTickEvent e) {
        if (networks.isEmpty() || !world.equals(e.world))
            return;

        List<WorldNetwork> emptyNetworks = new ArrayList<>();
        for (WorldNetwork network : networks.values()) {
            if (network.networkNodes.isEmpty()) {
                if (!emptyNetworks.contains(network))
                    emptyNetworks.add(network);

                TeckleMod.LOG.debug("Found empty network " + network);
                continue;
            }
            if (e.world.equals(network.world))
                network.update();
        }

        for (WorldNetwork emptyNetwork : emptyNetworks) {
            TeckleMod.LOG.debug("Removing empty network " + emptyNetwork);
            networks.remove(emptyNetwork.id);
        }

        if (!this.isDirty())
            this.markDirty();
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
