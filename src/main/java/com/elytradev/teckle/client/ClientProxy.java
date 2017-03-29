package com.elytradev.teckle.client;

import com.elytradev.teckle.common.CommonProxy;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.LoaderState;

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

        }
    }

    public void registerEntityRenderers() {
    }

    public void registerTileEntitySpecialRenderers() {

    }

    public void registerItemRenderers() {
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
}
