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

package com.elytradev.teckle.common.worldnetwork.common;

import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;

public class DropActions {
    public static final HashMap<String, IDropAction> ACTIONS = new HashMap<>();

    public static Tuple<String, IDropAction> ITEMSTACK = new Tuple<>("itemstack", traveller -> {
        try {
            World world = traveller.network.getWorld();
            BlockPos nodePos = traveller.currentNode.getPosition();
            if (traveller.data.hasKey("stack")) {
                ItemStack stackFromTag = new ItemStack(traveller.data.getCompoundTag("stack"));
                if (!stackFromTag.isEmpty()) {
                    world.spawnEntity(new EntityItem(world, nodePos.getX(), nodePos.getY(),
                            nodePos.getZ(), stackFromTag));
                }
                traveller.data.removeTag("stack");
            }
        } catch (NullPointerException npe) {
            boolean bool = traveller == null;
            String debugInfo = "traveller " + (bool ? "null" : traveller.toString());
            bool = bool || traveller.network == null;
            debugInfo += " network " + (bool ? "null" : traveller.network.toString());
            bool = bool || traveller.network.getWorld() == null;
            debugInfo += " world " + (bool ? "null" : traveller.network.getWorld().toString());
            TeckleMod.LOG.error("****************Caught exception when dropping itemstack*******");
            TeckleMod.LOG.error("Caught NPE in DropActions!, {}", traveller);
            TeckleMod.LOG.error("Exception follows, {}", npe);
            TeckleMod.LOG.error("Here's some useful debug info, {}", debugInfo);
            TeckleMod.LOG.error("***************************************************************");
        }
    });

    static {
        ACTIONS.put(ITEMSTACK.getFirst(), ITEMSTACK.getSecond());
    }

}
