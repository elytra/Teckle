package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.function.BiPredicate;

/**
 * Created by darkevilmac on 4/18/2017.
 */
public class TileAlloyFurnace extends TileEntity {

    public AdvancedItemStackHandler itemStackHandler = new AdvancedItemStackHandler(10).withSlotCheck((slot, stack) -> slot > 8 ? false : true);
    
}
