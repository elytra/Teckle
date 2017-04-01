package com.elytradev.teckle.client.render.tile;

import com.elytradev.teckle.client.sync.TravellerData;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

/**
 * Created by darkevilmac on 3/31/2017.
 */
public class TileTubeRenderer extends TileEntitySpecialRenderer<TileItemTube> {
    @Override
    public void renderTileEntityAt(TileItemTube te, double x, double y, double z, float partialTicks, int destroyStage) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(.5, .5, .5);
        for (TravellerData travellerData : te.travellers.values()) {
            ItemStack stack = new ItemStack(travellerData.tagCompound.getCompoundTag("stack"));
            if (!stack.isEmpty()) {
                RenderItem itemRenderer = Minecraft.getMinecraft().getRenderItem();
                GlStateManager.enableRescaleNormal();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableBlend();
                RenderHelper.enableStandardItemLighting();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.pushMatrix();
                Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                IBakedModel ibakedmodel = itemRenderer.getItemModelWithOverrides(stack, te.getWorld(), null);
                GlStateManager.pushMatrix();
                GlStateManager.pushMatrix();

                translateForMovement(travellerData, partialTicks);
                GlStateManager.rotate((((float) te.getWorld().getWorldTime() + partialTicks) / 20.0F + 0) * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(0.35, 0.35, 0.35);
                itemRenderer.renderItem(stack, ibakedmodel);

                GlStateManager.popMatrix();
                GlStateManager.popMatrix();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }
        GlStateManager.popMatrix();
    }

    public void translateForMovement(TravellerData data, float partialTicks) {
        EnumFacing facing = data.travelled < 0.5F ? WorldNetworkTraveller.getFacingFromVector(data.current().subtract(data.last()))
                : WorldNetworkTraveller.getFacingFromVector(data.current().subtract(data.next())).getOpposite();

        Vec3d offset = new Vec3d(facing.getDirectionVec());
        // Smooth the variables out.
        float dataTravelledOffset = data.travelled - 0.5F;
        float lastTravelled = dataTravelledOffset - (1F / 20F);
        double newX = (lastTravelled * offset.xCoord) + ((dataTravelledOffset * offset.xCoord) - (lastTravelled * offset.xCoord)) * partialTicks;
        double newY = (lastTravelled * offset.yCoord) + ((dataTravelledOffset * offset.yCoord) - (lastTravelled * offset.yCoord)) * partialTicks;
        double newZ = (lastTravelled * offset.zCoord) + ((dataTravelledOffset * offset.zCoord) - (lastTravelled * offset.zCoord)) * partialTicks;
        offset = new Vec3d(newX, newY, newZ);
        GlStateManager.translate(offset.xCoord, offset.yCoord, offset.zCoord);
    }

    protected int getModelCount(ItemStack stack) {
        int i = 1;

        if (stack.getCount() > 48) {
            i = 5;
        } else if (stack.getCount() > 32) {
            i = 4;
        } else if (stack.getCount() > 16) {
            i = 3;
        } else if (stack.getCount() > 1) {
            i = 2;
        }

        return i;
    }

}
