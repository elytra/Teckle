package com.elytradev.teckle.client.gui.base;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.List;

public abstract class GuiTeckle extends GuiContainer {

    protected List<GuiTeckleButton> hoverableButtons = Lists.newArrayList();
    protected int lastClick = -1;
    protected int mouseX, mouseY;

    public GuiTeckle(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.clear();
        this.hoverableButtons.clear();
        registerButtons();
    }

    /**
     * Used for registration of buttons, call addButton to add.
     */
    public void registerButtons() {
    }

    @Override
    protected <T extends GuiButton> T addButton(T button) {
        if (button instanceof GuiTeckleButton) {
            hoverableButtons.add((GuiTeckleButton) button);
        }
        return super.addButton(button);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-guiLeft, -guiTop, 0);
        for (GuiButton button : buttonList) {
            if (button instanceof GuiTeckleButton) {
                ((GuiTeckleButton) button).drawHover(mc, mouseX, mouseY);
            }
        }
        GlStateManager.popMatrix();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(getBackgroundTexture());
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
        GlStateManager.enableLighting();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.lastClick = mouseButton;
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 1) {
            for (GuiButton buttonIn : buttonList) {
                if (!(buttonIn instanceof GuiTeckleButton))
                    continue;
                GuiTeckleButton button = (GuiTeckleButton) buttonIn;
                if (button.acceptsSecondaryClick() && button.mousePressed(this.mc, mouseX, mouseY)) {
                    button.playPressSound(this.mc.getSoundHandler());
                    this.actionPerformed(button);
                }
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button instanceof GuiTeckleButton) {
            ((GuiTeckleButton) button).performAction(mouseX, mouseY, lastClick);
        }
    }

    public abstract ResourceLocation getBackgroundTexture();
}
