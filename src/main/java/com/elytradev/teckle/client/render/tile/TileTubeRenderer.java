package com.elytradev.teckle.client.render.tile;

import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import com.google.common.base.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by darkevilmac on 3/31/2017.
 */
public class TileTubeRenderer extends TileEntitySpecialRenderer<TileItemTube> {

    public static IBakedModel itemColourModel;

    @Override
    public void renderTileEntityAt(TileItemTube te, double x, double y, double z, float partialTicks, int destroyStage) {
        HashMap<DummyNetworkTraveller, Vec3d> colourTravellers = new HashMap<>();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(.5, .5, .5);
        for (DummyNetworkTraveller traveller : te.travellers.values()) {
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
                GlStateManager.pushMatrix();
                GlStateManager.pushMatrix();

                translateForMovement(traveller, partialTicks, colourTravellers);
                GlStateManager.rotate((((float) te.getWorld().getWorldTime() + partialTicks) / 10.0F + 0) * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(0.25, 0.25, 0.25);
                itemRenderer.renderItem(stack, ibakedmodel);

                GlStateManager.popMatrix();
                GlStateManager.popMatrix();
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
        double newX = (lastTravelled * offset.xCoord) + ((dataTravelledOffset * offset.xCoord) - (lastTravelled * offset.xCoord)) * partialTicks;
        double newY = (lastTravelled * offset.yCoord) + ((dataTravelledOffset * offset.yCoord) - (lastTravelled * offset.yCoord)) * partialTicks;
        double newZ = (lastTravelled * offset.zCoord) + ((dataTravelledOffset * offset.zCoord) - (lastTravelled * offset.zCoord)) * partialTicks;
        offset = new Vec3d(newX, newY, newZ);
        if (traveller.data.hasKey("colour")) {
            colourTravellers.put(traveller, offset);
        }
        GlStateManager.translate(offset.xCoord, offset.yCoord, offset.zCoord);
    }

    public void drawOutlines(HashMap<DummyNetworkTraveller, Vec3d> travellers, TileItemTube te, double x, double y, double z) {


        for (Map.Entry<DummyNetworkTraveller, Vec3d> dummyNetworkTravellerVec3dEntry : travellers.entrySet()) {
            renderOutline(te, dummyNetworkTravellerVec3dEntry, x, y, z);
        }

    }

    private void renderOutline(TileItemTube te, Map.Entry<DummyNetworkTraveller, Vec3d> entry,  double x, double y, double z) {
        loadModelIfApplicable();

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexBuffer = tessellator.getBuffer();
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        if (Minecraft.isAmbientOcclusionEnabled()) {
            GlStateManager.shadeModel(7425);
        } else {
            GlStateManager.shadeModel(7424);
        }

        vertexBuffer.begin(7, DefaultVertexFormats.BLOCK);


        DummyNetworkTraveller traveller = entry.getKey();
        Vec3d translate = entry.getValue();
        IExtendedBlockState state = (IExtendedBlockState) te.getWorld().getBlockState(te.getPos());

        EnumDyeColor dyeColour = EnumDyeColor.byMetadata(traveller.data.getInteger("colour"));
        Color javaColour = new Color(dyeColour.getMapColor().colorValue);
        vertexBuffer.setTranslation(x - (double) te.getPos().getX() + translate.xCoord, y - (double) te.getPos().getY() + translate.yCoord, z - (double) te.getPos().getZ() + translate.zCoord);

        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModel(getWorld(), itemColourModel, state.withProperty(BlockItemTube.COLOUR, dyeColour), te.getPos(), vertexBuffer, false);

        vertexBuffer.setTranslation(0.0D, 0.0D, 0.0D);
        tessellator.draw();
        RenderHelper.enableStandardItemLighting();
    }

    public void loadModelIfApplicable() {
        if (itemColourModel != null)
            return;

        try {
            Function<ResourceLocation, TextureAtlasSprite> textureGetter = location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
            IRetexturableModel unbakedModel = (IRetexturableModel) ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_colour"));
            itemColourModel = unbakedModel.bake(new TRSRTransformation(ModelRotation.X0_Y0), DefaultVertexFormats.BLOCK, textureGetter);
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to load item outline model!");
        }
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
