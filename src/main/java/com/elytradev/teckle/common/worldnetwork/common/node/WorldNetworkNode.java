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

package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode implements INBTSerializable<NBTTagCompound> {

    // Empty node, used instead of null because fuck NPEs.
    public static final WorldNetworkNode NONE = new WorldNetworkNode();
    public BlockPos position;
    public EnumFacing capabilityFace = null;
    public WorldNetworkTile tile = null;
    private IWorldNetwork network;
    private HashMap<UUID, WorldNetworkTraveller> travellers = new HashMap<>();

    public WorldNetworkNode() {
        this.position = new BlockPos(0, -1, 0);
        this.setNetwork(null);
    }

    public WorldNetworkNode(IWorldNetwork network, BlockPos position, EnumFacing capabilityFace) {
        this.setNetwork(network);
        this.position = position;
        this.setCapabilityFace(capabilityFace);
    }

    @Nullable
    public static WorldNetworkNode create(IWorldNetwork network, BlockPos pos, EnumFacing face, NBTTagCompound serializedData) {
        ResourceLocation id = new ResourceLocation(serializedData.getString("id"));
        Class<? extends WorldNetworkNode> tileClazz = NetworkNodeRegistry.getNetworkNode(id);
        WorldNetworkNode createdTile = null;
        try {
            Constructor<? extends WorldNetworkNode> constructor = tileClazz.getConstructor(IWorldNetwork.class, BlockPos.class, EnumFacing.class);
            createdTile = constructor.newInstance(network, pos, face);
        } catch (NoSuchMethodException e) {
            TeckleMod.LOG.error("Unable to find constructor matching (IWorldNetwork, BlockPos, EnumFacing) {}, the network tile will not be created...", tileClazz.getName());
            return null;
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to instantiate {}, the network node will be skipped.", tileClazz.getName());
            e.printStackTrace();
            return null;
        }
        createdTile.deserializeNBT(serializedData);
        return createdTile;
    }

    public boolean isLoaded() {
        if (getNetwork() == null || getNetwork().getWorld() == null)
            return false;
        return getNetwork().getWorld().isBlockLoaded(position);
    }

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (getNetworkTile() != null) {
            return getNetworkTile().canAcceptTraveller(traveller, from);
        } else {
            return true;
        }
    }

    /**
     * Forward method for network tiles, returns true if no network tile is associated with this.
     * <p>
     * Used to determine if this node can be connected to from the given side.
     */
    public boolean canConnectTo(EnumFacing side) {
        if (getNetworkTile() != null) {
            return getNetworkTile().canConnectTo(side);
        } else {
            return true;
        }
    }

    @Nullable
    public WorldNetworkTile getNetworkTile() {
        if (tile == null && isLoaded()) {
            if (CapabilityWorldNetworkTile.isPositionNetworkTile(getNetwork().getWorld(), position, capabilityFace)) {
                tile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(getNetwork().getWorld(), position, capabilityFace);
            }
        }
        return tile;
    }

    public void setNetworkTile(WorldNetworkTile tile) {
        this.tile = tile;
    }

    public boolean hasNetworkTile() {
        return isLoaded() && getNetworkTile() != null;
    }

    public void registerTraveller(WorldNetworkTraveller traveller) {
        travellers.put(traveller.data.getUniqueId("id"), traveller);
    }

    public void unregisterTraveller(WorldNetworkTraveller traveller) {
        travellers.remove(traveller.data.getUniqueId("id"));
    }

    public EnumFacing getCapabilityFace() {
        return capabilityFace;
    }

    public void setCapabilityFace(EnumFacing capabilityFace) {
        this.capabilityFace = capabilityFace;
    }

    public boolean isEndpoint() {
        return this instanceof WorldNetworkEndpoint;
    }

    public boolean isEntrypoint() {
        return this instanceof WorldNetworkEntryPoint;
    }

    public Collection<WorldNetworkTraveller> getTravellers() {
        return (travellers.values());
    }

    @Override
    public String toString() {
        return "WorldNetworkNode{" +
                "position=" + position +
                ", capabilityFace=" + capabilityFace +
                '}';
    }

    public IWorldNetwork getNetwork() {
        return network;
    }

    public void setNetwork(IWorldNetwork network) {
        this.network = network;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", NetworkNodeRegistry.getNetworkNodeName(this.getClass()).toString());
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        // NOOP
    }

}