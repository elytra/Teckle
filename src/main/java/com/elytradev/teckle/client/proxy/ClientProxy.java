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

package com.elytradev.teckle.client.proxy;

import com.elytradev.concrete.resgen.ConcreteResourcePack;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.client.MissingOreExceptionClient;
import com.elytradev.teckle.client.render.model.ModelMachineOverlay;
import com.elytradev.teckle.client.render.model.TubeModelLoader;
import com.elytradev.teckle.client.render.tile.TileRetrieverRender;
import com.elytradev.teckle.client.render.tile.TileSortingMachineRender;
import com.elytradev.teckle.client.render.tile.TileTubeRenderer;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.*;
import com.elytradev.teckle.common.exception.MissingOreException;
import com.elytradev.teckle.common.item.ItemIngot;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.elytradev.teckle.common.proxy.CommonProxy;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;


public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers(LoaderState.ModState state) {
        if (state == LoaderState.ModState.PREINITIALIZED) {
            ModelLoaderRegistry.registerLoader(new TubeModelLoader());
            new ConcreteResourcePack(TeckleMod.MOD_ID);
        }

        if (state == LoaderState.ModState.INITIALIZED) {
            registerTileEntitySpecialRenderers();
            registerItemRenderers();

            Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((blockState, worldIn, pos, tintIndex) -> {
                if (tintIndex == 1) {
                    IExtendedBlockState extendedBlockState = (IExtendedBlockState) blockState;
                    if (extendedBlockState.getValue(BlockItemTube.COLOUR) != null) {
                        return extendedBlockState.getValue(BlockItemTube.COLOUR).getColorValue();
                    }

                    return 11696387;
                }

                return -1;
            }, TeckleObjects.blockItemTube);

            Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) -> 11696387, TeckleObjects.blockItemTube);
        }
    }

    public void registerTileEntitySpecialRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileItemTube.class, new TileTubeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSortingMachine.class, new TileSortingMachineRender());
        ClientRegistry.bindTileEntitySpecialRenderer(TileRetriever.class, new TileRetrieverRender());
    }

    public void registerItemRenderers() {
        Item itemToRegister = null;
        ModelResourceLocation modelResourceLocation = null;

        // Do some general render registrations for OBJECTS, not considering meta.
        ItemModelMesher modelMesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        for (int i = 0; i < TeckleObjects.registeredBlocks.size(); i++) {
            modelResourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + TeckleObjects.registeredBlocks.keySet().toArray()[i], "inventory");
            itemToRegister = Item.getItemFromBlock((Block) TeckleObjects.registeredBlocks.values().toArray()[i]);

            modelMesher.register(itemToRegister, 0, modelResourceLocation);
        }

        for (int i = 0; i < TeckleObjects.registeredItems.size(); i++) {
            modelResourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + TeckleObjects.registeredItems.keySet().toArray()[i], "inventory");
            itemToRegister = (Item) TeckleObjects.registeredItems.values().toArray()[i];
            if (TeckleObjects.skipItemMesh.contains(itemToRegister) || itemToRegister instanceof IResourceHolder)
                continue;
            modelMesher.register(itemToRegister, 0, modelResourceLocation);
        }
    }

    @SubscribeEvent
    public void onModelBakeEvent(ModelBakeEvent e) {
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(resourceManager -> TileTubeRenderer.itemColourModel = null);
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ModelMachineOverlay.reloadListener);
        }
        try {
            IModel nodeModel = ModelLoaderRegistry.getModel(new ResourceLocation("teckle", "block/tube.item_node"));
            IBakedModel bakedModel = nodeModel.bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
            e.getModelRegistry().putObject(new ModelResourceLocation("teckle:tube.item", "inventory"), bakedModel);
        } catch (Exception exception) {
            TeckleLog.error("Failed to load node model for item tube item.", exception);
        }
    }

    @SubscribeEvent
    public void onModelRegistryEvent(ModelRegistryEvent event) {
        for (int i = 0; i < EnumDyeColor.values().length; i++) {
            EnumDyeColor color = EnumDyeColor.byDyeDamage(i);
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemPaintBrush, i, new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "paintbrush_" + color.getName(), "inventory"));
            TeckleLog.debug("Registering paintbrush model variant: " + i + " " + new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "paintbrush_" + color.getName()), "inventory");
        }

        for (int i = 0; i < ItemSiliconWafer.WaferType.values().length; i++) {
            ItemSiliconWafer.WaferType waferType = ItemSiliconWafer.WaferType.byMetadata(i);

            ModelResourceLocation resourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "siliconwafer" + waferType.getSuffix(), "inventory");
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemSiliconWafer, i, resourceLocation);
            TeckleLog.debug("Registering wafer model variant: " + i + " " + resourceLocation);
        }

        for (int i = 0; i < ItemIngot.IngotType.values().length; i++) {
            ItemIngot.IngotType ingotType = ItemIngot.IngotType.byMetadata(i);

            ModelResourceLocation resourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "ingot_" + ingotType.getName(), "inventory");
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemIngot, ingotType.getMetadata(), resourceLocation);
            TeckleLog.debug("Registering ingot model variant: " + ingotType.getMetadata() + " " + resourceLocation);
        }

        for (int i = 0; i < ItemIngot.IngotType.values().length; i++) {
            ItemIngot.IngotType ingotType = ItemIngot.IngotType.values()[i];

            ModelResourceLocation resourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + ingotType.getName(), "inventory");
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(TeckleObjects.blockCompacted), i, resourceLocation);
        }
    }

    private static final List<ResourceLocation> SPRITES = Lists.newArrayList(
            new ResourceLocation(TeckleMod.MOD_ID, "blocks/sortingmachineside_on"),
            new ResourceLocation(TeckleMod.MOD_ID, "blocks/sortingmachineside_blocked"),
            new ResourceLocation(TeckleMod.MOD_ID, "blocks/retrieverside_on"),
            new ResourceLocation(TeckleMod.MOD_ID, "blocks/retrieverside_blocked")
    );

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre e) {
        SPRITES.forEach(e.getMap()::registerSprite);
    }

    @Override
    public void registerHandlers() {
        super.registerHandlers();
        MinecraftForge.EVENT_BUS.register(ClientTravellerManager.class);
    }

    @Override
    public void handleMissingOres(String m) {
        throw new MissingOreExceptionClient(m, new MissingOreException(m.replaceAll("\n", "")));
    }
}
