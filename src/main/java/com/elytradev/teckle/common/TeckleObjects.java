package com.elytradev.teckle.common;

import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.handlers.PaintbrushRecipe;
import com.elytradev.teckle.common.item.ItemPaintbrush;
import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.TileItemTube;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

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
    public static ItemPaintbrush itemPaintBrush;
    public static Item itemSiliconBoule;

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
        registerBlock("tube.item", blockItemTube, true);

        blockFilter = new BlockFilter(Material.CIRCUITS);
        registerBlock("filter", blockFilter, true);

        itemPaintBrush = new ItemPaintbrush();
        registerItem("paintbrush", itemPaintBrush);
        skipItemMesh.add(itemPaintBrush);

        itemSiliconBoule = new Item();
        registerItem("siliconboule", itemSiliconBoule);
    }

    public void init(FMLInitializationEvent e) {
        GameRegistry.registerTileEntity(TileItemTube.class, "teckleItemTube");
        GameRegistry.registerTileEntity(TileFilter.class, "teckleFilter");

        CraftingManager.getInstance().addRecipe(new PaintbrushRecipe());

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
            CraftingManager.getInstance().addRecipe(new ItemStack(itemPaintBrush, 1, i),
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