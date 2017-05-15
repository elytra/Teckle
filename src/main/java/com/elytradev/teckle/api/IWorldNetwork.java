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
