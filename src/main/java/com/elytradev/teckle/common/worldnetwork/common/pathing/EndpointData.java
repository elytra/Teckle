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

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * Stores an endpoint position as well as the side that endpoint is being injected to.
 */
public class EndpointData {
    public EnumFacing side;
    public BlockPos pos;
    public PathNode node;
    public int cost;

    public EndpointData(PathNode node, EnumFacing side) {
        this.side = side;
        this.node = node;

        this.cost = node.cost;
        this.pos = node.realNode.getPosition();
    }

    @Override
    public String toString() {
        return "EndpointData{" +
                "side=" + side +
                ", pos=" + pos +
                ", cost=" + cost +
                ", node=" + node +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointData that = (EndpointData) o;
        return side == that.side &&
                Objects.equals(pos, that.pos);
    }
}