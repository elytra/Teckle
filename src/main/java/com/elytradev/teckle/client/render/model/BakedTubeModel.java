package com.elytradev.teckle.client.render.model;

import com.elytradev.teckle.common.block.BlockItemTube;
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
        List<IBakedModel> result = new ArrayList<>();

        if (state.getValue(BlockItemTube.NODE)) {
            result.add(nodeModel);
            if (state.getValue(BlockItemTube.UP)) {
                result.add(legModelsNode.get(EnumFacing.UP));
            }
            if (state.getValue(BlockItemTube.DOWN)) {
                result.add(legModelsNode.get(EnumFacing.DOWN));
            }
            if (state.getValue(BlockItemTube.NORTH)) {
                result.add(legModelsNode.get(EnumFacing.NORTH));
            }
            if (state.getValue(BlockItemTube.SOUTH)) {
                result.add(legModelsNode.get(EnumFacing.SOUTH));
            }
            if (state.getValue(BlockItemTube.WEST)) {
                result.add(legModelsNode.get(EnumFacing.WEST));
            }
            if (state.getValue(BlockItemTube.EAST)) {
                result.add(legModelsNode.get(EnumFacing.EAST));
            }
        } else {
            // No node.
            if (state.getValue(BlockItemTube.UP)) {
                result.add(legModels.get(EnumFacing.UP));
            }
            if (state.getValue(BlockItemTube.DOWN)) {
                result.add(legModels.get(EnumFacing.DOWN));
            }
            if (state.getValue(BlockItemTube.NORTH)) {
                result.add(legModels.get(EnumFacing.NORTH));
            }
            if (state.getValue(BlockItemTube.SOUTH)) {
                result.add(legModels.get(EnumFacing.SOUTH));
            }
            if (state.getValue(BlockItemTube.WEST)) {
                result.add(legModels.get(EnumFacing.WEST));
            }
            if (state.getValue(BlockItemTube.EAST)) {
                result.add(legModels.get(EnumFacing.EAST));
            }
        }

        return result;
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
