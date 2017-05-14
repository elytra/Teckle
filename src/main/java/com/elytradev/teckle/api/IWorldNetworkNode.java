package com.elytradev.teckle.api;

import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

/**
 * Used to store node data on tiles.
 */
public interface IWorldNetworkNode {

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
    WorldNetworkNode createNode(IWorldNetwork network);

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
     * Handles a traveller being returned to this tile after all destinations have been attempted.
     * Only relevant if this node is an entrypoint.
     *
     * @param traveller
     * @param side
     */
    default void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
    }


}
