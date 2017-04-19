package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;

public class TileAlloyFurnace extends TileEntity implements ITickable {

    public BiPredicate<Integer, ItemStack> insertCheck = (slot, stack) -> {
        if (slot == 9) {
            // Fuel slot
            return TileEntityFurnace.isItemFuel(stack);
        } else if (slot == 10) {
            // Output slot
            return false;
        } else {
            // Recipe slots.
            return true;
        }
    };

    public AdvancedItemStackHandler itemStackHandler = new AdvancedItemStackHandler(11).withInsertCheck(insertCheck);

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            switch (facing) {
                case UP: {

                }
                case DOWN: {

                }
                default: {

                }
            }
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        itemStackHandler.deserializeNBT(compound.getCompoundTag("inv"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("inv", itemStackHandler.serializeNBT());
        return super.writeToNBT(compound);
    }

    @Override
    public void update() {

    }
}
