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

package com.elytradev.teckle.common.item;

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.repack.concrete.resgen.EnumResourceType;
import com.elytradev.teckle.repack.concrete.resgen.IResourceHolder;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * Created by darkevilmac on 5/23/17.
 */
public class ItemIngot extends Item implements IResourceHolder {

    public ItemIngot() {

    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + ItemIngot.IngotType.byMetadata(stack.getMetadata()).getName();
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, NonNullList<ItemStack> subItems) {
        for (ItemIngot.IngotType ingotType : ItemIngot.IngotType.values()) {
            subItems.add(new ItemStack(itemIn, 1, ingotType.getMetadata()));
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
        ResourceLocation result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "items/siliconwafer_missingno");
        ItemIngot.IngotType type = ItemIngot.IngotType.byMetadata(meta);
        if (type != null) {
            result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "items/ingot_" + type.getName());
        }

        return result;
    }


    public enum IngotType implements IStringSerializable {
        BRASS(0, "brass", "ingotBrass"),
        RED_ALLOY(1, "redalloy", "ingotRedAlloy"),
        BLUE_ALLOY(2, "bluealloy", "ingotBlueAlloy");

        private static final ItemIngot.IngotType[] META_LOOKUP = new ItemIngot.IngotType[values().length];

        static {
            for (ItemIngot.IngotType ingotType : values()) {
                META_LOOKUP[ingotType.getMetadata()] = ingotType;
            }
        }

        private final int meta;
        private final String name, oreName;

        IngotType(int metaIn, String nameIn, String oreName) {
            this.meta = metaIn;
            this.name = nameIn;
            this.oreName = oreName;
        }

        public static ItemIngot.IngotType byMetadata(int meta) {
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

        public String getOreName() {
            return oreName;
        }
    }
}