package com.elytradev.teckle.common.item;

import net.minecraft.item.Item;

/**
 * Created by darkevilmac on 4/20/2017.
 */
public class ItemBlade extends Item {

    public ItemBlade() {
        setMaxStackSize(1);
        setMaxDamage(100);
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

}
