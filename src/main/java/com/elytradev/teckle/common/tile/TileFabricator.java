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

import com.elytradev.teckle.client.gui.GuiFabricator;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.container.ContainerFabricator;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.util.BlueprintUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileFabricator extends TileEntity implements ITickable, IElementProvider {

    public AdvancedItemStackHandler stackHandler = new AdvancedItemStackHandler(18);
    public AdvancedItemStackHandler blueprint = new AdvancedItemStackHandler(1).withChangeListener(this::onBlueprintChanged).withSlotLimit(s -> 1).withInsertCheck((integer, stack) -> stack.getItem() == TeckleObjects.itemBlueprint);
    public IRecipe templateRecipe;
    public InventoryCrafting craftingGrid = new InventoryCrafting((Container) getServerElement(null), 3, 3);
    public int cooldown = 5;
    public NonNullList<ItemStack> blueprintTemplate = NonNullList.withSize(9,ItemStack.EMPTY);

    public void onBlueprintChanged(int unused) {
        templateRecipe = BlueprintUtil.getRecipeFromBlueprint(getBlueprint(), getWorld()).orElse(null);
        blueprintTemplate = BlueprintUtil.getListFromBlueprint(getBlueprint());
        generateBlueprintRecipe();
    }

    public ItemStack getBlueprint() {
        return blueprint.getStackInSlot(0);
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Nonnull
    public ItemStack getTemplateSlot(int index) {
        return blueprintTemplate.get(index);
    }

    @Nonnull
    public ItemStack setTemplateSlot(int index, @Nonnull ItemStack stack) {
        if(getBlueprint().isEmpty())
            return ItemStack.EMPTY;
        ItemStack result = blueprintTemplate.set(index, stack);
        BlueprintUtil.setInventoryOfBlueprint(getBlueprint(),blueprintTemplate,getWorld());
        generateBlueprintRecipe();
        return result;
    }

    @Override
    public void update() {
        if (world.isRemote)
            return;
        if (cooldown <= 0) {
            craft();
            cooldown = TeckleMod.CONFIG.fabricatorCooldown;
        }

        cooldown--;
    }

    /**
     * Get the recipe matching our template if we have the required items, null otherwise.
     *
     * @return
     */
    public IRecipe getRecipe() {
        return templateRecipe;
    }

    private void generateBlueprintRecipe() {
        templateRecipe = BlueprintUtil.getRecipeFromBlueprint(getBlueprint(), getWorld()).orElse(null);
    }

    /**
     * Attempts to craft items from the template.
     */
    public void craft() {
        IRecipe recipe = getRecipe();
        if (recipe != null) {

            boolean complete = true;

            ItemStackHandler inventoryCopy = this.stackHandler.copy();
            NonNullList<ItemStack> components = BlueprintUtil.getListFromBlueprint(getBlueprint());
            for(ItemStack comp : components) {
                ItemStack stack = comp.copy();
                if (stack.getCount() > 0) {
                    for (int slot = 0; slot < inventoryCopy.getSlots(); slot++) {
                        ItemStack invStack = inventoryCopy.getStackInSlot(slot);
                        if(!invStack.isEmpty()) {
                            if (ItemStack.areItemsEqual(invStack, stack)) {
                                inventoryCopy.extractItem(slot, 1, false);
                                stack.shrink(1);
                                break;
                            }
                        }
                    }
                }

                if (stack.getCount() > 0) {
                    complete = false;
                    break;
                }
            }

            if(complete) {
                for (int i = 0; i < blueprintTemplate.size(); i++) {
                    ItemStack stack = blueprintTemplate.get(i);
                    craftingGrid.setInventorySlotContents(i, stack);
                }
                ItemStack result = recipe.getCraftingResult(craftingGrid);
                int insertInto = -1;
                for (int i = 0; i < inventoryCopy.getSlots(); i++) {
                    if (inventoryCopy.insertItem(i, result.copy(), true).isEmpty()) {
                        insertInto = i;
                        break;
                    }
                }
                if (insertInto != -1) {
                    NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(craftingGrid);
                    boolean canComplete = true;
                    for(ItemStack stack : remainingItems) {
                        boolean inserted = false;
                        for (int slot = 0; slot < stackHandler.getSlots(); slot++) {
                            if (stackHandler.insertItem(slot, stack.copy(), true).isEmpty()) {
                                inserted=true;
                                break;
                            }
                        }
                        if(!inserted)
                            canComplete=false;
                    }

                    if(canComplete) {
                        for (ItemStack comp : components) {
                            ItemStack stack = comp.copy();
                            if (stack.getCount() > 0) {
                                for (int slot = 0; slot < stackHandler.getSlots(); slot++) {
                                    ItemStack invStack = stackHandler.getStackInSlot(slot);
                                    if (ItemStack.areItemsEqual(invStack, stack)) {
                                        stackHandler.extractItem(slot, 1, false);
                                        break;
                                    }
                                }
                            }
                        }
                        stackHandler.insertItem(insertInto, result.copy(), false);

                        for(ItemStack stack : remainingItems) {
                            for (int slot = 0; slot < stackHandler.getSlots(); slot++) {
                                if (stackHandler.insertItem(slot, stack.copy(), false).isEmpty()) {
                                    break;
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);

    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
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

        if (compound.hasKey("stacks", Constants.NBT.TAG_COMPOUND)) { //Stupid workaround needed to save backward compat
            NBTTagCompound stacks = compound.getCompoundTag("stacks");
            stacks.removeTag("Size");
            stackHandler.deserializeNBT(stacks);
        } else {
            stackHandler.deserializeNBT(compound.getCompoundTag("inventory"));
        }
        blueprint.deserializeNBT(compound.getCompoundTag("blueprint"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);

        compound.setTag("inventory", stackHandler.serializeNBT());
        compound.setTag("blueprint", blueprint.serializeNBT());

        return compound;
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
