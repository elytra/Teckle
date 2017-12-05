package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.client.gui.base.GuiTeckle;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

public class GuiBeamQuarry extends GuiTeckle {
    public GuiBeamQuarry(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/beamquarry.png");
    }
}
