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
import com.elytradev.teckle.common.container.ContainerFilter;
import com.elytradev.teckle.common.helper.ColourHelper;
import com.elytradev.teckle.common.network.messages.serverbound.FilterColourChangeMessage;
import com.elytradev.teckle.common.tile.TileFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;

public class GuiFilter extends GuiTeckle {

    public final TileFilter filter;
    private GuiColourPicker colourPicker;

    public GuiFilter(TileFilter tile, EntityPlayer player) {
        super(new ContainerFilter(tile, player));
        this.filter = tile;
    }

    @Override
    public void registerButtons() {
        colourPicker = new GuiColourPicker(1, guiLeft + 115, guiTop + 61);
        addButton(colourPicker);
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/filter.png");
    }

    public class GuiColourPicker extends GuiTeckleButton {
        public GuiColourPicker(int buttonId, int x, int y) {
            super(buttonId, x, y, 9, 9, "");
            this.enableSecondaryClick();
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/filter.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 0;
                int xOffset = 176;
                this.drawTexturedModalRect(this.x, this.y, xOffset, yOffset, this.width, this.height);

                if (GuiFilter.this.filter.colour != null) {
                    float[] sheepColour = EntitySheep.getDyeRgb(GuiFilter.this.filter.colour);
                    GlStateManager.pushMatrix();
                    GlStateManager.color(sheepColour[0], sheepColour[1], sheepColour[2]);
                    this.drawTexturedModalRect(this.x + 1, this.y + 1, xOffset + 10, yOffset + 1, this.width - 2, this.height - 2);
                    GlStateManager.popMatrix();
                }
            }
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            EnumDyeColor colour = ColourHelper.cycleColour(filter.colour, mouseButton == 1);
            filter.colour = colour;
            new FilterColourChangeMessage(filter.getPos(), colour).sendToServer();
        }
    }
}
