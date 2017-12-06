package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.client.gui.base.GuiTeckle;
import com.elytradev.teckle.client.gui.base.GuiTeckleButton;
import com.elytradev.teckle.common.container.ContainerRetriever;
import com.elytradev.teckle.common.helper.ColourHelper;
import com.elytradev.teckle.common.network.messages.serverbound.RetrieverColourChangeMessage;
import com.elytradev.teckle.common.network.messages.serverbound.RetrieverMatchCountMessage;
import com.elytradev.teckle.common.network.messages.serverbound.RetrieverSelectorModeChangeMessage;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
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
        addButton(new GuiMatchSizeMode(2, guiLeft + 45, guiTop + 38));
        addButton(new GuiRetrieverMode(3, guiLeft + 45, guiTop + 54));
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/retriever.png");
    }


    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (retriever.useSelector()) {
            mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/retriever.png"));
            int x = 61;
            int y = 16;
            int xOffset = 176;
            int yOffset = 57;

            x += (retriever.getSelectorPosition() % 3) * 18;
            y += (retriever.getSelectorPosition() / 3) * 18;

            this.drawTexturedModalRect(x, y, xOffset, yOffset, 18, 18);
        }

        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    private void drawButtonBase(GuiTeckleButton b, Minecraft mc, int mouseX, int mouseY) {
        b.checkHovered(mouseX, mouseY);
        mc.getTextureManager().bindTexture(new ResourceLocation("teckle", "textures/gui/retriever.png"));
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int yOffset = 9;
        int xOffset = 176;
        if (b.isMouseOver())
            xOffset += 16;

        this.drawTexturedModalRect(b.x, b.y, xOffset, yOffset, b.width, b.height);
    }

    public class GuiRetrieverMode extends GuiTeckleButton {
        public GuiRetrieverMode(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                drawButtonBase(this, mc, mouseX, mouseY);
                this.drawButtonOverlay();
            }
        }

        public void drawButtonOverlay() {
            int yOffset = 9 + 16;
            int xOffset = 176;
            xOffset = retriever.useSelector() ? xOffset + 16 : xOffset;

            this.drawTexturedModalRect(this.x, this.y, xOffset, yOffset, this.width, this.height);
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            retriever.setUseSelector(!retriever.useSelector());
            new RetrieverSelectorModeChangeMessage(retriever.getPos(), retriever.useSelector()).sendToServer();
        }

        @Override
        public void drawHover(Minecraft mc, int mouseX, int mouseY) {
            if (!visible)
                return;

            if (isMouseOver()) {
                String baseString = "retrievermode." + (retriever.useSelector() ? "selector" : "noselector");
                String title = ChatFormatting.BOLD.toString();
                title += I18n.format(baseString);
                String tooltip = I18n.format(baseString + ".tooltip");
                GuiRetriever.this.drawHoveringText(java.util.Arrays.asList(title, tooltip), mouseX, mouseY);
            }
        }

    }

    public class GuiMatchSizeMode extends GuiTeckleButton {
        public GuiMatchSizeMode(int buttonId, int x, int y) {
            super(buttonId, x, y, 16, 16, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                drawButtonBase(this, mc, mouseX, mouseY);
                this.drawButtonOverlay();
            }
        }

        public void drawButtonOverlay() {
            int yOffset = 9 + 16 + 16;
            int xOffset = 176;
            xOffset = retriever.matchCount() ? xOffset + 16 : xOffset;

            this.drawTexturedModalRect(this.x, this.y, xOffset, yOffset, this.width, this.height);
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            retriever.setMatchCount(!retriever.matchCount());
            new RetrieverMatchCountMessage(retriever.getPos(), retriever.matchCount()).sendToServer();
        }

        @Override
        public void drawHover(Minecraft mc, int mouseX, int mouseY) {
            if (!visible)
                return;

            if (isMouseOver()) {
                String baseString = "retrievermode." + (retriever.matchCount() ? "matchcount" : "ignorecount");
                String title = ChatFormatting.BOLD.toString();
                title += I18n.format(baseString);
                String tooltip = I18n.format(baseString + ".tooltip");
                GuiRetriever.this.drawHoveringText(java.util.Arrays.asList(title, tooltip), mouseX, mouseY);
            }
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
