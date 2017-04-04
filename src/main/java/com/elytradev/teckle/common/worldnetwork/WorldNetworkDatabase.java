package com.elytradev.teckle.common.worldnetwork;

import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by darkevilmac on 4/3/2017.
 */
public class WorldNetworkDatabase {

    public static HashMap<Integer, WorldNetworkDatabase> NETWORKDBS = new HashMap<>();

    public World world;
    public HashMap<UUID, WorldNetwork> networks = new HashMap<>();

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

    public WorldNetworkDatabase(World world) {
        this.world = world;
    }

    public static void registerWorldNetwork(WorldNetwork network) {
        if (!NETWORKDBS.containsKey(network.world.provider.getDimension()))
            NETWORKDBS.put(network.world.provider.getDimension(), new WorldNetworkDatabase(network.world));

        NETWORKDBS.get(network.world.provider.getDimension()).networks.put(network.id, network);
    }

    public static WorldNetworkDatabase getNetworkDB(@Nonnull World world) {
        return NETWORKDBS.get(world.provider.getDimension());
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
    }
}
