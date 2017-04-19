package com.elytradev.teckle.common.tile.inv;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;

/**
 * Created by darkevilmac on 4/18/2017.
 */
public class AdvancedItemStackHandler extends ItemStackHandler {

    public BiPredicate<Integer, ItemStack> canPutInSlot;
    public IContentChangeListener changeListener;

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        if (canPutInSlot.test(slot, stack))
            super.setStackInSlot(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        changeListener.onContentChange(slot);
        super.onContentsChanged(slot);
    }

    public interface IContentChangeListener {
        void onContentChange(int slot);
    }

}