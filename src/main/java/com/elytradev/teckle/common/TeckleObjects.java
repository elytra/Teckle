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

package com.elytradev.teckle.common;

import com.elytradev.teckle.common.block.*;
import com.elytradev.teckle.common.crafting.RecipeSlice;
import com.elytradev.teckle.common.handlers.PaintbrushRecipe;
import com.elytradev.teckle.common.item.ItemBlade;
import com.elytradev.teckle.common.item.ItemPaintbrush;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.elytradev.teckle.common.tile.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Block registration is here, to keep the mod class nice and small.
 */

public class TeckleObjects {

    public static BlockItemTube blockItemTube;
    public static BlockFilter blockFilter;
    public static BlockTransposer blockTransposer;
    public static BlockAlloyFurnace blockAlloyFurnace;
    public static BlockNikoliteOre blockNikoliteOre;
    public static BlockFabricator blockFabricator;

    public static ItemPaintbrush itemPaintBrush;
    public static ItemBlade itemBlade;
    public static Item itemNikolite;
    public static Item itemSiliconBoule;
    public static Item itemBrassIngot;
    public static ItemSiliconWafer itemSiliconWafer;

    public static CreativeTabs creativeTab = new CreativeTabs(TeckleMod.MOD_ID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(itemPaintBrush, 1, new Random().nextInt(15));
        }
    };

    public static HashMap<String, Block> registeredBlocks;
    public static HashMap<String, Item> registeredItems;

    public static List<Object> skipItemMesh;

    public static String REGISTRY_PREFIX = TeckleMod.MOD_ID.toLowerCase();

    public void preInit(FMLPreInitializationEvent e) {
        registeredBlocks = new HashMap<>();
        registeredItems = new HashMap<>();
        skipItemMesh = new ArrayList<>();

        blockItemTube = new BlockItemTube(Material.CIRCUITS);
        registerBlock("tube.item", blockItemTube);

        blockFilter = new BlockFilter(Material.CIRCUITS);
        registerBlock("filter", blockFilter);

        blockTransposer = new BlockTransposer(Material.CIRCUITS);
        registerBlock("transposer", blockTransposer);

        blockAlloyFurnace = new BlockAlloyFurnace(Material.ROCK);
        registerBlock("alloyfurnace", blockAlloyFurnace);

        blockFabricator = new BlockFabricator(Material.CIRCUITS);
        registerBlock("fabricator", blockFabricator);

        blockNikoliteOre = new BlockNikoliteOre();
        registerBlock("nikolite_ore", blockNikoliteOre);
        OreDictionary.registerOre("oreNikolite", new ItemStack(blockNikoliteOre, 1));

        itemPaintBrush = new ItemPaintbrush();
        registerItem("paintbrush", itemPaintBrush);
        skipItemMesh.add(itemPaintBrush);

        itemBlade = new ItemBlade();
        registerItem("blade", itemBlade);

        itemNikolite = new Item();
        registerItem("nikolite", itemNikolite);
        OreDictionary.registerOre("dustNikolite", new ItemStack(itemNikolite, 1));

        itemSiliconBoule = new Item();
        registerItem("siliconboule", itemSiliconBoule);

        itemSiliconWafer = new ItemSiliconWafer();
        registerItem("siliconwafer", itemSiliconWafer);

        itemBrassIngot = new Item();
        registerItem("brassingot", itemBrassIngot);
        OreDictionary.registerOre("ingotBrass", new ItemStack(itemBrassIngot, 1));

        skipItemMesh.add(itemSiliconWafer);
    }

    public void init(FMLInitializationEvent e) {
        OreDictionary.registerOre("coal", Items.COAL); // Nothing to see here, move along.

        GameRegistry.registerTileEntity(TileItemTube.class, "teckleItemTube");
        GameRegistry.registerTileEntity(TileFilter.class, "teckleFilter");
        GameRegistry.registerTileEntity(TileTransposer.class, "teckleTransposer");
        GameRegistry.registerTileEntity(TileFabricator.class, "teckleFabricator");
        GameRegistry.registerTileEntity(TileAlloyFurnace.class, "teckleAlloyFurnace");

        GameRegistry.addRecipe(new PaintbrushRecipe());
        GameRegistry.addRecipe(new RecipeSlice(new ItemStack(TeckleObjects.itemSiliconWafer, 16), 1, itemSiliconBoule));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockAlloyFurnace), "BBB", "B B", "BBB", 'B', Blocks.BRICK_BLOCK));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockItemTube, 8), "BGB", 'B', itemBrassIngot, 'G', Blocks.GLASS));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockTransposer), "CCC", "WPW", "CRC", 'C', Blocks.COBBLESTONE, 'W', Blocks.PLANKS, 'P', Blocks.PISTON, 'R', Items.REDSTONE));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockFilter), "CCC", "GPG", "CWC", 'C', Blocks.COBBLESTONE, 'G', Items.GOLD_INGOT, 'P', Blocks.PISTON, 'W', new ItemStack(itemSiliconWafer, 1, ItemSiliconWafer.WaferType.RED.getMetadata())));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockFabricator), "BBB", "WCW", "WRW", 'B', itemBrassIngot, 'W', Blocks.PLANKS, 'C', Blocks.CRAFTING_TABLE, 'R', Items.REDSTONE));
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(itemBlade), "I  ", " S ", 'I', Items.IRON_INGOT, 'S', Items.STICK));


        // Forge doesn't use EnumDyeColor  for dye registration and also doesn't store this list anywhere public, so here we are copying forge colour arrays from OreDict.
        String[] dyes =
                {
                        "Black",
                        "Red",
                        "Green",
                        "Brown",
                        "Blue",
                        "Purple",
                        "Cyan",
                        "LightGray",
                        "Gray",
                        "Pink",
                        "Lime",
                        "Yellow",
                        "LightBlue",
                        "Magenta",
                        "Orange",
                        "White"
                };
        for (int i = 0; i < dyes.length; i++) {
            GameRegistry.addShapedRecipe(new ItemStack(itemPaintBrush, 1, i),
                    "D  ", " W ", "  S", 'D', "dye" + dyes[i], 'S', "stickWood", 'W', new ItemStack(Blocks.WOOL));
        }

    }

    public void postInit(FMLPostInitializationEvent e) {
    }

    private void registerBlock(String id, Block block) {
        registerBlock(id, block, true);
    }

    private void registerBlock(String id, Block block, boolean withItemBlock) {
        block.setUnlocalizedName("teckle." + id);
        block.setRegistryName(REGISTRY_PREFIX, id);
        block.setCreativeTab(creativeTab);
        GameRegistry.register(block);
        if (withItemBlock)
            GameRegistry.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        TeckleObjects.registeredBlocks.put(id, block);
    }

    private void registerBlock(String id, Block block, Class<? extends ItemBlock> itemBlockClass) {
        try {
            block.setUnlocalizedName("teckle." + id);
            block.setRegistryName(REGISTRY_PREFIX, id);
            GameRegistry.register(block);

            ItemBlock itemBlock = itemBlockClass.getDeclaredConstructor(Block.class).newInstance(block);
            itemBlock.setRegistryName(REGISTRY_PREFIX, id);
            itemBlock.setCreativeTab(creativeTab);
            GameRegistry.register(itemBlock);
            TeckleObjects.registeredBlocks.put(id, block);
        } catch (Exception e) {
            TeckleMod.LOG.error("Caught exception while registering " + block, e);
        }
    }

    private void registerItem(String id, Item item) {
        item.setUnlocalizedName("teckle." + id);
        item.setRegistryName(REGISTRY_PREFIX, id);
        item.setCreativeTab(creativeTab);
        GameRegistry.register(item);
        TeckleObjects.registeredItems.put(id, item);
    }
}