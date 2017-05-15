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

import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;

/**
 * Used to store tagCompound about the usefulness of a node for making a path.
 */
public class PathNode {
    public int cost;
    public PathNode from;
    public WorldNetworkNode realNode;

    public PathNode(PathNode from, WorldNetworkNode realNode) {
        this.from = from;
        this.realNode = realNode;
        this.cost = from != null ? from.cost + 1 : 0;
    }

}
