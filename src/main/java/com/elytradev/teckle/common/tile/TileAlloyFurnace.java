package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.block.BlockAlloyFurnace;
import com.elytradev.teckle.common.crafting.AlloyRecipe;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class TileAlloyFurnace extends TileEntity implements ITickable {

    public AlloyRecipe activeRecipe;
    public int fuelBurnTime, cookTime;

    public BiPredicate<Integer, ItemStack> insertCheck = (slot, stack) -> {
        if (slot == 9) {
            // Fuel slot
            return TileEntityFurnace.isItemFuel(stack);
        } else if (slot == 10) {
            // Output slot
            return activeRecipe != null ? ItemStack.areItemStacksEqual(stack, activeRecipe.getCraftingResult()) : false;
        } else {
            // Recipe slots.
            return true;
        }
    };

    public AdvancedItemStackHandler itemStackHandler = new AdvancedItemStackHandler(11).withInsertCheck(insertCheck);
    public IItemHandler topInputHandler = itemStackHandler.subHandler(0, 9),
            sideHandler = itemStackHandler.subHandler(9, 1),
            bottomHandler = itemStackHandler.subHandler(10, 1);

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
        if (world.isRemote)
            return;

        checkFuel();
        checkCookStatus();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        if (oldState.getBlock() == newSate.getBlock()) {
            return false;
        }

        return super.shouldRefresh(world, pos, oldState, newSate);
    }

    private void checkFuel() {
        if (fuelBurnTime <= 0) {
            if (!sideHandler.getStackInSlot(0).isEmpty()) {
                Optional<AlloyRecipe> recipe = AlloyRecipes.getInstance().recipes.stream().filter(alloyRecipe -> !alloyRecipe.matches(topInputHandler).isEmpty())
                        .findFirst();

                int fuelValue = TileEntityFurnace.getItemBurnTime(sideHandler.getStackInSlot(0));
                if (fuelValue > 0 && recipe.isPresent()) {
                    fuelBurnTime = fuelValue;
                    sideHandler.extractItem(0, 1, false);
                    world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockAlloyFurnace.LIT, true));
                } else {
                    world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockAlloyFurnace.LIT, false));
                }
            } else {
                cookTime = 0;
                world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockAlloyFurnace.LIT, false));
            }
        } else {
            fuelBurnTime--;
        }
    }

    private void checkCookStatus() {
        if (fuelBurnTime > 0) {
            if (cookTime > 0) {
                cookTime--;
                if (cookTime == 0 && activeRecipe != null) {
                    ItemStack stack = itemStackHandler.insertItem(10, activeRecipe.getCraftingResult(), false);
                    System.out.println(stack);
                }
            } else {
                Optional<AlloyRecipe> recipe = AlloyRecipes.getInstance().recipes.stream().filter(alloyRecipe -> !alloyRecipe.matches(topInputHandler).isEmpty())
                        .findFirst();

                if (recipe.isPresent() && (bottomHandler.getStackInSlot(0).isEmpty() || ItemHandlerHelper.canItemStacksStack(recipe.get().getCraftingResult(), bottomHandler.getStackInSlot(0)))) {
                    List<ItemStack> matching = recipe.get().matches(topInputHandler);
                    for (int i = 0; i < matching.size(); i++) {
                        ItemStack stack = matching.get(i).copy();
                        for (int j = 0; j < topInputHandler.getSlots(); j++) {
                            ItemStack stackInSlot = topInputHandler.getStackInSlot(j);
                            if (stackInSlot.isEmpty())
                                continue;

                            if (stack.isItemEqual(stackInSlot)) {
                                ItemStack extractResult = topInputHandler.extractItem(j, stack.getCount(), false);

                                stack = ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - extractResult.getCount());
                            }
                        }
                    }
                    cookTime = recipe.get().getCookTime();
                    this.activeRecipe = recipe.get();
                } else {
                    this.activeRecipe = null;
                }
            }
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
