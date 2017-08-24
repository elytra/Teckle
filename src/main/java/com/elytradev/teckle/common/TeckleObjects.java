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
import com.elytradev.teckle.common.crafting.AlloyRecipes;
import com.elytradev.teckle.common.crafting.RecipeSlice;
import com.elytradev.teckle.common.handlers.PaintbrushRecipe;
import com.elytradev.teckle.common.item.ItemBlade;
import com.elytradev.teckle.common.item.ItemIngot;
import com.elytradev.teckle.common.item.ItemPaintbrush;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.elytradev.teckle.common.tile.*;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileFilter;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileItemTube;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileTransposer;
import com.elytradev.teckle.common.tile.retriever.NetworkTileRetrieverInput;
import com.elytradev.teckle.common.tile.retriever.NetworkTileRetrieverOutput;
import com.elytradev.teckle.common.tile.retriever.TileRetriever;
import com.elytradev.teckle.common.tile.sortingmachine.NetworkTileSortingMachineInput;
import com.elytradev.teckle.common.tile.sortingmachine.NetworkTileSortingMachineOutput;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.worldnetwork.common.NetworkTileRegistry;
import com.elytradev.teckle.common.worldnetwork.common.node.NetworkNodeRegistry;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.*;

import static com.elytradev.teckle.common.TeckleMod.MOD_ID;

/**
 * Block registration is here, to keep the mod class nice and small.
 */

public class TeckleObjects {

    public static BlockItemTube blockItemTube;
    public static BlockFilter blockFilter;
    public static BlockTransposer blockTransposer;
    public static BlockSortingMachine blockSortingMachine;
    public static BlockRetriever blockRetriever;
    public static BlockAlloyFurnace blockAlloyFurnace;
    public static BlockNikoliteOre blockNikoliteOre;
    public static BlockFabricator blockFabricator;

    public static ItemPaintbrush itemPaintBrush;
    public static ItemBlade itemBlade;
    public static Item itemNikolite;
    public static Item itemSiliconBoule;
    public static ItemSiliconWafer itemSiliconWafer;
    public static ItemIngot itemIngot;

    public static CreativeTabs creativeTab = new CreativeTabs(MOD_ID) {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(itemPaintBrush, 1, new Random().nextInt(15));
        }
    };

    public static HashMap<String, Block> registeredBlocks;
    public static HashMap<String, Item> registeredItems;

    public static List<Object> skipItemMesh;
    public static String REGISTRY_PREFIX = MOD_ID.toLowerCase();
    private static List<Item> itemBlocksToRegister;
    private int recipeID = 0;

    public void preInit(FMLPreInitializationEvent e) {
        NetworkTileRegistry.setup();
        NetworkNodeRegistry.setup();
    }

    public void init(FMLInitializationEvent e) {
        GameRegistry.registerTileEntity(TileItemTube.class, "teckleItemTube");
        GameRegistry.registerTileEntity(TileFilter.class, "teckleFilter");
        GameRegistry.registerTileEntity(TileTransposer.class, "teckleTransposer");
        GameRegistry.registerTileEntity(TileSortingMachine.class, "teckleSortingMachine");
        GameRegistry.registerTileEntity(TileFabricator.class, "teckleFabricator");
        GameRegistry.registerTileEntity(TileAlloyFurnace.class, "teckleAlloyFurnace");
        GameRegistry.registerTileEntity(TileRetriever.class, "teckleRetriever");

        NetworkTileRegistry.registerNetworkTile("teckle", "itemTube", NetworkTileItemTube.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "filter", NetworkTileFilter.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "transposer", NetworkTileTransposer.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "sortingmachineIn", NetworkTileSortingMachineInput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "sortingmachineOut", NetworkTileSortingMachineOutput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "retrieverIn",  NetworkTileRetrieverInput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "retrieverOut", NetworkTileRetrieverOutput.class);
        NetworkNodeRegistry.registerNetworkNode("teckle", "itemendpoint", ItemNetworkEndpoint.class);

        // Crash if there's missing ores.
        boolean foundSilver = !OreDictionary.getOres("ingotSilver").isEmpty();
        boolean foundTin = !OreDictionary.getOres("ingotTin").isEmpty();
        boolean foundCopper = !OreDictionary.getOres("ingotCopper").isEmpty();

        boolean foundMatchingOres = foundSilver && foundTin && foundCopper;
        if (!foundMatchingOres && !TeckleMod.INDEV && !TeckleMod.CONFIG.skipOreChecks) {
            String additionalData = (foundSilver ? "Found " : "Couldn't find ") + "silver ingots. ";
            additionalData += (foundTin ? "Found " : "Couldn't find ") + "tin ingots. ";
            additionalData += (foundCopper ? "Found " : "Couldn't find ") + "copper ingots.";
            TeckleMod.PROXY.handleMissingOres("Teckle is missing ores, Tin, Silver, and Copper must be present to run. " + additionalData);
        }
    }

    public void postInit(FMLPostInitializationEvent e) {
    }

    @SubscribeEvent
    public void onRecipeRegisterEvent(RegistryEvent.Register<IRecipe> event) {
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        registry.register(new PaintbrushRecipe().setRegistryName(new ResourceLocation(TeckleMod.MOD_ID, "paintbrushrecipe")));
        registry.register(new RecipeSlice(new ItemStack(TeckleObjects.itemSiliconWafer, 16), 1, itemSiliconBoule).
                setRegistryName(new ResourceLocation(TeckleMod.MOD_ID, "waferslicerecipe")));
        registerShapedRecipe(registry, new ItemStack(blockAlloyFurnace), "BBB", "B B", "BBB",
                'B', Blocks.BRICK_BLOCK);
        registerShapedRecipe(registry, new ItemStack(blockItemTube, 8), "BGB",
                'B', new ItemStack(itemIngot, 1, ItemIngot.IngotType.BRASS.getMetadata()),
                'G', Blocks.GLASS);
        registerShapedRecipe(registry, new ItemStack(blockTransposer), "CCC", "WPW", "CRC",
                'C', Blocks.COBBLESTONE,
                'W', Blocks.PLANKS,
                'P', Blocks.PISTON,
                'R', Items.REDSTONE);
        registerShapedRecipe(registry, new ItemStack(blockFilter), "CCC", "GPG", "CWC",
                'C', Blocks.COBBLESTONE,
                'G', Items.GOLD_INGOT,
                'P', Blocks.PISTON,
                'W', new ItemStack(itemSiliconWafer, 1, ItemSiliconWafer.WaferType.RED.getMetadata()));
        registerShapedRecipe(registry, new ItemStack(blockFabricator), "BBB", "WCW", "WRW",
                'B', new ItemStack(itemIngot, 1, ItemIngot.IngotType.BRASS.getMetadata()),
                'W', Blocks.PLANKS,
                'C', Blocks.CRAFTING_TABLE,
                'R', Items.REDSTONE);
        registerShapedRecipe(registry, new ItemStack(itemBlade), "I  ", " S ",
                'I', Items.IRON_INGOT,
                'S', Items.STICK);
        registerShapedRecipe(registry, new ItemStack(blockSortingMachine), "IWI", "BFB", "IRI",
                'W', new ItemStack(itemSiliconWafer, 1, ItemSiliconWafer.WaferType.BLUE.getMetadata()),
                'I', new ItemStack(Items.IRON_INGOT),
                'B', new ItemStack(itemIngot, 1, ItemIngot.IngotType.BLUE_ALLOY.getMetadata()),
                'R', new ItemStack(itemSiliconWafer, 1, ItemSiliconWafer.WaferType.RED.getMetadata()),
                'F', new ItemStack(blockFilter));

        for (EnumDyeColor color : EnumDyeColor.values()) {
            registerShapedRecipe(registry, new ItemStack(itemPaintBrush, 1, color.getDyeDamage()),
                    "D  ", " W ", "  S", 'D', "dye" + color.getUnlocalizedName().substring(0, 1).toUpperCase() + color.getUnlocalizedName().substring(1), 'S', "stickWood", 'W', new ItemStack(Blocks.WOOL));
        }
        AlloyRecipes.getInstance().init();
    }

    @SubscribeEvent
    public void onBlockRegisterEvent(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        registeredBlocks = new HashMap<>();
        itemBlocksToRegister = new ArrayList<>();

        blockItemTube = new BlockItemTube(Material.IRON);
        registerBlock(registry, "tube.item", blockItemTube);

        blockFilter = new BlockFilter(Material.ROCK);
        registerBlock(registry, "filter", blockFilter);

        blockTransposer = new BlockTransposer(Material.ROCK);
        registerBlock(registry, "transposer", blockTransposer);

        blockSortingMachine = new BlockSortingMachine(Material.IRON);
        registerBlock(registry, "sortingmachine", blockSortingMachine);

        blockRetriever = new BlockRetriever(Material.IRON);
        registerBlock(registry, "retriever", blockRetriever);

        blockAlloyFurnace = new BlockAlloyFurnace(Material.ROCK);
        registerBlock(registry, "alloyfurnace", blockAlloyFurnace);

        blockFabricator = new BlockFabricator(Material.IRON);
        registerBlock(registry, "fabricator", blockFabricator);

        blockNikoliteOre = new BlockNikoliteOre();
        registerBlock(registry, "nikolite_ore", blockNikoliteOre);
    }

    @SubscribeEvent
    public void onItemRegisterEvent(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registeredItems = new HashMap<>();
        skipItemMesh = new ArrayList<>();

        itemPaintBrush = new ItemPaintbrush();
        registerItem(registry, "paintbrush", itemPaintBrush);
        skipItemMesh.add(itemPaintBrush);

        itemBlade = new ItemBlade();
        registerItem(registry, "blade", itemBlade);

        itemNikolite = new Item();
        registerItem(registry, "nikolite", itemNikolite);

        itemSiliconBoule = new Item();
        registerItem(registry, "siliconboule", itemSiliconBoule);

        itemSiliconWafer = new ItemSiliconWafer();
        registerItem(registry, "siliconwafer", itemSiliconWafer);
        skipItemMesh.add(itemSiliconWafer);

        itemIngot = new ItemIngot();
        registerItem(registry, "ingot", itemIngot);
        Arrays.stream(ItemIngot.IngotType.values()).forEach(ingotType ->
                OreDictionary.registerOre(ingotType.getOreName(),
                        new ItemStack(itemIngot, 1, ingotType.getMetadata())));
        skipItemMesh.add(itemIngot);
        itemBlocksToRegister.forEach(registry::register);

        OreDictionary.registerOre("coal", Items.COAL); // Nothing to see here, move along.
        OreDictionary.registerOre("dustNikolite", new ItemStack(itemNikolite, 1));
        OreDictionary.registerOre("dyeBlue", new ItemStack(itemNikolite, 1));
        OreDictionary.registerOre("oreNikolite", new ItemStack(blockNikoliteOre, 1));
    }

    @SubscribeEvent
    public void onMissingMappings(RegistryEvent.MissingMappings<Item> e) {
        for (RegistryEvent.MissingMappings.Mapping<Item> itemMapping : e.getAllMappings()) {
            if (itemMapping.key.equals(new ResourceLocation("teckle:brassingot"))) {
                itemMapping.remap(TeckleObjects.itemIngot);
            }
        }
    }

    private void registerShapedRecipe(IForgeRegistry<IRecipe> registry, ItemStack out, Object... input) {
        ResourceLocation resourceLocation = new ResourceLocation(MOD_ID, out.getUnlocalizedName() + recipeID++);
        registry.register(new ShapedOreRecipe(resourceLocation, out, input).setRegistryName(resourceLocation));
    }

    private void registerShapelessRecipe(IForgeRegistry<IRecipe> registry, ItemStack out, Object... input) {
        ResourceLocation resourceLocation = new ResourceLocation(MOD_ID, out.getUnlocalizedName() + recipeID++);
        registry.register(new ShapelessOreRecipe(resourceLocation, out, input).setRegistryName(resourceLocation));
    }

    private void registerBlock(IForgeRegistry<Block> registry, String id, Block block) {
        registerBlock(registry, id, block, true);
    }

    private void registerBlock(IForgeRegistry<Block> registry, String id, Block block, boolean withItemBlock) {
        block.setUnlocalizedName("teckle." + id);
        block.setRegistryName(REGISTRY_PREFIX, id);
        block.setCreativeTab(creativeTab);
        registry.register(block);
        if (withItemBlock)
            itemBlocksToRegister.add(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        TeckleObjects.registeredBlocks.put(id, block);
    }

    private void registerBlock(IForgeRegistry<Block> registry, String id, Block block, Class<? extends ItemBlock> itemBlockClass) {
        try {
            block.setUnlocalizedName("teckle." + id);
            block.setRegistryName(REGISTRY_PREFIX, id);
            registry.register(block);

            ItemBlock itemBlock = itemBlockClass.getDeclaredConstructor(Block.class).newInstance(block);
            itemBlock.setRegistryName(REGISTRY_PREFIX, id);
            itemBlock.setCreativeTab(creativeTab);
            itemBlocksToRegister.add(itemBlock);
            TeckleObjects.registeredBlocks.put(id, block);
        } catch (Exception e) {
            TeckleMod.LOG.error("Caught exception while registering " + block, e);
        }
    }

    private void registerItem(IForgeRegistry<Item> registry, String id, Item item) {
        item.setUnlocalizedName("teckle." + id);
        item.setRegistryName(REGISTRY_PREFIX, id);
        item.setCreativeTab(creativeTab);
        registry.register(item);
        TeckleObjects.registeredItems.put(id, item);
    }


}