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

package com.elytradev.teckle.api.capabilities;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

/**
 * Framework to build on for network tiles, doesn't implement everything you will need to make adjustments.
 */
public abstract class NetworkTileTransporter implements IWorldNetworkTile {

    public WorldNetworkNode node;
    public Class<? extends WorldNetworkNode> nodeClazz;

    public HashMap<NBTTagCompound, DummyNetworkTraveller> dummyTravellers = Maps.newHashMap();

    public NetworkTileTransporter(Class<? extends WorldNetworkNode> nodeClazz) {
        this.nodeClazz = nodeClazz;
    }

    /**
     * Only use this constructor if you will override createNode
     */
    public NetworkTileTransporter() {
    }

    /**
     * Add client traveller, used for rendering.
     *
     * @param traveller
     */
    @Override
    public void addClientTraveller(DummyNetworkTraveller traveller) {
        dummyTravellers.put(traveller.data, traveller);
    }

    /**
     * Remove client traveller by data, used for rendering.
     *
     * @param data
     */
    @Override
    public void removeClientTraveller(NBTTagCompound data) {
        if (dummyTravellers.containsKey(data))
            dummyTravellers.remove(data);
    }

    /**
     * Get an immutable map of all the client travellers on this tile.
     *
     * @return a map of travellers.
     */
    @Override
    public ImmutableMap<NBTTagCompound, DummyNetworkTraveller> getClientTravellers() {
        return ImmutableMap.copyOf(dummyTravellers);
    }

    /**
     * Get the current node stored in this object.
     *
     * @return
     */
    @Override
    public WorldNetworkNode getNode() {
        return node;
    }

    /**
     * Set the stored node of this tile.
     *
     * @param node the node to set to.
     */
    @Override
    public void setNode(WorldNetworkNode node) {
        this.node = node;
    }

    /**
     * Create a new node for the given network, should ALWAYS be a new instance.
     *
     * @param network the network to create the node for.
     * @return a new node.
     */
    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        try {
            return nodeClazz.getConstructor(IWorldNetwork.class, BlockPos.class).newInstance(network, pos);
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to instantiate a WorldNetworkNode for class {}", nodeClazz, e);
        }

        return null;
    }

}
