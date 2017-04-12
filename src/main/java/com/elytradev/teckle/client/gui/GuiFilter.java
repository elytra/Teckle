package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.common.container.ContainerFilter;
import com.elytradev.teckle.common.tile.TileFilter;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

/**
 * Created by darkevilmac on 4/12/2017.
 */
public class GuiFilter extends GuiContainer {

    public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("teckle", "textures/gui/filter.png");

    public GuiFilter(TileFilter tile, EntityPlayer player) {
        super(new ContainerFilter(tile, player));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }
}
