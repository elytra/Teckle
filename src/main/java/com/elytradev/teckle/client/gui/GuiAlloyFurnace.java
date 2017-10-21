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
import com.elytradev.teckle.common.container.ContainerAlloyFurnace;
import com.elytradev.teckle.common.tile.TileAlloyFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiAlloyFurnace extends GuiTeckle {

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
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/alloyfurnace.png");
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;

        if (alloyFurnace.fuelBurnTime > 0) {
            // Lit.

            int k = this.getBurnLeftScaled(13);
            this.drawTexturedModalRect(i + 19, j + 34 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        int l = this.getCookProgressScaled(24);
        if (alloyFurnace.cookTime == 0)
            l = 23;
        this.drawTexturedModalRect(i + 108, j + 33, 177, 14, 23 - l, 17);
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
