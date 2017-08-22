package com.elytradev.teckle.client.render.model;

import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.block.property.UnlistedBool;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BakedTubeModel implements IBakedModel {

    private HashMap<EnumFacing, IBakedModel> legModels;
    private HashMap<EnumFacing, IBakedModel> legModelsNode;
    private IBakedModel nodeModel;

    public BakedTubeModel(IBakedModel nodeModel, HashMap<EnumFacing, IBakedModel> legModels, HashMap<EnumFacing, IBakedModel> legModelsNode) {
        this.nodeModel = nodeModel;
        this.legModels = legModels;
        this.legModelsNode = legModelsNode;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (state instanceof IExtendedBlockState) {
            List<IBakedModel> modelData = getModelData((IExtendedBlockState) state);
            List<BakedQuad> quadData = new ArrayList<>();

            modelData.forEach(bakedModel -> quadData.addAll(bakedModel.getQuads(state, side, rand)));

            return quadData;
        }
        return Collections.emptyList();
    }

    public List<IBakedModel> getModelData(IExtendedBlockState state) {
        List<IBakedModel> result = Lists.newArrayList();

        if (state.getValue(BlockItemTube.NODE)) {
            result.add(nodeModel);
            result.addAll(addLegs(state, legModelsNode));
        } else {
            // No node.
            result.addAll(addLegs(state, legModels));
        }

        return result;
    }

    protected List<IBakedModel> addLegs(IExtendedBlockState state, HashMap<EnumFacing, IBakedModel> sourceModels) {
        List<IBakedModel> models = Lists.newArrayList();
        for (EnumFacing enumFacing : EnumFacing.values()) {
            UnlistedBool property = BlockItemTube.FACE_PROPERTIES.get(enumFacing);
            if (state.getValue(property).booleanValue()) {
                models.add(sourceModels.get(enumFacing));
            }
        }
        return models;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return nodeModel.getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }
}
