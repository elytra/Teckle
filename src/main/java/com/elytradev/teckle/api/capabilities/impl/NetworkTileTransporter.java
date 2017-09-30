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

package com.elytradev.teckle.api.capabilities.impl;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Framework to build on for network tiles, doesn't implement everything you will need to make adjustments.
 */
public abstract class NetworkTileTransporter extends WorldNetworkTile {

    public Class<? extends WorldNetworkNode> nodeClazz;

    public WorldNetworkNode node;

    public NetworkTileTransporter(World world, Class<? extends WorldNetworkNode> nodeClazz) {
        super(null, null, null);
        this.nodeClazz = nodeClazz;
    }

    /**
     * Only use this constructor if you will override createNode
     */
    public NetworkTileTransporter(World world) {
        super(null, null, null);
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
            return nodeClazz.getConstructor(IWorldNetwork.class, BlockPos.class, EnumFacing.class).newInstance(network, pos, getCapabilityFace());
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to instantiate a WorldNetworkNode for class {}", nodeClazz, e);
        }

        return null;
    }

}
