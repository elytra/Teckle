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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;

public class DropActions {
    public static final HashMap<String, IDropAction> ACTIONS = new HashMap<>();

    public static Tuple<String, IDropAction> ITEMSTACK = new Tuple<>("itemstack", traveller -> {
        try {
            World world = traveller.network.getWorld();
            BlockPos nodePos = traveller.currentNode.position;
            world.spawnEntity(new EntityItem(world, nodePos.getX(), nodePos.getY(),
                    nodePos.getZ(), new ItemStack(traveller.data.getCompoundTag("stack"))));
            traveller.data.setTag("stack", new NBTTagCompound());
        } catch (NullPointerException npe) {
            String debugInfo = "Traveller " + traveller == null ? "null" : traveller.toString();
            debugInfo += " network " + traveller.network == null ? "null" : traveller.network.toString();
            debugInfo += " world " + traveller.network.getWorld() == null ? "null" : traveller.network.getWorld().toString();
            TeckleMod.LOG.error("****************OH SHIT TECKLE BROKE*******************");
            TeckleMod.LOG.error("Caught NPE in DropActions, {}", traveller);
            TeckleMod.LOG.error("Exception follows, {}", npe);
            TeckleMod.LOG.error("Here's some useful debug info, {}", debugInfo);
            TeckleMod.LOG.error("****************OH SHIT TECKLE BROKE*******************");
        }
    });

    static {
        ACTIONS.put(ITEMSTACK.getFirst(), ITEMSTACK.getSecond());
    }

}
