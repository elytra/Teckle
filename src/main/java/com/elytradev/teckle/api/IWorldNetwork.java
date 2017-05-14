package com.elytradev.teckle.api;

import com.elytradev.teckle.common.worldnetwork.common.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Created by darkevilmac on 5/14/17.
 */
public interface IWorldNetwork extends INBTSerializable<NBTTagCompound>, ITickable {
    void registerNode(WorldNetworkNode node);

    void unregisterNode(WorldNetworkNode node);

    void unregisterNodeAtPosition(BlockPos nodePosition);

    WorldNetworkNode getNodeFromPosition(BlockPos pos);

    boolean isNodePresent(BlockPos nodePosition);

    Stream<WorldNetworkNode> nodeStream();

    List<WorldNetworkNode> getNodes();

    List<BlockPos> getNodePositions();

    void registerTraveller(WorldNetworkTraveller traveller, boolean send);

    void unregisterTraveller(WorldNetworkTraveller traveller, boolean immediate, boolean send);

    void unregisterTraveller(NBTTagCompound traveller, boolean immediate, boolean send);


    World getWorld();

    WorldNetwork merge(IWorldNetwork otherNetwork);

    void transferNetworkData(IWorldNetwork to);

    /**
     * Checks that the network's connections are fully valid, performs a split if needed.
     */
    void validateNetwork();

    UUID getNetworkID();
}
