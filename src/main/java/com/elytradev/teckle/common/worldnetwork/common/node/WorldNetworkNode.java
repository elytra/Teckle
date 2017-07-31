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
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode {

    // Empty node, used instead of null because fuck NPEs.
    public static final WorldNetworkNode NONE = new WorldNetworkNode();
    public BlockPos position;
    private IWorldNetwork network;
    public EnumFacing capabilityFace = null;
    // Used when a tile isn't loaded.
    public BiPredicate<WorldNetworkTraveller, EnumFacing> canAcceptTravellerPredicate = (t, f) -> false;
    public Predicate<EnumFacing> canConnectToSidePredicate = (f) -> true;
    private HashMap<UUID, WorldNetworkTraveller> travellers = new HashMap<>();

    public WorldNetworkNode() {
        this.position = new BlockPos(0, -1, 0);
        this.setNetwork(null);
    }

    public WorldNetworkNode(IWorldNetwork network, BlockPos position, EnumFacing capabilityFace) {
        this.position = position;
        this.setNetwork(network);
        this.capabilityFace = capabilityFace;
    }

    public boolean isLoaded() {
        if (getNetwork() == null || getNetwork().getWorld() == null)
            return false;
        return getNetwork().getWorld().isBlockLoaded(position);
    }

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (isLoaded()) {
            if (getNetworkTile() != null)
                return getNetworkTile().canAcceptTraveller(traveller, from);
        } else {
            return canAcceptTravellerPredicate.test(traveller, from);
        }
        return isLoaded();
    }

    /**
     * Forward method for network tiles, uses a predicate as fallback if the tile isn't loaded.
     * <p>
     * Used to determine if this node can be connected to from the given side.
     */
    public boolean canConnectTo(EnumFacing side) {
        if (isLoaded()) {
            return getNetworkTile().canConnectTo(side);
        } else {
            return canConnectToSidePredicate.test(side);
        }
    }

    public IWorldNetworkTile getNetworkTile() {
        if (CapabilityWorldNetworkTile.isPositionNetworkTile(getNetwork().getWorld(), position, capabilityFace))
            return CapabilityWorldNetworkTile.getNetworkTileAtPosition(getNetwork().getWorld(), position, capabilityFace);
        else return null;
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
}