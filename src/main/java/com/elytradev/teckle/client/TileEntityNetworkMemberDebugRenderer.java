package com.elytradev.teckle.client;

import com.elytradev.teckle.common.tile.base.TileItemNetworkMember;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**
 * Created by darkevilmac on 3/31/2017.
 */
public class TileEntityNetworkMemberDebugRenderer extends TileEntitySpecialRenderer<TileItemNetworkMember> {

    @Override
    public void renderTileEntityAt(TileItemNetworkMember te, double x, double y, double z, float partialTicks, int destroyStage) {
        super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage);
        // Doesn't do anything because we don't sync network data.
        //GlStateManager.pushMatrix();
        //GlStateManager.translate(x, y + 1.01, z);
        //GlStateManager.scale(.025, .025, .025);
        //GlStateManager.pushMatrix();
        //GlStateManager.rotate(90, 1, 0, 0);
        //FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        //fontRenderer.drawString(te.n != null ? te.network.toString() : "NONE", 0, 0, 0);
        //GlStateManager.popMatrix();
        //GlStateManager.popMatrix();
    }
}
