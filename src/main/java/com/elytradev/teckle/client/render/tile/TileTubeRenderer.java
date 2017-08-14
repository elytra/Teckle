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

package com.elytradev.teckle.client.render.tile;

import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TileTubeRenderer extends TileEntitySpecialRenderer<TileItemTube> {

    public static IBakedModel itemColourModel;

    @Override
    public void render(TileItemTube te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        WorldNetworkTile networkTile = te.getCapability(CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY, null);

        HashMap<DummyNetworkTraveller, Vec3d> colourTravellers = new HashMap<>();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(.5, .5, .5);
        for (DummyNetworkTraveller traveller : networkTile.getClientTravellers().values()) {
            ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
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
                translateForMovement(traveller, partialTicks, colourTravellers);
                GlStateManager.rotate((((float) getWorld().getTotalWorldTime() + partialTicks) / 40F) * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(0.25, 0.25, 0.25);
                itemRenderer.renderItem(stack, ibakedmodel);
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        }
        GlStateManager.popMatrix();

        drawOutlines(colourTravellers, te, x, y, z);
    }

    public void translateForMovement(DummyNetworkTraveller traveller, float partialTicks, java.util.HashMap<DummyNetworkTraveller, Vec3d> colourTravellers) {
        EnumFacing facing = traveller.travelledDistance < 0.5F ? WorldNetworkTraveller.getFacingFromVector(traveller.currentNode.position.subtract(traveller.previousNode.position))
                : WorldNetworkTraveller.getFacingFromVector(traveller.currentNode.position.subtract(traveller.nextNode.position)).getOpposite();

        Vec3d offset = new Vec3d(facing.getDirectionVec());
        // Smooth the variables out.
        float dataTravelledOffset = traveller.travelledDistance - 0.5F;
        float lastTravelled = dataTravelledOffset - (1F / 10F);
        double newX = (lastTravelled * offset.x) + ((dataTravelledOffset * offset.x) - (lastTravelled * offset.x)) * partialTicks;
        double newY = (lastTravelled * offset.y) + ((dataTravelledOffset * offset.y) - (lastTravelled * offset.y)) * partialTicks;
        double newZ = (lastTravelled * offset.z) + ((dataTravelledOffset * offset.z) - (lastTravelled * offset.z)) * partialTicks;

        offset = new Vec3d(newX, newY, newZ);
        if (traveller.data.hasKey("colour")) {
            colourTravellers.put(traveller, offset);
        }
        GlStateManager.translate(offset.x, offset.y, offset.z);
    }

    public void drawOutlines(HashMap<DummyNetworkTraveller, Vec3d> travellers, TileItemTube te, double x, double y, double z) {
        GlStateManager.pushMatrix();
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();

        if (Minecraft.isAmbientOcclusionEnabled()) {
            GlStateManager.shadeModel(7425);
        } else {
            GlStateManager.shadeModel(7424);
        }

        GlStateManager.translate(x, y, z);
        for (Map.Entry<DummyNetworkTraveller, Vec3d> dummyNetworkTravellerVec3dEntry : travellers.entrySet()) {
            renderOutline(te, dummyNetworkTravellerVec3dEntry);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private void renderOutline(TileItemTube te, Map.Entry<DummyNetworkTraveller, Vec3d> entry) {
        IBakedModel model = getItemColourModel();

        GlStateManager.pushMatrix();
        GlStateManager.translate(entry.getValue().x, entry.getValue().y, entry.getValue().z);
        if (model != null) {
            EnumDyeColor dyeColour = EnumDyeColor.byMetadata(entry.getKey().data.getInteger("colour"));
            Color jColor = new Color(dyeColour.getColorValue());
            float[] rgb = jColor.getRGBColorComponents(new float[5]);
            Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(model, 1, rgb[0], rgb[1], rgb[2]);
        }
        GlStateManager.popMatrix();
    }

    public IBakedModel getItemColourModel() {
        if (itemColourModel != null)
            return itemColourModel;

        try {
            IModel unbakedModel = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_colour"));
            itemColourModel = unbakedModel.bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to load item outline model! {}", e);
        }

        return itemColourModel;
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
