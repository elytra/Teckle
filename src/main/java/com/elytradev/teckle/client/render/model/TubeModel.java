package com.elytradev.teckle.client.render.model;

import com.elytradev.teckle.common.TeckleMod;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

public class TubeModel implements IModel {

    private HashMap<EnumFacing, IBakedModel> legModels = new HashMap<>();
    private HashMap<EnumFacing, IBakedModel> legModelsNode = new HashMap<>();
    private IBakedModel nodeModel;

    public TubeModel() {
    }

    private void loadData() {
        try {
            HashMap<EnumFacing, ModelRotation> rotations = new HashMap<>();
            rotations.put(EnumFacing.UP, ModelRotation.X0_Y0);
            rotations.put(EnumFacing.DOWN, ModelRotation.X180_Y0);
            rotations.put(EnumFacing.NORTH, ModelRotation.X90_Y0);
            rotations.put(EnumFacing.SOUTH, ModelRotation.X90_Y180);
            rotations.put(EnumFacing.WEST, ModelRotation.X90_Y270);
            rotations.put(EnumFacing.EAST, ModelRotation.X90_Y90);

            IModel unbakedNodeModel = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_node"));
            IModel unbakedLegModel = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_leg"));
            IModel unbakedLegModelInside = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_leg_inside"));
            IModel unbakedLegNodeModel = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_leg_node"));
            IModel unbakedLegNodeModelInside = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_leg_node_inside"));

            nodeModel = unbakedNodeModel.bake(new TRSRTransformation(ModelRotation.X0_Y0), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());

            for (EnumFacing facing : EnumFacing.VALUES) {
                MultipartBakedModel.Builder legBuilder = new MultipartBakedModel.Builder();
                legBuilder.putModel(input -> true, unbakedLegModel.bake(new TRSRTransformation(rotations.get(facing)), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()));
                legBuilder.putModel(input -> true, unbakedLegModelInside.bake(new TRSRTransformation(rotations.get(facing)), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()));
                legModels.put(facing, legBuilder.makeMultipartModel());

                MultipartBakedModel.Builder legNodeBuilder = new MultipartBakedModel.Builder();
                legNodeBuilder.putModel(input -> true, unbakedLegNodeModel.bake(new TRSRTransformation(rotations.get(facing)), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()));
                legNodeBuilder.putModel(input -> true, unbakedLegNodeModelInside.bake(new TRSRTransformation(rotations.get(facing)), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()));
                legModelsNode.put(facing, legNodeBuilder.makeMultipartModel());
            }
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to load tube model data, things will work but won't look very nice.", e);
        }
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        if (nodeModel == null) {
            loadData();
            if (nodeModel == null)
                return ModelLoaderRegistry.getMissingModel().bake(state, format, bakedTextureGetter);
        }
        return new BakedTubeModel(nodeModel, legModels, legModelsNode);
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return ImmutableList.of(new ResourceLocation("teckle", "block/tube.item_node"),
                new ResourceLocation("teckle", "block/tube.item_leg"),
                new ResourceLocation("teckle", "block/tube.item_leg_inside"),
                new ResourceLocation("teckle", "block/tube.item_leg_node"),
                new ResourceLocation("teckle", "block/tube.item_leg_node_inside"));
    }
}
