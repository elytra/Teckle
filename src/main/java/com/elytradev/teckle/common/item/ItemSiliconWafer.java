package com.elytradev.teckle.common.item;

import com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

public class ItemSiliconWafer extends Item implements IResourceHolder {

    public ItemSiliconWafer() {
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + WaferType.byMetadata(stack.getMetadata()).getName();
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, NonNullList<ItemStack> subItems) {
        for (WaferType waferType : WaferType.values()) {
            subItems.add(new ItemStack(itemIn, 1, waferType.getMetadata()));
        }
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public ResourceLocation getResource(EnumResourceType resourceType, int meta) {
        ResourceLocation result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "textures/items/siliconwafer_missingno.png");
        WaferType type = WaferType.byMetadata(meta);

        switch (type) {
            case PLAIN:
                result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "textures/items/siliconwafer.png");
                break;
            case RED:
                result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "textures/items/siliconwafer_red.png");
                break;
            case BLUE:
                result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "textures/items/siliconwafer_blue.png");
                break;
        }

        return result;
    }

    public enum WaferType implements IStringSerializable {
        PLAIN(0, "plain", ""),
        RED(1, "red", "_red"),
        BLUE(2, "blue", "_blue");

        private static final ItemSiliconWafer.WaferType[] META_LOOKUP = new ItemSiliconWafer.WaferType[values().length];

        static {
            for (WaferType waferType : values()) {
                META_LOOKUP[waferType.getMetadata()] = waferType;
            }
        }

        private final int meta;
        private final String name;
        private String suffix;

        WaferType(int metaIn, String nameIn, String suffix) {
            this.meta = metaIn;
            this.name = nameIn;
            this.suffix = suffix;
        }

        public static ItemSiliconWafer.WaferType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public int getMetadata() {
            return this.meta;
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return this.name;
        }

        public String getSuffix() {
            return this.suffix;
        }
    }
}
