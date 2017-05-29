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

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.network.SortingMachineColourChangeMessage;
import com.elytradev.teckle.common.network.SortingMachineDefaultRouteChangeMessage;
import com.elytradev.teckle.common.network.SortingMachinePullModeChangeMessage;
import com.elytradev.teckle.common.network.SortingMachineSortModeChangeMessage;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModeType;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;

import javax.vecmath.Point2i;
import java.util.Arrays;

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

        buttonList.add(new GuiSortTypeSelector(8, guiLeft + 10, guiTop + 130));
        buttonList.add(new GuiSortModeSelector(9, guiLeft + 40, guiTop + 130));
        buttonList.add(new GuiDefaultRouteSelector(10, guiLeft + 64, guiTop + 133));

        if (sortingMachine.getSource() != null) {
            buttonList.add(new GuiPullModeSelector(11, guiLeft + 150, guiTop + 130));
        }
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

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     *
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int selectorPosition = sortingMachine.getSortMode().selectorPosition(sortingMachine);
        if (selectorPosition != -1) {
            int textureX = 176;
            int textureY = 90;

            int posX = 8 + ((selectorPosition & 3) * 40);
            int posY = 5;

            if (selectorPosition > 3) {
                posY += 60;
            }

            Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
            this.drawTexturedModalRect(posX, posY, textureX, textureY, 40, 57);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(-guiLeft, -guiTop, 0);
        for (int i = 0; i < buttonList.size(); i++) {
            if (buttonList.get(i) instanceof IHoverable) {
                ((IHoverable) buttonList.get(i)).drawHover(mc, mouseX, mouseY);
            }
        }
        GlStateManager.popMatrix();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof GuiColourPicker) {
            // Adjust the colour of a compartment.
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
        } else if (button instanceof GuiDefaultRouteSelector) {
            // Adjust the default route.
            TileSortingMachine.DefaultRoute defaultRoute = sortingMachine.defaultRoute;
            int selectedMode = defaultRoute.getMetadata();

            if (selectedMode < TileSortingMachine.DefaultRoute.values().length - 1) {
                selectedMode++;
            } else if (selectedMode == TileSortingMachine.DefaultRoute.values().length - 1) {
                selectedMode = 0;
            }

            sortingMachine.defaultRoute = TileSortingMachine.DefaultRoute.byMetadata(selectedMode);
            new SortingMachineDefaultRouteChangeMessage(selectedMode, sortingMachine.getPos()).sendToServer();
        } else if (button instanceof GuiSortTypeSelector) {
            // Change the sort type, and the mode to match.
            try {
                if (sortingMachine.getSortMode().type == SortModeType.COMPARTMENT) {
                    sortingMachine.setSortMode(SortModeType.SLOT.getDefaultMode().newInstance());
                } else {
                    sortingMachine.setSortMode(SortModeType.COMPARTMENT.getDefaultMode().newInstance());
                }
                new SortingMachineSortModeChangeMessage(sortingMachine.getSortMode().getID(), sortingMachine.getPos()).sendToServer();
            } catch (Exception e) {
                TeckleMod.LOG.error("Failed to change mode type in sortingmachine gui, ", e);
            }
        } else if (button instanceof GuiSortModeSelector) {
            // Actually change the sort mode.
            int selectedMode = sortingMachine.getSortMode().getID();
            SortModeType sortModeType = sortingMachine.getSortMode().type;

            if (selectedMode < sortModeType.maxID()) {
                selectedMode++;
            } else if (selectedMode == sortModeType.maxID()) {
                selectedMode = sortModeType.minID();
            }

            try {
                sortingMachine.setSortMode(SortMode.SORT_MODES.get(selectedMode).newInstance());
            } catch (Exception e) {
                TeckleMod.LOG.error("Failed to change sort mode in sortingmachine gui, ", e);
            }

            new SortingMachineSortModeChangeMessage(selectedMode, sortingMachine.getPos()).sendToServer();
        } else if (button instanceof GuiPullModeSelector) {
            // Change the pull mode.
            if (sortingMachine.getSource() == null) {
                buttonList.remove(button);
                return;
            }
            int selectedMode = sortingMachine.getPullMode().getID();

            if (selectedMode < PullMode.PULL_MODES.size() - 1) {
                selectedMode++;
            } else if (selectedMode == PullMode.PULL_MODES.size() - 1) {
                selectedMode = 0;
            }

            if (PullMode.PULL_MODES.get(selectedMode) == PullMode.SINGLE_SWEEP) {
                // Prevent us from using an invalid pull mode if possible.
                if (sortingMachine.getSortMode().getClass() == SortMode.SLOT_ANY_STACK
                        || sortingMachine.getSortMode().getClass() == SortMode.SLOT_FULL_STACK
                        || sortingMachine.getSortMode().getClass() == SortMode.COMPARTMENT_FULL_MATCH) {
                    if (selectedMode < PullMode.PULL_MODES.size() - 1) {
                        selectedMode++;
                    } else if (selectedMode == PullMode.PULL_MODES.size() - 1) {
                        selectedMode = 0;
                    }
                }
            }

            try {
                sortingMachine.setPullMode(PullMode.PULL_MODES.get(selectedMode).newInstance());
            } catch (Exception e) {
                TeckleMod.LOG.error("Failed to change pull mode in sortingmachine gui, ", e);
            }

            new SortingMachinePullModeChangeMessage(selectedMode, sortingMachine.getPos()).sendToServer();
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

    public class GuiDefaultRouteSelector extends GuiButton implements IHoverable {

        public GuiDefaultRouteSelector(int buttonId, int x, int y) {
            super(buttonId, x, y, 9, 9, "");
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/sortingmachine.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 0;
                int xOffset = 230;

                if (!sortingMachine.defaultRoute.isColoured()) {
                    xOffset -= 18;
                    if (sortingMachine.defaultRoute.isBlocked()) {
                        xOffset += 9;
                    }
                }

                this.drawTexturedModalRect(this.xPosition, this.yPosition, xOffset, yOffset, this.width, this.height);
                if (sortingMachine.defaultRoute.isColoured()) {
                    float[] sheepColour = EntitySheep.getDyeRgb(sortingMachine.defaultRoute.getColour());
                    GlStateManager.pushMatrix();
                    GlStateManager.color(sheepColour[0], sheepColour[1], sheepColour[2]);
                    this.drawTexturedModalRect(this.xPosition + 1, this.yPosition + 1, xOffset + 1, yOffset + 1, this.width - 2, this.height - 2);
                    GlStateManager.popMatrix();
                }
            }
        }

        @Override
        public void drawHover(Minecraft mc, int mouseX, int mouseY) {
            if (!visible)
                return;

            if (sortingMachine.getSortMode() != null) {
                if (isMouseOver()) {
                    GuiSortingMachine.this.drawHoveringText(ChatFormatting.BOLD + I18n.format(sortingMachine.defaultRoute.getName()), mouseX, mouseY);
                }
            }
        }
    }

    public class GuiSortTypeSelector extends GuiButton implements IHoverable {

        public GuiSortTypeSelector(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/sortingmachine.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 10;
                int xOffset = 176;
                if (isMouseOver()) {
                    xOffset += 16;
                }

                this.drawTexturedModalRect(this.xPosition, this.yPosition, xOffset, yOffset, this.width, this.height);

                if (sortingMachine.getSortMode() != null) {
                    Point2i modeOffset = sortingMachine.getSortMode().getSortModeType().textureOffset();
                    this.drawTexturedModalRect(this.xPosition, this.yPosition, modeOffset.x, modeOffset.y, this.width, this.height);
                }
            }
        }

        @Override
        public void drawHover(Minecraft mc, int mouseX, int mouseY) {
            if (!visible)
                return;

            if (sortingMachine.getSortMode() != null) {
                if (isMouseOver()) {
                    GuiSortingMachine.this.drawHoveringText(ChatFormatting.BOLD + I18n.format(sortingMachine.getSortMode().type.getUnlocalizedName()), mouseX, mouseY);
                }
            }
        }
    }

    public class GuiSortModeSelector extends GuiButton implements IHoverable {

        public GuiSortModeSelector(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/sortingmachine.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 10;
                int xOffset = 176;
                if (isMouseOver()) {
                    xOffset += 16;
                }

                this.drawTexturedModalRect(this.xPosition, this.yPosition, xOffset, yOffset, this.width, this.height);

                if (sortingMachine.getSortMode() != null) {
                    Point2i modeOffset = sortingMachine.getSortMode().getSortModeType().textureOffset();
                    int additionalYOffset = (sortingMachine.getSortMode().getID() * 16);
                    if (sortingMachine.getSortMode().type == SortModeType.SLOT) {
                        additionalYOffset = ((sortingMachine.getSortMode().getID() - 2) * 16);
                    } else {
                        additionalYOffset += 16;
                    }
                    this.drawTexturedModalRect(this.xPosition, this.yPosition, modeOffset.x, modeOffset.y + additionalYOffset, this.width, this.height);
                }
            }
        }

        @Override
        public void drawHover(Minecraft mc, int mouseX, int mouseY) {
            if (!visible)
                return;

            if (sortingMachine.getSortMode() != null) {
                if (isMouseOver()) {
                    GuiSortingMachine.this.drawHoveringText(Arrays.asList(ChatFormatting.BOLD + I18n.format(sortingMachine.getSortMode().getUnlocalizedName()),
                            I18n.format(sortingMachine.getSortMode().getUnlocalizedName() + ".tooltip")), mouseX, mouseY);
                }
            }
        }
    }

    public class GuiPullModeSelector extends GuiButton implements IHoverable {

        public GuiPullModeSelector(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/sortingmachine.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 10;
                int xOffset = 176;
                if (isMouseOver()) {
                    xOffset += 16;
                }

                this.drawTexturedModalRect(this.xPosition, this.yPosition, xOffset, yOffset, this.width, this.height);

                if (sortingMachine.getSortMode() != null) {
                    Point2i modeOffset = sortingMachine.getPullMode().textureOffset();
                    this.drawTexturedModalRect(this.xPosition, this.yPosition, modeOffset.x, modeOffset.y, this.width, this.height);
                }
            }
        }

        @Override
        public void drawHover(Minecraft mc, int mouseX, int mouseY) {
            if (!visible)
                return;

            if (sortingMachine.getSortMode() != null) {
                if (isMouseOver()) {
                    GuiSortingMachine.this.drawHoveringText(Arrays.asList(ChatFormatting.BOLD + I18n.format(sortingMachine.getPullMode().getUnlocalizedName()),
                            I18n.format(sortingMachine.getPullMode().getUnlocalizedName() + ".tooltip")), mouseX, mouseY);
                }
            }
        }
    }
}
