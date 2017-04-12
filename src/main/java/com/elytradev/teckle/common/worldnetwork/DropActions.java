package com.elytradev.teckle.common.worldnetwork;

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
        World world = traveller.network.world;
        BlockPos nodePos = traveller.currentNode.position;
        world.spawnEntity(new EntityItem(world, nodePos.getX(), nodePos.getY(),
                nodePos.getZ(), new ItemStack(traveller.data.getCompoundTag("stack"))));
        traveller.data.setTag("stack", new NBTTagCompound());
    });

    public static void init() {
        ACTIONS.put(ITEMSTACK.getFirst(), ITEMSTACK.getSecond());
    }

}
