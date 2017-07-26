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
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;

/**
 * Used to store node data on tiles.
 */
public interface IWorldNetworkTile {

    /**
     * Add client traveller, used for rendering.
     *
     * @param traveller
     */
    void addClientTraveller(DummyNetworkTraveller traveller);

    /**
     * Remove client traveller by data, used for rendering.
     *
     * @param data
     */
    void removeClientTraveller(NBTTagCompound data);

    /**
     * Get an immutable map of all the client travellers on this tile.
     *
     * @return a map of travellers.
     */
    ImmutableMap<NBTTagCompound, DummyNetworkTraveller> getClientTravellers();

    /**
     * Called after a network has been loaded from WorldSavedData, allows adding of missing nodes for endpoints and such.
     *
     * @param network
     */
    default void networkReloaded(IWorldNetwork network) {
    }

    /**
     * Check if this tile can be added to a given network with a neighbour on a specified side.
     *
     * @param network the network to add to
     * @param side    the direction of the neighbour that wants to add
     * @return true if can be added false otherwise.
     */
    boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side);

    /**
     * Get the current node stored in this object.
     *
     * @return
     */
    WorldNetworkNode getNode();

    /**
     * Set the stored node of this tile.
     *
     * @param node the node to set to.
     */
    void setNode(WorldNetworkNode node);

    /**
     * Create a new node for the given network, should ALWAYS be a new instance.
     *
     * @param network the network to create the node for.
     * @return a new node.
     */
    WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos);

    /**
     * Check if the tile can accept a given traveller, DO NOT forward to your node.
     * This is secondary validation for things that you store in your tile
     *
     * @param traveller the traveller to check.
     * @param from      the direction the traveller is coming from
     * @return true if accepted, false otherwise.
     */
    boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from);

    /**
     * Can this node be connected to from the given side?
     *
     * @param side the side to connect to.
     * @return true if connections are possible, false otherwise.
     */
    boolean canConnectTo(EnumFacing side);

    /**
     * Use to determine if a traveller can enter from the specified face when the tile is not loaded.
     * Set on node creation.
     */
    default BiPredicate<WorldNetworkTraveller, EnumFacing> canAcceptTravellerPredicate() {
        return (t, t2) -> false;
    }

    /**
     * Handles a traveller being returned to this tile after all destinations have been attempted.
     * Only relevant if this node is an entrypoint.
     *
     * @param traveller
     * @param side
     */
    default void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
    }

    /**
     * Determines if the network tile will be notified of nodes being added and removed from the network.
     *
     * @return true to listen for changes, false if no changes are received.
     */
    default boolean listenToNetworkChange() {
        return false;
    }

    /**
     * Called when a node is added from the worldnetwork, only called if listenToNetworkChange is true.
     *
     * @param addedNode the node that was added.
     */
    default void onNodeAdded(WorldNetworkNode addedNode) {
    }

    /**
     * Called when a node is removed from the worldnetwork, only called if listenToNetworkChange is true.
     *
     * @param removedNode the node that was removed.
     */
    default void onNodeRemoved(WorldNetworkNode removedNode) {
    }

    /**
     * The output face of the tile, only applies to tiles that add things to networks.
     *
     * @return the output face of the tile, null if this doesn't output.
     */
    @Nullable
    default EnumFacing getOutputFace() {
        return null;
    }

    /**
     * Get the face that this tile is associated with, should be equal to the face used in getCapability calls.
     *
     * @return the face used to get this tile from a capability
     */
    default EnumFacing getCapabilityFace() {
        return null;
    }

    /**
     * Get the tile entity at the position of this node.
     *
     * @return the tile entity at this position.
     */
    default TileEntity getTileEntity() {
        return this.getNode().getNetwork().getWorld().getTileEntity(getNode().position);
    }
}
