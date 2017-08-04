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

import com.elytradev.teckle.common.container.ContainerAlloyFurnace;
import com.elytradev.teckle.common.tile.TileAlloyFurnace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Created by darkevilmac on 4/19/2017.
 */
public class GuiAlloyFurnace extends GuiContainer {

    public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("teckle", "textures/gui/alloyfurnace.png");
    public final TileAlloyFurnace alloyFurnace;

    public GuiAlloyFurnace(TileAlloyFurnace tile, EntityPlayer player) {
        super(new ContainerAlloyFurnace(tile, player));
        this.alloyFurnace = tile;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        fontRenderer.drawString(I18n.format("gui.inventory"), 8, 73, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GlStateManager.color(1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);

        if (alloyFurnace.fuelBurnTime > 0) {
            // Lit.

            int k = this.getBurnLeftScaled(13);
            this.drawTexturedModalRect(i + 19, j + 34 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        int l = this.getCookProgressScaled(24);
        if (alloyFurnace.cookTime == 0)
            l = 23;
        this.drawTexturedModalRect(i + 108, j + 33, 177, 14, 23 - l, 17);

        GlStateManager.enableLighting();
    }


    private int getCookProgressScaled(int pixels) {
        int i = this.alloyFurnace.cookTime;
        int j = 200;
        return i != 0 ? i * pixels / j : 0;
    }

    private int getBurnLeftScaled(int pixels) {
        int divisor = this.alloyFurnace.currentFuelWorth != 0 ? this.alloyFurnace.currentFuelWorth : 1;

        return this.alloyFurnace.fuelBurnTime * pixels / divisor;
    }

}
