package com.elytradev.teckle.client.render.model;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

public class TubeModelLoader implements ICustomModelLoader {

    private static TubeModel tubeModel;

    @Override
    public boolean accepts(ResourceLocation location) {
        return location.getNamespace().equals("teckle")
                && location.getPath().endsWith("tube.item");
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        if (tubeModel == null)
            tubeModel = new TubeModel();

        return tubeModel;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        tubeModel = new TubeModel();
    }
}
