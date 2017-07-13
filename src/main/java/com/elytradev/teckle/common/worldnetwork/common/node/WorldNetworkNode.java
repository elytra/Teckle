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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A node in a worldnetwork, contains the position and the current travellers.
 */
public class WorldNetworkNode {

    // Empty node, used instead of null because fuck NPEs.
    public static final WorldNetworkNode NONE = new WorldNetworkNode();
    public BlockPos position;
    public IWorldNetwork network;
    private boolean useFace = false;
    private List<EnumFacing> capabilityFaces = new ArrayList<>();
    private HashMap<UUID, WorldNetworkTraveller> travellers = new HashMap<>();

    public WorldNetworkNode() {
        this.position = new BlockPos(0, -1, 0);
        this.network = null;
    }

    public WorldNetworkNode(IWorldNetwork network, BlockPos position, List<EnumFacing> capabilityFaces) {
        this.position = position;
        this.network = network;
        if (capabilityFaces != null && capabilityFaces.stream().allMatch(Objects::nonNull)) {
            this.capabilityFaces = capabilityFaces;
            this.useFace = true;
        } else {
            this.useFace = false;
        }
    }

    public boolean isLoaded() {
        if (network == null || network.getWorld() == null)
            return false;
        return network.getWorld().isBlockLoaded(position);
    }

    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (isLoaded()) {
            if (getNetworkTile(from.getOpposite()) != null)
                return getNetworkTile(from.getOpposite()).canAcceptTraveller(traveller, from);
        }
        return isLoaded();
    }

    public IWorldNetworkTile getNetworkTile(EnumFacing capabilityFace) {
        if (CapabilityWorldNetworkTile.isPositionNetworkTile(network.getWorld(), position, capabilityFace))
            return CapabilityWorldNetworkTile.getNetworkTileAtPosition(network.getWorld(), position, capabilityFace);
        else return null;
    }

    public void registerTraveller(WorldNetworkTraveller traveller) {
        travellers.put(traveller.data.getUniqueId("id"), traveller);
    }

    public void unregisterTraveller(WorldNetworkTraveller traveller) {
        travellers.remove(traveller.data.getUniqueId("id"));
    }

    public ImmutableList<EnumFacing> getCapabilityFaces() {
        if (useFace) {
            return ImmutableList.copyOf(capabilityFaces);
        } else {
            return ImmutableList.of();
        }
    }

    public void addCapabilityFace(EnumFacing capabilityFace) {
        if (!capabilityFaces.contains(capabilityFace)) {
            capabilityFaces.add(capabilityFace);
            this.useFace = true;
        }
    }

    public void removeCapabilityFace(EnumFacing capabilityFace) {
        if (capabilityFaces.contains(capabilityFace)) {
            capabilityFaces.remove(capabilityFace);
            if (capabilityFaces.isEmpty()) {
                this.useFace = false;
            }
        }
    }

    public List<IWorldNetworkTile> getNetworkTiles() {
        if (useFace) {
            return getCapabilityFaces().stream().map(this::getNetworkTile).collect(Collectors.toList());
        } else {
            return Lists.newArrayList(getNetworkTile(null));
        }
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

    public boolean useFace() {
        return useFace;
    }
}
