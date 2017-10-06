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

package com.elytradev.teckle.common.worldnetwork.common.pathing;

import com.elytradev.concrete.network.Marshallable;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.DummyWorldNetworkEndpoint;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * More or less a wrapper for a list that makes code easier to understand.
 */
public class WorldNetworkPath implements Marshallable {

    public static final WorldNetworkPath NOT_POSSIBLE = new WorldNetworkPath(null, null);

    private List<PathNode> path;
    private int index = -1;


    // Default constructor to calm concrete down.
    public WorldNetworkPath() {
        path = new ArrayList<>();
        index = -1;
    }

    private WorldNetworkPath(WorldNetworkTraveller traveller, EndpointData endNode) {
        path = new ArrayList<>();
        index = -1;
    }

    /**
     * Create a path through the following nodes for the given traveller.
     * Assumes the given end node can be reached.
     *
     * @param traveller the traveller to create the path for.
     * @param endNode   the end position.
     * @return a network path to the destination.
     */
    public static WorldNetworkPath createPath(WorldNetworkTraveller traveller, EndpointData endNode) {
        WorldNetworkPath path = new WorldNetworkPath(traveller, endNode);
        path.path.add(endNode.node);
        PathNode from = endNode.node.from;
        while (from != null) {
            path.path.add(0, from);
            // the best code ive ever written right here.
            from = from.from;
        }
        return path;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<PathNode> getPath() {
        return path;
    }

    /**
     * Get all positions in the path, used for data sync.
     *
     * @return the data sorted from beginning to end.
     */
    public List<BlockPos> pathPositions() {
        List<BlockPos> out = new ArrayList<>();

        for (PathNode aPath : path) {
            out.add(aPath.realNode.getPosition());
        }

        return out;
    }

    /**
     * Move to the increment path node.
     *
     * @return the increment node.
     */
    public WorldNetworkNode next() {
        index++;
        WorldNetworkNode currentNode = null;
        try {
            currentNode = path.get(index).realNode;
        } catch (Exception e) {
            TeckleMod.LOG.warn("Failed to get next node in path on " + FMLCommonHandler.instance().getEffectiveSide());
        }
        return currentNode != null ? currentNode : WorldNetworkNode.NONE;
    }

    /**
     * Move to the previous path node.
     *
     * @return the previous node.
     */
    public WorldNetworkNode prev() {
        index--;
        WorldNetworkNode currentNode = path.get(index).realNode;
        return currentNode != null ? currentNode : WorldNetworkNode.NONE;
    }

    @Override
    public void writeToNetwork(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, path.size(), 3);

        for (PathNode pathNode : path) {
            buf.writeLong(pathNode.realNode.getPosition().toLong());
            buf.writeInt(pathNode.faceFrom != null ? pathNode.faceFrom.getIndex() : -1);
        }
    }

    @Override
    public void readFromNetwork(ByteBuf buf) {
        int size = ByteBufUtils.readVarInt(buf, 3);

        for (int i = 0; i < size; i++) {
            BlockPos pos = BlockPos.fromLong(buf.readLong());
            int faceIndex = buf.readInt();
            EnumFacing capFace = null;
            if (faceIndex != -1) {
                capFace = EnumFacing.VALUES[faceIndex];
            }
            WorldNetworkNode networkNode = new WorldNetworkNode(null, pos, capFace);
            if (i == size - 1) {
                networkNode = new DummyWorldNetworkEndpoint(null, pos);
            }
            PathNode from = null;
            if (i != 0) {
                from = path.get(i - 1);
            }

            path.add(new PathNode(from, networkNode, capFace));
        }
    }

    public PathNode getEnd() {
        return path.get(path.size() - 1);
    }

}
