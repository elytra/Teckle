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

import com.elytradev.teckle.common.container.ContainerFabricator;
import com.elytradev.teckle.common.network.messages.FabricatorTemplateMessage;
import com.elytradev.teckle.common.tile.TileFabricator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * Created by darkevilmac on 4/12/2017.
 */
public class GuiFabricator extends GuiContainer {

    public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("teckle", "textures/gui/fabricator.png");
    public final TileFabricator fabricator;
    public final EntityPlayer player;

    private Template[] templates = new Template[9];


    public GuiFabricator(TileFabricator tile, EntityPlayer player) {
        super(new ContainerFabricator(tile, player));
        this.fabricator = tile;
        this.player = player;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        //copied from guiscreen, used to detect right clicks on templates
        if (mouseButton == 1) {
            for (int i = 0; i < this.templates.length; ++i) {
                GuiButton guibutton = this.templates[i];

                if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                    net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre event = new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre(this, guibutton, this.buttonList);
                    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))
                        break;
                    guibutton = event.getButton();
                    guibutton.playPressSound(this.mc.getSoundHandler());
                    this.actionPerformed(guibutton);
                    if (this.equals(this.mc.currentScreen))
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Post(this, event.getButton(), this.buttonList));
                }
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                templates[j + i * 3] = new Template(j + i * 3, x + 7 + j * 18, y + 16 + i * 18);
                buttonList.add(templates[j + i * 3]);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof Template) {
            int templateID = ((Template) button).templateIndex;
            new FabricatorTemplateMessage(fabricator.getPos(), getMouseItem(), templateID).sendToServer();

            ItemStack mouseItem = getMouseItem();
            mouseItem.setCount(1);
            fabricator.setTemplateSlot(templateID, mouseItem);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        if (fabricator.templateRecipe != null) {
            RenderHelper.enableGUIStandardItemLighting();
            this.itemRender.renderItemIntoGUI(fabricator.templateRecipe.getRecipeOutput(), 79, 34);
        }
    }

    public ItemStack getMouseItem() {
        return player.inventory.getItemStack().copy();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GlStateManager.color(1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
        GlStateManager.enableLighting();
    }

    public class Template extends GuiButton {
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
            Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
            drawTexturedModalRect(x, y, 7, 16, this.width, this.height);
            GlStateManager.popMatrix();
        }

        @Override
        public void playPressSound(SoundHandler soundHandlerIn) {
            //dont.
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return super.mousePressed(mc, mouseX, mouseY) && fabricator.craftingGrid.getStackInSlot(templateIndex).isEmpty();
        }

        private ItemStack getTemplateStack() {
            return fabricator.getTemplateSlot(templateIndex);
        }
    }

}
