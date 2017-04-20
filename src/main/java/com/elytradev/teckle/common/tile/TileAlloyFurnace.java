package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.crafting.AlloyRecipe;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

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
    public IItemHandler topInputHandler = itemStackHandler.subHandler(0, 9),
            sideHandler = itemStackHandler.subHandler(9, 1),
            bottomHandler = itemStackHandler.subHandler(10, 1);

    public AlloyRecipe activeRecipe;
    public int fuelBurnTime, cookTime;

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null) {
            switch (facing) {
                case UP:
                    return (T) topInputHandler;
                case DOWN:
                    return (T) bottomHandler;
                default:
                    return (T) sideHandler;
            }
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        itemStackHandler.deserializeNBT(compound.getCompoundTag("inv"));
        fuelBurnTime = compound.getInteger("fuelBurnTime");
        cookTime = compound.getInteger("cookTime");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("inv", itemStackHandler.serializeNBT());
        compound.setInteger("fuelBurnTime", fuelBurnTime);
        compound.setInteger("cookTime", cookTime);
        return super.writeToNBT(compound);
    }

    @Override
    public void update() {
        checkFuel();
        checkCookStatus();
    }

    private void checkFuel() {
        if (fuelBurnTime <= 0) {
            if (!sideHandler.getStackInSlot(0).isEmpty()) {
                int fuelValue = TileEntityFurnace.getItemBurnTime(sideHandler.getStackInSlot(0));
                if (fuelValue > 0) {
                    fuelBurnTime = fuelValue;
                    sideHandler.extractItem(0, 1, false);
                }
            } else {
                cookTime = 0;
            }
        } else {
            fuelBurnTime--;
        }
    }

    private void checkCookStatus() {
        if (fuelBurnTime > 0) {
            if (cookTime > 0) {
                cookTime--;
            } else {
                AlloyRecipe recipe = AlloyRecipes.getInstance().recipes.stream().filter(alloyRecipe -> !alloyRecipe.matches(topInputHandler).isEmpty())
                        .findFirst().get();

                if (recipe != null) {
                    for (ItemStack stack : recipe.matches(topInputHandler)) {
                        for (int i = 0; i < topInputHandler.getSlots(); i++) {
                            if (ItemStack.areItemsEqual(topInputHandler.getStackInSlot(i), stack)) {
                                ItemStack extractResult = topInputHandler.extractItem(i, stack.getCount(), false);

                                stack.setCount(stack.getCount() - extractResult.getCount());
                            }
                        }
                    }
                    cookTime = recipe.getCookTime();
                }
            }
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
