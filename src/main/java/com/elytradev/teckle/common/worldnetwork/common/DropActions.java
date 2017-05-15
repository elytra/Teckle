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

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;

/**
 * Created by darkevilmac on 4/10/2017.
 */
public class DropActions {
    public static final HashMap<String, IDropAction> ACTIONS = new HashMap<>();

    public static Tuple<String, IDropAction> ITEMSTACK = new Tuple<>("itemstack", traveller -> {
        World world = traveller.network.getWorld();
        BlockPos nodePos = traveller.currentNode.position;
        world.spawnEntity(new EntityItem(world, nodePos.getX(), nodePos.getY(),
                nodePos.getZ(), new ItemStack(traveller.data.getCompoundTag("stack"))));
        traveller.data.setTag("stack", new NBTTagCompound());
    });

    public static void init() {
        ACTIONS.put(ITEMSTACK.getFirst(), ITEMSTACK.getSecond());
    }

}
