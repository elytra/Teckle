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
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        fontRenderer.drawString(I18n.format("gui.inventory"), 8, 73, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GlStateManager.color(1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(BACKGROUND_TEXTURE);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
        GlStateManager.enableLighting();
    }


}
