package com.elytradev.teckle.common.item;

import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.item.Item;

public class ItemBlueprint extends Item {
    public ItemBlueprint() {
        setMaxStackSize(1);
        setCreativeTab(TeckleObjects.creativeTab);
    }
}
