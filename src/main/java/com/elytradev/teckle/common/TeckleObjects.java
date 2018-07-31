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
import com.elytradev.teckle.common.item.*;
import com.elytradev.teckle.common.tile.*;
import com.elytradev.teckle.common.tile.networktiles.*;
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
import net.minecraftforge.common.MinecraftForge;
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
    public static BlockCompacted blockCompacted;
    public static BlockFabricator blockFabricator;
    public static BlockBeamQuarry blockBeamQuarry;
    public static BlockBreaker blockBreaker;

    public static ItemPaintbrush itemPaintBrush;
    public static ItemBlade itemBlade;
    public static Item itemNikolite;
    public static Item itemSiliconBoule;
    public static ItemSiliconWafer itemSiliconWafer;
    public static ItemIngot itemIngot;
    public static Item elytraLogo;

    public static CreativeTabs creativeTab = new CreativeTabs(MOD_ID) {

        @Override
        public ItemStack createIcon() {
            return new ItemStack(itemPaintBrush, 1, new Random().nextInt(15));
        }
    };
    public static HashMap<String, Block> registeredBlocks;
    public static HashMap<String, Item> registeredItems;
    public static List<Object> skipItemMesh;
    public static String REGISTRY_PREFIX = MOD_ID.toLowerCase();
    private static List<Item> itemBlocksToRegister;
    private int recipeID = 0;

    public TeckleObjects() {
        MinecraftForge.EVENT_BUS.register(this);
    }

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
        GameRegistry.registerTileEntity(TileBeamQuarry.class, "teckleBeamQuarry");
        GameRegistry.registerTileEntity(TileBreaker.class, "teckleBreaker");

        NetworkTileRegistry.registerNetworkTile("teckle", "itemTube", NetworkTileItemTube.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "filter", NetworkTileFilter.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "transposer", NetworkTileTransposer.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "sortingmachineIn", NetworkTileSortingMachineInput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "sortingmachineOut", NetworkTileSortingMachineOutput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "retrieverIn", NetworkTileRetrieverInput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "retrieverOut", NetworkTileRetrieverOutput.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "beamquarry", NetworkTileBeamQuarry.class);
        NetworkTileRegistry.registerNetworkTile("teckle", "breaker", NetworkTileBreaker.class);
        NetworkNodeRegistry.registerNetworkNode("teckle", "itemendpoint", ItemNetworkEndpoint.class);

        // Crash if there's missing ores.
        boolean foundSilver = !OreDictionary.getOres("ingotSilver").isEmpty();
        boolean foundTin = !OreDictionary.getOres("ingotTin").isEmpty();
        boolean foundCopper = !OreDictionary.getOres("ingotCopper").isEmpty();

        boolean foundMatchingOres = foundSilver && foundTin && foundCopper;
        if (!foundMatchingOres && !TeckleMod.INDEV && !TeckleMod.CONFIG.skipOreChecks) {
            String additionalData = "\n" + "Silver ingots " + (foundSilver ? "✔" : "✘");
            additionalData += "\n" + "Tin ingots " + (foundTin ? "✔" : "✘");
            additionalData += "\n" + "Copper ingots " + (foundCopper ? "✔" : "✘");
            TeckleMod.PROXY.handleMissingOres("Tin, Silver, and Copper must be present to run. " + additionalData);
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


        // hmmmmmm :thinking:
        registerShapedRecipe(registry, new ItemStack(blockBeamQuarry), "OWO", "IXI", "OWO",
                'O', "blockBlueAlloy",
                'W', "blockRedAlloy",
                'X', new ItemStack(Items.DIAMOND_PICKAXE),
                'I', "ingotBrass");

        for (EnumDyeColor color : EnumDyeColor.values()) {
            String dyeName = "dye" + color.getTranslationKey().substring(0, 1).toUpperCase() + color.getTranslationKey().substring(1);
            if (color == EnumDyeColor.SILVER)
                dyeName = "dyeLightGray";

            registerShapedRecipe(registry, new ItemStack(itemPaintBrush, 1, color.getDyeDamage()),
                    "D  ", " W ", "  S", 'D', dyeName, 'S', "stickWood", 'W', new ItemStack(Blocks.WOOL));
        }
        AlloyRecipes.getInstance().init();

        registerCompactedIngotRecipe(registry, ItemIngot.IngotType.RED_ALLOY);
        registerCompactedIngotRecipe(registry, ItemIngot.IngotType.BLUE_ALLOY);
    }

    private void registerCompactedIngotRecipe(IForgeRegistry<IRecipe> registry, ItemIngot.IngotType ingotType) {
        registerShapedRecipe(registry,
                new ItemStack(blockCompacted, 1, ingotType.getMetadata()),
                "XXX",
                "XXX",
                "XXX",
                'X', new ItemStack(itemIngot, 1, ingotType.getMetadata()));

        registerShapelessRecipe(registry,
                new ItemStack(itemIngot, 9, ingotType.getMetadata()),
                new ItemStack(blockCompacted, 1, ingotType.getMetadata()));
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

        blockBeamQuarry = new BlockBeamQuarry(Material.IRON);
        registerBlock(registry, "beamquarry", blockBeamQuarry);

        blockBreaker = new BlockBreaker(Material.ROCK);
        registerBlock(registry, "breaker", blockBreaker);

        blockCompacted = new BlockCompacted(Material.IRON);
        registerBlock(registry, "compacted_metal", blockCompacted, false);
        ItemCompacted itemCompacted = new ItemCompacted(blockCompacted, true);
        itemCompacted.setSubtypeNames(new String[]{"brass", "redalloy", "bluealloy"});
        itemCompacted.setRegistryName(MOD_ID, "compacted_metal");
        itemCompacted.setCreativeTab(creativeTab);
        itemCompacted.setTranslationKey("teckle.compacted_metal");
        itemBlocksToRegister.add(itemCompacted);
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

        elytraLogo = new Item();
        registerItem(registry, "elytralogo", elytraLogo);

        itemIngot = new ItemIngot();
        registerItem(registry, "ingot", itemIngot);

        skipItemMesh.add(itemIngot);
        itemBlocksToRegister.forEach(registry::register);

        OreDictionary.registerOre("coal", Items.COAL); // Nothing to see here, move along.
        OreDictionary.registerOre("dustNikolite", new ItemStack(itemNikolite, 1));
        OreDictionary.registerOre("dyeCyan", new ItemStack(itemNikolite, 1));
        OreDictionary.registerOre("oreNikolite", new ItemStack(blockNikoliteOre, 1));
        Arrays.stream(ItemIngot.IngotType.values()).forEach(ingotType -> {
            OreDictionary.registerOre(ingotType.getOreName(),
                    new ItemStack(itemIngot, 1, ingotType.getMetadata()));

            if (ingotType != ItemIngot.IngotType.BRASS) {
                OreDictionary.registerOre(ingotType.getOreName().replace("ingot", "block"),
                        new ItemStack(blockCompacted, 1, ingotType.getMetadata()));
            }
        });
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
        ResourceLocation resourceLocation = new ResourceLocation(MOD_ID, out.getTranslationKey() + recipeID++);
        registry.register(new ShapedOreRecipe(resourceLocation, out, input).setRegistryName(resourceLocation));
    }

    private void registerShapelessRecipe(IForgeRegistry<IRecipe> registry, ItemStack out, Object... input) {
        ResourceLocation resourceLocation = new ResourceLocation(MOD_ID, out.getTranslationKey() + recipeID++);
        registry.register(new ShapelessOreRecipe(resourceLocation, out, input).setRegistryName(resourceLocation));
    }

    private void registerBlock(IForgeRegistry<Block> registry, String id, Block block) {
        registerBlock(registry, id, block, true);
    }

    private void registerBlock(IForgeRegistry<Block> registry, String id, Block block, boolean withItemBlock) {
        block.setTranslationKey("teckle." + id);
        block.setRegistryName(REGISTRY_PREFIX, id);
        block.setCreativeTab(creativeTab);
        registry.register(block);
        if (withItemBlock)
            itemBlocksToRegister.add(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        TeckleObjects.registeredBlocks.put(id, block);
    }

    private void registerBlock(IForgeRegistry<Block> registry, String id, Block block, Class<? extends ItemBlock> itemBlockClass) {
        try {
            block.setTranslationKey("teckle." + id);
            block.setRegistryName(REGISTRY_PREFIX, id);
            registry.register(block);

            ItemBlock itemBlock = itemBlockClass.getDeclaredConstructor(Block.class).newInstance(block);
            itemBlock.setRegistryName(REGISTRY_PREFIX, id);
            itemBlock.setCreativeTab(creativeTab);
            itemBlocksToRegister.add(itemBlock);
            TeckleObjects.registeredBlocks.put(id, block);
        } catch (Exception e) {
            TeckleLog.error("Caught exception while registering " + block, e);
        }
    }

    private void registerItem(IForgeRegistry<Item> registry, String id, Item item) {
        item.setTranslationKey("teckle." + id);
        item.setRegistryName(REGISTRY_PREFIX, id);
        item.setCreativeTab(creativeTab);
        registry.register(item);
        TeckleObjects.registeredItems.put(id, item);
    }


}