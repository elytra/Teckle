package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.client.gui.base.GuiTeckle;
import com.elytradev.teckle.client.gui.base.GuiTeckleButton;
import com.elytradev.teckle.common.container.ContainerRetriever;
import com.elytradev.teckle.common.helper.ColourHelper;
import com.elytradev.teckle.common.network.messages.RetrieverColourChangeMessage;
import com.elytradev.teckle.common.network.messages.RetrieverSelectorModeChangeMessage;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;

public class GuiRetriever extends GuiTeckle {

    public final TileRetriever retriever;

    public GuiRetriever(TileRetriever tile, EntityPlayer player) {
        super(new ContainerRetriever(tile, player));
        this.retriever = tile;
    }

    @Override
    public void registerButtons() {
        addButton(new GuiColourPicker(1, guiLeft + 115, guiTop + 61));
        addButton(new GuiRetrieverMode(2, guiLeft + 45, guiTop + 54));
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/retriever.png");
    }

    public class GuiRetrieverMode extends GuiTeckleButton {
        public GuiRetrieverMode(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/retriever.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 9;
                int xOffset = 175;
                if (isMouseOver())
                    xOffset += 16;

                this.drawTexturedModalRect(this.x, this.y, xOffset, yOffset, this.width, this.height);
                this.drawButtonOverlay();
            }
        }

        public void drawButtonOverlay() {
            int yOffset = 9 + 16;
            int xOffset = 175;
            xOffset = retriever.useSelector() ? xOffset + 16 : xOffset;

            this.drawTexturedModalRect(this.x, this.y, xOffset, yOffset, this.width, this.height);
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            retriever.setUseSelector(!retriever.useSelector());
            new RetrieverSelectorModeChangeMessage(retriever.getPos(), retriever.useSelector()).sendToServer();
        }
    }

    public class GuiColourPicker extends GuiTeckleButton {
        public GuiColourPicker(int buttonId, int x, int y) {
            super(buttonId, x, y, 9, 9, "");
            this.enableSecondaryClick();
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/retriever.png"));
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                int yOffset = 0;
                int xOffset = 176;
                this.drawTexturedModalRect(this.x, this.y, xOffset, yOffset, this.width, this.height);
                EnumDyeColor colour = GuiRetriever.this.retriever.outputTile.getColour();
                if (colour != null) {
                    float[] sheepColour = EntitySheep.getDyeRgb(colour);
                    GlStateManager.pushMatrix();
                    GlStateManager.color(sheepColour[0], sheepColour[1], sheepColour[2]);
                    this.drawTexturedModalRect(this.x + 1, this.y + 1, xOffset + 10, yOffset + 1, this.width - 2, this.height - 2);
                    GlStateManager.popMatrix();
                }
            }
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            EnumDyeColor colour = ColourHelper.cycleColour(retriever.getColour(), mouseButton == 1);
            retriever.setColour(colour);
            new RetrieverColourChangeMessage(retriever.getPos(), colour).sendToServer();
        }
    }
}
