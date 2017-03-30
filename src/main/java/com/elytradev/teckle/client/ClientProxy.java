package com.elytradev.teckle.client;

import com.elytradev.teckle.client.model.ModelItemTube;
import com.elytradev.teckle.common.CommonProxy;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers(LoaderState.ModState state) {
        if (state == LoaderState.ModState.PREINITIALIZED) {
            registerEntityRenderers();
        }

        if (state == LoaderState.ModState.INITIALIZED) {
            registerTileEntitySpecialRenderers();
            registerItemRenderers();
        }

        if (state == LoaderState.ModState.POSTINITIALIZED) {
            if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ModelItemTube.reloadListener);
            }
        }
    }

    public void registerEntityRenderers() {
    }

    public void registerTileEntitySpecialRenderers() {

    }

    public void registerItemRenderers() {
        if (true)
            return;
        Item itemToRegister = null;
        ModelResourceLocation modelResourceLocation = null;

        ItemModelMesher modelMesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        // Do some general render registrations for OBJECTS, not considering meta.
        for (int i = 0; i < TeckleObjects.registeredBlocks.size(); i++) {
            modelResourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + TeckleObjects.registeredBlocks.keySet().toArray()[i], "inventory");
            itemToRegister = Item.getItemFromBlock((Block) TeckleObjects.registeredBlocks.values().toArray()[i]);

            modelMesher.register(itemToRegister, 0, modelResourceLocation);
        }

        for (int i = 0; i < TeckleObjects.registeredItems.size(); i++) {
            modelResourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + TeckleObjects.registeredItems.keySet().toArray()[i], "inventory");
            itemToRegister = (Item) TeckleObjects.registeredItems.values().toArray()[i];
            modelMesher.register(itemToRegister, 0, modelResourceLocation);
        }
    }

    @SubscribeEvent
    public void onModelBakeEvent(ModelBakeEvent e) {
        ModelItemTube tubeModel = new ModelItemTube();
        e.getModelRegistry().putObject(new ModelResourceLocation("teckle:tube.item", "normal"), tubeModel);
        e.getModelRegistry().putObject(new ModelResourceLocation("teckle:tube.item", "inventory"), tubeModel);
    }
}
