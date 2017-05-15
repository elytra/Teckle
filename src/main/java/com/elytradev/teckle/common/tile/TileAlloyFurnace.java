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

package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.client.gui.GuiAlloyFurnace;
import com.elytradev.teckle.common.block.BlockAlloyFurnace;
import com.elytradev.teckle.common.container.ContainerAlloyFurnace;
import com.elytradev.teckle.common.crafting.AlloyRecipe;
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import com.elytradev.teckle.common.tile.base.IElementProvider;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class TileAlloyFurnace extends TileEntity implements ITickable, IElementProvider {

    public AlloyRecipe activeRecipe;
    public int fuelBurnTime, currentFuelWorth, cookTime;

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
        itemStackHandler.deserializeNBT(compound.getCompoundTag("filterData"));
        fuelBurnTime = compound.getInteger("fuelBurnTime");
        currentFuelWorth = compound.getInteger("currentFuelWorth");
        cookTime = compound.getInteger("cookTime");

        if (compound.hasKey("alloyRecipeKey")) {
            this.activeRecipe = AlloyRecipes.getInstance().getRecipeByResult(new ItemStack(compound.getCompoundTag("alloyRecipeKey")));
        } else {
            this.activeRecipe = null;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("filterData", itemStackHandler.serializeNBT());
        compound.setInteger("fuelBurnTime", fuelBurnTime);
        compound.setInteger("currentFuelWorth", currentFuelWorth);
        compound.setInteger("cookTime", cookTime);

        if (activeRecipe != null) {
            compound.setTag("alloyRecipeKey", activeRecipe.getCraftingResult().serializeNBT());
        } else {
            compound.removeTag("alloyRecipeKey");
        }

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
                Optional<AlloyRecipe> recipe = AlloyRecipes.getInstance().getRecipes().stream().filter(alloyRecipe -> !alloyRecipe.matches(topInputHandler).isEmpty())
                        .findFirst();

                int fuelValue = TileEntityFurnace.getItemBurnTime(sideHandler.getStackInSlot(0));
                if (fuelValue > 0 && recipe.isPresent()) {
                    fuelBurnTime = fuelValue;
                    currentFuelWorth = fuelValue;
                    sideHandler.extractItem(0, 1, false);
                    world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockAlloyFurnace.LIT, true));
                } else {
                    world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockAlloyFurnace.LIT, false));
                }
            } else {
                cookTime = 0;
                world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockAlloyFurnace.LIT, false));
            }
        }

        if (fuelBurnTime != 0) {
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
                Optional<AlloyRecipe> recipe = AlloyRecipes.getInstance().getRecipes().stream().filter(alloyRecipe -> !alloyRecipe.matches(topInputHandler).isEmpty())
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

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerAlloyFurnace(this, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientElement(EntityPlayer player) {
        return new GuiAlloyFurnace(this, player);
    }
}
