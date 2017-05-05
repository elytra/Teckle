package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Created by darkevilmac on 5/3/17.
 */
public class TileFabricator extends TileEntity implements ITickable, IElementProvider {

    public AdvancedItemStackHandler stackHandler = new AdvancedItemStackHandler(18);
    public NonNullList<ItemStack> templates = NonNullList.withSize(9, ItemStack.EMPTY);
    public InventoryCrafting craftingGrid = new InventoryCrafting((Container) getServerElement(null), 3, 3);

    public int cooldown = 5;

    @Override
    public void update() {
        if (world.isRemote)
            return;
        if (cooldown <= 0) {
            pullFromNeighbours();
            craft();

            cooldown = 5;
        }

        cooldown--;
    }

    public IRecipe getRecipe() {
        for (IRecipe iRecipe : CraftingManager.getInstance().getRecipeList()) {
            if (iRecipe.matches(craftingGrid, world)) {
                return iRecipe;
            }
        }

        return null;
    }

    public void craft() {
        IRecipe recipe = getRecipe();
        if (recipe != null) {
        }
    }

    public void pullFromNeighbours() {
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbourPos = pos.add(facing.getDirectionVec());
            TileEntity neighbourTile = world.getTileEntity(neighbourPos);
            if (neighbourTile != null && neighbourTile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
                IItemHandler itemHandler = neighbourTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());

                for (int i = 0; i < craftingGrid.getSizeInventory(); i++) {
                    ItemStack templateStack = templates.get(i);
                    ItemStack craftingStack = craftingGrid.getStackInSlot(i);
                    for (int o = 0; o < itemHandler.getSlots(); o++) {
                        ItemStack stackInSlot = itemHandler.getStackInSlot(o);
                        if (!stackInSlot.isEmpty() && ItemHandlerHelper.canItemStacksStack(stackInSlot, templateStack)) {
                            ItemStack extractAttempt = itemHandler.extractItem(o, craftingStack.getMaxStackSize() - craftingStack.getCount(), false);
                            if (!extractAttempt.isEmpty()) {
                                ItemStack craftingStackCopy = craftingStack.copy();
                                craftingStackCopy.grow(extractAttempt.getCount());
                                craftingGrid.setInventorySlotContents(i, craftingStackCopy);
                            }
                        }

                        craftingStack = craftingGrid.getStackInSlot(i);
                        if (craftingStack.getMaxStackSize() == craftingStack.getCount())
                            break;
                    }
                }
            }
        }
    }

    @Override
    public Object getServerElement(EntityPlayer player) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientElement(EntityPlayer player) {
        return null;
    }

}
