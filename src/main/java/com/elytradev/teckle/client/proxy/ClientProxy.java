package com.elytradev.teckle.client.proxy;

import com.elytradev.teckle.client.render.model.ModelItemTube;
import com.elytradev.teckle.client.render.tile.TileTubeRenderer;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.proxy.CommonProxy;
import com.elytradev.teckle.common.tile.TileItemTube;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelBakeEvent;
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
            registerSpecialItemRenderers();
        }

        if (state == LoaderState.ModState.INITIALIZED) {
            registerTileEntitySpecialRenderers();
            registerItemRenderers();

            Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((blockState, worldIn, pos, tintIndex) -> {
                if (tintIndex == 1) {
                    IExtendedBlockState extendedBlockState = (IExtendedBlockState) blockState;
                    if (extendedBlockState.getValue(BlockItemTube.COLOUR) != null) {
                        return extendedBlockState.getValue(BlockItemTube.COLOUR).getMapColor().colorValue;
                    }

                    return 11696387;
                }

                return -1;
            }, TeckleObjects.blockItemTube);

            Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new IItemColor() {
                @Override
                public int getColorFromItemstack(ItemStack stack, int tintIndex) {
                    return 11696387;
                }
            }, TeckleObjects.blockItemTube);
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
            modelMesher.register(itemToRegister, 0, modelResourceLocation);
        }
    }

    public void registerSpecialItemRenderers() {
        for (int i = 0; i < EnumDyeColor.values().length; i++) {
            EnumDyeColor color = EnumDyeColor.byDyeDamage(i);
            ModelLoader.setCustomModelResourceLocation(TeckleObjects.itemPaintBrush, i, new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "paintbrush_" + color.getName()));
            TeckleMod.LOG.info("Registering paintbrush model variant: " + i + " " + new ModelResourceLocation(TeckleMod.RESOURCE_DOMAIN + "paintbrush_" + color.getName()));
        }
    }

    @SubscribeEvent
    public void onModelBakeEvent(ModelBakeEvent e) {
        ModelItemTube tubeModel = new ModelItemTube();
        e.getModelRegistry().putObject(new ModelResourceLocation("teckle:tube.item", "normal"), tubeModel);
    }

    @Override
    public void registerHandlers() {
        super.registerHandlers();
        MinecraftForge.EVENT_BUS.register(ClientTravellerManager.class);
    }
}
