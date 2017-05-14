package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.client.gui.GuiFabricator;
import com.elytradev.teckle.common.container.ContainerFabricator;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.ItemStream;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Collectors;

public class TileFabricator extends TileEntity implements ITickable, IElementProvider {

    public AdvancedItemStackHandler stackHandler = new AdvancedItemStackHandler(9);
    public IRecipe templateRecipe;
    public InventoryCrafting craftingGrid = new InventoryCrafting((Container) getServerElement(null), 3, 3);
    public int cooldown = 5;
    private NonNullList<ItemStack> templates = NonNullList.withSize(9, ItemStack.EMPTY);

    @Nonnull
    public ItemStack getTemplateSlot(int index) {
        return templates.get(index);
    }

    @Nonnull
    public ItemStack setTemplateSlot(int index, @Nonnull ItemStack stack) {
        ItemStack result = templates.set(index, stack);
        generateTemplateRecipe();
        return result;
    }

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

    /**
     * Get the recipe matching our template if we have the required items, null otherwise.
     *
     * @return
     */
    public IRecipe getRecipe() {
        InventoryCrafting templateGrid = new InventoryCrafting((Container) getServerElement(null), 3, 3);
        for (int i = 0; i < templates.size(); i++) {
            templateGrid.setInventorySlotContents(i, templates.get(i).copy());
        }

        if (templateRecipe == null) {
            generateTemplateRecipe();
        }

        if (templateRecipe == null || templateRecipe.matches(templateGrid, world) && templateRecipe.matches(craftingGrid, world))
            return templateRecipe;

        return null;
    }

    private void generateTemplateRecipe() {
        InventoryCrafting templateGrid = new InventoryCrafting((Container) getServerElement(null), 3, 3);
        for (int i = 0; i < templates.size(); i++) {
            templateGrid.setInventorySlotContents(i, templates.get(i).copy());
        }

        templateRecipe = null;
        for (IRecipe iRecipe : CraftingManager.getInstance().getRecipeList()) {
            if (iRecipe.matches(templateGrid, world)) {
                templateRecipe = iRecipe;
                break;
            }
        }
    }

    /**
     * Attempts to craft items from the template.
     */
    public void craft() {
        IRecipe recipe = getRecipe();
        if (recipe != null) {
            ItemStack result = recipe.getCraftingResult(craftingGrid).copy();

            int insertInto = -1;
            for (int i = 0; i < stackHandler.getStacks().size(); i++) {
                if (stackHandler.insertItem(i, result.copy(), true).isEmpty()) {
                    insertInto = i;
                    break;
                }
            }
            if (insertInto != -1) {
                NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(craftingGrid);
                for (int i = 0; i < remainingItems.size(); ++i) {
                    ItemStack gridStack = this.craftingGrid.getStackInSlot(i);
                    ItemStack remainingStack = remainingItems.get(i);

                    if (!gridStack.isEmpty()) {
                        this.craftingGrid.decrStackSize(i, 1);
                        gridStack = this.craftingGrid.getStackInSlot(i);
                    }

                    if (!remainingStack.isEmpty()) {
                        if (gridStack.isEmpty()) {
                            this.craftingGrid.setInventorySlotContents(i, remainingStack);
                        } else if (ItemStack.areItemsEqual(gridStack, remainingStack) && ItemStack.areItemStackTagsEqual(gridStack, remainingStack)) {
                            remainingStack.grow(gridStack.getCount());
                            this.craftingGrid.setInventorySlotContents(i, remainingStack);
                        }
                    }
                }

                stackHandler.insertItem(insertInto, result, false);
            }
        }
    }

    /**
     * Pulls items matching the template grid from neighbours.
     */
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
                            ItemStack extractAttempt = itemHandler.extractItem(o, MathHelper.clamp(craftingStack.getMaxStackSize() - craftingStack.getCount(), 0, 1), false);
                            if (!extractAttempt.isEmpty()) {
                                ItemStack craftingStackCopy = null;
                                if (craftingStack.isEmpty()) {
                                    craftingStackCopy = templateStack.copy();
                                    craftingStackCopy.setCount(extractAttempt.getCount());
                                } else {
                                    craftingStackCopy = craftingStack.copy();
                                    craftingStackCopy.grow(extractAttempt.getCount());
                                }
                                craftingGrid.setInventorySlotContents(i, craftingStackCopy);
                                break;
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
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == null) return false;

        if (Objects.equals(capability, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == null) return null;

        if (Objects.equals(capability, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)) {
            return (T) stackHandler;
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerFabricator(this, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientElement(EntityPlayer player) {
        return new GuiFabricator(this, player);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        NonNullList<ItemStack> craftingSupplies = NonNullList.withSize(9, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound.getCompoundTag("craftingSupplies"), craftingSupplies);
        for (int i = 0; i < craftingSupplies.size(); i++) {
            craftingGrid.setInventorySlotContents(i, craftingSupplies.get(i));
        }

        ItemStackHelper.loadAllItems(compound.getCompoundTag("templates"), templates);
        stackHandler.deserializeNBT(compound.getCompoundTag("stacks"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);

        NBTTagCompound craftingSupplies = new NBTTagCompound();
        NBTTagCompound templateData = new NBTTagCompound();
        ItemStackHelper.saveAllItems(craftingSupplies, ItemStream.convertCollectedListToNonNull(ItemStream.createItemStream(craftingGrid).collect(Collectors.toList())));
        ItemStackHelper.saveAllItems(templateData, templates);

        compound.setTag("craftingSupplies", craftingSupplies);
        compound.setTag("templates", templateData);
        compound.setTag("stacks", stackHandler.serializeNBT());

        return compound;
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
