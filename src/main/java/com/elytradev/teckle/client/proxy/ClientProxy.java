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

import com.elytradev.teckle.client.render.model.ModelItemTube;
import com.elytradev.teckle.client.render.tile.TileSortingMachineRender;
import com.elytradev.teckle.client.render.tile.TileTubeRenderer;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.item.ItemIngot;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.elytradev.teckle.common.proxy.CommonProxy;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.repack.concrete.resgen.ConcreteResourcePack;
import com.elytradev.teckle.repack.concrete.resgen.IResourceHolder;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class ClientProxy extends CommonProxy {

    @Override
    public void registerRenderers(LoaderState.ModState state) {
        if (state == LoaderState.ModState.PREINITIALIZED) {
            new ConcreteResourcePack(TeckleMod.MOD_ID);

            registerSpecialItemRenderers();
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

        if (state == LoaderState.ModState.POSTINITIALIZED) {
            if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ModelItemTube.reloadListener);
            }
            if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(resourceManager -> TileTubeRenderer.itemColourModel = null);
            }
        }
    }

    public void registerTileEntitySpecialRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileItemTube.class, new TileTubeRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSortingMachine.class, new TileSortingMachineRender());
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

    public void registerSpecialItemRenderers() {
        for (int i = 0; i < EnumDyeColor.values().length; i++) {
            EnumDyeColor color = EnumDyeColor.byDyeDamage(i);
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemPaintBrush, i, new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "paintbrush_" + color.getName(), "inventory"));
            TeckleMod.LOG.debug("Registering paintbrush model variant: " + i + " " + new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "paintbrush_" + color.getName()), "inventory");
        }

        for (int i = 0; i < ItemSiliconWafer.WaferType.values().length; i++) {
            ItemSiliconWafer.WaferType waferType = ItemSiliconWafer.WaferType.byMetadata(i);

            ModelResourceLocation resourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "siliconwafer" + waferType.getSuffix(), "inventory");
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemSiliconWafer, i, resourceLocation);
            TeckleMod.LOG.debug("Registering wafer model variant: " + i + " " + resourceLocation);
        }

        for (int i = 0; i < ItemIngot.IngotType.values().length; i++) {
            ItemIngot.IngotType ingotType = ItemIngot.IngotType.byMetadata(i);

            ModelResourceLocation resourceLocation = new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "ingot_" + ingotType.getName(), "inventory");
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemIngot, ingotType.getMetadata(), resourceLocation);
            TeckleMod.LOG.info("Registering ingot model variant: " + ingotType.getMetadata() + " " + resourceLocation);
        }
    }

    @SubscribeEvent
    public void onModelBakeEvent(ModelBakeEvent e) {
        ModelItemTube tubeModel = new ModelItemTube();
        e.getModelRegistry().putObject(new ModelResourceLocation("teckle:tube.item", "normal"), tubeModel);

    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre e) {
        e.getMap().registerSprite(new ResourceLocation(TeckleMod.MOD_ID, "blocks/sortingmachineblinkenlights"));
        e.getMap().registerSprite(new ResourceLocation(TeckleMod.MOD_ID, "blocks/sortingmachinemouth"));
    }

    @Override
    public void registerHandlers() {
        super.registerHandlers();
        MinecraftForge.EVENT_BUS.register(ClientTravellerManager.class);
    }
}
