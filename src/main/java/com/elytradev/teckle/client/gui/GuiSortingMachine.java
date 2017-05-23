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

import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.network.SortingMachineColourChangeMessage;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;

import javax.vecmath.Point2i;

public class GuiSortingMachine extends GuiContainer {

    public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("teckle", "textures/gui/sortingmachine.png");
    public EntityPlayer player;
    public TileSortingMachine sortingMachine;


    public GuiSortingMachine(TileSortingMachine tileSortingMachine, EntityPlayer player) {
        super(new ContainerSortingMachine(tileSortingMachine, player));
        this.player = player;
        this.sortingMachine = tileSortingMachine;

        this.xSize = 176;
        this.ySize = 242;
    }

    @Override
    public void initGui() {
        super.initGui();

        buttonList.clear();
        for (int i = 0; i < 8; i++) {
            int xS = 10 + ((i & 3) * 40);
            int yS = 61 + (i > 3 ? 62 : 0);

            buttonList.add(new GuiColourPicker(i, i, guiLeft + xS, guiTop + yS));
        }

        buttonList.add(new GuiSortTypeSelector(8, 10, 130));
    }

    /**
     * Draws the background layer of this container (behind the items).
     *
     * @param partialTicks
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
        GlStateManager.enableLighting();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof GuiColourPicker) {
            GuiColourPicker colourPicker = (GuiColourPicker) button;
            EnumDyeColor colour = sortingMachine.colours[colourPicker.colourIndex];
            if (colour == null) {
                colour = EnumDyeColor.byMetadata(0);
            } else {
                if (colour.getMetadata() == 15) {
                    colour = null;
                } else {
                    colour = EnumDyeColor.byMetadata(colour.getMetadata() + 1);
                }
            }

            sortingMachine.colours[colourPicker.colourIndex] = colour;
            new SortingMachineColourChangeMessage(sortingMachine.getPos(), colourPicker.colourIndex, colour).sendToServer();
        }
    }

    public class GuiColourPicker extends GuiButton {

        public int colourIndex = 0;

        public GuiColourPicker(int buttonId, int colourIndex, int x, int y) {
            super(buttonId, x, y, 36, 5, "");
            this.colourIndex = colourIndex;
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/sortingmachine.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 5;
                int xOffset = 176;
                this.drawTexturedModalRect(this.xPosition, this.yPosition, xOffset, yOffset, this.width, this.height);

                if (GuiSortingMachine.this.sortingMachine.colours[colourIndex] != null) {
                    float[] sheepColour = EntitySheep.getDyeRgb(GuiSortingMachine.this.sortingMachine.colours[colourIndex]);
                    GlStateManager.pushMatrix();
                    GlStateManager.color(sheepColour[0], sheepColour[1], sheepColour[2]);
                    this.drawTexturedModalRect(this.xPosition + 1, this.yPosition + 1, xOffset + 1, yOffset - 4, this.width - 2, this.height - 2);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    public class GuiSortTypeSelector extends GuiButton {

        public GuiSortTypeSelector(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/sortingmachine.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 10;
                int xOffset = 176;
                if (isMouseOver()) {
                    xOffset += 16;
                }

                this.drawTexturedModalRect(this.xPosition, this.yPosition, xOffset, yOffset, this.width, this.height);

                if (sortingMachine.sortMode != null) {
                    Point2i modeOffset = sortingMachine.sortMode.getSortModeType().textureOffset();
                    this.drawTexturedModalRect(this.xPosition, this.yPosition, modeOffset.x, modeOffset.y, this.width, this.height);
                }
            }
        }
    }
}
