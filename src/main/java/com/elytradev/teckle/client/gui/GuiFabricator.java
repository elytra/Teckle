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

package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.client.gui.base.GuiTeckle;
import com.elytradev.teckle.client.gui.base.GuiTeckleButton;
import com.elytradev.teckle.common.container.ContainerFabricator;
import com.elytradev.teckle.common.network.messages.serverbound.FabricatorTemplateMessage;
import com.elytradev.teckle.common.tile.TileFabricator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiFabricator extends GuiTeckle {

    public final TileFabricator fabricator;
    public final EntityPlayer player;

    private Template[] templates = new Template[9];

    public GuiFabricator(TileFabricator tile, EntityPlayer player) {
        super(new ContainerFabricator(tile, player));
        this.fabricator = tile;
        this.player = player;
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/fabricator.png");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        if (fabricator.templateRecipe != null) {
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRender.renderItemIntoGUI(fabricator.templateRecipe.getRecipeOutput(), 79, 34);
        }
    }

    @Override
    public void registerButtons() {
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                templates[j + i * 3] = new Template(j + i * 3, x + 7 + j * 18, y + 16 + i * 18);
                addButton(templates[j + i * 3]);
            }
        }
    }

    public ItemStack getMouseItem() {
        return player.inventory.getItemStack().copy();
    }

    public class Template extends GuiTeckleButton {
        public final int templateIndex;

        public Template(int buttonId, int x, int y) {
            super(buttonId, x, y, "");
            this.templateIndex = buttonId;

            this.height = this.width = 18;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible && !getTemplateStack().isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                GuiFabricator.this.itemRender.renderItemIntoGUI(getTemplateStack(), this.x + 1, this.y + 1);
            }

            GlStateManager.pushMatrix();
            GlStateManager.color(1, 1, 1, 1F / 3F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(getBackgroundTexture());
            drawTexturedModalRect(x, y, 7, 16, this.width, this.height);
            GlStateManager.popMatrix();
        }

        @Override
        public void playPressSound(SoundHandler soundHandlerIn) {
            //dont.
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return super.mousePressed(mc, mouseX, mouseY) &&
                    fabricator.craftingGrid.getStackInSlot(templateIndex).isEmpty();
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            ItemStack mouseItem = getMouseItem();
            mouseItem.setCount(1);
            new FabricatorTemplateMessage(fabricator.getPos(), mouseItem, templateIndex).sendToServer();
            fabricator.setTemplateSlot(templateIndex, mouseItem);
        }

        private ItemStack getTemplateStack() {
            return fabricator.getTemplateSlot(templateIndex);
        }
    }

}
