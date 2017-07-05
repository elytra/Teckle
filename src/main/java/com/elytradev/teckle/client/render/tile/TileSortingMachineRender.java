package com.elytradev.teckle.client.render.tile;

import com.elytradev.teckle.client.render.model.ModelMachineOverlay;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;


public class TileSortingMachineRender extends TileEntitySpecialRenderer<TileSortingMachine> {

    private ModelMachineOverlay overlay;

    @Override
    public void render(TileSortingMachine te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        IBlockState blockState = te.getWorld().getBlockState(te.getPos());
        if (blockState.getBlock() != TeckleObjects.blockSortingMachine || !te.isLit)
            return;

        EnumFacing pointTo = blockState.getValue(BlockSortingMachine.FACING);
        if (overlay == null) {
            overlay = new ModelMachineOverlay(this.getClass(), new ResourceLocation("teckle", "block/machineoverlay"));
        }
        overlay.render(getWorld(), new Vec3d(x, y, z), te.getPos(), blockState, pointTo);

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing.equals(pointTo) || facing.equals(pointTo.getOpposite()))
                continue;

            // We need to draw on all 4 sides.
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            drawDeco(facing, pointTo, te);
            GlStateManager.popMatrix();
        }
    }

    public void drawDeco(EnumFacing facing, EnumFacing pointTo, TileSortingMachine te) {
        switch (facing) {
            case UP:
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.translate(0, 0, -1);
                break;
            case DOWN:
                GlStateManager.rotate(90, -1, 0, 0);
                GlStateManager.rotate(180, 0, 0, 1);
                GlStateManager.translate(-1, 0, 0);
                break;
            case NORTH:
                break;
            case WEST:
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.translate(-1, 0, 0);
                break;
            case SOUTH:
                GlStateManager.rotate(180, 0, 1, 0);
                GlStateManager.translate(-1, 0, -1);
                break;
            case EAST:
                GlStateManager.rotate(270, 0, 1, 0);
                GlStateManager.translate(0, 0, -1);
                break;
            default:
                break;
        }

        switch (pointTo) {
            case UP: {
                GlStateManager.rotate(180, 0, 0, 1);
                GlStateManager.translate(-1, -1, 0);
                break;
            }
            case NORTH: {
                if (facing == EnumFacing.WEST) {
                    GlStateManager.rotate(90, 0, 0, 1);
                    GlStateManager.translate(0, -1, 0);
                } else if (facing == EnumFacing.EAST) {
                    GlStateManager.rotate(90, 0, 0, -1);
                    GlStateManager.translate(-1, 0, 0);
                }
                break;
            }
            case SOUTH: {
                if (facing == EnumFacing.WEST) {
                    GlStateManager.rotate(90, 0, 0, -1);
                    GlStateManager.translate(-1, 0, 0);
                } else if (facing == EnumFacing.EAST) {
                    GlStateManager.rotate(90, 0, 0, 1);
                    GlStateManager.translate(0, -1, 0);
                } else if (facing == EnumFacing.DOWN) {
                    GlStateManager.rotate(180, 0, 0, -1);
                    GlStateManager.translate(-1, -1, 0);
                } else if (facing == EnumFacing.UP) {
                    GlStateManager.rotate(180, 0, 0, -1);
                    GlStateManager.translate(-1, -1, 0);
                }
                break;
            }
            case WEST: {
                GlStateManager.rotate(90, 0, 0, 1);
                GlStateManager.translate(0, -1, 0);
                if (facing.equals(EnumFacing.NORTH) || facing.equals(EnumFacing.UP)) {
                    GlStateManager.rotate(180, 0, 0, 1);
                    GlStateManager.translate(-1, -1, 0);
                }
                break;
            }
            case EAST: {
                GlStateManager.rotate(90, 0, 0, 1);
                GlStateManager.translate(0, -1, 0);
                if (facing.equals(EnumFacing.SOUTH) || facing.equals(EnumFacing.DOWN)) {
                    GlStateManager.rotate(180, 0, 0, 1);
                    GlStateManager.translate(-1, -1, 0);
                }
                break;
            }
        }

        GlStateManager.pushMatrix();
        drawBlinkenLights(te, facing);
        GlStateManager.popMatrix();
    }

    public void drawBlinkenLights(TileSortingMachine te, EnumFacing facing) {
        GlStateManager.translate(0, 0, -0.0005f);
        GlStateManager.color(1, 1, 1);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tess.getBuffer();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        GlStateManager.disableLighting();
        TextureAtlasSprite tas = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
                new ResourceLocation(TeckleMod.MOD_ID, "blocks/sortingmachineblinkenlights").toString());
        float minU = tas.getMinU();
        float maxU = tas.getMaxU();
        float minV = tas.getMinV();
        float maxV = tas.getMaxV();

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferBuilder.pos(0, 0, 0).tex(maxU, maxV).endVertex();
        bufferBuilder.pos(0, 1, 0).tex(maxU, minV).endVertex();
        bufferBuilder.pos(1, 1, 0).tex(minU, minV).endVertex();
        bufferBuilder.pos(1, 0, 0).tex(minU, maxV).endVertex();
        tess.draw();

        GlStateManager.enableLighting();
        int light = te.getWorld().getLight(te.getPos().offset(facing), true);
        int j = light % 65536;
        int k = light / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j, k);
    }
}
