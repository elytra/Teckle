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
 * Base interface for world networks. In case the default implementation is not suitable.
 */
public interface IWorldNetwork extends INBTSerializable<NBTTagCompound>, ITickable {
    /**
     * Register a node in the network.
     *
     * @param node the node to register.
     */
    void registerNode(WorldNetworkNode node);

    /**
     * Unregister a node in the network.
     *
     * @param node the node to unregister.
     */
    void unregisterNode(WorldNetworkNode node);

    /**
     * Unregisters the node at the given position.
     *
     * @param nodePosition the position of the node to unregister.
     */
    void unregisterNodeAtPosition(BlockPos nodePosition);

    /**
     * Gets the node at the given position.
     *
     * @param pos the position of the node.
     * @return the node found.
     */
    WorldNetworkNode getNodeFromPosition(BlockPos pos);

    /**
     * Checks if there's a node present at the given position.
     *
     * @param nodePosition the position to check.
     * @return true if a node was found, false otherwise.
     */
    boolean isNodePresent(BlockPos nodePosition);

    /**
     * Creates a stream of all the nodes in the network
     *
     * @return the stream of all nodes in this network.
     */
    Stream<WorldNetworkNode> nodeStream();

    /**
     * Creates a list of all the nodes in the network.
     *
     * @return the list of all nodes in the network.
     */
    List<WorldNetworkNode> getNodes();

    /**
     * Creates a list of all the positions that nodes occupy in the network.
     *
     * @return a list of all occupied positions.
     */
    List<BlockPos> getNodePositions();

    /**
     * Register a traveller in the network.
     *
     * @param traveller the traveller to register.
     * @param send      if data should be sent to clients set to true, if not set to false.
     */
    void registerTraveller(WorldNetworkTraveller traveller, boolean send);

    /**
     * Unregister a traveller in the network
     *
     * @param traveller the traveller to remove.
     * @param immediate if true remove the traveller immediately instead of queueing for removal on next tick.
     * @param send      if data should be sent to clients set to true if not set to false.
     */
    void unregisterTraveller(WorldNetworkTraveller traveller, boolean immediate, boolean send);

    /**
     * Unregister a traveller in the network
     *
     * @param data      the data of the traveller to remove.
     * @param immediate if true remove the traveller immediately instead of queueing for removal on next tick.
     * @param send      if data should be sent to clients set to true if not set to false.
     */
    void unregisterTraveller(NBTTagCompound data, boolean immediate, boolean send);

    /**
     * Get the world that this network resides in.
     *
     * @return the world the network is in.
     */
    World getWorld();

    /**
     * Merge this network and the given network into one network.
     *
     * @param otherNetwork the network to merge with.
     * @return the resulted merged network.
     */
    WorldNetwork merge(IWorldNetwork otherNetwork);

    /**
     * Transfer the data from this network to the given network.
     *
     * @param to the network to transfer to.
     */
    void transferNetworkData(IWorldNetwork to);

    /**
     * Checks that the network's connections are fully valid, performs a split if needed.
     */
    void validateNetwork();

    /**
     * Get the unique id of this network.
     *
     * @return the network's id.
     */
    UUID getNetworkID();
}
