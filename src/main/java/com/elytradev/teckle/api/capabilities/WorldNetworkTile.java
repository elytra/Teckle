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
import com.elytradev.teckle.common.worldnetwork.common.NetworkTileRegistry;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.function.BiPredicate;

/**
 * Used to store node data on tiles.
 */
public abstract class WorldNetworkTile implements INBTSerializable<NBTTagCompound> {

    protected World world;
    protected WorldNetworkNode node;
    protected BlockPos pos;
    protected EnumFacing capabilityFace;

    private HashMap<NBTTagCompound, DummyNetworkTraveller> dummyTravellers = Maps.newHashMap();

    /**
     * WorldNetworkTile default constructor, you need to have a constructor with a world parameter and position to properly load.
     *
     * @param world the world the tile resides in.
     * @param pos   the position of the tile.
     * @param face  the face of the tile.
     */
    public WorldNetworkTile(World world, BlockPos pos, EnumFacing face) {
        this.setWorld(world);
        this.setPos(pos);
        this.setCapabilityFace(face);
        TeckleMod.LOG.debug("Created a network tile, {} {} {}", world, pos, false);
    }

    @Nullable
    public static WorldNetworkTile create(IWorldNetwork network, BlockPos pos, EnumFacing face, NBTTagCompound serializedData) {
        ResourceLocation id = new ResourceLocation(serializedData.getString("id"));
        Class<? extends WorldNetworkTile> tileClazz = NetworkTileRegistry.getNetworkTile(id);
        WorldNetworkTile createdTile = null;
        try {
            Constructor<? extends WorldNetworkTile> constructor = tileClazz.getConstructor(World.class, BlockPos.class, EnumFacing.class);
            createdTile = constructor.newInstance(network.getWorld(), pos, face);
        } catch (NoSuchMethodException e) {
            TeckleMod.LOG.error("Unable to find constructor with world parameter for {}, the network tile will not be created...", tileClazz.getName());
            return null;
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to instantiate {}, the network tile will be skipped.", tileClazz.getName());
            e.printStackTrace();
            return null;
        }
        createdTile.deserializeData(serializedData);
        return createdTile;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    /**
     * Add client traveller, used for rendering.
     *
     * @param traveller
     */
    public void addClientTraveller(DummyNetworkTraveller traveller) {
        dummyTravellers.put(traveller.data, traveller);
    }

    /**
     * Remove client traveller by data, used for rendering.
     *
     * @param data
     */
    public void removeClientTraveller(NBTTagCompound data) {
        if (dummyTravellers.containsKey(data))
            dummyTravellers.remove(data);
    }

    /**
     * Get an immutable map of all the client travellers on this tile.
     *
     * @return a map of travellers.
     */
    public ImmutableMap<NBTTagCompound, DummyNetworkTraveller> getClientTravellers() {
        return ImmutableMap.copyOf(dummyTravellers);
    }

    /**
     * Called after a network has been loaded from WorldSavedData, allows adding of missing nodes for endpoints and such.
     *
     * @param network
     */
    public void networkReloaded(IWorldNetwork network) {
    }

    /**
     * Check if this tile can be added to a given network with a neighbour on a specified side.
     *
     * @param network the network to add to
     * @param side    the direction of the neighbour that wants to add
     * @return true if can be added false otherwise.
     */
    public abstract boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side);

    /**
     * Get the current node stored in this object.
     *
     * @return
     */
    public WorldNetworkNode getNode() {
        return this.node;
    }

    /**
     * Set the stored node of this tile.
     *
     * @param node the node to set to.
     */
    public void setNode(WorldNetworkNode node) {
        this.node = node;
    }

    /**
     * Create a new node for the given network, should ALWAYS be a new instance.
     *
     * @param network the network to create the node for.
     * @return a new node.
     */
    public abstract WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos);

    /**
     * Check if the tile can accept a given traveller, DO NOT forward to your node.
     * This is secondary validation for things that you store in your tile
     *
     * @param traveller the traveller to check.
     * @param from      the direction the traveller is coming from
     * @return true if accepted, false otherwise.
     */
    public abstract boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from);

    /**
     * Can this node be connected to from the given side?
     *
     * @param side the side to connect to.
     * @return true if connections are possible, false otherwise.
     */
    public abstract boolean canConnectTo(EnumFacing side);

    /**
     * Use to determine if a traveller can enter from the specified face when the tile is not loaded.
     * Set on node creation.
     */
    public BiPredicate<WorldNetworkTraveller, EnumFacing> canAcceptTravellerPredicate() {
        return (t, t2) -> false;
    }

    /**
     * Handles a traveller being returned to this tile after all destinations have been attempted.
     * Only relevant if this node is an entrypoint.
     *
     * @param traveller
     * @param side
     */
    public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
    }

    /**
     * Determines if the network tile will be notified of nodes being added and removed from the network.
     *
     * @return true to listen for changes, false if no changes are received.
     */
    public boolean listenToNetworkChange() {
        return false;
    }

    /**
     * Called when a node is added from the worldnetwork, only called if listenToNetworkChange is true.
     *
     * @param addedNode the node that was added.
     */
    public void onNodeAdded(WorldNetworkNode addedNode) {
    }

    /**
     * Called when a node is removed from the worldnetwork, only called if listenToNetworkChange is true.
     *
     * @param removedNode the node that was removed.
     */
    public void onNodeRemoved(WorldNetworkNode removedNode) {
    }

    /**
     * The output face of the tile, only applies to tiles that add things to networks.
     *
     * @return the output face of the tile, null if this doesn't output.
     */
    @Nullable
    public EnumFacing getOutputFace() {
        return null;
    }

    /**
     * Get the face that this tile is associated with, should be equal to the face used in getCapability calls.
     *
     * @return the face used to get this tile from a capability
     */
    public EnumFacing getCapabilityFace() {
        return capabilityFace;
    }

    /**
     * Set the capability face this tile is associated with, only used during instantiation.
     *
     * @param capabilityFace the new capability face.
     */
    public void setCapabilityFace(EnumFacing capabilityFace) {
        this.capabilityFace = capabilityFace;
    }

    /**
     * Get the tile entity at the position of this node.
     *
     * @return the tile entity at this position.
     */
    public TileEntity getTileEntity() {
        return getWorld().getTileEntity(getNode().position);
    }

    public NBTTagCompound serializeData(NBTTagCompound tag) {
        tag.setString("id", NetworkTileRegistry.getNetworkTileName(this.getClass()).toString());
        tag.setLong("pos", pos.toLong());
        tag.setInteger("face", getCapabilityFace() == null ? -1 : getCapabilityFace().getIndex());
        tag.setTag("ImplementationData", serializeNBT());
        return tag;
    }

    public void deserializeData(NBTTagCompound tag) {
        this.pos = BlockPos.fromLong(tag.getLong("pos"));
        this.setCapabilityFace(tag.getInteger("face") > 0 ? EnumFacing.values()[tag.getInteger("face")] : null);
        this.deserializeNBT(tag.getCompoundTag("ImplementationData"));
    }

}
